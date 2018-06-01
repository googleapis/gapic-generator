// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	"bufio"
	"flag"
	"fmt"
	"io"
	"io/ioutil"
	"log"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strings"
)

const usage = `Reads calling forms from gapic_yaml_file and makes sure each is used by
some *.baseline file in dir.
The line specifying the calling form is in the format:

  callingFormCheck: <valueSetID> <language>: <callingForm1> <callingForm2> ...

The string "callingFormCheck" must start the line, preceded only by a possibly empty
run of whitespaces. To check for multiple languages or value sets, simply write the line
multiple times. Hint: YAML files allow multi-line string literals like

  sometext: >
    callingFormCheck: valud_id java: form1 form2
    callingFormCheck: another_value_id python: formSpam formEgg formHam
`

func main() {
	yamlFname := flag.String("yaml", "", "gapic yaml file")
	flag.Usage = func() {
		binName := os.Args[0]
		out := os.Stderr // Should be flag.CommandLine.Output(), but Go version on Travis is too old.
		fmt.Fprintf(out, "Usage of %s:\n", binName)
		fmt.Fprintf(out, "%s -yaml <gapic_yaml_file> [dir]\n", binName)
		flag.PrintDefaults()
		fmt.Fprint(out, usage)
	}
	flag.Parse()

	if *yamlFname == "" {
		flag.Usage()
		os.Exit(1)
	}
	yamlf, err := os.Open(*yamlFname)
	if err != nil {
		log.Fatal(err)
	}
	defer yamlf.Close()

	checks, err := readChecks(yamlf)
	if err != nil {
		log.Fatal(err)
	}

	walkFn := func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if len(checks) == 0 {
			return filepath.SkipDir
		}
		if !info.Mode().IsRegular() {
			return nil
		}
		if filepath.Ext(path) != ".baseline" {
			return nil
		}
		baseline, err := ioutil.ReadFile(path)
		if err != nil {
			return err
		}
		lang := filepath.Base(filepath.Dir(path))
		deleteFoundForms(string(baseline), lang, checks)
		return nil
	}
	dir := "."
	if flag.NArg() > 0 {
		dir = flag.Arg(0)
	}
	if err := filepath.Walk(dir, walkFn); err != nil {
		log.Fatal(err)
	}

	if len(checks) > 0 {
		var errs []checkConfig
		for c := range checks {
			errs = append(errs, c)
		}
		sort.Sort(checkConfigSlice(errs)) // Not sort.Slice, Go version on Travis is too old.
		fmt.Printf("The following value set/calling form cominations are specified in %q but weren't found in *.baseline files:\n", *yamlFname)
		for _, e := range errs {
			fmt.Println(e)
		}
		os.Exit(1)
	}
}

type checkConfigSlice []checkConfig

func (s checkConfigSlice) Len() int      { return len(s) }
func (s checkConfigSlice) Swap(i, j int) { s[i], s[j] = s[j], s[i] }
func (s checkConfigSlice) Less(i, j int) bool {
	if s[i].lang != s[j].lang {
		return s[i].lang < s[j].lang
	}
	if s[i].id != s[j].id {
		return s[i].id < s[j].id
	}
	return s[i].form < s[j].form
}

var checkPrefix = "callingFormCheck:"

// readChecks reads r for callingFormCheck lines as described in command's usage
// and returns the set of the checkConfigs to be verified in the baselines.
func readChecks(r io.Reader) (map[checkConfig]bool, error) {
	sc := bufio.NewScanner(r)
	checks := map[checkConfig]bool{}
	for sc.Scan() {
		line := sc.Text()
		line = strings.TrimSpace(line)
		if !strings.HasPrefix(line, checkPrefix) {
			continue
		}
		line = line[len(checkPrefix):]

		p := strings.IndexByte(line, ':')
		if p < 0 {
			goto fail
		}
		idLang := strings.Fields(line[:p])
		if len(idLang) != 2 {
			goto fail
		}
		for _, form := range strings.Fields(line[p+1:]) {
			cc := checkConfig{
				id:   idLang[0],
				lang: idLang[1],
				form: form,
			}
			checks[cc] = true
		}
	}
	if err := sc.Err(); err != nil {
		return nil, err
	}
	return checks, nil

fail:
	return nil, fmt.Errorf("malformed line: %q", sc.Text())
}

type checkConfig struct {
	id   string
	lang string
	form string
}

var (
	callingFormRe = regexp.MustCompile(`calling form: "(.*?)"`)
	valueSetRe    = regexp.MustCompile(`valueSet "(.*?)"`)
)

// deleteFoundForms reads the baseline, then deletes any used calling forms and value sets from forms.
func deleteFoundForms(baseline string, lang string, forms map[checkConfig]bool) error {
	// Split the baseline file into its "logical files"
	var files []string
	for {
		const fileHeader = "============== file:"
		p := strings.Index(baseline, fileHeader)
		if p < 0 {
			files = append(files, baseline)
			break
		}
		files = append(files, baseline[:p])

		baseline = baseline[p:]
		baseline = baseline[strings.IndexByte(baseline, '\n')+1:]
	}

	for _, file := range files {
		form := callingFormRe.FindStringSubmatch(file)
		if len(form) == 0 {
			continue
		}

		valSet := valueSetRe.FindStringSubmatch(file)
		if len(valSet) == 0 {
			continue
		}

		delete(forms, checkConfig{
			id:   valSet[1],
			form: form[1],
			lang: lang,
		})
	}

	return nil
}
