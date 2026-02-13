package io.nightfish.potatolib.source

import android.net.Uri
import android.util.Log
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import io.nightfish.lightnovelreader.api.book.MutableBookInformation
import io.nightfish.lightnovelreader.api.book.WordCount
import io.nightfish.lightnovelreader.api.util.local
import io.nightfish.lightnovelreader.api.web.search.SearchProvider
import io.nightfish.lightnovelreader.api.web.search.SearchResult
import io.nightfish.lightnovelreader.api.web.search.SearchType
import io.nightfish.potatolib.source.PotatoLibWebDataSource.Companion.HOST
import io.nightfish.potatolib.source.PotatoLibWebDataSource.Companion.TAG
import io.nightfish.potatolib.utils.UserAgentGenerator
import io.nightfish.potatolib.utils.autoReconnectionGet
import io.nightfish.potatolib.utils.selectSingleXPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.TextNode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PotatoLibSearchProvider: SearchProvider {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd 更新")

    override val searchTypes: List<SearchType> = listOf(
        SearchType("title", "按照标题搜索".local(), "请输入标题".local()),
        SearchType("author", "按照作者搜索".local(), "请输入作者名称".local())
    )

    override fun search(
        searchType: SearchType,
        keyword: String
    ): Flow<SearchResult> = flow {
        val doc = autoReconnectionGet("$HOST/search?type=${searchType.type}&q=$keyword") {
            header("user-agent", UserAgentGenerator.generate())
        }.let {
            if (it.isOk) return@let it.unwrap()
            else {
                Log.e(TAG, "failed to get search request")
                it.unwrapError().printStackTrace()
                emit(SearchResult.Error(it.unwrapError().message.toString()))
                return@flow
            }
        }
        val elements = doc.selectXpath("/html/body/main/div/div/a[contains(@class,\"transition-all\")]")
        if (elements.count() == 1) {
            elements.first()?.attr("href")?.replace("/book/", "")?.let {
                emit(SearchResult.SingleBook(it))
                return@flow
            }
        }
        for (element in elements) {
            val id = element.attr("href").replace("/book/", "")
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

            val bookInformation = MutableBookInformation(
                id = id,
                title = title,
                subtitle = subTitle,
                coverUrl = coverUrl,
                author = author,
                description = description,
                tags = tags,
                publishingHouse = publishingHouse,
                wordCount = wordCount,
                lastUpdated = lastUpdate,
                isComplete = isCompleted
            )
            emit(SearchResult.MultipleBook(bookInformation))
        }
        emit(SearchResult.End())
    }
}