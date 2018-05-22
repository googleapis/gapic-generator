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

// +build ignore

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

const usage = `Reads calling forms from gapic_yaml_file and make sure each is used by
some *.baseline file in dir.
The line specifying the calling form is in format

  callingFormCheck: <id> <language>: <callingForm1> <callingForm2> ...

The string "callingFormCheck" must start the line, preceded only by a possilby empty
run of whitespaces.
`

func main() {
	yamlFname := flag.String("yaml", "", "gapic yaml file")
	flag.Usage = func() {
		binName := os.Args[0]
		out := os.Stderr // Should be flag.CommandLine.Output(), but Travis is too old
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
		return delFoundForms(path, checks)
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
		sort.Sort(checkConfigSlice(errs)) // Not sort.Slice, Travis is too old.
		fmt.Println("not found:", errs)
		os.Exit(1)
	}
}

type checkConfigSlice []checkConfig

func (s checkConfigSlice) Len() int      { return len(s) }
func (s checkConfigSlice) Swap(i, j int) { s[i], s[j] = s[j], s[i] }
func (s checkConfigSlice) Less(i, j int) bool {
	if errs[i].lang != errs[j].lang {
		return errs[i].lang < errs[j].lang
	}
	if errs[i].id != errs[j].id {
		return errs[i].id < errs[j].id
	}
	return errs[i].form < errs[j].form
}

var checkPrefix = "callingFormCheck:"

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

func delFoundForms(fname string, forms map[checkConfig]bool) error {
	totalBytes, err := ioutil.ReadFile(fname)
	if err != nil {
		return err
	}
	totalTxt := string(totalBytes)

	// Split the baseline file into its "logical file"
	lang := filepath.Base(filepath.Dir(fname))
	var files []string
	for {
		const fileHeader = "============== file:"
		p := strings.Index(totalTxt, fileHeader)
		if p < 0 {
			files = append(files, totalTxt)
			break
		}
		files = append(files, totalTxt[:p])

		totalTxt = totalTxt[p:]
		totalTxt = totalTxt[strings.IndexByte(totalTxt, '\n')+1:]
	}

	for _, file := range files {
		cf := callingFormRe.FindStringSubmatch(file)
		if len(cf) == 0 {
			continue
		}

		vs := valueSetRe.FindStringSubmatch(file)
		if len(vs) == 0 {
			continue
		}

		delete(forms, checkConfig{
			id:   vs[1],
			form: cf[1],
			lang: lang,
		})
	}

	return nil
}
