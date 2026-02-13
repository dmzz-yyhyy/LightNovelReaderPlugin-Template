package io.nightfish.potatolib.source

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.github.michaelbull.result.unwrap
import com.github.michaelbull.result.unwrapError
import cxhttp.CxHttp
import cxhttp.CxHttpHelper
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.BookVolumes
import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.book.ChapterInformation
import io.nightfish.lightnovelreader.api.book.MutableBookInformation
import io.nightfish.lightnovelreader.api.book.MutableChapterContent
import io.nightfish.lightnovelreader.api.book.Volume
import io.nightfish.lightnovelreader.api.book.WordCount
import io.nightfish.lightnovelreader.api.content.builder.ContentBuilder
import io.nightfish.lightnovelreader.api.content.builder.image
import io.nightfish.lightnovelreader.api.content.builder.simpleText
import io.nightfish.lightnovelreader.api.util.Cache
import io.nightfish.lightnovelreader.api.web.WebBookDataSource
import io.nightfish.lightnovelreader.api.web.WebDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExplorePageProvider
import io.nightfish.lightnovelreader.api.web.search.SearchProvider
import io.nightfish.potatolib.source.explore.PotatoLibExplorePageProvider
import io.nightfish.potatolib.utils.KotlinSerializationCborConverter
import io.nightfish.potatolib.utils.UserAgentGenerator
import io.nightfish.potatolib.utils.autoReconnectionGet
import io.nightfish.potatolib.utils.selectSingleXPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jsoup.nodes.Element
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Suppress("unused")
@WebDataSource(
    name = "PotatoLib",
    provider = "library.curiousers.org by NightFish"
)
class PotatoLibWebDataSource: WebBookDataSource {
    companion object {
        const val TAG = "PotatoLibWebDataSource"
        const val HOST = "https://library.curiousers.org"
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val offlineMutaStateFlow = MutableStateFlow(true)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    init {
        @Suppress("OPT_IN_USAGE")
        CxHttpHelper.init(
            scope = MainScope(),
            debugLog = true,
            converter = KotlinSerializationCborConverter()
        )
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

    override val id: Int = "PotatoLib".hashCode()

    override val offLine get() = offlineMutaStateFlow.value

    override suspend fun isOffLine(): Boolean  {
        return !CxHttp
            .get(HOST) {
                header("user-agent", UserAgentGenerator.generate())
            }
            .await()
            .isSuccessful
    }

    override val isOffLineFlow: StateFlow<Boolean>  = offlineMutaStateFlow

    override suspend fun getBookInformation(id: String): BookInformation {
        val doc = autoReconnectionGet("$HOST/book/$id") {
            header("user-agent", UserAgentGenerator.generate())
        }.let {
            if (it.isOk) return@let it.unwrap()
            else {
                Log.e(TAG, "failed to get book information (id=$id)")
                it.unwrapError().printStackTrace()
                return BookInformation.empty(id)
            }
        }

        val title = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/h1")?.text() ?: return BookInformation.empty(id)
        val subTitle = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/p[1]")?.text() ?: return BookInformation.empty(id)
        val author = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/div[1]/div[1]/p[2]")?.text() ?: return BookInformation.empty(id)
        val lastUpdate = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/div[1]/div[2]/p[2]")
            ?.text()
            ?.let { LocalDate.parse(it, dateTimeFormatter) }
            ?.atStartOfDay()
            ?: return BookInformation.empty(id)
        val wordCunt = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/div[1]/div[3]/p[2]")
            ?.text()
            ?.split(" ")
            ?.firstOrNull()
            ?.toIntOrNull()
            ?.let { WordCount(it) }
            ?: return BookInformation.empty(id)
        val isCompleted = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/div[1]/div[4]/p[2]")
            ?.text()
            ?.contains("已完结")
            ?: return BookInformation.empty(id)
        val publishingHouse = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/div[1]/div[5]/p[2]")
            ?.text()
            ?: return BookInformation.empty(id)
        val tags = doc.selectXpath("/html/body/main/div/div[1]/div[2]/div/a")
            .map {
                it.text().replaceFirst("#", "")
            }
        val description = doc.selectSingleXPath("/html/body/main/div/div[1]/div[2]/p[2]")
            ?.text()
            ?: return BookInformation.empty(id)
        val coverUrl = doc.selectSingleXPath("/html/body/main/div/div[1]/div[1]/img")
            ?.attr("src")
            ?.let { HOST + it }
            ?.let(Uri::parse)
            ?: return BookInformation.empty(id)

        return MutableBookInformation(
            id = id,
            title = title,
            subtitle = subTitle,
            coverUrl = coverUrl,
            author = author,
            description = description,
            tags = tags,
            publishingHouse = publishingHouse,
            wordCount = wordCunt,
            lastUpdated = lastUpdate,
            isComplete = isCompleted
        )
    }

    override suspend fun getBookVolumes(id: String): BookVolumes {
        val doc = autoReconnectionGet("$HOST/book/$id") {
            header("user-agent", UserAgentGenerator.generate())
        }.let {
            if (it.isOk) return@let it.unwrap()
            else {
                Log.e(TAG, "failed to get book volumes (id=$id)")
                it.unwrapError().printStackTrace()
                return BookVolumes.empty(id)
            }
        }

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
        return BookVolumes(
            bookId = id,
            volumes = volumes
        )
    }

    override suspend fun getChapterContent(chapterId: String, bookId: String): ChapterContent {
        val doc = autoReconnectionGet("$HOST/book/$chapterId") {
            header("user-agent", UserAgentGenerator.generate())
        }.let {
            if (it.isOk) return@let it.unwrap()
            else {
                Log.e(TAG, "failed to get chapter content (bookId=$bookId,chapterId=$chapterId)")
                it.unwrapError().printStackTrace()
                return ChapterContent.empty(chapterId)
            }
        }

        val title = doc.selectSingleXPath("/html/body/main/div/div/header/h1")?.text()
            ?: return ChapterContent.empty(chapterId).also { Log.e(TAG, "failed to get chapter content title") }
        val content = doc.selectSingleXPath("/html/body/main/div/div/article")?.let { contentNode ->
            ContentBuilder().apply {
                for (node in contentNode.childNodes()) {
                    when (node) {
                        is Element if node.selectFirst("img") == null -> simpleText(node.childNodes().first().nodeValue())
                        is Element if node.selectFirst("img") != null -> image((HOST + node.childNodes().first().attr("src").also { println(it) }).toUri())
                    }
                }
            }.build()
        } ?: return ChapterContent.empty(chapterId).also { Log.e(TAG, "failed to get chapter content title") }
        val lastChapter = doc.selectSingleXPath("/html/body/main/div/div/footer/div/a[1]")
            ?.let { node ->
                if (node.attr("aria-disabled") == "true") return@let ""
                return@let node.attr("href").replace("/book/", "")
            } ?: return ChapterContent.empty(chapterId).also { Log.e(TAG, "failed to get last chapter id") }
        val nextChapter = doc.selectSingleXPath("/html/body/main/div/div/footer/div/a[3]")
            ?.let { node ->
                if (node.attr("aria-disabled") == "true") return@let ""
                return@let node.attr("href").replace("/book/", "")
            } ?: return ChapterContent.empty(chapterId).also { Log.e(TAG, "failed to get next chapter id") }

        return MutableChapterContent(
            id = chapterId,
            title = title,
            content = content,
            lastChapter = lastChapter,
            nextChapter = nextChapter
        )
    }

    override val explorePageProvider: ExplorePageProvider = PotatoLibExplorePageProvider(::getBookInformation)
    override val searchProvider: SearchProvider = PotatoLibSearchProvider()
}