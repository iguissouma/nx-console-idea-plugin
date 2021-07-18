"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.polyfillMetadataRule = void 0;
/**
 * @license
 * Copyright Google LLC All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
const schematics_1 = require("@angular-devkit/schematics");
const ts = require("../../third_party/github.com/Microsoft/TypeScript/lib/typescript");
const workspace_1 = require("../../utility/workspace");
const workspace_models_1 = require("../../utility/workspace-models");
/**
 * Remove the Reflect import from a polyfill file.
 * @param tree The tree to use.
 * @param path Path of the polyfill file found.
 * @private
 */
function _removeReflectFromPolyfills(tree, path) {
    const source = tree.read(path);
    if (!source) {
        return;
    }
    // Start the update of the file.
    const recorder = tree.beginUpdate(path);
    const sourceFile = ts.createSourceFile(path, source.toString(), ts.ScriptTarget.Latest);
    const imports = sourceFile.statements.filter(ts.isImportDeclaration);
    for (const i of imports) {
        const module = ts.isStringLiteral(i.moduleSpecifier) && i.moduleSpecifier.text;
        switch (module) {
            case 'core-js/es7/reflect':
                recorder.remove(i.getFullStart(), i.getFullWidth());
                break;
        }
    }
    tree.commitUpdate(recorder);
}
function polyfillMetadataRule() {
    return async (tree) => {
        var _a;
        const workspace = await workspace_1.getWorkspace(tree);
        const rules = [];
        for (const [, project] of workspace.projects) {
            if (typeof project.root !== 'string') {
                continue;
            }
            for (const [, target] of project.targets) {
                if (target.builder !== workspace_models_1.Builders.Browser) {
                    continue;
                }
                const optionPolyfills = (_a = target.options) === null || _a === void 0 ? void 0 : _a.polyfills;
                if (optionPolyfills && typeof optionPolyfills === 'string') {
                    rules.push((tree) => _removeReflectFromPolyfills(tree, optionPolyfills));
                }
                if (!target.configurations) {
                    continue;
                }
                for (const configuration of Object.values(target.configurations)) {
                    const configurationPolyfills = configuration === null || configuration === void 0 ? void 0 : configuration.polyfills;
                    if (configurationPolyfills && typeof configurationPolyfills === 'string') {
                        rules.push((tree) => _removeReflectFromPolyfills(tree, configurationPolyfills));
                    }
                }
            }
        }
        return schematics_1.chain(rules);
    };
}
exports.polyfillMetadataRule = polyfillMetadataRule;
