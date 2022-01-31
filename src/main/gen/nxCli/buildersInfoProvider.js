"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const read_projects_1 = require("./read-projects");
const path = require("path");
const projectLocation = process.argv[2];
const builderName = process.argv[4];
const parentDir = path.dirname(projectLocation);
(async function () {
    let options = await (0, read_projects_1.readBuilderSchema)(parentDir, builderName);
    console.info(JSON.stringify(options, null, 2));
})().catch(err => console.error(err.stack || err));
