# Nx Console Idea Plugin

![Build](https://github.com/iguissouma/nx-console-idea-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/com.github.iguissouma.nxconsole.svg)](https://plugins.jetbrains.com/plugin/15101-nx-console-idea)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/com.github.iguissouma.nxconsole.svg)](https://plugins.jetbrains.com/plugin/15101-nx-console-idea)

## Template ToDo list
- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [ ] Verify the [pluginGroup](/gradle.properties), [plugin ID](/src/main/resources/META-INF/plugin.xml) and [sources package](/src/main/kotlin).
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html).
- [ ] [Publish a plugin manually](https://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/publishing_plugin.html) for the first time.
- [ ] Set the Plugin ID in the above README badges.
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.

<!-- Plugin description -->
This plugin will add support for projects using [Nx](http://nx.dev/) dev tools. 
- Open `nx.json` file
- Right button and choose `Show Nx Tasks`
- A tool window will appear with the available tasks, you can double-click to run the Nx task.

- You can also use nx run tasks from the `RunAnything` command, just type `nx run <task>` you'll get the list of available tasks, choose one to execute.

- You can also use nx generate  from the `RunAnything` command, just type `nx generate <schematic>` you'll get the list of available schematic, choose one and add options then to execute.
- If you want to use the nx generate with the UI Form, just append `--ui` to your command 

<!-- Plugin description end -->

## Installation

- Using IDE built-in plugin system:
  
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "nx-console-idea-plugin"</kbd> >
  <kbd>Install Plugin</kbd>
  
- Manually:

  Download the [latest release](https://github.com/iguissouma/nx-console-idea-plugin/releases/latest) and install it manually using
  <kbd>Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
