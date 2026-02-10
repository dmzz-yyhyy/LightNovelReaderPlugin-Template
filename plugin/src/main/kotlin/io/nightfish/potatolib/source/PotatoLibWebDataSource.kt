package io.nightfish.potatolib.source

import cxhttp.CxHttp
import cxhttp.CxHttpHelper
import io.nightfish.lightnovelreader.api.book.BookInformation
import io.nightfish.lightnovelreader.api.book.BookVolumes
import io.nightfish.lightnovelreader.api.book.ChapterContent
import io.nightfish.lightnovelreader.api.web.WebBookDataSource
import io.nightfish.lightnovelreader.api.web.WebDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExplorePageProvider
import io.nightfish.lightnovelreader.api.web.search.SearchProvider
import io.nightfish.potatolib.utils.KotlinSerializationCborConverter
import io.nightfish.potatolib.utils.UserAgentGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Suppress("unused")
@WebDataSource(
    name = "PotatoLib",
    provider = "library.curiousers.org by NightFish"
)
class PotatoLibWebDataSource: WebBookDataSource {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val offlineMutaStateFlow = MutableStateFlow(true)

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

    override val isOffLineFlow: StateFlow<Boolean> = offlineMutaStateFlow

    override val explorePageProvider: ExplorePageProvider = PotatoLibExplorePageProvider()
    override val searchProvider: SearchProvider = PotatoLibSearchProvider()
    override suspend fun getBookInformation(id: String): BookInformation = BookInformation.empty()

    override suspend fun getBookVolumes(id: String): BookVolumes = BookVolumes.empty("")

    override suspend fun getChapterContent(chapterId: String, bookId: String): ChapterContent = ChapterContent.empty()
}