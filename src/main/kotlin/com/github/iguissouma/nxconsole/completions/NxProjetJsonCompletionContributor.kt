package com.github.iguissouma.nxconsole.completions

import com.github.iguissouma.nxconsole.cli.config.NxConfigProvider
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiFilePattern
import com.intellij.util.ProcessingContext

class NxProjetJsonCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC, Holder.PATTERN_BUILD_TARGET,
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val project = parameters.position.containingFile.project
                    val nxConfig = NxConfigProvider.getNxConfig(project, project.baseDir) ?: return
                    nxConfig.projects
                        .forEach {
                            val name = it.name
                            it.architect.forEach { (t, u) ->
                                if (u.configurations.isEmpty()) {
                                    result.addElement(LookupElementBuilder.create("$name:$t"))
                                } else {
                                    u.configurations.keys.forEach {
                                        result.addElement(LookupElementBuilder.create("$name:$t:$it"))
                                    }
                                }
                            }
                        }

                }
            }
        )

        extend(CompletionType.BASIC, Holder.PATTERN_CACHEABLE_OPERATIONS,
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val project = parameters.position.containingFile.project
                    val nxConfig = NxConfigProvider.getNxConfig(project, project.baseDir) ?: return
                    nxConfig.projects.flatMap { it.architect.keys }.distinct().forEach {
                        result.addElement(LookupElementBuilder.create(it))
                    }
                }

            }
        )

        extend(CompletionType.BASIC, Holder.PATTERN_DEPENDS_ON,
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val project = parameters.position.containingFile.project
                    val nxConfig = NxConfigProvider.getNxConfig(project, project.baseDir) ?: return
                    val distinct = nxConfig.projects.flatMap { it.architect.keys }.distinct()
                    distinct.forEach {
                        result.addElement(LookupElementBuilder.create("^$it"))
                    }
                    distinct.forEach {
                        result.addElement(LookupElementBuilder.create(it))
                    }
                }

            }
        )
    }

    private object Holder {

        val NX_PROJECT_CONFIG_PATTERN: PsiFilePattern.Capture<JsonFile> = PlatformPatterns
            .psiFile(JsonFile::class.java)
            .withName("project.json")

        val NX_CONFIG_PATTERN: PsiFilePattern.Capture<JsonFile> = PlatformPatterns
            .psiFile(JsonFile::class.java)
            .withName("nx.json")

        val STRING_LITERAL_IN_CONFIG = PlatformPatterns.psiElement(JsonStringLiteral::class.java)
            .inFile(NX_PROJECT_CONFIG_PATTERN)


        val PATTERN_BUILD_TARGET = PlatformPatterns.psiElement()
            .inFile(Holder.NX_PROJECT_CONFIG_PATTERN)
            .afterLeaf(":")
            .withSuperParent(2, JsonProperty::class.java)
            .and(PlatformPatterns.psiElement().withParent(JsonStringLiteral::class.java))
            .withSuperParent(2, PlatformPatterns.psiElement(JsonProperty::class.java).withName("buildTarget"))


        val PATTERN_CACHEABLE_OPERATIONS = PlatformPatterns.psiElement()
            .inFile(NX_CONFIG_PATTERN)
            .withSuperParent(2, JsonArray::class.java)
            .and(PlatformPatterns.psiElement().withParent(JsonStringLiteral::class.java))
            .withSuperParent(3, PlatformPatterns.psiElement(JsonProperty::class.java).withName("cacheableOperations"))


        val PATTERN_DEPENDS_ON = PlatformPatterns.psiElement()
            .inFile(NX_CONFIG_PATTERN)
            .withSuperParent(2, JsonArray::class.java)
            .and(PlatformPatterns.psiElement().withParent(JsonStringLiteral::class.java))
            .withSuperParent(3, PlatformPatterns.psiElement(JsonProperty::class.java).withName("dependsOn"))



    }

}
