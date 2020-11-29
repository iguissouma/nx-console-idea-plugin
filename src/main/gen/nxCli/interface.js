"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
/**
 * Command constructor.
 */
/*export interface CommandConstructor {
    new(
        context: CommandContext,
        description: CommandDescription,
        logger: logging.Logger,
    ): CommandInterface;
}*/
/**
 * A command runner context.
 */
/*export interface CommandContext {
    currentDirectory: string;
    root: string;

    workspace?: AngularWorkspace;

    // This property is optional for backward compatibility.
    analytics?: analytics.Analytics;
}*/
/**
 * Value types of an Option.
 */
var OptionType;
(function (OptionType) {
    OptionType["Any"] = "any";
    OptionType["Array"] = "array";
    OptionType["Boolean"] = "boolean";
    OptionType["Number"] = "number";
    OptionType["String"] = "string";
})(OptionType = exports.OptionType || (exports.OptionType = {}));
/**
 * Scope of the command.
 */
var CommandScope;
(function (CommandScope) {
    CommandScope["InProject"] = "in";
    CommandScope["OutProject"] = "out";
    CommandScope["Everywhere"] = "all";
    CommandScope["Default"] = "in";
})(CommandScope = exports.CommandScope || (exports.CommandScope = {}));
/*export interface CommandDescriptionMap {
    [key: string]: CommandDescription;
}*/
