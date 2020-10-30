# Nx Console Idea Plugin

![Build](https://github.com/iguissouma/nx-console-idea-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/com.github.iguissouma.nxconsole.svg)](https://plugins.jetbrains.com/plugin/15101-nx-console-idea)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/com.github.iguissouma.nxconsole.svg)](https://plugins.jetbrains.com/plugin/15101-nx-console-idea)

<!-- Plugin description -->

This plugin will add support for projects using [Nx](http://nx.dev/) dev tools. 

<p>Donate(<a href="https://www.paypal.com/donate?hosted_button_id=A2YAJPJ9UBKZQ">PayPal</a>) </p>
<br>

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
