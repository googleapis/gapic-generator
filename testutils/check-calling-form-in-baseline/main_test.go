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
	"strings"
	"testing"
)

func TestReadChecks(t *testing.T) {
	const checkFile = `
callingFormCheck: set1 java: foo bar
 callingFormCheck: set2 go: zip zap
# callingFormCheck: ignored because: not at line start
unrelated line
`
	checks, err := readChecks(strings.NewReader(checkFile))
	if err != nil {
		t.Fatal(err)
	}

	for _, exp := range []checkConfig{
		{
			id:   "set1",
			lang: "java",
			form: "foo",
		},
		{
			id:   "set1",
			lang: "java",
			form: "bar",
		},
		{
			id:   "set2",
			lang: "go",
			form: "zip",
		},
		{
			id:   "set2",
			lang: "go",
			form: "zap",
		},
	} {
		if !checks[exp] {
			t.Errorf("expected %v, not found", exp)
		}
		delete(checks, exp)
	}
	if len(checks) != 0 {
		t.Errorf("unexpected checks: %v", checks)
	}
}

func TestDeleteFoundForms(t *testing.T) {
	const lang = "mylang"
	const baselineFile = `
============== file: foo ==============
// calling form: "form1"
// valueSet "set1"
============== file: bar ==============
// calling form: "badform"
============== file: bar ==============
// valueSet "badset"
============== file: x ==============
// Deleting a config not present in the set is a no-op
// calling form: "formNotFound"
// valueSet "setNotFound"
============== file: x ==============
// Deleting partial matches are no-ops
// calling form: "form1"
// valueSet "set2"
============== file: x ==============
// Deleting partial matches are no-ops
// calling form: "form2"
// valueSet "set1"`
	tests := []struct {
		conf       checkConfig
		inBaseline bool
	}{
		{
			conf: checkConfig{
				id:   "set1",
				form: "form1",
				lang: lang,
			},
			inBaseline: true,
		},
		{
			conf: checkConfig{
				id:   "badset",
				form: "badform",
				lang: lang,
			},
			inBaseline: false,
		},
		{
			conf: checkConfig{
				id:   "form2",
				form: "set2",
				lang: lang,
			},
			inBaseline: false,
		},
	}

	checks := map[checkConfig]bool{}
	for _, ts := range tests {
		checks[ts.conf] = true
	}

	deleteFoundForms(baselineFile, lang+"wrong", checks)
	for _, ts := range tests {
		if !checks[ts.conf] {
			t.Errorf("deleteFoundForms in a different language should be no-op; expected %v but not found", ts.conf)
		}
	}

	deleteFoundForms(baselineFile, lang, checks)
	for _, ts := range tests {
		if ts.inBaseline && checks[ts.conf] {
			t.Errorf("config is in baseline but not deleted: %v", ts.conf)
		} else if !ts.inBaseline && !checks[ts.conf] {
			t.Errorf("config is not in baseline but deleted: %v", ts.conf)
		}
	}
}
