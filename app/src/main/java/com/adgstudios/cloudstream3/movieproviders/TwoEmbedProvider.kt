package com.adgstudios.cloudstream3.movieproviders

import com.fasterxml.jackson.annotation.JsonProperty
import com.adgstudios.cloudstream3.*
import com.adgstudios.cloudstream3.APIHolder.getCaptchaToken
import com.adgstudios.cloudstream3.metaproviders.TmdbLink
import com.adgstudios.cloudstream3.metaproviders.TmdbProvider
import com.adgstudios.cloudstream3.movieproviders.SflixProvider.Companion.extractRabbitStream
import com.adgstudios.cloudstream3.utils.AppUtils.parseJson
import com.adgstudios.cloudstream3.utils.ExtractorLink
import com.adgstudios.cloudstream3.utils.loadExtractor

class TwoEmbedProvider : TmdbProvider() {
    override val apiName = "2Embed"
    override var name = "2Embed"
    override var mainUrl = "https://www.2embed.ru"
    override val useMetaLoadResponse = true
    override val instantLinkLoading = false
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )

    data class EmbedJson (
        @JsonProperty("type") val type: String?,
        @JsonProperty("link") val link: String,
        @JsonProperty("sources") val sources: List<String?>,
        @JsonProperty("tracks") val tracks: List<String>?
    )

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val mappedData = parseJson<TmdbLink>(data)
        val (id, site) = if (mappedData.imdbID != null) listOf(
            mappedData.imdbID,
            "imdb"
        ) else listOf(mappedData.tmdbID.toString(), "tmdb")
        val isMovie = mappedData.episode == null && mappedData.season == null
        val embedUrl = if (isMovie) {
            "$mainUrl/embed/$site/movie?id=$id"

        } else {
            val suffix = "$id&s=${mappedData.season ?: 1}&e=${mappedData.episode ?: 1}"
            "$mainUrl/embed/$site/tv?id=$suffix"
        }
        val document = app.get(embedUrl).document
        val captchaKey =
            document.select("script[src*=https://www.google.com/recaptcha/api.js?render=]")
                .attr("src").substringAfter("render=")

        val servers =  document.select(".dropdown-menu a[data-id]").map { it.attr("data-id") }
        servers.apmap { serverID ->
            val token = getCaptchaToken(embedUrl, captchaKey)
            val ajax = app.get("$mainUrl/ajax/embed/play?id=$serverID&_token=$token", referer = embedUrl).text
            val mappedservers = parseJson<EmbedJson>(ajax)
            val iframeLink = mappedservers.link
            if (iframeLink.contains("rabbitstream")) {
                extractRabbitStream(iframeLink, subtitleCallback, callback) { it }
            } else {
                loadExtractor(iframeLink, embedUrl, callback)
            }
        }
        return true
    }
}