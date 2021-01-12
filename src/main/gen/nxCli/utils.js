"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const core_1 = require("@angular-devkit/core");
const formats_1 = require("@angular-devkit/schematics/src/formats");
const json_schema_1 = require("./json-schema");
const fs_1 = require("fs");
const JSON5 = require("json5");
const os_1 = require("os");
const path = require("path");
exports.files = {};
exports.fileContents = {};
const IMPORTANT_FIELD_NAMES = [
    'name',
    'project',
    'module',
    'watch',
    'style',
    'directory',
    'port'
];
const IMPORTANT_FIELDS_SET = new Set(IMPORTANT_FIELD_NAMES);
function findClosestNg(dir) {
    if (directoryExists(path.join(dir, 'node_modules'))) {
        if (os_1.platform() === 'win32') {
            if (fileExistsSync(path.join(dir, 'ng.cmd'))) {
                return path.join(dir, 'ng.cmd');
            }
            else {
                return path.join(dir, 'node_modules', '.bin', 'ng.cmd');
            }
        }
        else {
            if (fileExistsSync(path.join(dir, 'node_modules', '.bin', 'ng'))) {
                return path.join(dir, 'node_modules', '.bin', 'ng');
            }
            else {
                return path.join(dir, 'node_modules', '@angular', 'cli', 'bin', 'ng');
            }
        }
    }
    else {
        return findClosestNg(path.dirname(dir));
    }
}
exports.findClosestNg = findClosestNg;
function findClosestNx(dir) {
    if (directoryExists(path.join(dir, 'node_modules'))) {
        if (os_1.platform() === 'win32') {
            if (fileExistsSync(path.join(dir, 'nx.cmd'))) {
                return path.join(dir, 'nx.cmd');
            }
            else {
                return path.join(dir, 'node_modules', '.bin', 'nx.cmd');
            }
        }
        else {
            if (fileExistsSync(path.join(dir, 'node_modules', '.bin', 'nx'))) {
                return path.join(dir, 'node_modules', '.bin', 'nx');
            }
            else {
                return path.join(dir, 'node_modules', '@nrwl', 'cli', 'bin', 'nx.js');
            }
        }
    }
    else {
        return findClosestNx(path.dirname(dir));
    }
}
exports.findClosestNx = findClosestNx;
function listOfUnnestedNpmPackages(nodeModulesDir) {
    const res = [];
    if (!fs_1.existsSync(nodeModulesDir)) {
        return res;
    }
    fs_1.readdirSync(nodeModulesDir).forEach(npmPackageOrScope => {
        if (npmPackageOrScope.startsWith('@')) {
            fs_1.readdirSync(path.join(nodeModulesDir, npmPackageOrScope)).forEach(p => {
                res.push(`${npmPackageOrScope}/${p}`);
            });
        }
        else {
            res.push(npmPackageOrScope);
        }
    });
    return res;
}
exports.listOfUnnestedNpmPackages = listOfUnnestedNpmPackages;
function listFiles(dirName) {
    // TODO use .gitignore to skip files
    if (dirName.indexOf('node_modules') > -1)
        return [];
    if (dirName.indexOf('dist') > -1)
        return [];
    const res = [dirName];
    // the try-catch here is intentional. It's only used in auto-completion.
    // If it doesn't work, we don't want the process to exit
    try {
        fs_1.readdirSync(dirName).forEach(c => {
            const child = path.join(dirName, c);
            try {
                if (!fs_1.statSync(child).isDirectory()) {
                    res.push(child);
                }
                else if (fs_1.statSync(child).isDirectory()) {
                    res.push(...listFiles(child));
                }
            }
            catch (e) { }
        });
    }
    catch (e) { }
    return res;
}
exports.listFiles = listFiles;
function directoryExists(filePath) {
    try {
        return fs_1.statSync(filePath).isDirectory();
    }
    catch (err) {
        return false;
    }
}
exports.directoryExists = directoryExists;
function fileExistsSync(filePath) {
    try {
        return fs_1.statSync(filePath).isFile();
    }
    catch (err) {
        return false;
    }
}
exports.fileExistsSync = fileExistsSync;
function readAndParseJson(fullFilePath) {
    return JSON5.parse(fs_1.readFileSync(fullFilePath).toString());
}
exports.readAndParseJson = readAndParseJson;
function readAndCacheJsonFile(filePath, basedir) {
    const fullFilePath = path.join(basedir, filePath);
    if (exports.fileContents[fullFilePath] || fs_1.existsSync(fullFilePath)) {
        exports.fileContents[fullFilePath] =
            exports.fileContents[fullFilePath] || readAndParseJson(fullFilePath);
        return {
            path: fullFilePath,
            json: exports.fileContents[fullFilePath]
        };
    }
    else {
        return {
            path: fullFilePath,
            json: {}
        };
    }
}
exports.readAndCacheJsonFile = readAndCacheJsonFile;
const registry = new core_1.schema.CoreSchemaRegistry(formats_1.standardFormats);
async function normalizeSchema(s, projectDefaults) {
    const options = await json_schema_1.parseJsonSchemaToOptions(registry, s);
    const requiredFields = new Set(s.required || []);
    options.forEach(option => {
        const workspaceDefault = projectDefaults && projectDefaults[option.name];
        if (workspaceDefault !== undefined) {
            option.default = workspaceDefault;
        }
        if (requiredFields.has(option.name)) {
            option.required = true;
        }
    });
    return options.sort((a, b) => {
        if (typeof a.positional === 'number' && typeof b.positional === 'number') {
            return a.positional - b.positional;
        }
        if (typeof a.positional === 'number') {
            return -1;
        }
        else if (typeof b.positional === 'number') {
            return 1;
        }
        else if (a.required) {
            if (b.required) {
                return a.name.localeCompare(b.name);
            }
            return -1;
        }
        else if (b.required) {
            return 1;
        }
        else if (IMPORTANT_FIELDS_SET.has(a.name)) {
            if (IMPORTANT_FIELDS_SET.has(b.name)) {
                return (IMPORTANT_FIELD_NAMES.indexOf(a.name) -
                    IMPORTANT_FIELD_NAMES.indexOf(b.name));
            }
            return -1;
        }
        else if (IMPORTANT_FIELDS_SET.has(b.name)) {
            return 1;
        }
        else {
            return a.name.localeCompare(b.name);
        }
    });
}
exports.normalizeSchema = normalizeSchema;
function getPrimitiveValue(value) {
    if (typeof value === 'string' ||
        typeof value === 'number' ||
        typeof value === 'boolean') {
        return value.toString();
    }
    else {
        return undefined;
    }
}
exports.getPrimitiveValue = getPrimitiveValue;
function renameProperty(obj, from, to) {
    obj[to] = obj[from];
    delete obj[from];
}
function toLegacyWorkspaceFormat(w) {
    Object.values(w.projects || {}).forEach((project) => {
        if (project.targets) {
            renameProperty(project, 'targets', 'architect');
        }
        if (project.generators) {
            renameProperty(project, 'generators', 'schematics');
        }
        Object.values(project.architect || {}).forEach((target) => {
            if (target.executor) {
                renameProperty(target, 'executor', 'builder');
            }
        });
    });
    if (w.generators) {
        renameProperty(w, 'generators', 'schematics');
    }
    return w;
}
exports.toLegacyWorkspaceFormat = toLegacyWorkspaceFormat;
