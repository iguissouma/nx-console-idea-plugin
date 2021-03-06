import { json } from '@angular-devkit/core';
import {Option, OptionType, Value} from "./interface";


function _getEnumFromValue<E, T extends E[keyof E]>(
    value: json.JsonValue,
    enumeration: E,
    defaultValue: T,
): T {
    if (typeof value !== 'string') {
        return defaultValue;
    }

    if (Object.values(enumeration).includes(value)) {
        return value as unknown as T;
    }

    return defaultValue;
}


export async function parseJsonSchemaToOptions(
    registry: json.schema.SchemaRegistry,
    schema: json.JsonObject,
): Promise<Option[]> {
    const options: Option[] = [];

    function visitor(
        current: json.JsonObject | json.JsonArray,
        pointer: json.schema.JsonPointer,
        parentSchema?: json.JsonObject | json.JsonArray,
    ) {
        if (!parentSchema) {
            // Ignore root.
            return;
        } else if (pointer.split(/\/(?:properties|items|definitions)\//g).length > 2) {
            // Ignore subitems (objects or arrays).
            return;
        } else if (json.isJsonArray(current)) {
            return;
        }

        if (pointer.indexOf('/not/') != -1) {
            // We don't support anyOf/not.
            throw new Error('The "not" keyword is not supported in JSON Schema.');
        }

        const ptr = json.schema.parseJsonPointer(pointer);
        const name = ptr[ptr.length - 1];

        if (ptr[ptr.length - 2] != 'properties') {
            // Skip any non-property items.
            return;
        }

        const typeSet = json.schema.getTypesOfSchema(current);

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
                    if (json.isJsonObject(current.items)
                        && typeof current.items.type == 'string'
                        && ['boolean', 'number', 'string'].includes(current.items.type)) {
                        return true;
                    }

                    return false;

                default:
                    return false;
            }
        }).map(x => _getEnumFromValue(x, OptionType, OptionType.String));

        if (types.length == 0) {
            // This means it's not usable on the command line. e.g. an Object.
            return;
        }

        // Only keep enum values we support (booleans, numbers and strings).
        const enumValues = (json.isJsonArray(current.enum) && current.enum || []).filter(x => {
            switch (typeof x) {
                case 'boolean':
                case 'number':
                case 'string':
                    return true;

                default:
                    return false;
            }
        }) as Value[];

        let defaultValue: string | number | boolean | undefined = undefined;
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
        const $defaultIndex = (json.isJsonObject($default) && $default['$source'] == 'argv')
            ? $default['index'] : undefined;
        const positional: number | undefined = typeof $defaultIndex == 'number'
            ? $defaultIndex : undefined;

        const required = json.isJsonArray(current.required)
            ? current.required.indexOf(name) != -1 : false;
        const aliases = json.isJsonArray(current.aliases) ? [...current.aliases].map(x => '' + x)
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

        const option: Option = {
            name,
            description: '' + (current.description === undefined ? '' : current.description),
            ...types.length == 1 ? { type } : { type, types },
            ...defaultValue !== undefined ? { default: defaultValue } : {},
            ...enumValues && enumValues.length > 0 ? { enum: enumValues } : {},
            required,
            aliases,
            ...format !== undefined ? { format } : {},
            hidden,
            ...userAnalytics ? { userAnalytics } : {},
            ...deprecated !== undefined ? { deprecated } : {},
            ...positional !== undefined ? { positional } : {},
        };

        options.push(option);
    }

    const flattenedSchema = await registry.flatten(schema).toPromise();
    json.schema.visitJsonSchema(flattenedSchema, visitor);

    // Sort by positional.
    return options.sort((a, b) => {
        if (a.positional) {
            if (b.positional) {
                return a.positional - b.positional;
            } else {
                return 1;
            }
        } else if (b.positional) {
            return -1;
        } else {
            return 0;
        }
    });
}
