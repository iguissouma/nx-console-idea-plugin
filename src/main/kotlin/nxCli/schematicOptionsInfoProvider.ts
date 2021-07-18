import {readSchematicOptions} from "./read-schematic-collection";
import * as path from "path";

const projectLocation = process.argv[2];
const collectionName = process.argv[4];
const schematicName = process.argv[5];

const parentDir = path.dirname(projectLocation);
(async function () {
    let options = await readSchematicOptions(
        parentDir,
        collectionName,
        schematicName
    );
    console.info(JSON.stringify(options, null, 2))

})().catch(err => console.error(err.stack || err))
