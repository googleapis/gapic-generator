Baseline Checks
===============
Scripts in this directory sanity-checks the `*.baseline` files.

check-baselines.sh
------------------
Checks that string `$unhandledCallingForm` does not appear in baseline files,
since it is used as a placeholder for unimplemented generator features.

Checks that every calling form defined in `CallingForm.java` is used by at least
one baseline.

From the root of the repository, run `./testutils/check-baselines.sh`

check-calling-form-in-baseline
------------------------------
Checks that calling forms and value sets defined in GAPIC yaml file is used in baseline.

To test the test, run `go test testutils/check-calling-form-in-baseline/*.go` from the root of the repository.

To run the test, run `go run testutils/check-calling-form-in-baseline/main.go -yaml src/test/java/com/google/api/codegen/testsrc/library_gapic.yaml src`
from the root of the repository.
