# Changelog

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
