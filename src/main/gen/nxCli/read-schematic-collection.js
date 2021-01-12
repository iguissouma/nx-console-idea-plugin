"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const path_1 = require("path");
const utils_1 = require("./utils");
const utils_2 = require("./utils");
async function readAllSchematicCollections(workspaceJsonPath, workspaceSchematicsPath) {
    const basedir = path_1.join(workspaceJsonPath, '..');
    let collections = await readSchematicCollectionsFromNodeModules(workspaceJsonPath);
    if (utils_1.directoryExists(path_1.join(basedir, workspaceSchematicsPath))) {
        collections = [
            await readWorkspaceSchematicsCollection(basedir, workspaceSchematicsPath),
            ...collections
        ];
    }
    return collections.filter((collection) => !!collection && collection.schematics.length > 0);
}
exports.readAllSchematicCollections = readAllSchematicCollections;
function readWorkspaceJsonDefaults(workspaceJsonPath) {
    const defaults = utils_2.toLegacyWorkspaceFormat(utils_1.readAndParseJson(workspaceJsonPath)).schematics ||
        {};
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
            Object.keys(defaults[collectionName]).forEach(schematicName => {
                collectionDefaultsMap[collectionName][schematicName] =
                    defaults[collectionName][schematicName];
            });
        }
        return collectionDefaultsMap;
    }, {});
    return collectionDefaults;
}
async function readSchematicCollectionsFromNodeModules(workspaceJsonPath) {
    const basedir = path_1.join(workspaceJsonPath, '..');
    const nodeModulesDir = path_1.join(basedir, 'node_modules');
    const packages = utils_1.listOfUnnestedNpmPackages(nodeModulesDir);
    const schematicCollections = packages.filter(p => {
        try {
            const packageJson = utils_1.readAndCacheJsonFile(path_1.join(p, 'package.json'), nodeModulesDir).json;
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
    const defaults = readWorkspaceJsonDefaults(workspaceJsonPath);
    return (await Promise.all(schematicCollections.map(c => readCollection(nodeModulesDir, c, defaults)))).filter((c) => Boolean(c));
}
async function readWorkspaceSchematicsCollection(basedir, workspaceSchematicsPath) {
    const collectionDir = path_1.join(basedir, workspaceSchematicsPath);
    const collectionName = 'workspace-schematic';
    if (utils_1.fileExistsSync(path_1.join(collectionDir, 'collection.json'))) {
        const collection = utils_1.readAndCacheJsonFile('collection.json', collectionDir);
        return await readCollectionSchematics(collectionName, collection.path, collection.json);
    }
    else {
        const schematics = await Promise.all(utils_1.listFiles(collectionDir)
            .filter(f => path_1.basename(f) === 'schema.json')
            .map(async (f) => {
            const schemaJson = utils_1.readAndCacheJsonFile(f, '');
            return {
                name: schemaJson.json.id,
                collection: collectionName,
                options: await utils_1.normalizeSchema(schemaJson.json),
                description: ''
            };
        }));
        return { name: collectionName, schematics };
    }
}
async function readCollection(basedir, collectionName, defaults) {
    try {
        const packageJson = utils_1.readAndCacheJsonFile(path_1.join(collectionName, 'package.json'), basedir);
        const collection = utils_1.readAndCacheJsonFile(packageJson.json.schematics || packageJson.json.generators, path_1.dirname(packageJson.path));
        return readCollectionSchematics(collectionName, collection.path, collection.json, defaults);
    }
    catch (e) {
        // this happens when package is misconfigured. We decided to ignore such a case.
        return null;
    }
}
async function readCollectionSchematics(collectionName, collectionPath, collectionJson, defaults) {
    const schematicCollection = {
        name: collectionName,
        schematics: []
    };
    try {
        Object.entries(collectionJson.schematics || collectionJson.generators).forEach(async ([k, v]) => {
            try {
                if (canAdd(k, v)) {
                    const schematicSchema = utils_1.readAndCacheJsonFile(v.schema, path_1.dirname(collectionPath));
                    const projectDefaults = defaults && defaults[collectionName] && defaults[collectionName][k];
                    schematicCollection.schematics.push({
                        name: k,
                        collection: collectionName,
                        options: await utils_1.normalizeSchema(schematicSchema.json, projectDefaults),
                        description: v.description || ''
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
function canAdd(name, s) {
    return !s.hidden && !s.private && !s.extends && name !== 'ng-add';
}
exports.canAdd = canAdd;
