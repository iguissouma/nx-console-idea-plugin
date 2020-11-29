"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const read_schematic_collection_1 = require("./read-schematic-collection");
const path_1 = require("path");
const projectLocation = process.argv[2];
read_schematic_collection_1.readAllSchematicCollections(projectLocation, path_1.join('tools', 'schematics')).then((result) => console.log(JSON.stringify(result)), (error) => console.log("No schematics." + error));
