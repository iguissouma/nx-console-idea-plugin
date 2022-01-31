"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.readBuilderSchema = exports.readArchitect = exports.readArchitectDef = exports.readProjects = void 0;
const path = require("path");
const utils_1 = require("./utils");
function readProjects(json) {
    return Object.entries(json)
        .map(([key, value]) => ({
        name: key,
        root: value.root,
        projectType: value.projectType,
        architect: readArchitect(key, value.architect)
    }))
        .sort((a, b) => a.root.localeCompare(b.root));
}
exports.readProjects = readProjects;
function readArchitectDef(architectName, architectDef, project) {
    const configurations = architectDef.configurations
        ? Object.keys(architectDef.configurations).map(name => ({
            name,
            defaultValues: readDefaultValues(architectDef.configurations, name)
        }))
        : [];
    return {
        options: [],
        configurations,
        name: architectName,
        project,
        description: architectDef.description || '',
        builder: architectDef.builder
    };
}
exports.readArchitectDef = readArchitectDef;
function readArchitect(project, architect) {
    if (!architect)
        return [];
    return Object.entries(architect).map(([key, value]) => {
        return readArchitectDef(key, value, project);
    });
}
exports.readArchitect = readArchitect;
function readDefaultValues(configurations, name) {
    const defaults = [];
    const config = configurations[name];
    if (!config)
        return defaults;
    return Object.keys(config).reduce((m, k) => [...m, { name: k, defaultValue: (0, utils_1.getPrimitiveValue)(config[k]) }], defaults);
}
async function readBuilderSchema(basedir, builder) {
    const [npmPackage, builderName] = builder.split(':');
    const packageJson = (0, utils_1.readAndCacheJsonFile)(path.join(npmPackage, 'package.json'), path.join(basedir, 'node_modules'));
    const b = packageJson.json.builders || packageJson.json.executors;
    const buildersPath = b.startsWith('.') ? b : `./${b}`;
    const buildersJson = (0, utils_1.readAndCacheJsonFile)(buildersPath, path.dirname(packageJson.path));
    const builderDef = (buildersJson.json.builders ||
        buildersJson.json.executors)[builderName];
    const builderSchema = (0, utils_1.readAndCacheJsonFile)(builderDef.schema, path.dirname(buildersJson.path));
    return await (0, utils_1.normalizeSchema)(builderSchema.json);
}
exports.readBuilderSchema = readBuilderSchema;
