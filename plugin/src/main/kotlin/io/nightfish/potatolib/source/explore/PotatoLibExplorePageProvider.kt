package io.nightfish.potatolib.source.explore

import io.ktor.client.HttpClient
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.web.explore.AbstractDefaultExplorePageProvider

class PotatoLibExplorePageProvider(
    ktorClient: HttpClient,
    getBookInformation: suspend (id: String) -> BookInformation
): AbstractDefaultExplorePageProvider() {
    init {
        registerTapPage(HomeTapPageDataSource(ktorClient, getBookInformation))
    }
}