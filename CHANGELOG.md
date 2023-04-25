# Changelog

## [Unreleased]

## [0.0.12] - 2023-04-19
- Support ruff 0.0.260 [[#144](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/144)]
- Fix multi-edit fixes [[#145](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/145)]

## [0.0.11] - 2023-04-19
- Fix unicode corruption on Windows [[#137](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/137)]

## [0.0.10] - 2023-03-13
- Fix invalid text range error [[#116](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/116)]
- Fix Cannot create listener error [[#117](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/117)]
- Enable showRuleCode as default [[#118](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/118)]

## [0.0.9] - 2023-02-20
- Detect project ruff after packages refreshed [[#96](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/96)]
- Refactor unnecessary variables [[#97](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/97)]
- Fix string index out of bounds exception [[#98](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/98)]
- Add file path args when a save action [[#100](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/100)]
- Improve ruff action [[#101](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/101)]

## [0.0.8] - 2023-02-17
- Support conda system path  [[#84](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/84)]
- Add global ruff path to settings [[#86](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/86)]
- Fix misspelled displayName in plugin.xml [[#88](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/88)]
- Support ruff config path [[#89](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/89)]
- Fix unexpected io error [[#90](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/90)]
- Fix unexpected decode error  [[#92](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/92)]
- Improve error handling [[#94](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/94)]

## [0.0.7] - 2023-02-09
- Support Windows [[#73](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/73)]
- Support ruff command in user site  [[#77](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/77)]
- Support PyCharm 231 [[#78](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/78)]

## [0.0.6] - 2023-01-22
- Support file path when run ruff command [[#59](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/59)]
- Support showing rule code in inspection message [[#60](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/60)]

## [0.0.5] - 2023-01-17
- Fix system ruff detection [[#53](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/53)]

## [0.0.4] - 2023-01-05
- Support fix message [[#43](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/43)]
- Fix element detection [[#44](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/44)]

## [0.0.3] - 2022-12-21
- Add ruff inspection [[#28](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/28)]
- Improve ruff operation [[#29](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/29)]

## [0.0.2] - 2022-12-04
- Fix undo problem [[#16](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/16)]
- Improve python file detection [[#17](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/17)]

## [0.0.1] - 2022-12-04
- Add fix action [[#1](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/1)]
- Refresh virtual file after run ruff [[#7](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/7)]
- Run `ruff --fix` for a file when the file is saved [[#8](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/8)]
- Support Reformat Code [[#9](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/9)]
- skip publish plugin [[#10](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/10)]
- Add build local step [[#11](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/11)]
- Add --exit-zero option to argument [[#12](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/12)]
- Fix textRange logic [[#13](https://github.com/koxudaxi/ruff-pycharm-plugin/pull/13)]

[Unreleased]: https://github.com/koxudaxi/ruff-pycharm-plugin/compare/v0.0.12...HEAD
[0.0.12]: https://github.com/koxudaxi/ruff-pycharm-plugin/compare/v0.0.11...v0.0.12
[0.0.11]: https://github.com/koxudaxi/ruff-pycharm-plugin/compare/v0.0.10...v0.0.11
[0.0.10]: https://github.com/koxudaxi/ruff-pycharm-plugin/compare/v0.0.9...v0.0.10
[0.0.9]: https://github.com/koxudaxi/ruff-pycharm-plugin/compare/v0.0.8...v0.0.9
[0.0.8]: https://github.com/koxudaxi/ruff-pycharm-plugin/compare/v0.0.7...v0.0.8
[0.0.7]: https://github.com/koxudaxi/ruff-pycharm-plugin/compare/v0.0.6...v0.0.7
[0.0.6]: https://github.com/koxudaxi/ruff-pycharm-plugin/compare/v0.0.5...v0.0.6
[0.0.5]: https://github.com/koxudaxi/ruff-pycharm-plugin/compare/v0.0.4...v0.0.5
[0.0.4]: https://github.com/koxudaxi/ruff-pycharm-plugin/compare/v0.0.3...v0.0.4
[0.0.3]: https://github.com/koxudaxi/ruff-pycharm-plugin/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/koxudaxi/ruff-pycharm-plugin/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/koxudaxi/ruff-pycharm-plugin/commits/v0.0.1
