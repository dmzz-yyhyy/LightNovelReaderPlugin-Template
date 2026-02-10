package io.nightfish.potatolib.source

import cxhttp.CxHttpHelper
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.BookVolumes
import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.web.WebBookDataSource
import io.nightfish.lightnovelreader.api.web.WebDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExplorePageProvider
import io.nightfish.lightnovelreader.api.web.search.SearchProvider
import io.nightfish.potatolib.utils.KotlinSerializationCborConverter
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Suppress("unused")
@WebDataSource(
    name = "PotatoLib",
    provider = "library.curiousers.org by NightFish"
)
class PotatoLibWebDataSource: WebBookDataSource {
    init {
        @Suppress("OPT_IN_USAGE")
        CxHttpHelper.init(scope=MainScope(), debugLog=true, converter = KotlinSerializationCborConverter())
    }
    override val id: Int = "PotatoLib".hashCode()
    override suspend fun isOffLine(): Boolean = true
    override val offLine: Boolean = true

    override val isOffLineFlow: StateFlow<Boolean> = MutableStateFlow(true)
    override val explorePageProvider: ExplorePageProvider = PotatoLibExplorePageProvider()
    override val searchProvider: SearchProvider = PotatoLibSearchProvider()
    override suspend fun getBookInformation(id: String): BookInformation = BookInformation.empty()

    override suspend fun getBookVolumes(id: String): BookVolumes = BookVolumes.empty("")

    override suspend fun getChapterContent(chapterId: String, bookId: String): ChapterContent = ChapterContent.empty()
}