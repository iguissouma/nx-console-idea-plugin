"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const read_schematic_collection_1 = require("./read-schematic-collection");
const path_1 = require("path");
const projectLocation = process.argv[2];
(async function () {
    let allSchematics = await read_schematic_collection_1.readAllSchematicCollections(projectLocation, path_1.join('tools', 'schematics'));
    console.info(JSON.stringify(allSchematics, null, 2));
})().catch(err => console.error(err.stack || err));
