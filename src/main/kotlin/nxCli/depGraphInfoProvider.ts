import { createProjectGraph } from '@nrwl/workspace/src/core/project-graph';

const graph = createProjectGraph();

(async function () {
    console.info(JSON.stringify(graph, null, 2))
})().catch(err => console.error(err.stack || err))
