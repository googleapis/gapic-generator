# Changelog

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
