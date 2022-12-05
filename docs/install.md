# Installation
## MarketPlace
I'm waiting for approval from Jetbrains.

The plugin is in Jetbrains repository ([Ruff Plugin Page](https://plugins.jetbrains.com/plugin/20574-ruff))

You can install the stable version on PyCharm's `Marketplace` (Preference -> Plugins -> Marketplace) [Official Documentation](https://www.jetbrains.com/help/idea/managing-plugins.html)

![search plugin](https://raw.githubusercontent.com/koxudaxi/ruff-pycharm-plugin/main/docs/search_plugin.png)

## Complied binary
The ['Releases'](https://github.com/koxudaxi/ruff-pycharm-plugin/releases/) section of this repository contains a compiled version of the plugin: [Ruff-${version}.zip](https://github.com/koxudaxi/ruff-pycharm-plugin/releases/latest/)

After downloading this file, you can install the plugin from disk by following [the JetBrains instructions here](https://www.jetbrains.com/help/pycharm/plugins-settings.html).

## Source
Alternatively, you can clone this repository and follow the instructions under the "Building the plugin" heading below to build from source.
The build process will create the file `build/distributions/Ruff-${version}.zip`.
This file can be installed as a PyCharm plugin from disk following the same instructions.
