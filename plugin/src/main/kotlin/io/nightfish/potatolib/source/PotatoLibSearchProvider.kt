package io.nightfish.potatolib.source

import io.nightfish.lightnovelreader.api.web.search.SearchProvider
import io.nightfish.lightnovelreader.api.web.search.SearchResult
import io.nightfish.lightnovelreader.api.web.search.SearchType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PotatoLibSearchProvider: SearchProvider {
    override val searchTypes: List<SearchType> = emptyList()

    override fun search(
        searchType: SearchType,
        keyword: String
    ): Flow<SearchResult> = flow {
    }
}