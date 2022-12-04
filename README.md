# Ruff PyCharm Plugin
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

[//]: # ([![]&#40;https://img.shields.io/jetbrains/plugin/v/20574&#41;]&#40;https://plugins.jetbrains.com/plugin/20574-ruff&#41;)
[A JetBrains PyCharm plugin](https://plugins.jetbrains.com/plugin/20574-ruff) for [`ruff`](https://github.com/charliermarsh/ruff).

## Help
See [documentation](https://koxudaxi.github.io/ruff-pycharm-plugin/) for more details.

## Sponsors
[![JetBrains](https://avatars.githubusercontent.com/u/60931315?s=200&v=4)](https://github.com/JetBrainsOfficial)

## ScreenShots
![action](https://raw.githubusercontent.com/koxudaxi/ruff-pycharm-plugin/main/docs/action.png)
![settings](https://raw.githubusercontent.com/koxudaxi/ruff-pycharm-plugin/main/docs/settings.png)

<!-- Plugin description -->
## Features
- [x] Run `ruff --fix` as an action
- [x] Integrate `Reformat Code`
- [x] Run `ruff --fix` for a file when the file is saved

## Core
- [x] detect global `ruff` and project `ruff`
- [x] execute `ruff` command as a new process
- [ ] call `ruff` via LSP


<!-- Plugin description end -->

## Quick Installation
### MarketPlace
I'm waiting for approval from Jetbrains.

~~The plugin is in Jetbrains repository ([Ruff Plugin Page](https://plugins.jetbrains.com/plugin/20574-ruff))~~

~~You can install the stable version on PyCharm's `Marketplace` (Preference -> Plugins -> Marketplace) [Official Documentation](https://www.jetbrains.com/help/idea/managing-plugins.html)~~

[//]: # (![search plugin]&#40;search_plugin.png&#41;)

### Complied binary
The ['Releases'](https://github.com/koxudaxi/ruff-pycharm-plugin/releases/) section of this repository contains a compiled version of the plugin: [Ruff-${version}.zip](https://github.com/koxudaxi/ruff-pycharm-plugin/releases/latest/)

After downloading this file, you can install the plugin from disk by following [the JetBrains instructions here](https://www.jetbrains.com/help/pycharm/plugins-settings.html).

### Source
Alternatively, you can clone this repository and follow the instructions under the "Building the plugin" heading below to build from source.
The build process will create the file `build/distributions/Ruff-${version}.zip`.
This file can be installed as a PyCharm plugin from disk following the same instructions.


## Contribute
We are waiting for your contributions to `ruff-pycharm-plugin`.


## Links
### JetBrains Plugin Page

### Other PyCharm plugin projects
[Pydantic PyCharm Plugin](https://github.com/koxudaxi/pydantic-pycharm-plugin/)

