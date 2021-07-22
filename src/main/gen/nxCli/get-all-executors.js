"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.getAllExecutors = void 0;
/* eslint-disable @typescript-eslint/no-explicit-any */
const utils_1 = require("./utils");
const path_1 = require("path");
const os_1 = require("os");
function getAllExecutors(workspaceJsonPath, clearPackageJsonCache) {
    return readExecutorCollectionsFromNodeModules(workspaceJsonPath, clearPackageJsonCache);
}
exports.getAllExecutors = getAllExecutors;
function readExecutorCollectionsFromNodeModules(workspaceJsonPath, clearPackageJsonCache) {
    const basedir = path_1.dirname(workspaceJsonPath);
    const nodeModulesDir = path_1.join(basedir, 'node_modules');
    if (clearPackageJsonCache) {
        utils_1.clearJsonCache('package.json', basedir);
    }
    const packageJson = utils_1.readAndCacheJsonFile('package.json', basedir).json;
    const packages = Object.assign(Object.assign({}, (packageJson.devDependencies || {})), (packageJson.dependencies || {}));
    const executorCollections = Object.keys(packages).filter((p) => {
        try {
            const packageJson = utils_1.readAndCacheJsonFile(path_1.join(p, 'package.json'), nodeModulesDir).json;
            // TODO: to add support for schematics, we can change this to include schematics/generators
            return !!(packageJson.builders || packageJson.executors);
        }
        catch (e) {
            if (e.message &&
                (e.message.indexOf('no such file') > -1 ||
                    e.message.indexOf('not a directory') > -1)) {
                return false;
            }
            else {
                throw e;
            }
        }
    });
    return executorCollections
        .map((c) => readCollections(nodeModulesDir, c))
        .flat()
        .filter((c) => Boolean(c));
}
function readCollections(basedir, collectionName) {
    try {
        const packageJson = utils_1.readAndCacheJsonFile(path_1.join(collectionName, 'package.json'), basedir);
        const collection = utils_1.readAndCacheJsonFile(packageJson.json.builders || packageJson.json.executors, path_1.dirname(packageJson.path));
        return getBuilderPaths(collectionName, collection.path, collection.json);
    }
    catch (e) {
        return null;
    }
}
function getBuilderPaths(collectionName, path, json) {
    const baseDir = path_1.dirname(path);
    const builders = [];
    for (const [key, value] of Object.entries(json.builders || json.executors)) {
        let path = '';
        if (os_1.platform() === 'win32') {
            path = `file:///${path_1.join(baseDir, value.schema).replace(/\\/g, '/')}`;
        }
        else {
            path = path_1.join(baseDir, value.schema);
        }
        builders.push({
            name: `${collectionName}:${key}`,
            path,
        });
    }
    return builders;
}
