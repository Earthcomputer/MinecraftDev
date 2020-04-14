<p align="center"><a href="https://minecraftdev.org/"><img src="https://minecraftdev.org/assets/icon.svg" height="120"></img></a></p>

Minecraft Development for IntelliJ
==================================

Info and Documentation
----------------------

This is Earthcomputer's fork of the Minecraft Development plugin for IntelliJ. The main additional features are:
- Fabric project generation

Planned features include:
- Changing existing inspections to support Fabric naming
- Making inspections more customizable, e.g. specifying methods for translations inspections to apply to
- Bug fixes
- Mixin generators
- Further enhancements to mixin support

Visit [https://minecraftdev.org](https://minecraftdev.org) for a little information about the official project.


Installation
------------

1. Go into the plugins menu in IntelliJ (Settings -> Plugins)
1. Click the gear icon on the top, and click "Manage Plugin Repositories"
1. Add a raw GitHub link to the update file in the [updates directory](https://github.com/Earthcomputer/MinecraftDev/tree/dev_new/updates), corresponding to the version of IntelliJ you have. For example, if you have 2019.3, use https://raw.githubusercontent.com/Earthcomputer/MinecraftDev/dev_new/updates/updatePlugins-193.xml
1. MinecraftDev should now show an update if you have the official version installed, which corresponds to Earth's edition of MinecraftDev. If you didn't have the official version installed, installing MinecraftDev will also install Earth's edition.
1. Lately (as of April 2020) some people seem to be having issues getting IDEA to recognize the plugin updates. You can instead install it manually from the zip. You can find a direct download to the zip in the update xml pretty easily (it's pretty obvious where it is).

### Installation instructions for the official project:

This plugin is available on the [JetBrains IntelliJ plugin repository](https://plugins.jetbrains.com/plugin/8327).

Because of this, you can install the plugin through IntelliJ's internal plugin browser. Navigate to
`File -> Settings -> Plugins` and click the `Browse Repositories...` button at the bottom of the window. In the search
box, simply search for `Minecraft`. You can install it from there and restart IntelliJ to activate the plugin.

Building
--------

JDK 8 is required.

Build the plugin with:

`./gradlew build`

The output .zip file for the plugin will be in `build/distributions`.

Test the plugin in IntelliJ with:

`./gradlew runIde`

Code is generated during the build task, to run the generation task without building use:

`./gradlew generate`

This task is necessary to work on the code without errors before the initial build.

To format the code in this project:

`./gradlew format`

This will format using `ktlint` described below in the [style guide](#style-guide) section below.

The [Gradle IntelliJ Plugin](https://github.com/JetBrains/gradle-intellij-plugin)
will handle downloading the IntelliJ dependencies and packaging the
plugin.

Style Guide
-----------

This projects follows the opinionated [`ktlint`](https://ktlint.github.io/) linter and formatter. It uses the
[`ktlint-gradle`](https://github.com/jlleitschuh/ktlint-gradle) plugin to automatically check and format the code in
this repo.

IDE Setup
---------

It's recommended to run the `ktlintApplyToIdea` and `addKtlintFormatGitPreCommitHook` tasks to configure your
IDE with `ktlint` style settings and to automatically format this project's code before committing:

```
./gradlew ktlintApplyToIdea addKtlintFormatGitPreCommitHook
```

Developers
----------

- Project Owner - [**@DemonWav** - Kyle Wood](https://github.com/DemonWav)
- [**@Minecrell**](https://github.com/Minecrell)
- [**@PaleoCrafter** - Marvin Rösch](https://github.com/PaleoCrafter)

#### **Contributors**

- [**@gabizou** - Gabriel Harris-Rouquette](https://github.com/gabizou)
- [**@kashike**](https://github.com/kashike)
- [**@jamierocks** - Jamie Mansfield](https://github.com/jamierocks)
- [**@RedNesto**](https://github.com/RedNesto)

License
-------

This project is licensed under [MIT](license.txt).

Supported Platforms
-------------------

- [![Bukkit Icon](src/main/resources/assets/icons/platform/Bukkit.png?raw=true) **Bukkit**](https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse) ([![Spigot Icon](src/main/resources/assets/icons/platform/Spigot.png?raw=true) Spigot](https://spigotmc.org/) and [![Paper Icon](src/main/resources/assets/icons/platform/Paper.png?raw=true) Paper](https://papermc.io/))
- [![Sponge Icon](src/main/resources/assets/icons/platform/Sponge_dark.png?raw=true) **Sponge**](https://www.spongepowered.org/)
- [![Forge Icon](src/main/resources/assets/icons/platform/Forge.png?raw=true) **Minecraft Forge**](http://minecraftforge.net/forum)
- [![LiteLoader Icon](src/main/resources/assets/icons/platform/LiteLoader.png?raw=true) **LiteLoader**](http://www.liteloader.com/)
- [![MCP Icon](src/main/resources/assets/icons/platform/MCP.png?raw=true) **MCP**](http://www.modcoderpack.com/)
- [![Mixins Icon](src/main/resources/assets/icons/platform/Mixins_dark.png?raw=true) **Mixins**](https://github.com/SpongePowered/Mixin)
- [![BungeeCord Icon](src/main/resources/assets/icons/platform/BungeeCord.png?raw=true) **BungeeCord**](https://www.spigotmc.org/wiki/bungeecord/) ([![Waterfall Icon](src/main/resources/assets/icons/platform/Waterfall.png?raw=true) Waterfall](https://github.com/WaterfallMC))
- [![Fabric Icon](src/main/resources/assets/icons/platform/Fabric.png?raw=true) **Fabric**](https://fabricmc.net/)
