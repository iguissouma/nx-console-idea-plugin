"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.toLegacyWorkspaceFormat = exports.getPrimitiveValue = exports.normalizeSchema = exports.readAndCacheJsonFile = exports.cacheJson = exports.clearJsonCache = exports.readAndParseJson = exports.fileExistsSync = exports.directoryExists = exports.listFiles = exports.listOfUnnestedNpmPackages = exports.findClosestNx = exports.findClosestNg = exports.fileContents = exports.files = void 0;
const core_1 = require("@angular-devkit/core");
const formats_1 = require("@angular-devkit/schematics/src/formats");
const json_schema_1 = require("@angular/cli/utilities/json-schema");
const fs_1 = require("fs");
const os_1 = require("os");
const path = require("path");
const jsonc_parser_1 = require("jsonc-parser");
exports.files = {};
exports.fileContents = {};
const IMPORTANT_FIELD_NAMES = [
    'name',
    'project',
    'module',
    'watch',
    'style',
    'directory',
    'port',
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
        fs_1.readdirSync(dirName).forEach((c) => {
            const child = path.join(dirName, c);
            try {
                if (!fs_1.statSync(child).isDirectory()) {
                    res.push(child);
                }
                else if (fs_1.statSync(child).isDirectory()) {
                    res.push(...listFiles(child));
                }
            }
            catch (_a) {
                // noop
            }
        });
    }
    catch (_a) {
        // noop
    }
    return res;
}
exports.listFiles = listFiles;
function directoryExists(filePath) {
    try {
        return fs_1.statSync(filePath).isDirectory();
    }
    catch (_a) {
        return false;
    }
}
exports.directoryExists = directoryExists;
function fileExistsSync(filePath) {
    try {
        return fs_1.statSync(filePath).isFile();
    }
    catch (_a) {
        return false;
    }
}
exports.fileExistsSync = fileExistsSync;
function readAndParseJson(filePath) {
    const content = fs_1.readFileSync(filePath, 'utf-8');
    try {
        return JSON.parse(content);
    }
    catch (_a) {
        const errors = [];
        const result = jsonc_parser_1.parse(content, errors);
        if (errors.length > 0) {
            for (const { error, offset } of errors) {
                //getOutputChannel().appendLine(
                //  `${printParseErrorCode(
                //    error
                //  )} in JSON at position ${offset} in ${filePath}`
                //);
            }
        }
        return result;
    }
}
exports.readAndParseJson = readAndParseJson;
function clearJsonCache(filePath, basedir = '') {
    const fullFilePath = path.join(basedir, filePath);
    return delete exports.fileContents[fullFilePath];
}
exports.clearJsonCache = clearJsonCache;
/**
 * Caches already created json contents to a file path
 */
function cacheJson(filePath, basedir = '', content) {
    const fullFilePath = path.join(basedir, filePath);
    if (exports.fileContents[fullFilePath]) {
        return {
            json: exports.fileContents[fullFilePath],
            path: fullFilePath,
        };
    }
    if (content) {
        exports.fileContents[fullFilePath] = content;
    }
    return {
        json: content,
        path: fullFilePath,
    };
}
exports.cacheJson = cacheJson;
function readAndCacheJsonFile(filePath, basedir = '') {
    const fullFilePath = path.join(basedir, filePath);
    if (exports.fileContents[fullFilePath] || fs_1.existsSync(fullFilePath)) {
        exports.fileContents[fullFilePath] || (exports.fileContents[fullFilePath] = readAndParseJson(fullFilePath));
        return {
            path: fullFilePath,
            json: exports.fileContents[fullFilePath],
        };
    }
    else {
        return {
            path: fullFilePath,
            json: {},
        };
    }
}
exports.readAndCacheJsonFile = readAndCacheJsonFile;
const registry = new core_1.schema.CoreSchemaRegistry(formats_1.standardFormats);
async function normalizeSchema(s, projectDefaults) {
    const options = await json_schema_1.parseJsonSchemaToOptions(registry, s);
    const requiredFields = new Set(s.required || []);
    const nxOptions = options.map((option) => {
        const xPrompt = s.properties[option.name]['x-prompt'];
        const workspaceDefault = projectDefaults && projectDefaults[option.name];
        const $default = s.properties[option.name].$default;
        const nxOption = Object.assign(Object.assign(Object.assign(Object.assign(Object.assign(Object.assign({}, option), { required: isFieldRequired(requiredFields, option, xPrompt, $default) }), (workspaceDefault !== undefined && { default: workspaceDefault })), ($default && { $default })), (option.enum && { items: option.enum.map((item) => item.toString()) })), getItems(s.properties[option.name]));
        if (xPrompt) {
            nxOption.tooltip = isLongFormXPrompt(xPrompt) ? xPrompt.message : xPrompt;
            nxOption.itemTooltips = getEnumTooltips(xPrompt);
            if (isLongFormXPrompt(xPrompt) && !nxOption.items) {
                const items = (xPrompt.items || []).map((item) => isOptionItemLabelValue(item) ? item.value : item);
                if (items.length > 0) {
                    nxOption.items = items;
                }
            }
        }
        return nxOption;
    });
    return nxOptions.sort((a, b) => {
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
function isFieldRequired(requiredFields, nxOption, xPrompt, $default) {
    // checks schema.json requiredFields and xPrompt for required
    return (requiredFields.has(nxOption.name) ||
        // makes xPrompt fields required so nx command can run with --no-interactive
        // - except properties with a default (also falsey, empty, null)
        // - except properties with a $default $source
        // - except boolean properties (should also have default of `true`)
        (!!xPrompt && !nxOption.default && !$default && nxOption.type !== 'boolean'));
}
//function getItems(option: Option): { items: string[] } | undefined {
function getItems(option) {
    var _a;
    return (option.items && {
        items: ((_a = option.items) === null || _a === void 0 ? void 0 : _a.enum) ||
            (option.items.length && option.items),
    });
}
function isLongFormXPrompt(xPrompt) {
    return xPrompt.message !== undefined;
}
function getEnumTooltips(xPrompt) {
    const enumTooltips = {};
    if (!!xPrompt && isLongFormXPrompt(xPrompt)) {
        (xPrompt.items || []).forEach((item) => {
            if (isOptionItemLabelValue(item) && !!item.label) {
                enumTooltips[item.value] = item.label;
            }
        });
    }
    return enumTooltips;
}
function isOptionItemLabelValue(item) {
    return (item.value !== undefined ||
        item.label !== undefined);
}
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
