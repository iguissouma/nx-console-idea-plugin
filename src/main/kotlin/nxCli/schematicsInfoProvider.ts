import {readAllSchematicCollections} from "./read-schematic-collection";
import {join} from "path";

const projectLocation = process.argv[2];

readAllSchematicCollections(
    projectLocation,
    join('tools', 'schematics')
).then(
    (result) => console.log(JSON.stringify(result)),
    (error) => console.log("No schematics." + error),
);
