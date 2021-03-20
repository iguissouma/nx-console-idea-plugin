"use strict";
/**
 * @license
 * Copyright Google Inc. All Rights Reserved.
 *
 * Use of this source code is governed by an MIT-style license that can be
 * found in the LICENSE file at https://angular.io/license
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.getProjectDependencies = exports.findPackageJson = exports.readPackageJson = void 0;
const fs = require("fs");
const path_1 = require("path");
const resolve = require("resolve");
const util_1 = require("util");
const readFile = util_1.promisify(fs.readFile);
function getAllDependencies(pkg) {
    return new Set([
        ...Object.entries(pkg.dependencies || []),
        ...Object.entries(pkg.devDependencies || []),
        ...Object.entries(pkg.peerDependencies || []),
        ...Object.entries(pkg.optionalDependencies || []),
    ]);
}
async function readPackageJson(packageJsonPath) {
    try {
        return JSON.parse((await readFile(packageJsonPath)).toString());
    }
    catch (_a) {
        return undefined;
    }
}
exports.readPackageJson = readPackageJson;
function findPackageJson(workspaceDir, packageName) {
    try {
        // avoid require.resolve here, see: https://github.com/angular/angular-cli/pull/18610#issuecomment-681980185
        const packageJsonPath = resolve.sync(`${packageName}/package.json`, { basedir: workspaceDir });
        return packageJsonPath;
    }
    catch (_a) {
        return undefined;
    }
}
exports.findPackageJson = findPackageJson;
async function getProjectDependencies(dir) {
    const pkg = await readPackageJson(path_1.join(dir, 'package.json'));
    if (!pkg) {
        throw new Error('Could not find package.json');
    }
    const results = new Map();
    for (const [name, version] of getAllDependencies(pkg)) {
        const packageJsonPath = findPackageJson(dir, name);
        if (!packageJsonPath) {
            continue;
        }
        results.set(name, {
            name,
            version,
            path: path_1.dirname(packageJsonPath),
            package: await readPackageJson(packageJsonPath),
        });
    }
    return results;
}
exports.getProjectDependencies = getProjectDependencies;
