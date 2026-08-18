package io.github.darriousliu.han1meviewer.util

import io.github.darriousliu.han1meviewer.core.common.util.LanguageHelper
import io.github.darriousliu.han1meviewer.core.common.util.loadBundledJson
import io.github.darriousliu.han1meviewer.logic.model.SearchOption
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object TagLocalizer {

    private data class TagMappings(
        val labels: Map<String, String>,
        val searchKeys: Map<String, String>,
    )

    private val loadMutex = Mutex()
    private var tagOptions: List<SearchOption>? = null

    private var cachedLanguageTag: String? = null
    private var cachedMappings: TagMappings? = null

    private val cachedTagMappings: TagMappings?
        get() {
            val options = tagOptions ?: return null
            val languageTag = LanguageHelper.preferredLanguage.toLanguageTag()
            val mappings = cachedMappings
            if (cachedLanguageTag == languageTag && mappings != null) return mappings
            return buildTagMappings(options).also {
                cachedLanguageTag = languageTag
                cachedMappings = it
            }
        }

    private suspend fun ensureLoaded() {
        if (tagOptions != null) return
        loadMutex.withLock {
            if (tagOptions != null) return
            val tags = loadBundledJson<Map<String, List<SearchOption>>>(
                "files/search_options/tags.json"
            ).orEmpty().values.flatten()
            val genres = loadBundledJson<List<SearchOption>>(
                "files/search_options/genre.json"
            ).orEmpty()
            tagOptions = tags + genres
        }
    }

    suspend fun localizeTags(tags: List<String>): List<String> {
        ensureLoaded()
        return localizeTagsCached(tags)
    }

    fun localizeTagsCached(tags: List<String>): List<String> {
        if (tags.isEmpty()) return tags
        return tags.map(::localizeTagCached)
    }

    suspend fun localizeTag(tag: String): String {
        ensureLoaded()
        return localizeTagCached(tag)
    }

    fun localizeTagCached(tag: String): String = cachedTagMappings?.labels?.get(tag) ?: tag

    suspend fun resolveSearchKey(tag: String): String {
        ensureLoaded()
        return resolveSearchKeyCached(tag)
    }

    fun resolveSearchKeyCached(tag: String): String =
        cachedTagMappings?.searchKeys?.get(tag) ?: tag

    private fun buildTagMappings(options: List<SearchOption>): TagMappings {
        val labels = mutableMapOf<String, String>()
        val searchKeys = mutableMapOf<String, String>()
        options.forEach { option ->
            val label = option.value.takeIf { it.isNotBlank() } ?: return@forEach
            val searchKey = option.searchKey?.takeIf { it.isNotBlank() } ?: return@forEach
            listOfNotNull(
                option.searchKey,
                option.name,
                option.lang?.zhrCN,
                option.lang?.zhrTW,
                option.lang?.en,
                option.lang?.ja,
            ).forEach { rawTag ->
                labels.getOrPut(rawTag) { label }
                searchKeys.getOrPut(rawTag) { searchKey }
            }
        }
        return TagMappings(labels = labels, searchKeys = searchKeys)
    }
}
