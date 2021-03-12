"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const project_graph_1 = require("@nrwl/workspace/src/core/project-graph");
const graph = project_graph_1.createProjectGraph();
(async function () {
    console.info(JSON.stringify(graph, null, 2));
})().catch(err => console.error(err.stack || err));
