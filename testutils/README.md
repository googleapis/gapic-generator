Baseline Checks
===============
Scripts in this directory sanity-check the `*.baseline` files.

check-baselines.sh
------------------
Checks that string `$unhandledCallingForm` does not appear in baseline files,
since it is used as a placeholder for unimplemented generator features.

From the root of the repository, run `./testutils/check-baselines.sh`

check-calling-form-in-baseline
------------------------------
Checks that calling forms and value sets tagged in `*_gapic.yaml` files is used in baselines.

To test the test, run :

  go test testutils/check-calling-form-in-baseline/*.go

from the root of the repository.

To run the test, run 

  go run testutils/check-calling-form-in-baseline/main.go -yaml src/test/java/com/google/api/codegen/testsrc/library_gapic.yaml src
  
from the root of the repository.

check-lang-uses-form
--------------------
Checks that calling forms defined in `CallingForm.java` are used by the specified languages.
