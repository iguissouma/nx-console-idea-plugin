import {readAllSchematicCollections} from "./read-schematic-collection";
import {join} from "path";

const projectLocation = process.argv[2];

(async function () {
    let allSchematics = await readAllSchematicCollections(
        projectLocation
    )
    console.info(JSON.stringify(allSchematics, null, 2))

})().catch(err => console.error(err.stack || err))
