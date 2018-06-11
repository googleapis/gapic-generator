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
	"log"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"unicode"
)

const usage = `Reads calling forms from gapic_yaml_file and makes sure each is used by
some *.baseline file in dir.
The line specifying the calling form is in the format:

  # callingFormCheck: <valueSetID> <language>: <callingForm1> <callingForm2> ...

The string "# callingFormCheck" must start the line, preceded only by a possibly empty
run of whitespaces. To check for multiple languages or value sets, simply write the line
multiple times. The '#' makes the line a YAML comment, so that it can be inserted without
affecting the meaning of the file.

NOTE: It is regretable that we have to write valueSetID twice, once in the YAML "id" field,
and again in the comment. We would like to improve this in the future, but this will require
the additional complexity of parsing the YAML.
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
		lang := filepath.Base(filepath.Dir(path))
		f, err := os.Open(path)
		if err != nil {
			return err
		}
		defer f.Close()

		return deleteFoundForms(f, lang, checks)
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

var checkPrefix = "# callingFormCheck:"

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

// deleteFoundForms reads the baseline, then deletes any used calling forms and value sets from forms.
func deleteFoundForms(r io.Reader, lang string, forms map[checkConfig]bool) error {
	var callingForm, valueSet string

	// del deletes the form specified in callingForm, valueSet, and lang from forms and reset.
	// We call this at the end of every logical file.
	del := func() {
		delete(forms, checkConfig{
			id:   valueSet,
			form: callingForm,
			lang: lang,
		})
		callingForm = ""
		valueSet = ""
	}

	br := bufio.NewReader(r)
	for {
		line, err := br.ReadString('\n')
		if err == io.EOF {
			del()
			return nil
		}
		if err != nil {
			return err
		}

		if strings.HasPrefix(line, "============== file:") {
			del()
			continue
		}

		// The expected lines look like this:
		//   // calling form: "form"
		//   ## valueSet "set" ("title")
		// The comment character differs between languages.
		// Some langs also have colons and others don't. Instead of mandating
		// exact format, just be a little resilient.

		// Trim out commets and whitespaces
		line = strings.TrimFunc(line, func(r rune) bool {
			return unicode.IsSpace(r) || r == '/' || r == '#'
		})
		var set *string
		const (
			formPrefix = "calling form"
			setPrefix  = "valueSet"
		)
		switch {
		case strings.HasPrefix(line, formPrefix):
			set = &callingForm
			line = line[len(formPrefix):]
		case strings.HasPrefix(line, setPrefix):
			set = &valueSet
			line = line[len(setPrefix):]
		default:
			continue
		}

		// If there's a title, get rid of it
		if p := strings.IndexByte(line, '('); p >= 0 {
			line = line[:p]
		}

		// Trim colon and space.
		line = strings.TrimFunc(line, func(r rune) bool {
			return unicode.IsSpace(r) || r == ':'
		})
		if l, err := strconv.Unquote(line); err != nil {
			return fmt.Errorf("%v: %q", err, line)
		} else {
			*set = l
		}
	}
}
