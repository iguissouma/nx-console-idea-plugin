"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.updateES5Projects = void 0;
/**
 * @license
 * Copyright Google LLC All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
const core_1 = require("@angular-devkit/core");
const json_file_1 = require("../../utility/json-file");
const workspace_1 = require("../../utility/workspace");
const workspace_models_1 = require("../../utility/workspace-models");
const browserslistContent = `# This file is used by the build system to adjust CSS and JS output to support the specified browsers below.
# For additional information regarding the format and rule options, please see:
# https://github.com/browserslist/browserslist#queries

# You can see what browsers were selected by your queries by running:
#   npx browserslist

> 0.5%
last 2 versions
Firefox ESR
not dead
not IE 9-11 # For IE 9-11 support, remove 'not'.`;
function updateES5Projects() {
    return async (tree) => {
        var _a, _b;
        // update workspace tsconfig
        updateTsConfig(tree, '/tsconfig.json');
        let workspace;
        try {
            workspace = await workspace_1.getWorkspace(tree);
        }
        catch (_c) {
            return;
        }
        for (const [projectName, project] of workspace.projects) {
            if (typeof project.root !== 'string') {
                continue;
            }
            if (projectName.endsWith('-e2e')) {
                // Skip existing separate E2E projects
                continue;
            }
            const buildTarget = project.targets.get('build');
            if (!buildTarget || buildTarget.builder !== workspace_models_1.Builders.Browser) {
                continue;
            }
            const buildTsConfig = (_a = buildTarget === null || buildTarget === void 0 ? void 0 : buildTarget.options) === null || _a === void 0 ? void 0 : _a.tsConfig;
            if (buildTsConfig && typeof buildTsConfig === 'string') {
                updateTsConfig(tree, buildTsConfig);
            }
            const testTarget = project.targets.get('test');
            if (!testTarget) {
                continue;
            }
            const testTsConfig = (_b = testTarget === null || testTarget === void 0 ? void 0 : testTarget.options) === null || _b === void 0 ? void 0 : _b.tsConfig;
            if (testTsConfig && typeof testTsConfig === 'string') {
                updateTsConfig(tree, testTsConfig);
            }
            const browserslistPath = core_1.join(core_1.normalize(project.root), 'browserslist');
            // Move the CLI 7 style browserlist to root if it's there.
            const sourceRoot = typeof project.sourceRoot === 'string'
                ? project.sourceRoot
                : core_1.join(core_1.normalize(project.root), 'src');
            const srcBrowsersList = core_1.join(core_1.normalize(sourceRoot), 'browserslist');
            if (tree.exists(srcBrowsersList)) {
                tree.rename(srcBrowsersList, browserslistPath);
            }
            else if (!tree.exists(browserslistPath)) {
                tree.create(browserslistPath, browserslistContent);
            }
        }
    };
}
exports.updateES5Projects = updateES5Projects;
function updateTsConfig(tree, tsConfigPath) {
    let tsConfigJson;
    try {
        tsConfigJson = new json_file_1.JSONFile(tree, tsConfigPath);
    }
    catch (_a) {
        return;
    }
    const compilerOptions = tsConfigJson.get(['compilerOptions']);
    if (!compilerOptions || typeof compilerOptions !== 'object') {
        return;
    }
    const configExtends = tsConfigJson.get(['extends']);
    const isExtended = configExtends && typeof configExtends === 'string';
    if (isExtended) {
        tsConfigJson.remove(['compilerOptions', 'target']);
        tsConfigJson.remove(['compilerOptions', 'module']);
        tsConfigJson.remove(['compilerOptions', 'downlevelIteration']);
    }
    else {
        tsConfigJson.modify(['compilerOptions', 'target'], 'es2015');
        tsConfigJson.modify(['compilerOptions', 'module'], 'esnext');
        tsConfigJson.modify(['compilerOptions', 'downlevelIteration'], true);
    }
}
