package io.nightfish.potatolib

import android.util.Log
import io.nightfish.lightnovelreader.api.plugin.LightNovelReaderPlugin
import io.nightfish.lightnovelreader.api.plugin.Plugin
import io.nightfish.lightnovelreader.api.userdata.UserDataRepositoryApi

@Suppress("unused")
@Plugin(
    version = BuildConfig.VERSION_CODE,
    name = "PotatoLib",
    versionName = BuildConfig.VERSION_NAME,
    author = "NightFish",
    description = "土豆文库数据源",
    updateUrl = "",
    apiVersion = 2
)
class PotatoLibPlugin(
    val userDataRepositoryApi: UserDataRepositoryApi
) : LightNovelReaderPlugin {
    companion object {
        const val TAG = "PotatoLibPlugin"
    }

    override fun onLoad() {
        Log.i(TAG, "土豆文库插件已加载")
    }
}

