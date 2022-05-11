package com.github.iguissouma.nxconsole.readers

fun getExecutors(
    workspacePath: String,
    projects: WorkspaceProjects,
    clearPackageJsonCache: Boolean,
): List<CollectionInfo> {
    return readCollections(workspacePath, ReadCollectionsOptions(projects, clearPackageJsonCache))
        .filter { it.type == CollectionType.executor }
}
