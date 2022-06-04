package com.github.iguissouma.nxconsole.schematics

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object NxSchematicsJsonParser {
    fun parse(output: String?): List<NxSchematicCollection> {
        val listType = object : TypeToken<ArrayList<NxSchematicCollection?>?>() {}.type
        return GsonBuilder().create().fromJson(output, listType)
    }
}

fun main() {
    val s = """
        [
          {
            "name": "@nrwl/react",
            "schematics": [
              {
                "name": "application",
                "collection": "@nrwl/react",
                "description": "Create an application",
                "options": [
                  {
                    "name": "name",
                    "description": "The name of the application.",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    },
                    "tooltip": "What name would you like to use for the application?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "style",
                    "description": "The file extension to be used for style files.",
                    "type": "string",
                    "default": "css",
                    "required": false,
                    "aliases": [
                      "s"
                    ],
                    "hidden": false,
                    "tooltip": "Which stylesheet format would you like to use?",
                    "itemTooltips": {
                      "css": "CSS",
                      "scss": "SASS(.scss)       [ http://sass-lang.com                     ]",
                      "styl": "Stylus(.styl)     [ http://stylus-lang.com                   ]",
                      "less": "LESS              [ http://lesscss.org                       ]",
                      "styled-components": "styled-components [ https://styled-components.com            ]",
                      "@emotion/styled": "emotion           [ https://emotion.sh                       ]",
                      "styled-jsx": "styled-jsx        [ https://www.npmjs.com/package/styled-jsx ]",
                      "none": "None"
                    },
                    "items": [
                      "css",
                      "scss",
                      "styl",
                      "less",
                      "styled-components",
                      "@emotion/styled",
                      "styled-jsx",
                      "none"
                    ]
                  },
                  {
                    "name": "directory",
                    "description": "The directory of the new application.",
                    "type": "string",
                    "required": false,
                    "aliases": [
                      "d"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "classComponent",
                    "description": "Use class components instead of functional component.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [
                      "C"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "e2eTestRunner",
                    "description": "Test runner to use for end to end (e2e) tests.",
                    "type": "string",
                    "default": "cypress",
                    "enum": [
                      "cypress",
                      "none"
                    ],
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "cypress",
                      "none"
                    ]
                  },
                  {
                    "name": "globalCss",
                    "description": "Default is false. When true, the component is generated with *.css/*.scss instead of *.module.css/*.module.scss",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "js",
                    "description": "Generate JavaScript files rather than TypeScript files.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "linter",
                    "description": "The tool to use for running lint checks.",
                    "type": "string",
                    "default": "eslint",
                    "enum": [
                      "eslint",
                      "tslint"
                    ],
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "eslint",
                      "tslint"
                    ]
                  },
                  {
                    "name": "pascalCaseFiles",
                    "description": "Use pascal case component file name (e.g. App.tsx).",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [
                      "P"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "routing",
                    "description": "Generate application with routes.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "tooltip": "Would you like to add React Router to this application?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "skipFormat",
                    "description": "Skip formatting files.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "skipWorkspaceJson",
                    "description": "Skip updating workspace.json with default options based on values provided to this app (e.g. babel, style).",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "tags",
                    "description": "Add tags to the application (used for linting).",
                    "type": "string",
                    "required": false,
                    "aliases": [
                      "t"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "unitTestRunner",
                    "description": "Test runner to use for unit tests.",
                    "type": "string",
                    "default": "jest",
                    "enum": [
                      "jest",
                      "none"
                    ],
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "jest",
                      "none"
                    ]
                  }
                ]
              },
              {
                "name": "library",
                "collection": "@nrwl/react",
                "description": "Create a library",
                "options": [
                  {
                    "name": "name",
                    "description": "Library name",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    },
                    "tooltip": "What name would you like to use for the library?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "style",
                    "description": "The file extension to be used for style files.",
                    "type": "string",
                    "default": "css",
                    "required": false,
                    "aliases": [
                      "s"
                    ],
                    "hidden": false,
                    "tooltip": "Which stylesheet format would you like to use?",
                    "itemTooltips": {
                      "css": "CSS",
                      "scss": "SASS(.scss)       [ http://sass-lang.com          ]",
                      "styl": "Stylus(.styl)     [ http://stylus-lang.com        ]",
                      "less": "LESS              [ http://lesscss.org            ]",
                      "styled-components": "styled-components [ https://styled-components.com ]",
                      "@emotion/styled": "emotion           [ https://emotion.sh            ]",
                      "styled-jsx": "styled-jsx        [ https://www.npmjs.com/package/styled-jsx ]",
                      "none": "None"
                    },
                    "items": [
                      "css",
                      "scss",
                      "styl",
                      "less",
                      "styled-components",
                      "@emotion/styled",
                      "styled-jsx",
                      "none"
                    ]
                  },
                  {
                    "name": "directory",
                    "description": "A directory where the lib is placed.",
                    "type": "string",
                    "required": false,
                    "aliases": [
                      "d"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "appProject",
                    "description": "The application project to add the library route to.",
                    "type": "string",
                    "required": false,
                    "aliases": [
                      "a"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "buildable",
                    "description": "Generate a buildable library.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "component",
                    "description": "Generate a default component.",
                    "type": "boolean",
                    "default": true,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "globalCss",
                    "description": "When true, the stylesheet is generated using global CSS instead of CSS modules (e.g. file is '*.css' rather than '*.module.css').",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "importPath",
                    "description": "The library name used to import it, like @myorg/my-awesome-lib",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "js",
                    "description": "Generate JavaScript files rather than TypeScript files.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "linter",
                    "description": "The tool to use for running lint checks.",
                    "type": "string",
                    "default": "eslint",
                    "enum": [
                      "eslint",
                      "tslint"
                    ],
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "eslint",
                      "tslint"
                    ]
                  },
                  {
                    "name": "pascalCaseFiles",
                    "description": "Use pascal case component file name (e.g. App.tsx).",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [
                      "P"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "publishable",
                    "description": "Create a publishable library.",
                    "type": "boolean",
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "routing",
                    "description": "Generate library with routes.",
                    "type": "boolean",
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "skipFormat",
                    "description": "Skip formatting files.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "skipTsConfig",
                    "description": "Do not update tsconfig.json for development experience.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "tags",
                    "description": "Add tags to the library (used for linting).",
                    "type": "string",
                    "required": false,
                    "aliases": [
                      "t"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "unitTestRunner",
                    "description": "Test runner to use for unit tests.",
                    "type": "string",
                    "default": "jest",
                    "enum": [
                      "jest",
                      "none"
                    ],
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "jest",
                      "none"
                    ]
                  }
                ]
              },
              {
                "name": "component",
                "collection": "@nrwl/react",
                "description": "Create a component",
                "options": [
                  {
                    "name": "name",
                    "description": "The name of the component.",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    },
                    "tooltip": "What name would you like to use for the component?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "project",
                    "description": "The name of the project.",
                    "type": "string",
                    "required": true,
                    "aliases": [
                      "p"
                    ],
                    "hidden": false,
                    "${'$'}default": {
                      "${'$'}source": "projectName"
                    },
                    "tooltip": "What is the name of the project for this component?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "style",
                    "description": "The file extension to be used for style files.",
                    "type": "string",
                    "default": "css",
                    "required": false,
                    "aliases": [
                      "s"
                    ],
                    "hidden": false,
                    "tooltip": "Which stylesheet format would you like to use?",
                    "itemTooltips": {
                      "css": "CSS",
                      "scss": "SASS(.scss)       [ http://sass-lang.com          ]",
                      "styl": "Stylus(.styl)     [ http://stylus-lang.com        ]",
                      "less": "LESS              [ http://lesscss.org            ]",
                      "styled-components": "styled-components [ https://styled-components.com ]",
                      "@emotion/styled": "emotion           [ https://emotion.sh            ]",
                      "styled-jsx": "styled-jsx        [ https://www.npmjs.com/package/styled-jsx ]",
                      "none": "None"
                    },
                    "items": [
                      "css",
                      "scss",
                      "styl",
                      "less",
                      "styled-components",
                      "@emotion/styled",
                      "styled-jsx",
                      "none"
                    ]
                  },
                  {
                    "name": "directory",
                    "description": "Create the component under this directory (can be nested).",
                    "type": "string",
                    "required": false,
                    "aliases": [
                      "d"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "classComponent",
                    "description": "Use class components instead of functional component.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [
                      "C"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "export",
                    "description": "When true, the component is exported from the project index.ts (if it exists).",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [
                      "e"
                    ],
                    "hidden": false,
                    "tooltip": "Should this component be exported in the project?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "flat",
                    "description": "Create component at the source root rather than its own directory.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "globalCss",
                    "description": "Default is false. When true, the component is generated with *.css/*.scss instead of *.module.css/*.module.scss",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "js",
                    "description": "Generate JavaScript files rather than TypeScript files.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "pascalCaseFiles",
                    "description": "Use pascal case component file name (e.g. App.tsx).",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [
                      "P"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "routing",
                    "description": "Generate a library with routes.",
                    "type": "boolean",
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "skipTests",
                    "description": "When true, does not create \"spec.ts\" test files for the new component.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  }
                ]
              },
              {
                "name": "redux",
                "collection": "@nrwl/react",
                "description": "Create a redux slice for a project",
                "options": [
                  {
                    "name": "name",
                    "description": "Redux slice name.",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    }
                  },
                  {
                    "name": "project",
                    "description": "The name of the project to add the slice to. If it is an application, then the store configuration will be updated too.",
                    "type": "string",
                    "required": false,
                    "aliases": [
                      "p"
                    ],
                    "hidden": false,
                    "${'$'}default": {
                      "${'$'}source": "projectName"
                    },
                    "tooltip": "What is the name of the project for this slice?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "directory",
                    "description": "The name of the folder used to contain/group the generated Redux files.",
                    "type": "string",
                    "default": "",
                    "required": false,
                    "aliases": [
                      "d"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "appProject",
                    "description": "The application project to add the slice to.",
                    "type": "string",
                    "required": false,
                    "aliases": [
                      "a"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "js",
                    "description": "Generate JavaScript files rather than TypeScript files.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  }
                ]
              },
              {
                "name": "storybook-configuration",
                "collection": "@nrwl/react",
                "description": "Set up storybook for a react library",
                "options": [
                  {
                    "name": "name",
                    "description": "Library or application name",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    }
                  },
                  {
                    "name": "configureCypress",
                    "description": "Run the cypress-configure generator.",
                    "type": "boolean",
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "tooltip": "Configure a cypress e2e app to run against the storybook instance?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "generateCypressSpecs",
                    "description": "Automatically generate *.spec.ts files in the cypress e2e app generated by the cypress-configure generator",
                    "type": "boolean",
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "tooltip": "Automatically generate *.spec.ts files in the cypress e2e app generated by the cypress-configure generator?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "generateStories",
                    "description": "Automatically generate *.stories.ts files for components declared in this library.",
                    "type": "boolean",
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "tooltip": "Automatically generate story files for components declared in this library?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "js",
                    "description": "Generate JavaScript files rather than TypeScript files.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "linter",
                    "description": "The tool to use for running lint checks.",
                    "type": "string",
                    "default": "eslint",
                    "enum": [
                      "eslint",
                      "tslint"
                    ],
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "eslint",
                      "tslint"
                    ]
                  }
                ]
              },
              {
                "name": "storybook-migrate-defaults-5-to-6",
                "collection": "@nrwl/react",
                "description": "Generate default Storybook configuration files using Storybook version >=6.x specs, for projects that already have Storybook instances and configurations of versions <6.x.",
                "options": [
                  {
                    "name": "name",
                    "description": "Leave empty to upgrade all Storybook instances. Only use this if you want to do a gradual migration. Library or application name for which you want to generate the new Storybook configuration.",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    }
                  },
                  {
                    "name": "all",
                    "description": "Generate new Storybook configurations for all Storybook instances across all apps and libs.",
                    "type": "boolean",
                    "default": true,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "keepOld",
                    "description": "Keep the old configuration files - put them in a folder called .old_storybook.",
                    "type": "boolean",
                    "default": true,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  }
                ]
              },
              {
                "name": "component-story",
                "collection": "@nrwl/react",
                "description": "Generate storybook story for a react component",
                "options": [
                  {
                    "name": "componentPath",
                    "description": "Relative path to the component file from the library root",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "tooltip": "What's path of the component relative to the project's lib root?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "project",
                    "description": "The project name where to add the components.",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "${'$'}default": {
                      "${'$'}source": "projectName",
                      "index": 0
                    },
                    "tooltip": "What's name of the project where the component lives?",
                    "itemTooltips": {}
                  }
                ]
              },
              {
                "name": "stories",
                "collection": "@nrwl/react",
                "description": "Create stories/specs for all components declared in a library",
                "options": [
                  {
                    "name": "project",
                    "description": "Library or application name",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "${'$'}default": {
                      "${'$'}source": "projectName",
                      "index": 0
                    },
                    "tooltip": "What's name of the project for which you want to generate stories?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "generateCypressSpecs",
                    "description": "Automatically generate *.spec.ts files in the cypress e2e app generated by the cypress-configure generator.",
                    "type": "boolean",
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "tooltip": "Do you want to generate Cypress specs as well?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "js",
                    "description": "Generate JavaScript files rather than TypeScript files.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  }
                ]
              },
              {
                "name": "component-cypress-spec",
                "collection": "@nrwl/react",
                "description": "Create a cypress spec for a ui component that has a story",
                "options": [
                  {
                    "name": "componentPath",
                    "description": "Relative path to the component file from the library root?",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "tooltip": "What's path of the component relative to the project's lib root for which to generate a test?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "project",
                    "description": "The project name for which to generate tests.",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "${'$'}default": {
                      "${'$'}source": "projectName",
                      "index": 0
                    },
                    "tooltip": "What's name of the project for which to generate tests?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "js",
                    "description": "Generate JavaScript files rather than TypeScript files.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  }
                ]
              }
            ]
          },
          {
            "name": "@nrwl/storybook",
            "schematics": [
              {
                "name": "configuration",
                "collection": "@nrwl/storybook",
                "description": "Add storybook configuration to a ui library or an application",
                "options": [
                  {
                    "name": "name",
                    "description": "Library or application name",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    }
                  },
                  {
                    "name": "uiFramework",
                    "description": "Storybook UI Framework to use",
                    "type": "string",
                    "enum": [
                      "@storybook/angular",
                      "@storybook/react"
                    ],
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "@storybook/angular",
                      "@storybook/react"
                    ],
                    "tooltip": "What UI framework plugin should storybook use?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "configureCypress",
                    "description": "Run the cypress-configure generator",
                    "type": "boolean",
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "tooltip": "Configure a cypress e2e app to run against the storybook instance?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "js",
                    "description": "Generate JavaScript files rather than TypeScript files",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "linter",
                    "description": "The tool to use for running lint checks.",
                    "type": "string",
                    "default": "eslint",
                    "enum": [
                      "eslint",
                      "tslint"
                    ],
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "eslint",
                      "tslint"
                    ]
                  }
                ]
              },
              {
                "name": "cypress-project",
                "collection": "@nrwl/storybook",
                "description": "Add cypress e2e app to test a ui library that is set up for storybook",
                "options": [
                  {
                    "name": "name",
                    "description": "Library or application name",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    }
                  },
                  {
                    "name": "js",
                    "description": "Generate JavaScript files rather than TypeScript files",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "linter",
                    "description": "The tool to use for running lint checks.",
                    "type": "string",
                    "default": "eslint",
                    "enum": [
                      "eslint",
                      "tslint"
                    ],
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "eslint",
                      "tslint"
                    ]
                  }
                ]
              },
              {
                "name": "migrate-defaults-5-to-6",
                "collection": "@nrwl/storybook",
                "description": "Generate default Storybook configuration files using Storybook version >=6.x specs, for projects that already have Storybook instances and configurations of versions <6.x.",
                "options": [
                  {
                    "name": "name",
                    "description": "Leave empty to upgrade all Storybook instances. Only use this if you want to do a gradual migration. Library or application name for which you want to generate the new Storybook configuration.",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    }
                  },
                  {
                    "name": "all",
                    "description": "Generate new Storybook configurations for all Storybook instances across all apps and libs.",
                    "type": "boolean",
                    "default": true,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "keepOld",
                    "description": "Keep the old configuration files - put them in a folder called .old_storybook.",
                    "type": "boolean",
                    "default": true,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  }
                ]
              }
            ]
          },
          {
            "name": "@nrwl/web",
            "schematics": [
              {
                "name": "application",
                "collection": "@nrwl/web",
                "description": "Create an application",
                "options": [
                  {
                    "name": "name",
                    "description": "The name of the application.",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    },
                    "tooltip": "What name would you like to use for the application?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "style",
                    "description": "The file extension to be used for style files.",
                    "type": "string",
                    "default": "css",
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "tooltip": "Which stylesheet format would you like to use?",
                    "itemTooltips": {
                      "css": "CSS",
                      "scss": "SASS(.scss)  [ http://sass-lang.com   ]",
                      "styl": "Stylus(.styl)[ http://stylus-lang.com ]",
                      "less": "LESS         [ http://lesscss.org     ]"
                    },
                    "items": [
                      "css",
                      "scss",
                      "styl",
                      "less"
                    ]
                  },
                  {
                    "name": "directory",
                    "description": "The directory of the new application.",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "babelJest",
                    "description": "Use babel instead ts-jest",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "e2eTestRunner",
                    "description": "Test runner to use for end to end (e2e) tests",
                    "type": "string",
                    "default": "cypress",
                    "enum": [
                      "cypress",
                      "none"
                    ],
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "cypress",
                      "none"
                    ]
                  },
                  {
                    "name": "linter",
                    "description": "The tool to use for running lint checks.",
                    "type": "string",
                    "default": "eslint",
                    "enum": [
                      "eslint",
                      "tslint"
                    ],
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "eslint",
                      "tslint"
                    ]
                  },
                  {
                    "name": "skipFormat",
                    "description": "Skip formatting files",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "tags",
                    "description": "Add tags to the application (used for linting)",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "unitTestRunner",
                    "description": "Test runner to use for unit tests",
                    "type": "string",
                    "default": "jest",
                    "enum": [
                      "jest",
                      "none"
                    ],
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "jest",
                      "none"
                    ]
                  }
                ]
              }
            ]
          },
          {
            "name": "@nrwl/workspace",
            "schematics": [
              {
                "name": "move",
                "collection": "@nrwl/workspace",
                "description": "Move an application or library to another folder",
                "options": [
                  {
                    "name": "destination",
                    "description": "The folder to move the project into",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    }
                  },
                  {
                    "name": "projectName",
                    "description": "The name of the project to move",
                    "type": "string",
                    "required": true,
                    "aliases": [
                      "project"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "importPath",
                    "description": "The new import path to use in the tsconfig.base.json",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "skipFormat",
                    "description": "Skip formatting files.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [
                      "skip-format"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "updateImportPath",
                    "description": "Should the generator update the import path to reflect the new location?",
                    "type": "boolean",
                    "default": true,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  }
                ]
              },
              {
                "name": "remove",
                "collection": "@nrwl/workspace",
                "description": "Remove an application or library",
                "options": [
                  {
                    "name": "projectName",
                    "description": "The name of the project to remove",
                    "type": "string",
                    "required": true,
                    "aliases": [
                      "project"
                    ],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    }
                  },
                  {
                    "name": "forceRemove",
                    "description": "When true, forces removal even if the project is still in use.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [
                      "force-remove"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "skipFormat",
                    "description": "Skip formatting files.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [
                      "skip-format"
                    ],
                    "hidden": false
                  }
                ]
              },
              {
                "name": "library",
                "collection": "@nrwl/workspace",
                "description": "Create a library",
                "options": [
                  {
                    "name": "name",
                    "description": "Library name",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    },
                    "tooltip": "What name would you like to use for the library?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "directory",
                    "description": "A directory where the lib is placed",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "babelJest",
                    "description": "Use babel instead ts-jest",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "importPath",
                    "description": "The library name used to import it, like @myorg/my-awesome-lib",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "js",
                    "description": "Generate JavaScript files rather than TypeScript files",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "linter",
                    "description": "The tool to use for running lint checks.",
                    "type": "string",
                    "default": "eslint",
                    "enum": [
                      "eslint",
                      "tslint"
                    ],
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "eslint",
                      "tslint"
                    ]
                  },
                  {
                    "name": "pascalCaseFiles",
                    "description": "Use pascal case file names.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [
                      "P"
                    ],
                    "hidden": false
                  },
                  {
                    "name": "skipBabelrc",
                    "description": "Do not generate .babelrc file. Useful for Node libraries that are not compiled by Babel",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "skipFormat",
                    "description": "Skip formatting files",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "skipTsConfig",
                    "description": "Do not update tsconfig.json for development experience.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "strict",
                    "description": "Whether to enable tsconfig strict mode or not.",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "tags",
                    "description": "Add tags to the library (used for linting)",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "testEnvironment",
                    "description": "The test environment to use if unitTestRunner is set to jest",
                    "type": "string",
                    "default": "jsdom",
                    "enum": [
                      "jsdom",
                      "node"
                    ],
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "jsdom",
                      "node"
                    ]
                  },
                  {
                    "name": "unitTestRunner",
                    "description": "Test runner to use for unit tests",
                    "type": "string",
                    "default": "jest",
                    "enum": [
                      "jest",
                      "none"
                    ],
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "items": [
                      "jest",
                      "none"
                    ]
                  }
                ]
              },
              {
                "name": "workspace-generator",
                "collection": "@nrwl/workspace",
                "description": "Generates a workspace generator",
                "options": [
                  {
                    "name": "name",
                    "description": "Generator name",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    },
                    "tooltip": "What name would you like to use for the workspace generator?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "skipFormat",
                    "description": "Skip formatting files",
                    "type": "boolean",
                    "default": false,
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  }
                ]
              },
              {
                "name": "run-commands",
                "collection": "@nrwl/workspace",
                "description": "Generates a target to run any command in the terminal",
                "options": [
                  {
                    "name": "name",
                    "description": "Target name",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "positional": 0,
                    "${'$'}default": {
                      "${'$'}source": "argv",
                      "index": 0
                    },
                    "tooltip": "What name would you like to use to invoke the command?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "command",
                    "description": "Command to run",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "tooltip": "What command would you like to run?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "project",
                    "description": "Project name",
                    "type": "string",
                    "required": true,
                    "aliases": [],
                    "hidden": false,
                    "tooltip": "What project does the target belong to?",
                    "itemTooltips": {}
                  },
                  {
                    "name": "cwd",
                    "description": "Current working directory of the command",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "envFile",
                    "description": "Env files to be loaded before executing the commands",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  },
                  {
                    "name": "outputs",
                    "description": "Allows you to specify where the build artifacts are stored. This allows Nx Cloud to pick them up correctly, in the case that the build artifacts are placed somewhere other than the top level dist folder.",
                    "type": "string",
                    "required": false,
                    "aliases": [],
                    "hidden": false
                  }
                ]
              }
            ]
          }
        ]
    """.trimIndent()

    println(s)
    val parse = NxSchematicsJsonParser.parse(s)
}
