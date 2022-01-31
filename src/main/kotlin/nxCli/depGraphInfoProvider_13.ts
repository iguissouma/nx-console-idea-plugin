import { createProjectGraphAsync } from '@nrwl/workspace/src/core/project-graph';

(async function () {
    const graph = await createProjectGraphAsync();
    console.info(JSON.stringify(graph, null, 2))
})().catch(err => console.error(err.stack || err))
