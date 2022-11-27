package com.github.iguissouma.nxconsole.readers

data class GetExecutorsOptions(val includeHidden: Boolean = false)
fun getExecutors(
    workspacePath: String,
    projects: WorkspaceProjects,
    clearPackageJsonCache: Boolean,
    options: GetExecutorsOptions = GetExecutorsOptions()
): List<CollectionInfo> {
    return readCollections(
        workspacePath = workspacePath,
        options = ReadCollectionsOptions(projects, clearPackageJsonCache, options.includeHidden, false)
    ).filter { it.type == CollectionType.executor }
}
