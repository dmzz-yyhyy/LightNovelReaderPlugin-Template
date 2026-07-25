package io.nightfish.potatolib.source

import android.net.Uri
import androidx.core.net.toUri
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.BookVolumes
import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.book.ChapterInformation
import io.nightfish.lightnovelreader.api.book.Volume
import io.nightfish.lightnovelreader.api.book.WordCount
import io.nightfish.lightnovelreader.api.content.builder.ContentBuilder
import io.nightfish.lightnovelreader.api.content.builder.image
import io.nightfish.lightnovelreader.api.content.builder.simpleText
import io.nightfish.lightnovelreader.api.error.WebRequestError
import io.nightfish.lightnovelreader.api.error.mapAsWebRequestError
import io.nightfish.lightnovelreader.api.util.Cache
import io.nightfish.lightnovelreader.api.web.WebBookDataSource
import io.nightfish.lightnovelreader.api.web.WebDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExplorePageProvider
import io.nightfish.lightnovelreader.api.web.search.SearchProvider
import io.nightfish.potatolib.source.explore.PotatoLibExplorePageProvider
import io.nightfish.potatolib.utils.safeGet
import io.nightfish.potatolib.utils.selectSingleXPath
import io.nightfish.potatolib.utils.toId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.io.EOFException
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.ConnectException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Suppress("unused")
@WebDataSource(
    name = "PotatoLib",
    provider = "library.nariko.org by NightFish"
)
class PotatoLibWebDataSource: WebBookDataSource {
    companion object {
        const val TAG = "PotatoLibWebDataSource"
        const val HOST = "https://library.nariko.org"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val offlineMutaStateFlow = MutableStateFlow(true)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    val ktorClient = HttpClient(CIO) {
        install(UserAgent) {
            agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/125.0.0.0 Safari/537.36"
        }

        install(HttpCookies)
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            exponentialDelay()
            retryIf { _, response ->
                !response.status.isSuccess()
            }
            retryOnExceptionIf { _, cause ->
                cause is EOFException || cause is ConnectException
            }
        }
        install(HttpTimeout)
        install(Logging) {
            logger = Logger.ANDROID
            level = LogLevel.INFO
        }
    }

    override fun onLoad() {
        // 初始化离线判断热流
        coroutineScope.launch {
            while (true) {
                offlineMutaStateFlow.value = isOffLine()
                if (offLine) {
                    // 对于已经是离线状态下, 应当尽快尝试重试
                    delay(1.seconds)
                } else {
                    // 对于已经是再线状态下, 不必高速更新状态
                    delay(40.seconds)
                }
            }
        }
    }

    override val permits = 3
    override val cache = Cache(
        timeout = 1.hours.toInt(DurationUnit.MILLISECONDS) // 对于大多数的数据源来说, 1小时的缓存有效时长是非常足够了
    )

    override val id = "potato_lib".toId()

    override val offLine get() = offlineMutaStateFlow.value

    override suspend fun isOffLine(): Boolean  {
        return !ktorClient.get(HOST).status.isSuccess()
    }

    override val isOffLineFlow: StateFlow<Boolean>  = offlineMutaStateFlow

    override suspend fun getBookInformation(id: String) = coroutineBinding {
        val doc = ktorClient.safeGet("$HOST/book/$id")
            .mapAsWebRequestError("网络请求失败", "请求书本详情信息时失败(id=$id)")
            .bind()
            .let { Jsoup.parse(it.bodyAsText()) }

        val title = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/h1")?.text()
            ?: Err(WebRequestError("解析失败", "解析书本标题时失败(id=$id)")).bind()
        val subTitle = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/p[1]")?.text()
            ?: Err(WebRequestError("解析失败", "解析书本副标题时失败(id=$id)")).bind()
        val author = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/div[1]/div[1]/p[2]")?.text()
            ?: Err(WebRequestError("解析失败", "解析书本作者时失败(id=$id)")).bind()
        val lastUpdate = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/div[1]/div[2]/p[2]")
            ?.text()
            ?.let { LocalDate.parse(it, dateTimeFormatter) }
            ?.atStartOfDay()
            ?: Err(WebRequestError("解析失败", "解析书本上次更新时间时失败(id=$id)")).bind()
        val wordCunt = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/div[1]/div[3]/p[2]")
            ?.text()
            ?.split(" ")
            ?.firstOrNull()
            ?.toIntOrNull()
            ?.let { WordCount(it) }
            ?: Err(WebRequestError("解析失败", "解析书本字数时失败(id=$id)")).bind()
        val isCompleted = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/div[1]/div[4]/p[2]")
            ?.text()
            ?.contains("已完结")
            ?: Err(WebRequestError("解析失败", "解析书本完结状态时失败(id=$id)")).bind()
        val publishingHouse = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/div[1]/div[5]/p[2]")
            ?.text()
            ?: Err(WebRequestError("解析失败", "解析书本出版社时失败(id=$id)")).bind()
        val tags = doc.selectXpath("/html/body/main/div/div[1]/div[2]/div/a")
            .map {
                it.text().replaceFirst("#", "")
            }
        val description = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/p[2]")
            ?.text()
            ?: Err(WebRequestError("解析失败", "解析书本描述时失败(id=$id)")).bind()
        val coverUri = doc.selectSingleXPath("/html/body/main/div/div[1]/div[1]/img")
            ?.attr("src")
            ?.let { HOST + it }
            ?.let(Uri::parse)
            ?: Err(WebRequestError("解析失败", "解析书本封面时失败(id=$id)")).bind()

        return@coroutineBinding BookInformation(
            id = id,
            title = title,
            subtitle = subTitle,
            coverUri = coverUri,
            author = author,
            description = description,
            tags = tags,
            publishingHouse = publishingHouse,
            wordCount = wordCunt,
            lastUpdated = lastUpdate,
            isComplete = isCompleted
        )
    }

    override suspend fun getBookVolumes(id: String) = coroutineBinding {
        val doc = ktorClient.safeGet("$HOST/book/$id")
            .mapAsWebRequestError("网络请求失败", "请求书本目录时失败(id=$id)")
            .bind()
            .let { Jsoup.parse(it.bodyAsText()) }

        val volumes = doc.selectXpath("/html/body/main/div/div[2]/div")
            .mapIndexed { index, volumeNode ->
                val volumeTitle = volumeNode.select("h3").text()
                val chapters = volumeNode.select("div > a")
                    .map { chapterNode ->
                        ChapterInformation(
                            id = chapterNode.attr("href").replaceFirst("/book/", ""),
                            title = chapterNode.text()
                        )
                    }
                return@mapIndexed Volume(
                    volumeId = id + index.toString(),
                    volumeTitle = volumeTitle,
                    chapters = chapters,
                )
            }
        return@coroutineBinding BookVolumes(
            bookId = id,
            volumes = volumes
        )
    }

    override suspend fun getChapterContent(chapterId: String, bookId: String) = coroutineBinding {
        val doc = ktorClient.safeGet("$HOST/book/$chapterId")
            .mapAsWebRequestError("网络请求失败", "请求章节内容时失败(chapterId=$chapterId,bookId=$bookId)")
            .bind()
            .let { Jsoup.parse(it.bodyAsText()) }

        val title = doc.selectSingleXPath("/html/body/main/div/div/header/h1")?.text()
            ?: Err(WebRequestError("解析失败", "解析章节标题时失败(id=$id)")).bind()
        val content = doc.selectSingleXPath("/html/body/main/div/div/article")?.let { contentNode ->
            ContentBuilder().apply {
                for (node in contentNode.childNodes()) {
                    when (node) {
                        is Element if node.selectFirst("img") == null -> simpleText(node.childNodes().first().nodeValue())
                        is Element if node.selectFirst("img") != null -> image((HOST + node.childNodes().first().attr("src").also { println(it) }).toUri())
                    }
                }
            }.build()
        } ?: Err(WebRequestError("解析失败", "解析章节内容时失败(id=$id)")).bind()
        val prevChapter = doc.selectSingleXPath("/html/body/main/div/div/footer/div/a[1]")
            ?.let { node ->
                if (node.attr("aria-disabled") == "true") return@let ""
                return@let node.attr("href").replace("/book/", "")
            }
        val nextChapter = doc.selectSingleXPath("/html/body/main/div/div/footer/div/a[3]")
            ?.let { node ->
                if (node.attr("aria-disabled") == "true") return@let ""
                return@let node.attr("href").replace("/book/", "")
            }

        return@coroutineBinding ChapterContent(
            id = chapterId,
            title = title,
            content = content,
            prevChapter = prevChapter,
            nextChapter = nextChapter
        )
    }

    override val explorePageProvider: ExplorePageProvider = PotatoLibExplorePageProvider(ktorClient, ::getBookInformation)
    override val searchProvider: SearchProvider = PotatoLibSearchProvider(this, ktorClient)
}