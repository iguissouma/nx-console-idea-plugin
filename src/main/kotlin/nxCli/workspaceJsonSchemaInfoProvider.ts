import * as path from "path";
import {setupSchema} from "./workspace-json-schema";

const workspaceJsonPath = process.argv[2];
const schemaVersion = process.argv[4];

(async function () {
    let schema = await setupSchema(
        workspaceJsonPath,
        +schemaVersion
    );
    console.info(schema)
})().catch(err => console.error(err.stack || err))
