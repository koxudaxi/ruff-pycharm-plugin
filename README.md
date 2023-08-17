# Ruff PyCharm Plugin
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![JetBrains IntelliJ Plugins](https://img.shields.io/jetbrains/plugin/v/20574)](https://plugins.jetbrains.com/plugin/20574-ruff)
[![Rating](https://img.shields.io/jetbrains/plugin/r/rating/20574-ruff)](https://plugins.jetbrains.com/plugin/20574-ruff)
[![Total downloads](https://img.shields.io/jetbrains/plugin/d/20574-ruff)](https://plugins.jetbrains.com/plugin/20574-ruff)

A [`ruff`](https://github.com/charliermarsh/ruff) integration [plugin](https://plugins.jetbrains.com/plugin/20574-ruff) for [JetBrains PyCharm](https://www.jetbrains.com/pycharm/).

See [documentation](https://koxudaxi.github.io/ruff-pycharm-plugin/) for more details.

<!-- Plugin description -->
## Features
- [x] Inspection and highlighting
- [x] Integrating `Reformat Code` with `⌥⇧ ⌘ L` or `Ctrl+Alt+L`
- [x] Quick Fix (from mouse-over, `⌥⏎` or `Alt+Enter`)
  - [x] Show fix message
  - [x] Suppressing warnings with `# noqa:`
- [x] Show code explanation tooltips when hovering `# noqa: <code>`
- [x] Run `ruff --fix` as an action
- [x] Run `ruff --fix` for a file when the file is saved
- [x] Detect both global and project-specific instances of `ruff`
  - [x] Always use global `ruff` command
  - [x] Custom global `ruff` executable path
  - [x] Detect a project ruff after packages refreshed
- [x] Execute `ruff` command as a new process
- [x] Support `ruff` config file path as an option
- [x] Detect `ruff` executalbe in Conda environment
- [x] Detect `ruff` executalbe in WSL
- [x] Support `ruff-lsp` with [LSP integration](https://blog.jetbrains.com/platform/2023/07/lsp-for-plugin-developers/) for PyCharm Pro/IDEA Ultimate [Experimental] 

### Support `ruff-lsp` for only PyCharm Pro/IDEA Ultimate
You can enable it in `Preferences/Settings` -> `Tools` -> `Ruff` -> `Use ruff-lsp (Experimental) for PyCharm Pro/IDEA Ultimate`

The lsp integration applies only below features:
- Errors/warnings highlighting ([textDocument/publishDiagnostics](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_publishDiagnostics))
- Quick-fixes for these errors/warnings ([textDocument/codeAction](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_codeAction)

_This is experimental feature._

## Screenshots

![inspection](https://raw.githubusercontent.com/koxudaxi/ruff-pycharm-plugin/main/docs/inspection.png)
![quickfix](https://raw.githubusercontent.com/koxudaxi/ruff-pycharm-plugin/main/docs/quickfix.png)
![settings](https://raw.githubusercontent.com/koxudaxi/ruff-pycharm-plugin/main/docs/settings.png)

<!-- Plugin description end -->

## Installation

### JetBrains Marketplace

You can install the stable version on PyCharm's `Marketplace` (_Preferences_ -> _Plugins_ -> _Marketplace_).

![search plugin](https://raw.githubusercontent.com/koxudaxi/ruff-pycharm-plugin/main/docs/search_plugin.png)

See the section on [managing plugins](https://www.jetbrains.com/help/idea/managing-plugins.html) in the official documentation.

## Contribute

See [Development](https://koxudaxi.github.io/ruff-pycharm-plugin/development/) section of the documentation.

We are waiting for your contributions to `ruff-pycharm-plugin`!


## Links
* [JetBrains Plugin Page](https://plugins.jetbrains.com/plugin/20574-ruff)
* [Pydantic PyCharm Plugin](https://github.com/koxudaxi/pydantic-pycharm-plugin/)

## Sponsors
[![JetBrains](https://avatars.githubusercontent.com/u/60931315?s=100&v=4)](https://github.com/JetBrainsOfficial)
