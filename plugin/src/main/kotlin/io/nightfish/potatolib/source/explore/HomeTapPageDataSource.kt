package io.nightfish.potatolib.source.explore

import android.net.Uri
import android.util.Log
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.error.WebRequestError
import io.nightfish.lightnovelreader.api.explore.ExploreBooksRow
import io.nightfish.lightnovelreader.api.explore.ExploreDisplayBook
import io.nightfish.lightnovelreader.api.web.explore.ExploreTapPageDataSource
import io.nightfish.potatolib.source.PotatoLibWebDataSource.Companion.HOST
import io.nightfish.potatolib.source.PotatoLibWebDataSource.Companion.TAG
import io.nightfish.potatolib.utils.safeGet
import io.nightfish.potatolib.utils.selectSingleXPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.TextNode

class HomeTapPageDataSource(
    val ktorClient: HttpClient,
    val getBookInformation: suspend (id: String) -> Result<BookInformation, WebRequestError>
): ExploreTapPageDataSource {
    override val title = "首页"

    override fun getRowsFlow(): Flow<List<ExploreBooksRow>> = flow {
        val rows = mutableListOf<ExploreBooksRow>()
        val doc = ktorClient.safeGet(HOST)
            .onErr {
                Log.e(TAG, "failed to get home page")
                it.printStackTrace()
                return@flow
            }
            .onOk {
                if (it.status.isSuccess()) return@onOk
                Log.e(TAG, "failed to get home page with state: ${it.status}")
                return@flow
            }.component1()?.let { Jsoup.parse(it.bodyAsText()) } ?: return@flow

        rows.add(getBestOfYear(doc))
        emit(rows)
        rows.add(getLastUpdate(doc))
        emit(rows)
    }

    private fun getBestOfYear(doc: Document): ExploreBooksRow {
        val exploreDisplayBooks = doc.selectXpath("/html/body/main/div/section/div/a")
            .mapNotNull { element ->
                val id = element.attr("href").replace("/book/", "")
                val title = element.selectSingleXPath("./div[2]/h3")
                    ?.childNodes()
                    ?.first { it is TextNode }
                    ?.nodeValue()
                    ?: return@mapNotNull null
                val author = element.selectSingleXPath("./div[2]/p[1]/span[1]")
                    ?.text()
                    ?: return@mapNotNull null
                val coverUrl = element.selectSingleXPath("./div[1]/img")
                    ?.attr("src")
                    ?.let { HOST + it }
                    ?.let(Uri::parse)
                    ?: return@mapNotNull null

                ExploreDisplayBook(
                    id = id,
                    title = title,
                    author = author,
                    coverUri = coverUrl
                )
            }
        return ExploreBooksRow(
            title = "年度最佳",
            bookList = exploreDisplayBooks,
            expandable = false
        )
    }

    private suspend fun getLastUpdate(doc: Document): ExploreBooksRow {
        val exploreDisplayBooks = doc.selectXpath("/html/body/main/div/div/div/section[1]/div/a")
            .mapNotNull { element ->
                element.attr("href").replace("/book/", "")
            }.mapNotNull {
                getBookInformation.invoke(it).get()
            }.map {
                ExploreDisplayBook(
                    id = it.id,
                    title = it.title,
                    author = it.author,
                    coverUri = it.coverUri
                )
            }
        return ExploreBooksRow(
            title = "最近更新",
            bookList = exploreDisplayBooks,
            expandable = false
        )
    }
}