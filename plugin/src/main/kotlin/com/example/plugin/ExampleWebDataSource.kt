package com.example.plugin

import com.example.plugin.utils.KotlinSerializationCborConverter
import cxhttp.CxHttpHelper
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.BookVolumes
import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.web.WebBookDataSource
import io.nightfish.lightnovelreader.api.web.WebDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExplorePageProvider
import io.nightfish.lightnovelreader.api.web.search.SearchProvider
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.StateFlow

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
    override val id: Int
        get() = "example".hashCode()
    override val offLine: Boolean
        get() = TODO("Not yet implemented")
    override val isOffLineFlow: StateFlow<Boolean>
        get() = TODO("Not yet implemented")

    override val searchProvider: SearchProvider
        get() = TODO("Not yet implemented")
    override val explorePageProvider: ExplorePageProvider
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

}