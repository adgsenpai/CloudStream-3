package com.adgstudios.cloudstream3.ui.search

import com.adgstudios.cloudstream3.SearchQuality
import com.adgstudios.cloudstream3.SearchResponse
import com.adgstudios.cloudstream3.TvType
import com.adgstudios.cloudstream3.syncproviders.OAuth2API

class SyncSearchViewModel {
    private val repos = OAuth2API.SyncApis

    data class SyncSearchResultSearchResponse(
        override val name: String,
        override val url: String,
        override val apiName: String,
        override var type: TvType?,
        override var posterUrl: String?,
        override var id: Int?,
        override var quality: SearchQuality? = null,
        override var posterHeaders: Map<String, String>? = null,
    ) : SearchResponse

}