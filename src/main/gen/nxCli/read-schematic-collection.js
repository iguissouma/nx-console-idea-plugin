"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.readSchematicOptions = exports.readAllSchematicCollections = void 0;
const path_1 = require("path");
const utils_1 = require("./utils");
async function readAllSchematicCollections(workspaceJsonPath) {
    const basedir = (0, path_1.join)(workspaceJsonPath, '..');
    let collections = await readSchematicCollectionsFromNodeModules(workspaceJsonPath);
    collections = [
        ...collections,
        ...(await checkAndReadWorkspaceCollection(basedir, (0, path_1.join)('tools', 'schematics'))),
        ...(await checkAndReadWorkspaceCollection(basedir, (0, path_1.join)('tools', 'generators'))),
    ];
    return collections.filter((collection) => collection && collection.schematics.length > 0);
}
exports.readAllSchematicCollections = readAllSchematicCollections;
async function checkAndReadWorkspaceCollection(basedir, workspaceSchematicsPath) {
    if ((0, utils_1.directoryExists)((0, path_1.join)(basedir, workspaceSchematicsPath))) {
        return readWorkspaceSchematicsCollection(basedir, workspaceSchematicsPath).then((val) => [val]);
    }
    return Promise.resolve([]);
}
function readWorkspaceJsonDefaults(workspaceJsonPath) {
    const defaults = (0, utils_1.toLegacyWorkspaceFormat)((0, utils_1.readAndCacheJsonFile)(workspaceJsonPath).json)
        .schematics || {};
    const collectionDefaults = Object.keys(defaults).reduce((collectionDefaultsMap, key) => {
        if (key.includes(':')) {
            const [collectionName, schematicName] = key.split(':');
            if (!collectionDefaultsMap[collectionName]) {
                collectionDefaultsMap[collectionName] = {};
            }
            collectionDefaultsMap[collectionName][schematicName] = defaults[key];
        }
        else {
            const collectionName = key;
            if (!collectionDefaultsMap[collectionName]) {
                collectionDefaultsMap[collectionName] = {};
            }
            Object.keys(defaults[collectionName]).forEach((schematicName) => {
                collectionDefaultsMap[collectionName][schematicName] =
                    defaults[collectionName][schematicName];
            });
        }
        return collectionDefaultsMap;
    }, {});
    return collectionDefaults;
}
async function readSchematicCollectionsFromNodeModules(workspaceJsonPath) {
    const basedir = (0, path_1.join)(workspaceJsonPath, '..');
    const nodeModulesDir = (0, path_1.join)(basedir, 'node_modules');
    const packages = (0, utils_1.listOfUnnestedNpmPackages)(nodeModulesDir);
    const schematicCollections = packages.filter((p) => {
        try {
            const packageJson = (0, utils_1.readAndCacheJsonFile)((0, path_1.join)(p, 'package.json'), nodeModulesDir).json;
            return !!(packageJson.schematics || packageJson.generators);
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
    return (await Promise.all(schematicCollections.map((c) => readCollection(nodeModulesDir, c)))).filter((c) => Boolean(c));
}
async function readWorkspaceSchematicsCollection(basedir, workspaceSchematicsPath) {
    const collectionDir = (0, path_1.join)(basedir, workspaceSchematicsPath);
    const collectionName = 'workspace-schematic';
    if ((0, utils_1.fileExistsSync)((0, path_1.join)(collectionDir, 'collection.json'))) {
        const collection = (0, utils_1.readAndCacheJsonFile)('collection.json', collectionDir);
        return await readCollectionSchematics(collectionName, collection.path, collection.json);
    }
    else {
        const schematics = await Promise.all((0, utils_1.listFiles)(collectionDir)
            .filter((f) => (0, path_1.basename)(f) === 'schema.json')
            .map(async (f) => {
            const schemaJson = (0, utils_1.readAndCacheJsonFile)(f, '');
            return {
                name: schemaJson.json.id || schemaJson.json.$id,
                collection: collectionName,
                options: await (0, utils_1.normalizeSchema)(schemaJson.json),
                description: '',
            };
        }));
        return { name: collectionName, schematics };
    }
}
async function readCollection(basedir, collectionName) {
    try {
        const packageJson = (0, utils_1.readAndCacheJsonFile)((0, path_1.join)(collectionName, 'package.json'), basedir);
        const collection = (0, utils_1.readAndCacheJsonFile)(packageJson.json.schematics || packageJson.json.generators, (0, path_1.dirname)(packageJson.path));
        return readCollectionSchematics(collectionName, collection.path, collection.json);
    }
    catch (e) {
        // this happens when package is misconfigured. We decided to ignore such a case.
        return null;
    }
}
async function readCollectionSchematics(collectionName, collectionPath, collectionJson) {
    const schematicCollection = {
        name: collectionName,
        schematics: []
    };
    try {
        Object.entries(Object.assign({}, collectionJson.schematics || collectionJson.generators)).forEach(async ([k, v]) => {
            try {
                if (canAdd(k, v)) {
                    // added not in nx console
                    const schematicSchema = (0, utils_1.readAndCacheJsonFile)(v.schema, (0, path_1.dirname)(collectionPath));
                    const options = await (0, utils_1.normalizeSchema)(schematicSchema.json);
                    schematicCollection.schematics.push({
                        name: k,
                        collection: collectionName,
                        description: v.description || '',
                        options: options
                    });
                }
            }
            catch (e) {
                console.error(e);
                console.error(`Invalid package.json for schematic ${collectionName}:${k}`);
            }
        });
    }
    catch (e) {
        console.error(e);
        console.error(`Invalid package.json for schematic ${collectionName}`);
    }
    return schematicCollection;
}
async function readSchematicOptions(workspaceJsonPath, collectionName, schematicName) {
    const basedir = (0, path_1.join)(workspaceJsonPath, '..');
    const nodeModulesDir = (0, path_1.join)(basedir, 'node_modules');
    const collectionPackageJson = (0, utils_1.readAndCacheJsonFile)((0, path_1.join)(collectionName, 'package.json'), nodeModulesDir);
    const collectionJson = (0, utils_1.readAndCacheJsonFile)(collectionPackageJson.json.schematics ||
        collectionPackageJson.json.generators, (0, path_1.dirname)(collectionPackageJson.path));
    const schematics = Object.assign({}, collectionJson.json.schematics, collectionJson.json.generators);
    const schematicSchema = (0, utils_1.readAndCacheJsonFile)(schematics[schematicName].schema, (0, path_1.dirname)(collectionJson.path));
    const workspaceDefaults = readWorkspaceJsonDefaults(workspaceJsonPath);
    const defaults = workspaceDefaults &&
        workspaceDefaults[collectionName] &&
        workspaceDefaults[collectionName][schematicName];
    return await (0, utils_1.normalizeSchema)(schematicSchema.json, defaults);
}
exports.readSchematicOptions = readSchematicOptions;
function canAdd(name, s) {
    return !s.hidden && !s.private && !s.extends && name !== 'ng-add';
}
