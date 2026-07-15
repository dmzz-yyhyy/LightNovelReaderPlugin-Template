package io.nightfish.potatolib

import android.util.Log
import io.nightfish.lightnovelreader.api.image.ImagePostProcessingPipeline
import io.nightfish.lightnovelreader.api.image.ImageTransPostProcessingManagerApi
import io.nightfish.lightnovelreader.api.plugin.LightNovelReaderPlugin
import io.nightfish.lightnovelreader.api.plugin.Plugin
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi
import io.nightfish.potatolib.image.PotatoImageTransformation
import io.nightfish.potatolib.utils.toId

@Suppress("unused")
@Plugin(
    version = BuildConfig.VERSION_CODE,
    name = "PotatoLib",
    versionName = BuildConfig.VERSION_NAME,
    author = "NightFish",
    description = "土豆文库数据源",
    updateUrl = "",
    apiVersion = 4
)
class PotatoLibPlugin(
    val userDataRepositoryApi: UserDataRepositoryApi,
    val imageTransPostProcessingManagerApi: ImageTransPostProcessingManagerApi
) : LightNovelReaderPlugin {
    companion object {
        const val TAG = "PotatoLibPlugin"
    }

    override fun onLoad() {
        Log.i(TAG, "土豆文库插件已加载")
        imageTransPostProcessingManagerApi.registerImageTransformation(
            ImagePostProcessingPipeline.bookCover,
            "potato_image_tansformatinon".toId(),
            PotatoImageTransformation()
        )
        imageTransPostProcessingManagerApi.registerImageTransformation(
            ImagePostProcessingPipeline.imageComponent,
            "potato_image_tansformatinon".toId(),
            PotatoImageTransformation()
        )
    }
}

