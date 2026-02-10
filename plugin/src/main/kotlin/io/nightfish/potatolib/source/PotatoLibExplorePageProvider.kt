package io.nightfish.potatolib.source

import io.nightfish.lightnovelreader.api.web.explore.ExploreExpandedPageDataSource
import io.nightfish.lightnovelreader.api.web.explore.ExplorePageProvider
import io.nightfish.lightnovelreader.api.web.explore.ExploreTapPageDataSource

class PotatoLibExplorePageProvider: ExplorePageProvider.DefaultExplorePageProvider {
    override val explorePageIdList: List<String> = emptyList()
    override val exploreTapPageDataSourceMap: Map<String, ExploreTapPageDataSource> = emptyMap()
    override val exploreExpandedPageDataSourceMap: Map<String, ExploreExpandedPageDataSource> = emptyMap()
}