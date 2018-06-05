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
	"sort"
	"strings"
	"testing"
)

func TestReadChecks(t *testing.T) {
	const checkFile = `
Form1, // used by: java py
FormNotChecked,
// NestedCommentsIgnored, // used by: java
FormNoComma // used by: java
`
	checks, err := readChecks(strings.NewReader(checkFile))
	if err != nil {
		t.Fatal(err)
	}

	wants := map[string][]string{
		"java": {"Form1", "FormNoComma"},
		"py":   {"Form1"},
	}

	for lang, want := range wants {
		if got := checks[lang]; !strSetEq(got, want) {
			t.Errorf("checks[%q] = %q, want %q", lang, got, want)
		}
		delete(wants, lang)
		delete(checks, lang)
	}
	if len(checks) != 0 {
		t.Errorf("found unexpected checks: %q", checks)
	}
}

func TestNotFoundWords(t *testing.T) {
	for _, tst := range []struct {
		text     string
		in, want []string
	}{
		{
			text: "ab",
			in:   []string{"a", "b", "ab"},
			want: []string{"a", "b"},
		},
		{
			text: "foo bar zipzap",
			in:   []string{"foo", "bar", "zip", "zap"},
			want: []string{"zip", "zap"},
		},
		{
			text: "abcabc",
			in:   []string{"abc"},
			want: []string{"abc"},
		},
	} {
		got := notFoundWords(tst.text, tst.in)
		if !strSetEq(tst.want, got) {
			t.Errorf("notFoundWords(%q) = %q, want %q", tst.text, got, tst.want)
		}
	}
}

// strSetEq reports whether a and b contains the same strings.
// a and b might be modified.
func strSetEq(a, b []string) bool {
	if len(a) != len(b) {
		return false
	}
	sort.Strings(a)
	sort.Strings(b)
	for i := 0; i < len(a); i++ {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}
