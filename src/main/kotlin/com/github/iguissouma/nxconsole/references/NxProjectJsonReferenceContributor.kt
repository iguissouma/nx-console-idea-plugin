package com.github.iguissouma.nxconsole.references

import com.github.iguissouma.nxconsole.references.NxProjectJsonReferenceContributor.Holder.PATTERN_ROOT
import com.github.iguissouma.nxconsole.references.NxProjectJsonReferenceContributor.Holder.PATTERN_SOURCE_ROOT
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.util.Condition
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiFilePattern
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.util.ProcessingContext

class NxProjectJsonReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {

        //val referenceProvider =
        //    JSFileReferencesUtil.getSimpleReferencesPathProvider(TypeScriptUtil.TYPESCRIPT_EXTENSIONS)

        val directoryReferenceProvider = object : PsiReferenceProvider() {

            override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {

                if (element is JsonStringLiteral) {
                    val value: String = element.value
                    return object : FileReferenceSet(value, element, 1, null, false) {

                        override fun getDefaultContexts(): MutableCollection<PsiFileSystemItem> {
                            val manager = element.containingFile.manager
                            return mutableSetOf(manager.findDirectory(element.project.baseDir) as PsiFileSystemItem)
                        }

                        override fun getReferenceCompletionFilter(): Condition<PsiFileSystemItem?>? {
                            return DIRECTORY_FILTER
                        }

                    }.allReferences.toList().toTypedArray()
                }
                return PsiReference.EMPTY_ARRAY
            }

        }

        registrar.registerReferenceProvider<PsiElement>(
            StandardPatterns.or(
                PATTERN_SOURCE_ROOT,
                PATTERN_ROOT
            ), directoryReferenceProvider, 100.0
        )

    }


    private object Holder {

        private val NX_PROJECT_CONFIG_PATTERN: PsiFilePattern.Capture<JsonFile> = PlatformPatterns
            .psiFile(JsonFile::class.java)
            .withName("project.json")

        val STRING_LITERAL_IN_CONFIG = PlatformPatterns.psiElement(JsonStringLiteral::class.java)
            .inFile(NX_PROJECT_CONFIG_PATTERN)

        val PATTERN_SOURCE_ROOT = STRING_LITERAL_IN_CONFIG
            .withSuperParent(1, PlatformPatterns.psiElement(JsonProperty::class.java).withName("sourceRoot"))

        val PATTERN_ROOT = STRING_LITERAL_IN_CONFIG
            .withSuperParent(1, PlatformPatterns.psiElement(JsonProperty::class.java).withName("root"))


        val PATTERN_ROOT_DIRS_ARRAY = STRING_LITERAL_IN_CONFIG
            .withSuperParent(2, PlatformPatterns.psiElement(JsonProperty::class.java).withName("rootDirs"))

    }
}
