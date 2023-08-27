<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Nx Console Idea Plugin Changelog

## [Unreleased]

## [0.59.0] - 2023-08-27

### Added
- Add deprecation notification

## [0.58.0] - 2023-05-20

### Added
- Add new generator as popup
- Add support  for 2023.2 version

## [0.57.0] - 2023-04-09

### Added
- Add support  for 2023.1 version

## [0.56.0] - 2023-03-01

### Fixed
- Fix need of nx package from run configuration

## [0.55.0] - 2023-02-16

### Fixed
- Fix project names for tasks

## [0.54.0] - 2023-01-27

### Fixed
- Fix missing angular generators #137

## [0.53.0] - 2023-01-19

### Fixed
- Fix move/rename app/lib exception

## [0.52.4] - 2022-12-13

### Fixed
- catch exceptions process canceled and log as info

## [0.52.3] - 2022-12-09

### Fixed
- Fix exceptions at startup caused by process canceled

## [0.52.2] - 2022-12-06

### Fixed
- Fix exceptions at startup caused by action group slow operations

## [0.52.1] - 2022-11-30

### Fixed
- Fix exceptions at startup caused by icons for apps an libs

### Added
- Add support for nx local plugins

## [0.51.0]

### Fixed
- Fix run Nx tasks from nx workspace not a root project but a module of project
- Fix nx project names taken from `project.json` or `package.json`

## [0.50.0]

### Fixed
- Fix nx config for workspaces without workspace.json and project.json files

## [0.49.1]

### Added
- Add support  for 2022.3 EAP version #109

## [0.49.0]

### Added
- Add dependsOn autocompletion in nx.json
- Add cacheable operation autocompletion in nx.json
- Add root directory autocompletion in project.json
- Add buildTarget autocompletion in project.json

## [0.48.2]

### Fixed
- Fix run nx dep graph from toolwindow

## [0.48.1]

### Fixed
- Fix Override schematics #97

## [0.48.0]

### Fixed
- Fix loading generators without workspace.json
- Fix java.lang.ClassCastException #98
- Fix Override schematics #97

## [0.47.2]

### Fixed
- Fix hyphenate arguments for ng workspaces

## [0.47.1]

### Added
- Add support for node remote interpreter for executing nx tasks using nx npm run scritpts (#90)

## [0.46.3]

### Fixed
- Fix load generators when `id` or `$id` is missing in `schema.json`

## [0.46.2]

### Fixed
- Fix persist don't ask again at project level
- Fix NxAddNxToMonoRepoAction declaration in plugin.xml

## [0.46.1]

### Fixed
- Fix fileNames on for fileType extension

## [0.46.0]

### Added
- Add support for add nx to workspaces(Lerna, Yarn)
- Add support for cra to nx
- Add support for ng add @nrwl/angular

## [0.45.0]

### Added
- Add first support for yarn workspace

### Fixed
- Fix workspace level generator

## [0.44.1]

### Fixed
- Fix minor issues for angular projects

## [0.44.0]

### Added
- Add support for angular projects without nx(Run action from Gutter and Schematics generation)
- Add support for 2022.2 EAP

## [0.43.2]

### Added
- Add new way to load generators/builders (kotlin implemenetation)
- Add load default for generators from nx.json

### Fixed
- fix issues when root property is missing

## [0.40.0] - 2022-04-12

## [0.39.1]

## [0.39.0]

## [0.38.0]

## [0.36.0]

## [0.35.3]

## [0.35.2]

## [0.35.1]

## [0.35.0]

## [0.34.0]

## [0.33.0]

## [0.32.1]

## [0.32.0]

## [0.31.0]

## [0.30.0]

## [0.29.1]

## [0.29.0]

## [0.28.0]

## [0.27.0]

## [0.26.0]

## [0.25.0]

### Added
- Add Grouped tasks in Nx tree view
- Add Install plugins as devDependencies by default

### Fixed
- Fix loading generators in New context menu after adding new one

## [0.24.0]

### Fixed
- Fix loading generators in a React project

## [0.23.0] - 2021-03-13

### Added
- Add Nx Load/Unload App & Libs Action

## [0.22.0] - 2021-02-28

### Added
- Add Nx remove Library or App Action

## [0.21.0] - 2021-02-21

### Added
- Add Nx UI tasks

## [0.20.0] - 2021-02-09

### Added
- Remove AngularJS plugin dependency

## [0.19.0] - 2021-02-04

### Added
- Add Nx plugin project generator
- Support build 211.*

## [0.18.0] - 2021-01-26

### Added
- Add environment field in run configuration

## [0.17.0] - 2021-01-13

### Added
- Add debug support for node process when running Nx Targets

## [0.16.0] - 2021-01-11

### Added
- Support for the new Nx v11 `workspace.json` format

## [0.15.0] - 2021-01-10

### Added
- Ability to switch between generators(schematics)
- Assigned shortcut for Run(cmd+enter) Dry Run(shift+enter) from the Generate UI
- Support autocompletion in project and pth fields

## [0.14.0] - 2021-01-01

### Fixed
- Fix run configuration using @nrwl/cli installed locally

## [0.13.0] - 2020-12-13

### Fixed
- Fix Generate.nx file is not updating

## [0.12.0] - 2020-12-07

### Fixed
- Fix configurable settings exceptions

## [0.11.0] - 2020-11-29

### Added
- Support Generate Schematics on non-angular monorepo preset

## [0.10.0] - 2020-11-14

### Added
- Show generate ui from explorer context menu

## [0.9.0] - 2020-11-09

### Added
- Support for `workspace.json` file for non-angular preset

## [0.8.0] - 2020-11-03

### Added
- Arguments field for run configuration
- fix Crash at the startup #3

## [0.7.0] - 2020-10-26

### Added
- Nx Graph Show Affected Action

## [0.6.0] - 2020-10-07

### Added
- Nx scopes Apps&Libs
- Nx format:write before commit
- Nx settings and plugins management

## [0.5.0] - 2020-10-03

### Added
- Move Nx action
- Nx config file type
- Nx icons for app lib directories
- Nx affected by files and changes

## [0.4.0] - 2020-09-30

### Added
- Preserve args and options when switching from commands to ui
- Navigation from Nx dep-grah ui

## [0.3.0] - 2020-09-28

### Added
- Run Nx generate form ui (add --ui to your command)

## [0.2.0] - 2020-09-25

### Added
- Nx workspace generator
- Run Nx generate from Run Anything

## [0.1.0] - 2020-09-20

### Added
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- Show Nx Project tasks as toolWindow
- Run Nx Tasks from Run Anything
- Run Nx Tasks from angular.json file
- dep graph file perspective for nx.json

[Unreleased]: null/compare/v0.59.0...HEAD
[0.59.0]: null/compare/v0.58.0...v0.59.0
[0.58.0]: null/compare/v0.57.0...v0.58.0
[0.57.0]: null/compare/v0.56.0...v0.57.0
[0.56.0]: null/compare/v0.55.0...v0.56.0
[0.55.0]: null/compare/v0.54.0...v0.55.0
[0.54.0]: null/compare/v0.53.0...v0.54.0
[0.53.0]: null/compare/v0.52.4...v0.53.0
[0.52.4]: null/compare/v0.52.3...v0.52.4
[0.52.3]: null/compare/v0.52.2...v0.52.3
[0.52.2]: null/compare/v0.52.1...v0.52.2
[0.52.1]: null/compare/v0.51.0...v0.52.1
[0.51.0]: null/compare/v0.50.0...v0.51.0
[0.50.0]: null/compare/v0.49.1...v0.50.0
[0.49.1]: null/compare/v0.49.0...v0.49.1
[0.49.0]: null/compare/v0.48.2...v0.49.0
[0.48.2]: null/compare/v0.48.1...v0.48.2
[0.48.1]: null/compare/v0.48.0...v0.48.1
[0.48.0]: null/compare/v0.47.2...v0.48.0
[0.47.2]: null/compare/v0.47.1...v0.47.2
[0.47.1]: null/compare/v0.46.3...v0.47.1
[0.46.3]: null/compare/v0.46.2...v0.46.3
[0.46.2]: null/compare/v0.46.1...v0.46.2
[0.46.1]: null/compare/v0.46.0...v0.46.1
[0.46.0]: null/compare/v0.45.0...v0.46.0
[0.45.0]: null/compare/v0.44.1...v0.45.0
[0.44.1]: null/compare/v0.44.0...v0.44.1
[0.44.0]: null/compare/v0.43.2...v0.44.0
[0.43.2]: null/compare/v0.40.0...v0.43.2
[0.40.0]: null/compare/v0.39.1...v0.40.0
[0.39.1]: null/compare/v0.39.0...v0.39.1
[0.39.0]: null/compare/v0.38.0...v0.39.0
[0.38.0]: null/compare/v0.36.0...v0.38.0
[0.36.0]: null/compare/v0.35.3...v0.36.0
[0.35.3]: null/compare/v0.35.2...v0.35.3
[0.35.2]: null/compare/v0.35.1...v0.35.2
[0.35.1]: null/compare/v0.35.0...v0.35.1
[0.35.0]: null/compare/v0.34.0...v0.35.0
[0.34.0]: null/compare/v0.33.0...v0.34.0
[0.33.0]: null/compare/v0.32.1...v0.33.0
[0.32.1]: null/compare/v0.32.0...v0.32.1
[0.32.0]: null/compare/v0.31.0...v0.32.0
[0.31.0]: null/compare/v0.30.0...v0.31.0
[0.30.0]: null/compare/v0.29.1...v0.30.0
[0.29.1]: null/compare/v0.29.0...v0.29.1
[0.29.0]: null/compare/v0.28.0...v0.29.0
[0.28.0]: null/compare/v0.27.0...v0.28.0
[0.27.0]: null/compare/v0.26.0...v0.27.0
[0.26.0]: null/compare/v0.25.0...v0.26.0
[0.25.0]: null/compare/v0.24.0...v0.25.0
[0.24.0]: null/compare/v0.23.0...v0.24.0
[0.23.0]: null/compare/v0.22.0...v0.23.0
[0.22.0]: null/compare/v0.21.0...v0.22.0
[0.21.0]: null/compare/v0.20.0...v0.21.0
[0.20.0]: null/compare/v0.19.0...v0.20.0
[0.19.0]: null/compare/v0.18.0...v0.19.0
[0.18.0]: null/compare/v0.17.0...v0.18.0
[0.17.0]: null/compare/v0.16.0...v0.17.0
[0.16.0]: null/compare/v0.15.0...v0.16.0
[0.15.0]: null/compare/v0.14.0...v0.15.0
[0.14.0]: null/compare/v0.13.0...v0.14.0
[0.13.0]: null/compare/v0.12.0...v0.13.0
[0.12.0]: null/compare/v0.11.0...v0.12.0
[0.11.0]: null/compare/v0.10.0...v0.11.0
[0.10.0]: null/compare/v0.9.0...v0.10.0
[0.9.0]: null/compare/v0.8.0...v0.9.0
[0.8.0]: null/compare/v0.7.0...v0.8.0
[0.7.0]: null/compare/v0.6.0...v0.7.0
[0.6.0]: null/compare/v0.5.0...v0.6.0
[0.5.0]: null/compare/v0.4.0...v0.5.0
[0.4.0]: null/compare/v0.3.0...v0.4.0
[0.3.0]: null/compare/v0.2.0...v0.3.0
[0.2.0]: null/compare/v0.1.0...v0.2.0
[0.1.0]: null/commits/v0.1.0
