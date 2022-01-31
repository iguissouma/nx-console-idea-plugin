"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const read_schematic_collection_1 = require("./read-schematic-collection");
const path = require("path");
const projectLocation = process.argv[2];
const collectionName = process.argv[4];
const schematicName = process.argv[5];
const parentDir = path.dirname(projectLocation);
(async function () {
    let options = await (0, read_schematic_collection_1.readSchematicOptions)(parentDir, collectionName, schematicName);
    console.info(JSON.stringify(options, null, 2));
})().catch(err => console.error(err.stack || err));
