# Changelog

### [2.10.2](https://www.github.com/googleapis/gapic-generator/compare/v2.10.1...v2.10.2) (2021-01-08)


### Bug Fixes

* preserve the value of opt_args parameter ([#3319](https://www.github.com/googleapis/gapic-generator/issues/3319)) ([60a2d2c](https://www.github.com/googleapis/gapic-generator/commit/60a2d2c3925e131dbba56f667111645820a3af5e))

### [2.10.1](https://www.github.com/googleapis/gapic-generator/compare/v2.10.0...v2.10.1) (2020-12-30)


### Reverts

* Revert "fix: don't change PHP functions which are reserved words  (#3317)" (#3322) ([9c22369](https://www.github.com/googleapis/gapic-generator/commit/9c2236970ad655c070281bea691aade3e3a7084e)), closes [#3317](https://www.github.com/googleapis/gapic-generator/issues/3317) [#3322](https://www.github.com/googleapis/gapic-generator/issues/3322)

## [2.10.0](https://www.github.com/googleapis/gapic-generator/compare/v2.9.0...v2.10.0) (2020-12-24)


### Features

* add DIREGAPIC support for PHP ([#3305](https://www.github.com/googleapis/gapic-generator/issues/3305)) ([0456289](https://www.github.com/googleapis/gapic-generator/commit/04562898ae3c15199dcc5c2f45670edf3ac58886))
* add page streaming for php ([#3318](https://www.github.com/googleapis/gapic-generator/issues/3318)) ([6de594b](https://www.github.com/googleapis/gapic-generator/commit/6de594b3c4bc95abaeac4f603627a7b686e4a7a6))


### Bug Fixes

* don't change PHP functions which are reserved words  ([#3317](https://www.github.com/googleapis/gapic-generator/issues/3317)) ([3ff8ca9](https://www.github.com/googleapis/gapic-generator/commit/3ff8ca91ff8023549dc8d95cee29277520ef87ce))

## [2.9.0](https://www.github.com/googleapis/gapic-generator/compare/v2.8.0...v2.9.0) (2020-12-08)


### Features

* generate GAPIC metadata file for Node.js by default ([#3313](https://www.github.com/googleapis/gapic-generator/issues/3313)) ([887ddd9](https://www.github.com/googleapis/gapic-generator/commit/887ddd96bbda8bbf1ad5fb1c72f5f62afeea95f9))

## [2.8.0](https://www.github.com/googleapis/gapic-generator/compare/v2.7.0...v2.8.0) (2020-11-13)


### Features

* switch PHP GAPICs to use default scopes ([#3306](https://www.github.com/googleapis/gapic-generator/issues/3306)) ([f6ec598](https://www.github.com/googleapis/gapic-generator/commit/f6ec5980156e48f7609b33493e8c3c7876f6d51a))


### Bug Fixes

* update protobuf to 3.13.0 ([#3307](https://www.github.com/googleapis/gapic-generator/issues/3307)) ([65490f3](https://www.github.com/googleapis/gapic-generator/commit/65490f3f3881071ce4b905707c575a8c55cd762e))

## [2.7.0](https://www.github.com/googleapis/gapic-generator/compare/v2.6.2...v2.7.0) (2020-10-23)


### Features

* Java REGAPIC Pagination implementation ([#3295](https://www.github.com/googleapis/gapic-generator/issues/3295)) ([52a2e70](https://www.github.com/googleapis/gapic-generator/commit/52a2e70494c38b15c6f92a668fb1d1f634f432c3))

### [2.6.2](https://www.github.com/googleapis/gapic-generator/compare/v2.6.1...v2.6.2) (2020-10-19)


### Bug Fixes

* use remote lro Go gapic instead of local ([#3299](https://www.github.com/googleapis/gapic-generator/issues/3299)) ([2fe44f7](https://www.github.com/googleapis/gapic-generator/commit/2fe44f7ce21268a63e65e67818737ed5bacf7d97))

### [2.6.1](https://www.github.com/googleapis/gapic-generator/compare/v2.6.0...v2.6.1) (2020-10-13)


### Bug Fixes

* use separate value generator in test generation ([#3294](https://www.github.com/googleapis/gapic-generator/issues/3294)) ([9f97d83](https://www.github.com/googleapis/gapic-generator/commit/9f97d83cc02596149a58d7e6ef330125aaa7caa2))

## [2.6.0](https://www.github.com/googleapis/gapic-generator/compare/v2.5.0...v2.6.0) (2020-10-12)


### Features

* REST GAPIC (REGAPIC) Support for Java ([#3275](https://www.github.com/googleapis/gapic-generator/issues/3275)) ([782d11a](https://www.github.com/googleapis/gapic-generator/commit/782d11a44761e4329349cfeba4e9d1ed494a984b))

## [2.5.0](https://www.github.com/googleapis/gapic-generator/compare/v2.4.6...v2.5.0) (2020-10-02)


### Features

* update C# to use the micro-generator ([#3285](https://www.github.com/googleapis/gapic-generator/issues/3285)) ([f73bd35](https://www.github.com/googleapis/gapic-generator/commit/f73bd35c63e474e9dfd7c6364ff84d749a870f7e)), closes [#3274](https://www.github.com/googleapis/gapic-generator/issues/3274)

### [2.4.6](https://www.github.com/googleapis/gapic-generator/compare/v2.4.5...v2.4.6) (2020-09-02)


### Bug Fixes

* support reroute_to_grpc_interface with grpc_service_config ([#3272](https://www.github.com/googleapis/gapic-generator/issues/3272)) ([231ad97](https://www.github.com/googleapis/gapic-generator/commit/231ad97ba4a05268efbe2c596e3b433e81819507))

### [2.4.5](https://www.github.com/googleapis/gapic-generator/compare/v2.4.4...v2.4.5) (2020-08-07)


### Bug Fixes

* Make java lowerCamel methos naming match protobuf java stubs output methods naming. ([#3265](https://www.github.com/googleapis/gapic-generator/issues/3265)) ([4d1eef9](https://www.github.com/googleapis/gapic-generator/commit/4d1eef9608a67220a2b08df5d26335790c8fe1e1))

### [2.4.4](https://www.github.com/googleapis/gapic-generator/compare/v2.4.3...v2.4.4) (2020-07-24)


### Bug Fixes

* **deps:** bump protobuf versions for Java artman builds ([#3261](https://www.github.com/googleapis/gapic-generator/issues/3261)) ([8620732](https://www.github.com/googleapis/gapic-generator/commit/8620732e45c50f7d139847e05f10055aa8709c83))
* **deps:** update dependency puppeteer to v5 ([#3247](https://www.github.com/googleapis/gapic-generator/issues/3247)) ([c52f71b](https://www.github.com/googleapis/gapic-generator/commit/c52f71bdd8cc12c2a9a8431ec1f0c42e5f70ef69))

### [2.4.3](https://www.github.com/googleapis/gapic-generator/compare/v2.4.2...v2.4.3) (2020-07-17)


### Bug Fixes

* Fix tar packaging in bazel rules for mac ([#3255](https://www.github.com/googleapis/gapic-generator/issues/3255)) ([216f03f](https://www.github.com/googleapis/gapic-generator/commit/216f03fc6bba591e3607f7cebb4f325910f9e085))

### [2.4.2](https://www.github.com/googleapis/gapic-generator/compare/v2.4.1...v2.4.2) (2020-07-16)


### Bug Fixes

* Fix for java_gapic_assembly_gradle_pkg rule. ([#3253](https://www.github.com/googleapis/gapic-generator/issues/3253)) ([a98a2a0](https://www.github.com/googleapis/gapic-generator/commit/a98a2a0edc62214cf2764a1644c32e631318123f))

### [2.4.1](https://www.github.com/googleapis/gapic-generator/compare/v2.4.0...v2.4.1) (2020-07-14)


### Bug Fixes
* fix errorprone bazel errors (#3250)([f1381f9](https://github.com/googleapis/gapic-generator/commit/f1381f999795a62720462d3ec7095b1d566a3653))
* remove speech library generation from circleci ([#3251](https://www.github.com/googleapis/gapic-generator/issues/3251)) ([965504d](https://www.github.com/googleapis/gapic-generator/commit/965504dfda60c649f37738b2efd022d07ec9d8ce))

## [2.4.0](https://www.github.com/googleapis/gapic-generator/compare/v2.3.0...v2.4.0) (2020-06-26)


### Features

* add opt_file_args to pass the file arguments to the protoc plugin ([#3239](https://www.github.com/googleapis/gapic-generator/issues/3239)) ([cce9967](https://www.github.com/googleapis/gapic-generator/commit/cce996760223cc3980af8da44e8f0c609dba871c))


### Bug Fixes

* **deps:** update dependency mocha to v8 ([#3228](https://www.github.com/googleapis/gapic-generator/issues/3228)) ([550cb95](https://www.github.com/googleapis/gapic-generator/commit/550cb959ba4cec7063ccb9f80aa913943384d7e7))
* **deps:** update dependency puppeteer to v4 ([#3234](https://www.github.com/googleapis/gapic-generator/issues/3234)) ([84fbc59](https://www.github.com/googleapis/gapic-generator/commit/84fbc5901935a5571977cb6b5f1f0c8889817c02))

## [2.3.0](https://www.github.com/googleapis/gapic-generator/compare/v2.2.0...v2.3.0) (2020-06-24)


### Features

* preserve some values when regenerating BUILD.bazel ([#3237](https://www.github.com/googleapis/gapic-generator/issues/3237)) ([ba34bae](https://www.github.com/googleapis/gapic-generator/commit/ba34baefc8ce09ac5e1b1bf454b928e63b144dd9))

## [2.2.0](https://www.github.com/googleapis/gapic-generator/compare/v2.1.0...v2.2.0) (2020-06-12)


### Features

* build_gen all langs use grpc_service_config ([#3226](https://www.github.com/googleapis/gapic-generator/issues/3226)) ([22ce9a7](https://www.github.com/googleapis/gapic-generator/commit/22ce9a7b376f3a51006ed1ef7a1d70527fe3f2ca))

## [2.1.0](https://www.github.com/googleapis/gapic-generator/compare/v2.0.3...v2.1.0) (2020-06-05)


### Features

* add proto3_optional to proto_custom_library ([#3222](https://www.github.com/googleapis/gapic-generator/issues/3222)) ([9f11f73](https://www.github.com/googleapis/gapic-generator/commit/9f11f7341e0300cbc5b846dd2c6847b08c65e2f8))


### Bug Fixes

* update gax-java dependency version ([#3213](https://www.github.com/googleapis/gapic-generator/issues/3213)) ([bdaf640](https://www.github.com/googleapis/gapic-generator/commit/bdaf640a56eff853ad76be908981ac7fbbc3a61b))

### [2.0.3](https://www.github.com/googleapis/gapic-generator/compare/v2.0.2...v2.0.3) (2020-06-02)


### Bug Fixes

* adds missing whitespace ([#3179](https://www.github.com/googleapis/gapic-generator/issues/3179)) ([17fbb3b](https://www.github.com/googleapis/gapic-generator/commit/17fbb3b38a20d13e69879d845a045dc1f8448a3d)), closes [#3178](https://www.github.com/googleapis/gapic-generator/issues/3178)
* build_gen use lro gapic as lro dep ([#3215](https://www.github.com/googleapis/gapic-generator/issues/3215)) ([996bbdd](https://www.github.com/googleapis/gapic-generator/commit/996bbdd2d37f3f83a7f30226101dad9465590691))

### [2.0.2](https://www.github.com/googleapis/gapic-generator/compare/v2.0.1...v2.0.2) (2020-05-29)


### Bug Fixes

* nodejs build_gen uses grpc_service_config ([#3206](https://www.github.com/googleapis/gapic-generator/issues/3206)) ([375959e](https://www.github.com/googleapis/gapic-generator/commit/375959e1e9d8f6bdcaafba1a47a98b96e899e831))
* ignore grpc_service_config unless using gapic_v2 ([#3206](https://www.github.com/googleapis/gapic-generator/issues/3206)) ([375959e](https://www.github.com/googleapis/gapic-generator/commit/375959e1e9d8f6bdcaafba1a47a98b96e899e831))
* build_gen use None instead of "" when grpc_service_config is not present ([#3206](https://www.github.com/googleapis/gapic-generator/issues/3206)) ([375959e](https://www.github.com/googleapis/gapic-generator/commit/375959e1e9d8f6bdcaafba1a47a98b96e899e831))

### [2.0.1](https://www.github.com/googleapis/gapic-generator/compare/v2.0.0...v2.0.1) (2020-05-27)


### Bug Fixes

* grpc_service_config timeout settings ([#3200](https://www.github.com/googleapis/gapic-generator/issues/3200)) ([1a9835a](https://www.github.com/googleapis/gapic-generator/commit/1a9835a9483d705723ed6bfce8133cb88f9e0575))

## [2.0.0](https://www.github.com/googleapis/gapic-generator/compare/v0.0.1...v2.0.0) (2020-05-20)


This is the first actual release of this library, so the change log would've been **too long**.
The following releases will have proper changelogs.

Version number (2.0.0) is supposed to match the version of [artman](https://github.com/googleapis/artman/releases)
(until `artman` is finally deprecated).
