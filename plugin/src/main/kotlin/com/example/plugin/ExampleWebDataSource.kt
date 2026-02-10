package com.example.plugin

import com.example.plugin.utils.KotlinSerializationCborConverter
import cxhttp.CxHttpHelper
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.BookVolumes
import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.web.WebBookDataSource
import io.nightfish.lightnovelreader.api.web.WebDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExploreExpandedPageDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExplorePageProvider
import io.nightfish.lightnovelreader.api.web.explore.ExploreTapPageDataSource
import io.nightfish.lightnovelreader.api.web.search.SearchProvider
import io.nightfish.lightnovelreader.api.web.search.SearchResult
import io.nightfish.lightnovelreader.api.web.search.SearchType
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

@Suppress("unused")
@WebDataSource(
    name = "ExampleWebDataSource",
    provider = "example.com"
)
class ExampleWebDataSource: WebBookDataSource {
    init {
        @Suppress("OPT_IN_USAGE")
        CxHttpHelper.init(scope=MainScope(), debugLog=true, converter = KotlinSerializationCborConverter())
    }
    override val id: Int = "example".hashCode()
    override suspend fun isOffLine(): Boolean = true

    override val offLine: Boolean = true
    override val isOffLineFlow: StateFlow<Boolean> = MutableStateFlow(true)
    override val explorePageProvider: ExplorePageProvider = object: ExplorePageProvider.DefaultExplorePageProvider {
        override val explorePageIdList: List<String> = emptyList()
        override val exploreTapPageDataSourceMap: Map<String, ExploreTapPageDataSource> = emptyMap()
        override val exploreExpandedPageDataSourceMap: Map<String, ExploreExpandedPageDataSource> = emptyMap()
    }
    override val searchProvider: SearchProvider = object: SearchProvider {
        override val searchTypes: List<SearchType> = emptyList()

        override fun search(
            searchType: SearchType,
            keyword: String
        ): Flow<SearchResult> = flow {
        }

    }
    override suspend fun getBookInformation(id: String): BookInformation = BookInformation.empty()

    override suspend fun getBookVolumes(id: String): BookVolumes = BookVolumes.empty("")

    override suspend fun getChapterContent(chapterId: String, bookId: String): ChapterContent = ChapterContent.empty()
}