"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const workspace_json_schema_1 = require("./workspace-json-schema");
const workspaceJsonPath = process.argv[2];
const schemaVersion = process.argv[4];
(async function () {
    let schema = await workspace_json_schema_1.setupSchema(workspaceJsonPath, +schemaVersion);
    console.info(schema);
})().catch(err => console.error(err.stack || err));
