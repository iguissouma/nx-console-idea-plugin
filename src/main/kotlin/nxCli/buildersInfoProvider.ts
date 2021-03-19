import {readBuilderSchema} from "./read-projects";
import * as path from "path";

const projectLocation = process.argv[2];
const builderName = process.argv[4];

const parentDir = path.dirname(projectLocation);
(async function () {
    let options = await readBuilderSchema(
        parentDir,
        builderName
    );
    console.info(JSON.stringify(options, null, 2))

})().catch(err => console.error(err.stack || err))
