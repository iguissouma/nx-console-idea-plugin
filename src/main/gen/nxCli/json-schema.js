"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const core_1 = require("@angular-devkit/core");
const interface_1 = require("./interface");
function _getEnumFromValue(value, enumeration, defaultValue) {
    if (typeof value !== 'string') {
        return defaultValue;
    }
    if (Object.values(enumeration).includes(value)) {
        return value;
    }
    return defaultValue;
}
async function parseJsonSchemaToOptions(registry, schema) {
    const options = [];
    function visitor(current, pointer, parentSchema) {
        if (!parentSchema) {
            // Ignore root.
            return;
        }
        else if (pointer.split(/\/(?:properties|items|definitions)\//g).length > 2) {
            // Ignore subitems (objects or arrays).
            return;
        }
        else if (core_1.json.isJsonArray(current)) {
            return;
        }
        if (pointer.indexOf('/not/') != -1) {
            // We don't support anyOf/not.
            throw new Error('The "not" keyword is not supported in JSON Schema.');
        }
        const ptr = core_1.json.schema.parseJsonPointer(pointer);
        const name = ptr[ptr.length - 1];
        if (ptr[ptr.length - 2] != 'properties') {
            // Skip any non-property items.
            return;
        }
        const typeSet = core_1.json.schema.getTypesOfSchema(current);
        if (typeSet.size == 0) {
            throw new Error('Cannot find type of schema.');
        }
        // We only support number, string or boolean (or array of those), so remove everything else.
        const types = [...typeSet].filter(x => {
            switch (x) {
                case 'boolean':
                case 'number':
                case 'string':
                    return true;
                case 'array':
                    // Only include arrays if they're boolean, string or number.
                    if (core_1.json.isJsonObject(current.items)
                        && typeof current.items.type == 'string'
                        && ['boolean', 'number', 'string'].includes(current.items.type)) {
                        return true;
                    }
                    return false;
                default:
                    return false;
            }
        }).map(x => _getEnumFromValue(x, interface_1.OptionType, interface_1.OptionType.String));
        if (types.length == 0) {
            // This means it's not usable on the command line. e.g. an Object.
            return;
        }
        // Only keep enum values we support (booleans, numbers and strings).
        const enumValues = (core_1.json.isJsonArray(current.enum) && current.enum || []).filter(x => {
            switch (typeof x) {
                case 'boolean':
                case 'number':
                case 'string':
                    return true;
                default:
                    return false;
            }
        });
        let defaultValue = undefined;
        if (current.default !== undefined) {
            switch (types[0]) {
                case 'string':
                    if (typeof current.default == 'string') {
                        defaultValue = current.default;
                    }
                    break;
                case 'number':
                    if (typeof current.default == 'number') {
                        defaultValue = current.default;
                    }
                    break;
                case 'boolean':
                    if (typeof current.default == 'boolean') {
                        defaultValue = current.default;
                    }
                    break;
            }
        }
        const type = types[0];
        const $default = current.$default;
        const $defaultIndex = (core_1.json.isJsonObject($default) && $default['$source'] == 'argv')
            ? $default['index'] : undefined;
        const positional = typeof $defaultIndex == 'number'
            ? $defaultIndex : undefined;
        const required = core_1.json.isJsonArray(current.required)
            ? current.required.indexOf(name) != -1 : false;
        const aliases = core_1.json.isJsonArray(current.aliases) ? [...current.aliases].map(x => '' + x)
            : current.alias ? ['' + current.alias] : [];
        const format = typeof current.format == 'string' ? current.format : undefined;
        const visible = current.visible === undefined || current.visible === true;
        const hidden = !!current.hidden || !visible;
        const xUserAnalytics = current['x-user-analytics'];
        const userAnalytics = typeof xUserAnalytics == 'number' ? xUserAnalytics : undefined;
        // Deprecated is set only if it's true or a string.
        const xDeprecated = current['x-deprecated'];
        const deprecated = (xDeprecated === true || typeof xDeprecated === 'string')
            ? xDeprecated : undefined;
        const option = Object.assign(Object.assign(Object.assign(Object.assign(Object.assign(Object.assign(Object.assign(Object.assign(Object.assign({ name, description: '' + (current.description === undefined ? '' : current.description) }, types.length == 1 ? { type } : { type, types }), defaultValue !== undefined ? { default: defaultValue } : {}), enumValues && enumValues.length > 0 ? { enum: enumValues } : {}), { required,
            aliases }), format !== undefined ? { format } : {}), { hidden }), userAnalytics ? { userAnalytics } : {}), deprecated !== undefined ? { deprecated } : {}), positional !== undefined ? { positional } : {});
        options.push(option);
    }
    const flattenedSchema = await registry.flatten(schema).toPromise();
    core_1.json.schema.visitJsonSchema(flattenedSchema, visitor);
    // Sort by positional.
    return options.sort((a, b) => {
        if (a.positional) {
            if (b.positional) {
                return a.positional - b.positional;
            }
            else {
                return 1;
            }
        }
        else if (b.positional) {
            return -1;
        }
        else {
            return 0;
        }
    });
}
exports.parseJsonSchemaToOptions = parseJsonSchemaToOptions;
