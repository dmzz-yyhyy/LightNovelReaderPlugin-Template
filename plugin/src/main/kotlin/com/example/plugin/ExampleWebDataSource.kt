package com.example.plugin

import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.BookVolumes
import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.web.WebBookDataSource
import io.nightfish.lightnovelreader.api.web.WebDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExploreExpandedPageDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExplorePageDataSource
import kotlinx.coroutines.flow.Flow

@WebDataSource(
    name = "ExampleWebDataSource",
    provider = "example.com"
)
class ExampleWebDataSource: WebBookDataSource {
    override val id: Int
        get() = "example".hashCode()
    override val offLine: Boolean
        get() = TODO("Not yet implemented")
    override val isOffLineFlow: Flow<Boolean>
        get() = TODO("Not yet implemented")
    override val explorePageIdList: List<String>
        get() = TODO("Not yet implemented")
    override val explorePageDataSourceMap: Map<String, ExplorePageDataSource>
        get() = TODO("Not yet implemented")
    override val exploreExpandedPageDataSourceMap: Map<String, ExploreExpandedPageDataSource>
        get() = TODO("Not yet implemented")
    override val searchTypeMap: Map<String, String>
        get() = TODO("Not yet implemented")
    override val searchTipMap: Map<String, String>
        get() = TODO("Not yet implemented")
    override val searchTypeIdList: List<String>
        get() = TODO("Not yet implemented")

    override suspend fun isOffLine(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getBookInformation(id: String): BookInformation {
        TODO("Not yet implemented")
    }

    override suspend fun getBookVolumes(id: String): BookVolumes {
        TODO("Not yet implemented")
    }

    override suspend fun getChapterContent(
        chapterId: String,
        bookId: String
    ): ChapterContent {
        TODO("Not yet implemented")
    }

    override fun search(
        searchType: String,
        keyword: String
    ): Flow<List<BookInformation>> {
        TODO("Not yet implemented")
    }

    override fun stopAllSearch() {
        TODO("Not yet implemented")
    }

}