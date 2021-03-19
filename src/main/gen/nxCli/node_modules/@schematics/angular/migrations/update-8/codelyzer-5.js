"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.updatePackageJson = exports.updateTsLintConfig = void 0;
const dependencies_1 = require("../../utility/dependencies");
const json_file_1 = require("../../utility/json-file");
const ruleMapping = {
    'contextual-life-cycle': 'contextual-lifecycle',
    'no-conflicting-life-cycle-hooks': 'no-conflicting-lifecycle',
    'no-life-cycle-call': 'no-lifecycle-call',
    'use-life-cycle-interface': 'use-lifecycle-interface',
    'decorator-not-allowed': 'contextual-decorator',
    'enforce-component-selector': 'use-component-selector',
    'no-output-named-after-standard-event': 'no-output-native',
    'use-host-property-decorator': 'no-host-metadata-property',
    'use-input-property-decorator': 'no-inputs-metadata-property',
    'use-output-property-decorator': 'no-outputs-metadata-property',
    'no-queries-parameter': 'no-queries-metadata-property',
    'pipe-impure': 'no-pipe-impure',
    'use-view-encapsulation': 'use-component-view-encapsulation',
    i18n: 'template-i18n',
    'banana-in-box': 'template-banana-in-box',
    'no-template-call-expression': 'template-no-call-expression',
    'templates-no-negated-async': 'template-no-negated-async',
    'trackBy-function': 'template-use-track-by-function',
    'no-attribute-parameter-decorator': 'no-attribute-decorator',
    'max-inline-declarations': 'component-max-inline-declarations',
};
const updateTsLintConfig = () => {
    return (host) => {
        const tsLintPath = '/tslint.json';
        let tsLintJson;
        try {
            tsLintJson = new json_file_1.JSONFile(host, tsLintPath);
        }
        catch (_a) {
            return;
        }
        for (const [existingRule, newRule] of Object.entries(ruleMapping)) {
            const ruleValue = tsLintJson.get(['rules', existingRule]);
            if (ruleValue !== undefined) {
                tsLintJson.remove(['rules', existingRule]);
                tsLintJson.modify(['rules', newRule], ruleValue);
            }
        }
    };
};
exports.updateTsLintConfig = updateTsLintConfig;
const updatePackageJson = () => {
    return (host) => {
        const dependency = {
            type: dependencies_1.NodeDependencyType.Dev,
            name: 'codelyzer',
            version: '^5.0.1',
            overwrite: true,
        };
        dependencies_1.addPackageJsonDependency(host, dependency);
    };
};
exports.updatePackageJson = updatePackageJson;
