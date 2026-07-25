package io.nightfish.potatolib.source.explore

import com.github.michaelbull.result.Result
import io.ktor.client.HttpClient
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.error.WebRequestError
import io.nightfish.lightnovelreader.api.web.explore.AbstractDefaultExplorePageProvider

class PotatoLibExplorePageProvider(
    ktorClient: HttpClient,
    getBookInformation: suspend (id: String) -> Result<BookInformation, WebRequestError>
): AbstractDefaultExplorePageProvider() {
    init {
        registerTapPage(HomeTapPageDataSource(ktorClient, getBookInformation))
    }
}