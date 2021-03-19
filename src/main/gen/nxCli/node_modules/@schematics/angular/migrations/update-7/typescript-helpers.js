"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.typeScriptHelpersRule = void 0;
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
const schematics_1 = require("@angular-devkit/schematics");
const tasks_1 = require("@angular-devkit/schematics/tasks");
const dependencies_1 = require("../../utility/dependencies");
const json_file_1 = require("../../utility/json-file");
const latest_versions_1 = require("../../utility/latest-versions");
function typeScriptHelpersRule() {
    return schematics_1.chain([
        _updateTsConfig(),
        (tree, context) => {
            const existing = dependencies_1.getPackageJsonDependency(tree, 'tslib');
            const type = existing ? existing.type : dependencies_1.NodeDependencyType.Default;
            dependencies_1.addPackageJsonDependency(tree, {
                type,
                name: 'tslib',
                version: latest_versions_1.latestVersions.TsLib,
                overwrite: true,
            });
            context.addTask(new tasks_1.NodePackageInstallTask());
        },
    ]);
}
exports.typeScriptHelpersRule = typeScriptHelpersRule;
function _updateTsConfig() {
    return (host) => {
        const tsConfigPath = '/tsconfig.json';
        let tsConfigJson;
        try {
            tsConfigJson = new json_file_1.JSONFile(host, tsConfigPath);
        }
        catch (_a) {
            return;
        }
        const compilerOptions = tsConfigJson.get(['compilerOptions']);
        if (!compilerOptions || typeof compilerOptions !== 'object') {
            return;
        }
        const importHelpersPath = ['compilerOptions', 'importHelpers'];
        const importHelpers = tsConfigJson.get(importHelpersPath);
        if (importHelpers === true) {
            return;
        }
        tsConfigJson.modify(importHelpersPath, true);
    };
}
