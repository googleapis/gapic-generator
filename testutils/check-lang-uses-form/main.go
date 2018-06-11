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
	"strings"
	"unicode/utf8"
)

const usage = `Reads calling forms from the file specified in -forms and makes sure each is used by
the specified languages' *.baseline files in dir.
The line specifying the calling forms is in the format:

  CallingForm[,] // used by: lang1 lang2 ...

The spaces in "// used by" is significant.
If the string "// used by" is itself preceeded by another "//" in the same line,
it is ignored so that it is possible to talk about the comment in the file itself.

Baseline files of the specified languages (in this case "lang1" and "lang2") will be checked
`

func main() {
	formFname := flag.String("forms", "", "file containing calling forms")
	flag.Usage = func() {
		binName := os.Args[0]
		out := os.Stderr // Should be flag.CommandLine.Output(), but Go version on Travis is too old.
		fmt.Fprintf(out, "Usage of %s:\n", binName)
		fmt.Fprintf(out, "%s -forms CallingForm.java [dir]\n", binName)
		flag.PrintDefaults()
		fmt.Fprint(out, usage)
	}
	flag.Parse()

	if *formFname == "" {
		flag.Usage()
		os.Exit(1)
	}

	dir := "."
	if flag.NArg() != 0 {
		dir = flag.Arg(0)
	}

	remain, err := unusedCallingForms(*formFname, dir)
	if err != nil {
		log.Fatal(err)
	}

	if len(remain) != 0 {
		fmt.Println("The following {language, callingForms}s are declared but not used:")
		for lang, forms := range remain {
			for _, form := range forms {
				fmt.Printf("{%q, %q}\n", lang, form)
			}
		}
		os.Exit(1)
	}
}

func unusedCallingForms(formFname, dir string) (map[string][]string, error) {
	formFile, err := os.Open(formFname)
	if err != nil {
		return nil, err
	}
	defer formFile.Close()

	checks, err := readChecks(formFile)
	if err != nil {
		return nil, err
	}

	walkFn := func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if len(checks) == 0 {
			return filepath.SkipDir
		}
		if !info.Mode().IsRegular() || filepath.Ext(path) != ".baseline" {
			return nil
		}

		lang := filepath.Base(filepath.Dir(path))
		formsToCheck := checks[lang]
		if len(formsToCheck) == 0 {
			return nil
		}

		baseline, err := ioutil.ReadFile(path)
		if err != nil {
			return err
		}

		formsToCheck = formsRemaining(string(baseline), formsToCheck)
		if len(formsToCheck) == 0 {
			delete(checks, lang)
		} else {
			checks[lang] = formsToCheck
		}
		return nil
	}

	if err := filepath.Walk(dir, walkFn); err != nil {
		return nil, err
	}
	return checks, nil
}

// readChecks reads in from r calling forms and languages where calling forms are expected
// and returns map[language][]callingForm.
// The format for r is described by the program's usage text.
func readChecks(r io.Reader) (map[string][]string, error) {
	checks := make(map[string][]string)
	br := bufio.NewReader(r)
	for {
		line, err := br.ReadString('\n')
		if err == io.EOF {
			return checks, nil
		}
		if err != nil {
			return nil, err
		}
		p := strings.Index(line, "//")
		const prefix = "// used by:"
		if p < 0 || !strings.HasPrefix(line[p:], prefix) {
			continue
		}
		form := line[:p]
		form = strings.TrimSpace(form)
		form = strings.TrimRight(form, ",")

		for _, lang := range strings.Fields(line[p+len(prefix):]) {
			checks[lang] = append(checks[lang], form)
		}
	}
}

// formsRemaining searches s for words and report words not found in s.
// A word must be delimited by either string boundary or non-word character,
// eg string "ab" does not contain the word "b".
func formsRemaining(s string, words []string) []string {
	var notFound []string
	for _, w := range words {
		if !hasWord(s, w) {
			notFound = append(notFound, w)
		}
	}
	return notFound
}

// hasWord reports wheter s contains word w. See formsRemaining for details.
func hasWord(s, w string) bool {
	p := 0
	for {
		dp := strings.Index(s[p:], w)
		if dp < 0 {
			return false
		}
		isWord := !wordChar(utf8.DecodeLastRuneInString(s[:p+dp]))
		isWord = isWord && !wordChar(utf8.DecodeRuneInString(s[p+dp+len(w):]))
		if isWord {
			return true
		}
		p += dp + len(w)
	}
}

func wordChar(c rune, w int) bool {
	if w == 0 {
		return false
	}
	return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'
}
