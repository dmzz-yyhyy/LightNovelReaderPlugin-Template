package io.nightfish.potatolib.source

import android.net.Uri
import android.util.Log
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.WordCount
import io.nightfish.lightnovelreader.api.util.local
import io.nightfish.lightnovelreader.api.web.search.SearchProvider
import io.nightfish.lightnovelreader.api.web.search.SearchResult
import io.nightfish.lightnovelreader.api.web.search.SearchType
import io.nightfish.potatolib.source.PotatoLibWebDataSource.Companion.HOST
import io.nightfish.potatolib.source.PotatoLibWebDataSource.Companion.TAG
import io.nightfish.potatolib.utils.safeGet
import io.nightfish.potatolib.utils.selectSingleXPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.Jsoup
import org.jsoup.nodes.TextNode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PotatoLibSearchProvider(
    val potatoLibWebDataSource: PotatoLibWebDataSource,
    val ktorClient: HttpClient
): SearchProvider {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd 更新")

    override val searchTypes: List<SearchType> = listOf(
        SearchType("title", "按照标题搜索".local(), "请输入标题".local()),
        SearchType("author", "按照作者搜索".local(), "请输入作者名称".local())
    )

    override fun search(
        searchType: SearchType,
        keyword: String
    ): Flow<SearchResult> = flow {

        val doc = ktorClient.safeGet("$HOST/search?type=${searchType.type}&q=$keyword")
            .onErr {
                Log.e(TAG, "failed to get search request")
                it.printStackTrace()
                return@flow
            }
            .onOk {
                if (it.status.isSuccess()) return@onOk
                Log.e(TAG, "failed to get search request with state: ${it.status}")
                return@flow
            }.component1()?.let { Jsoup.parse(it.bodyAsText()) } ?: return@flow

        val elements = doc.selectXpath("/html/body/main/div/div/a[contains(@class,\"transition-all\")]")
        if (elements.count() == 1) {
            elements.first()?.attr("href")?.replace("/book/", "")?.let {
                emit(SearchResult.SingleBook(it))
                return@flow
            }
        }
        for (element in elements) {
            val id = element.attr("href").replace("/book/", "")
            //仅需提供书本id
            emit(SearchResult.MultipleBook(id))
            val title = element.selectSingleXPath("./div[2]/h3")
                ?.childNodes()
                ?.first { it is TextNode }
                ?.nodeValue()
                ?: continue
            val subTitle = element.selectSingleXPath("./div[2]/h3/span")
                ?.text()
                ?.replace(" · ", "")
                ?: ""
            val author = element.selectSingleXPath("./div[2]/p[1]/span[1]")
                ?.text()
                ?: continue
            val lastUpdate = element.selectSingleXPath("./div[2]/div[2]/span[3]")
                ?.text()
                ?.let { LocalDate.parse(it, dateTimeFormatter) }
                ?.atStartOfDay()
                ?: continue
            val wordCount = element.selectSingleXPath("./div[2]/div[2]/span[2]")
                ?.text()
                ?.split(" ")
                ?.firstOrNull()
                ?.toIntOrNull()
                ?.let { WordCount(it) }
                ?: continue
            val publishingHouse = element.selectSingleXPath("./div[2]/p[1]/span[3]")
                ?.text()
                ?: continue
            val isCompleted = element.selectSingleXPath("./div[2]/div[2]/span[1]")
                ?.text()
                ?.contains("已完结")
                ?: continue
            val tags = element.selectXpath("./div[2]/div[1]/span[1]")
                .map {
                    it.text().replaceFirst("#", "")
                }
            val description = element.selectSingleXPath("./div[2]/p[2]")
                ?.text()
                ?: continue
            val coverUrl = element.selectSingleXPath("./div[1]/img")
                ?.attr("src")
                ?.let { HOST + it }
                ?.let(Uri::parse)
                ?: continue
            //如果探索页面本身可以提供足够多的数据, 可以将其存放至缓存里, 可以减少重复请求次数
            val bookInformation = BookInformation(
                id = id,
                title = title,
                subtitle = subTitle,
                coverUri = coverUrl,
                author = author,
                description = description,
                tags = tags,
                publishingHouse = publishingHouse,
                wordCount = wordCount,
                lastUpdated = lastUpdate,
                isComplete = isCompleted
            )
            potatoLibWebDataSource.cache.cache(id.hashCode()) {
                bookInformation
            }
        }
        emit(SearchResult.End())
    }
}