package io.nightfish.potatolib.source.explore

import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.web.explore.AbstractDefaultExplorePageProvider

class PotatoLibExplorePageProvider(
    getBookInformation: suspend (id: String) -> BookInformation
): AbstractDefaultExplorePageProvider() {
    init {
        registerTapPage(HomeTapPageDataSource(getBookInformation))
    }
}