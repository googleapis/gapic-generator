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
# callingFormCheck: set1 java: foo bar
 # callingFormCheck: set2 go: zip zap
callingFormCheck: ignored because no hash
sometext # callingFormCheck: ignored because: not at line start
unrelated line
`
	checks, err := readChecks(strings.NewReader(checkFile))
	if err != nil {
		t.Fatal(err)
	}

	for _, exp := range []checkConfig{
		{
			valueSet: "set1",
			lang:     "java",
			form:     "foo",
		},
		{
			valueSet: "set1",
			lang:     "java",
			form:     "bar",
		},
		{
			valueSet: "set2",
			lang:     "go",
			form:     "zip",
		},
		{
			valueSet: "set2",
			lang:     "go",
			form:     "zap",
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
// valueSet "set1" ("title1")
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
// valueSet "set1"

============== file: foo ==============
// DO NOT EDIT! This is a generated sample ("form10", "set10")
============== file: bar ==============
// DO NOT EDIT! This is a generated sample ("badform10", "set10")
============== file: bar ==============
// DO NOT EDIT! This is a generated sample ("form10", "badset10")
============== file: x ==============
// Deleting a config not present in the set is a no-op
// DO NOT EDIT! This is a generated sample ("formNotFound", "setNotFound")
============== file: x ==============
// Deleting partial matches are no-ops
// DO NOT EDIT! This is a generated sample ("form10", "set20")
============== file: x ==============
// Deleting partial matches are no-ops
// DO NOT EDIT! This is a generated sample ("form20", "set10")
`
	tests := []struct {
		conf        checkConfig
		expectMatch bool
	}{
		{
			conf: checkConfig{
				valueSet: "set1",
				form:     "form1",
				lang:     lang,
			},
			expectMatch: true,
		},
		{
			conf: checkConfig{
				valueSet: "badset",
				form:     "badform",
				lang:     lang,
			},
			expectMatch: false,
		},
		{
			conf: checkConfig{
				valueSet: "form2",
				form:     "set2",
				lang:     lang,
			},
			expectMatch: false,
		},

		{
			conf: checkConfig{
				valueSet: "set10",
				form:     "form10",
				lang:     lang,
			},
			expectMatch: true,
		},
		{
			conf: checkConfig{
				valueSet: "badset10",
				form:     "badform10",
				lang:     lang,
			},
			expectMatch: false,
		},
		{
			conf: checkConfig{
				valueSet: "form20",
				form:     "set20",
				lang:     lang,
			},
			expectMatch: false,
		},
	}

	checks := map[checkConfig]bool{}
	for _, ts := range tests {
		checks[ts.conf] = true
	}

	err := deleteFoundForms(strings.NewReader(baselineFile), lang+"wrong", checks)
	if err != nil {
		t.Fatal(err)
	}
	for _, ts := range tests {
		if !checks[ts.conf] {
			t.Errorf("deleteFoundForms in a different language should be no-op; expected %v but not found", ts.conf)
		}
	}

	err = deleteFoundForms(strings.NewReader(baselineFile), lang, checks)
	if err != nil {
		t.Fatal(err)
	}
	for _, ts := range tests {
		if ts.expectMatch && checks[ts.conf] {
			t.Errorf("config is in baseline but not deleted: %v", ts.conf)
		} else if !ts.expectMatch && !checks[ts.conf] {
			t.Errorf("config is not in baseline but deleted: %v", ts.conf)
		}
	}
}
