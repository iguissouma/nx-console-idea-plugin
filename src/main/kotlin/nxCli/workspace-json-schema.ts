import {ExecutorInfo, getAllExecutors} from './get-all-executors';
import {dirname, join, relative} from 'path';

export function setupSchema(
    workspacePath: string,
    schemaVersion: number,
    clearPackageJsonCache = false
): string {
    const collections = getAllExecutors(workspacePath, clearPackageJsonCache);
    const contents = getWorkspaceJsonSchema(workspacePath, schemaVersion, collections);
    return contents;
}

function getWorkspaceJsonSchema(workspacePath: string, schemaVersion: number, collections: ExecutorInfo[]) {
    if (schemaVersion == 1)
        return createJsonSchema1(workspacePath, collections)
    else if (schemaVersion == 2)
        return createJsonSchema2(workspacePath, collections);
    else Error("unknown version")
}

function createJsonSchema2(workspaceJsonPath: string, collections: ExecutorInfo[]) {
    const basedir = dirname(workspaceJsonPath);
    const dotIdeaDir = join(basedir, '.idea');

    const executorNames = collections
        .map(
            (collection) => `
            "${collection.name}"
            `).join(',');

    const executors = collections
        .map(
            (collection) => `
{
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "executor": {
      "const": "${collection.name}"
    },
    "defaultConfiguration": {
      "type": "string",
      "description": "A default named configuration to use when a target configuration is not provided."
    },
    "options": {
      "$ref": "${relative(dotIdeaDir, collection.path)}"
    },
    "configurations": {
      "type": "object",
      "additionalProperties": {
        "$ref": "${relative(dotIdeaDir, collection.path)}"
      }
    }
  }
}
`
        )
        .join(',');

    return `
{
  "$schema": "http://json-schema.org/draft-07/schema",
  "$id": "https://nx.dev/v2",
  "title": "Nx Workspace Configuration",
  "type": "object",
  "properties": {
    "$schema": {
      "type": "string"
    },
    "version": {
      "$ref": "#/definitions/fileVersion"
    },
    "cli": {
      "$ref": "#/definitions/cliOptions"
    },
    "schematics": {
      "$ref": "#/definitions/schematicOptions"
    },
    "newProjectRoot": {
      "type": "string",
      "description": "Path where new projects will be created."
    },
    "defaultProject": {
      "type": "string",
      "description": "Default project name used in commands."
    },
    "projects": {
      "type": "object",
      "patternProperties": {
        "^(?:@[a-zA-Z0-9_-]+/)?[a-zA-Z0-9_-]+$": {
          "$ref": "#/definitions/project"
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false,
  "required": ["version"],
  "definitions": {
    "cliOptions": {
      "type": "object",
      "properties": {
        "defaultCollection": {
          "description": "The default schematics collection to use.",
          "type": "string"
        },
        "packageManager": {
          "description": "Specify which package manager tool to use.",
          "type": "string",
          "enum": ["npm", "cnpm", "yarn", "pnpm"]
        },
        "warnings": {
          "description": "Control CLI specific console warnings",
          "type": "object",
          "properties": {
            "versionMismatch": {
              "description": "Show a warning when the global version is newer than the local one.",
              "type": "boolean"
            }
          }
        },
        "analytics": {
          "type": ["boolean", "string"],
          "description": "Share anonymous usage data with the Angular Team at Google."
        },
        "analyticsSharing": {
          "type": "object",
          "properties": {
            "tracking": {
              "description": "Analytics sharing info tracking ID.",
              "type": "string",
              "pattern": "^GA-\\\\d+-\\\\d+$"
            },
            "uuid": {
              "description": "Analytics sharing info universally unique identifier.",
              "type": "string"
            }
          }
        }
      },
      "additionalProperties": false
    },
    "schematicOptions": {
      "type": "object",
      "properties": {
        "@schematics/angular:application": {
          "$ref": "../../../../schematics/angular/application/schema.json"
        },
        "@schematics/angular:class": {
          "$ref": "../../../../schematics/angular/class/schema.json"
        },
        "@schematics/angular:component": {
          "$ref": "../../../../schematics/angular/component/schema.json"
        },
        "@schematics/angular:directive": {
          "$ref": "../../../../schematics/angular/directive/schema.json"
        },
        "@schematics/angular:enum": {
          "$ref": "../../../../schematics/angular/enum/schema.json"
        },
        "@schematics/angular:guard": {
          "$ref": "../../../../schematics/angular/guard/schema.json"
        },
        "@schematics/angular:interceptor": {
          "$ref": "../../../../schematics/angular/interceptor/schema.json"
        },
        "@schematics/angular:interface": {
          "$ref": "../../../../schematics/angular/interface/schema.json"
        },
        "@schematics/angular:library": {
          "$ref": "../../../../schematics/angular/library/schema.json"
        },
        "@schematics/angular:pipe": {
          "$ref": "../../../../schematics/angular/pipe/schema.json"
        },
        "@schematics/angular:ng-new": {
          "$ref": "../../../../schematics/angular/ng-new/schema.json"
        },
        "@schematics/angular:resolver": {
          "$ref": "../../../../schematics/angular/resolver/schema.json"
        },
        "@schematics/angular:service": {
          "$ref": "../../../../schematics/angular/service/schema.json"
        },
        "@schematics/angular:web-worker": {
          "$ref": "../../../../schematics/angular/web-worker/schema.json"
        }
      },
      "additionalProperties": {
        "type": "object"
      }
    },
    "fileVersion": {
      "type": "integer",
      "description": "File format version",
      "minimum": 1
    },
    "project": {
      "type": "object",
      "properties": {
        "cli": {
          "$ref": "#/definitions/cliOptions"
        },
        "schematics": {
          "$ref": "#/definitions/schematicOptions"
        },
        "prefix": {
          "type": "string",
          "format": "html-selector",
          "description": "The prefix to apply to generated selectors."
        },
        "root": {
          "type": "string",
          "description": "Root of the project files."
        },
        "i18n": {
          "$ref": "#/definitions/project/definitions/i18n"
        },
        "sourceRoot": {
          "type": "string",
          "description": "The root of the source files, assets and index.html file structure."
        },
        "projectType": {
          "type": "string",
          "description": "Project type.",
          "enum": ["application", "library"]
        },
        "architect": {
          "type": "object",
          "additionalProperties": {
            "$ref": "#/definitions/project/definitions/target"
          }
        },
        "targets": {
          "type": "object",
          "additionalProperties": {
            "$ref": "#/definitions/project/definitions/target"
          }
        }
      },
      "required": ["root", "projectType"],
      "anyOf": [
        {
          "required": ["architect"],
          "not": {
            "required": ["targets"]
          }
        },
        {
          "required": ["targets"],
          "not": {
            "required": ["architect"]
          }
        },
        {
          "not": {
            "required": ["targets", "architect"]
          }
        }
      ],
      "additionalProperties": false,
      "patternProperties": {
        "^[a-z]{1,3}-.*": {}
      },
      "definitions": {
        "i18n": {
          "description": "Project i18n options",
          "type": "object",
          "properties": {
            "sourceLocale": {
              "oneOf": [
                {
                  "type": "string",
                  "description": "Specifies the source locale of the application.",
                  "default": "en-US",
                  "$comment": "IETF BCP 47 language tag (simplified)",
                  "pattern": "^[a-zA-Z]{2,3}(-[a-zA-Z]{4})?(-([a-zA-Z]{2}|[0-9]{3}))?(-[a-zA-Z]{5,8})?(-x(-[a-zA-Z0-9]{1,8})+)?$"
                },
                {
                  "type": "object",
                  "description": "Localization options to use for the source locale",
                  "properties": {
                    "code": {
                      "type": "string",
                      "description": "Specifies the locale code of the source locale",
                      "pattern": "^[a-zA-Z]{2,3}(-[a-zA-Z]{4})?(-([a-zA-Z]{2}|[0-9]{3}))?(-[a-zA-Z]{5,8})?(-x(-[a-zA-Z0-9]{1,8})+)?$"
                    },
                    "baseHref": {
                      "type": "string",
                      "description": "HTML base HREF to use for the locale (defaults to the locale code)"
                    }
                  },
                  "additionalProperties": false
                }
              ]
            },
            "locales": {
              "type": "object",
              "additionalProperties": false,
              "patternProperties": {
                "^[a-zA-Z]{2,3}(-[a-zA-Z]{4})?(-([a-zA-Z]{2}|[0-9]{3}))?(-[a-zA-Z]{5,8})?(-x(-[a-zA-Z0-9]{1,8})+)?$": {
                  "oneOf": [
                    {
                      "type": "string",
                      "description": "Localization file to use for i18n"
                    },
                    {
                      "type": "array",
                      "description": "Localization files to use for i18n",
                      "items": {
                        "type": "string",
                        "uniqueItems": true
                      }
                    },
                    {
                      "type": "object",
                      "description": "Localization options to use for the locale",
                      "properties": {
                        "translation": {
                          "oneOf": [
                            {
                              "type": "string",
                              "description": "Localization file to use for i18n"
                            },
                            {
                              "type": "array",
                              "description": "Localization files to use for i18n",
                              "items": {
                                "type": "string",
                                "uniqueItems": true
                              }
                            }
                          ]
                        },
                        "baseHref": {
                          "type": "string",
                          "description": "HTML base HREF to use for the locale (defaults to the locale code)"
                        }
                      },
                      "additionalProperties": false
                    }
                  ]
                }
              }
            }
          },
          "additionalProperties": false
        },
        "target": {
          "oneOf": [
            {
              "$comment": "Extendable target with custom executor",
              "type": "object",
              "properties": {
                "executor": {
                  "type": "string",
                  "description": "The executor used for this package.",
                  "not": {
                    "enum": [
                      ${executorNames}
                    ]
                  }
                },
                "defaultConfiguration": {
                  "type": "string",
                  "description": "A default named configuration to use when a target configuration is not provided."
                },
                "options": {
                  "type": "object"
                },
                "configurations": {
                  "type": "object",
                  "description": "A map of alternative target options.",
                  "additionalProperties": {
                    "type": "object"
                  }
                }
              },
              "additionalProperties": false,
              "required": ["executor"]
            },
            ${executors}
          ]
        }
      }
    },
    "global": {
      "type": "object",
      "properties": {
        "$schema": {
          "type": "string",
          "format": "uri"
        },
        "version": {
          "$ref": "#/definitions/fileVersion"
        },
        "cli": {
          "$ref": "#/definitions/cliOptions"
        },
        "schematics": {
          "$ref": "#/definitions/schematicOptions"
        }
      },
      "required": ["version"]
    }
  }
}`;
}

function createJsonSchema1(workspaceJsonPath: string, collections: ExecutorInfo[]) {

    const basedir = dirname(workspaceJsonPath);
    const dotIdeaDir = join(basedir, '.idea');

    const builderNames = collections
        .map(
            (collection) => `
            "${collection.name}"
            `).join(',');

    const builders = collections
        .map(
            (collection) => `
{
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "builder": {
      "const": "${collection.name}"
    },
    "defaultConfiguration": {
      "type": "string",
      "description": "A default named configuration to use when a target configuration is not provided."
    },
    "options": {
      "$ref": "${relative(dotIdeaDir, collection.path)}"
    },
    "configurations": {
      "type": "object",
      "additionalProperties": {
        "$ref": "${relative(dotIdeaDir, collection.path)}"
      }
    }
  }
}
`
        )
        .join(',');

    return `
{
  "$schema": "http://json-schema.org/draft-07/schema",
  "$id": "https://nx.dev/v2",
  "title": "Nx Workspace Configuration",
  "type": "object",
  "properties": {
    "$schema": {
      "type": "string"
    },
    "version": {
      "$ref": "#/definitions/fileVersion"
    },
    "cli": {
      "$ref": "#/definitions/cliOptions"
    },
    "schematics": {
      "$ref": "#/definitions/schematicOptions"
    },
    "newProjectRoot": {
      "type": "string",
      "description": "Path where new projects will be created."
    },
    "defaultProject": {
      "type": "string",
      "description": "Default project name used in commands."
    },
    "projects": {
      "type": "object",
      "patternProperties": {
        "^(?:@[a-zA-Z0-9_-]+/)?[a-zA-Z0-9_-]+$": {
          "$ref": "#/definitions/project"
        }
      },
      "additionalProperties": false
    }
  },
  "additionalProperties": false,
  "required": ["version"],
  "definitions": {
    "cliOptions": {
      "type": "object",
      "properties": {
        "defaultCollection": {
          "description": "The default schematics collection to use.",
          "type": "string"
        },
        "packageManager": {
          "description": "Specify which package manager tool to use.",
          "type": "string",
          "enum": ["npm", "cnpm", "yarn", "pnpm"]
        },
        "warnings": {
          "description": "Control CLI specific console warnings",
          "type": "object",
          "properties": {
            "versionMismatch": {
              "description": "Show a warning when the global version is newer than the local one.",
              "type": "boolean"
            }
          }
        },
        "analytics": {
          "type": ["boolean", "string"],
          "description": "Share anonymous usage data with the Angular Team at Google."
        },
        "analyticsSharing": {
          "type": "object",
          "properties": {
            "tracking": {
              "description": "Analytics sharing info tracking ID.",
              "type": "string",
              "pattern": "^GA-\\\\d+-\\\\d+$"
            },
            "uuid": {
              "description": "Analytics sharing info universally unique identifier.",
              "type": "string"
            }
          }
        }
      },
      "additionalProperties": false
    },
    "schematicOptions": {
      "type": "object",
      "properties": {
        "@schematics/angular:application": {
          "$ref": "../../../../schematics/angular/application/schema.json"
        },
        "@schematics/angular:class": {
          "$ref": "../../../../schematics/angular/class/schema.json"
        },
        "@schematics/angular:component": {
          "$ref": "../../../../schematics/angular/component/schema.json"
        },
        "@schematics/angular:directive": {
          "$ref": "../../../../schematics/angular/directive/schema.json"
        },
        "@schematics/angular:enum": {
          "$ref": "../../../../schematics/angular/enum/schema.json"
        },
        "@schematics/angular:guard": {
          "$ref": "../../../../schematics/angular/guard/schema.json"
        },
        "@schematics/angular:interceptor": {
          "$ref": "../../../../schematics/angular/interceptor/schema.json"
        },
        "@schematics/angular:interface": {
          "$ref": "../../../../schematics/angular/interface/schema.json"
        },
        "@schematics/angular:library": {
          "$ref": "../../../../schematics/angular/library/schema.json"
        },
        "@schematics/angular:pipe": {
          "$ref": "../../../../schematics/angular/pipe/schema.json"
        },
        "@schematics/angular:ng-new": {
          "$ref": "../../../../schematics/angular/ng-new/schema.json"
        },
        "@schematics/angular:resolver": {
          "$ref": "../../../../schematics/angular/resolver/schema.json"
        },
        "@schematics/angular:service": {
          "$ref": "../../../../schematics/angular/service/schema.json"
        },
        "@schematics/angular:web-worker": {
          "$ref": "../../../../schematics/angular/web-worker/schema.json"
        }
      },
      "additionalProperties": {
        "type": "object"
      }
    },
    "fileVersion": {
      "type": "integer",
      "description": "File format version",
      "minimum": 1
    },
    "project": {
      "type": "object",
      "properties": {
        "cli": {
          "$ref": "#/definitions/cliOptions"
        },
        "schematics": {
          "$ref": "#/definitions/schematicOptions"
        },
        "prefix": {
          "type": "string",
          "format": "html-selector",
          "description": "The prefix to apply to generated selectors."
        },
        "root": {
          "type": "string",
          "description": "Root of the project files."
        },
        "i18n": {
          "$ref": "#/definitions/project/definitions/i18n"
        },
        "sourceRoot": {
          "type": "string",
          "description": "The root of the source files, assets and index.html file structure."
        },
        "projectType": {
          "type": "string",
          "description": "Project type.",
          "enum": ["application", "library"]
        },
        "architect": {
          "type": "object",
          "additionalProperties": {
            "$ref": "#/definitions/project/definitions/target"
          }
        },
        "targets": {
          "type": "object",
          "additionalProperties": {
            "$ref": "#/definitions/project/definitions/target"
          }
        }
      },
      "required": ["root", "projectType"],
      "anyOf": [
        {
          "required": ["architect"],
          "not": {
            "required": ["targets"]
          }
        },
        {
          "required": ["targets"],
          "not": {
            "required": ["architect"]
          }
        },
        {
          "not": {
            "required": ["targets", "architect"]
          }
        }
      ],
      "additionalProperties": false,
      "patternProperties": {
        "^[a-z]{1,3}-.*": {}
      },
      "definitions": {
        "i18n": {
          "description": "Project i18n options",
          "type": "object",
          "properties": {
            "sourceLocale": {
              "oneOf": [
                {
                  "type": "string",
                  "description": "Specifies the source locale of the application.",
                  "default": "en-US",
                  "$comment": "IETF BCP 47 language tag (simplified)",
                  "pattern": "^[a-zA-Z]{2,3}(-[a-zA-Z]{4})?(-([a-zA-Z]{2}|[0-9]{3}))?(-[a-zA-Z]{5,8})?(-x(-[a-zA-Z0-9]{1,8})+)?$"
                },
                {
                  "type": "object",
                  "description": "Localization options to use for the source locale",
                  "properties": {
                    "code": {
                      "type": "string",
                      "description": "Specifies the locale code of the source locale",
                      "pattern": "^[a-zA-Z]{2,3}(-[a-zA-Z]{4})?(-([a-zA-Z]{2}|[0-9]{3}))?(-[a-zA-Z]{5,8})?(-x(-[a-zA-Z0-9]{1,8})+)?$"
                    },
                    "baseHref": {
                      "type": "string",
                      "description": "HTML base HREF to use for the locale (defaults to the locale code)"
                    }
                  },
                  "additionalProperties": false
                }
              ]
            },
            "locales": {
              "type": "object",
              "additionalProperties": false,
              "patternProperties": {
                "^[a-zA-Z]{2,3}(-[a-zA-Z]{4})?(-([a-zA-Z]{2}|[0-9]{3}))?(-[a-zA-Z]{5,8})?(-x(-[a-zA-Z0-9]{1,8})+)?$": {
                  "oneOf": [
                    {
                      "type": "string",
                      "description": "Localization file to use for i18n"
                    },
                    {
                      "type": "array",
                      "description": "Localization files to use for i18n",
                      "items": {
                        "type": "string",
                        "uniqueItems": true
                      }
                    },
                    {
                      "type": "object",
                      "description": "Localization options to use for the locale",
                      "properties": {
                        "translation": {
                          "oneOf": [
                            {
                              "type": "string",
                              "description": "Localization file to use for i18n"
                            },
                            {
                              "type": "array",
                              "description": "Localization files to use for i18n",
                              "items": {
                                "type": "string",
                                "uniqueItems": true
                              }
                            }
                          ]
                        },
                        "baseHref": {
                          "type": "string",
                          "description": "HTML base HREF to use for the locale (defaults to the locale code)"
                        }
                      },
                      "additionalProperties": false
                    }
                  ]
                }
              }
            }
          },
          "additionalProperties": false
        },
        "target": {
          "oneOf": [
            {
              "$comment": "Extendable target with custom builder",
              "type": "object",
              "properties": {
                "builder": {
                  "type": "string",
                  "description": "The builder used for this package.",
                  "not": {
                    "enum": [
                      ${builderNames}
                    ]
                  }
                },
                "defaultConfiguration": {
                  "type": "string",
                  "description": "A default named configuration to use when a target configuration is not provided."
                },
                "options": {
                  "type": "object"
                },
                "configurations": {
                  "type": "object",
                  "description": "A map of alternative target options.",
                  "additionalProperties": {
                    "type": "object"
                  }
                }
              },
              "additionalProperties": false,
              "required": ["builder"]
            },
            ${builders}
          ]
        }
      }
    },
    "global": {
      "type": "object",
      "properties": {
        "$schema": {
          "type": "string",
          "format": "uri"
        },
        "version": {
          "$ref": "#/definitions/fileVersion"
        },
        "cli": {
          "$ref": "#/definitions/cliOptions"
        },
        "schematics": {
          "$ref": "#/definitions/schematicOptions"
        }
      },
      "required": ["version"]
    }
  }
}`;
}
