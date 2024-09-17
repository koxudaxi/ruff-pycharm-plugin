# Ruff PyCharm Plugin
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![JetBrains IntelliJ Plugins](https://img.shields.io/jetbrains/plugin/v/20574)](https://plugins.jetbrains.com/plugin/20574-ruff)
[![Rating](https://img.shields.io/jetbrains/plugin/r/rating/20574-ruff)](https://plugins.jetbrains.com/plugin/20574-ruff)
[![Total downloads](https://img.shields.io/jetbrains/plugin/d/20574-ruff)](https://plugins.jetbrains.com/plugin/20574-ruff)


## Help
See [documentation](https://koxudaxi.github.io/ruff-pycharm-plugin/) for more details.

## Sponsors
<table>
  <tr>
    <td valign="top" align="center">
    <a href="https://github.com/JetBrainsOfficial">
      <img src="https://avatars.githubusercontent.com/u/60931315?s=100&v=4" alt="JetBrains Logo" style="width: 100px;">
      <p>JetBrains</p>
    </a>
    </td>
  <td valign="top" align="center">
    <a href="https://github.com/astral-sh">
      <img src="https://avatars.githubusercontent.com/u/115962839?s=200&v=4" alt="Astral Logo" style="width: 100px;">
      <p>Astral</p>
    </a>
  </td>
  <td valign="top" align="center">
    <a href="https://github.com/DataDog">
      <img src="https://avatars.githubusercontent.com/u/365230?s=200&v=4" alt="Datadog, Inc. Logo" style="width: 100px;">
      <p>Datadog, Inc.</p>
    </a>
  </td>
  </tr>
</table>


## Quick Installation

## ScreenShots
![inspection](https://raw.githubusercontent.com/koxudaxi/ruff-pycharm-plugin/main/docs/inspection.png)
![quickfix](https://raw.githubusercontent.com/koxudaxi/ruff-pycharm-plugin/main/docs/quickfix.png)
![action](https://raw.githubusercontent.com/koxudaxi/ruff-pycharm-plugin/main/docs/action.png)
![settings](https://raw.githubusercontent.com/koxudaxi/ruff-pycharm-plugin/main/docs/settings.png)

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
- [x] Detect `ruff` executable in Conda environment
- [x] Detect `ruff` executable in WSL
- [x] Support Ruff LSP feature
  - LSP Client
    - [x] [Intellij LSP integration](https://blog.jetbrains.com/platform/2023/07/lsp-for-plugin-developers/) for PyCharm Pro/IDEA Ultimate
    - [x] [LSP4IJ by RedHat](https://github.com/redhat-developer/lsp4ij) (Requires installation of the [LSP4IJ plugin](https://plugins.jetbrains.com/plugin/23257-lsp4ij))
  - LSP Server
    - [x] `ruff-lsp` integration
    - [x] `ruff server` integration
  - [x] Live Config Reload: Automatically updates from `pyproject.toml` and `ruff.toml` without restarting
- [x] Support `ruff format` for ruff version `0.0.289` or later [Experimental]

### Support `ruff-lsp` for only PyCharm Pro/IDEA Ultimate
You can enable it in `Preferences/Settings` -> `Tools` -> `Ruff` -> `Use ruff-lsp (Experimental) for PyCharm Pro/IDEA Ultimate`

The lsp integration applies only below features:
- Errors/warnings highlighting ([textDocument/publishDiagnostics](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_publishDiagnostics))
- Quick-fixes for these errors/warnings ([textDocument/codeAction](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_codeAction)

_This is experimental feature._

## Contribute
We are waiting for your contributions to `ruff-pycharm-plugin`.


## Links
### JetBrains Plugin Page

### Other PyCharm plugin projects
[Pydantic PyCharm Plugin](https://github.com/koxudaxi/pydantic-pycharm-plugin/)

