"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const read_projects_1 = require("./read-projects");
const path = require("path");
const projectLocation = process.argv[2];
const builderName3 = process.argv[3];
const builderName = process.argv[4];
//console.info("builderName3="+  builderName3);
//console.info("builderName="+  builderName);
const parentDir = path.dirname(projectLocation);
(async function () {
    let options = await read_projects_1.readBuilderSchema(parentDir, builderName);
    console.info(JSON.stringify(options, null, 2));
})().catch(err => console.error(err.stack || err));
