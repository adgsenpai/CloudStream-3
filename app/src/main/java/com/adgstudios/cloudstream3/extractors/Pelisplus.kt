package com.adgstudios.cloudstream3.extractors

import com.adgstudios.cloudstream3.apmap
import com.adgstudios.cloudstream3.app
import com.adgstudios.cloudstream3.mvvm.suspendSafeApiCall
import com.adgstudios.cloudstream3.utils.ExtractorLink
import com.adgstudios.cloudstream3.utils.extractorApis
import com.adgstudios.cloudstream3.utils.getQualityFromName
import com.adgstudios.cloudstream3.utils.loadExtractor
import org.jsoup.Jsoup

/**
 * overrideMainUrl is necessary for for other vidstream clones like vidembed.cc
 * If they diverge it'd be better to make them separate.
 * */
class Pelisplus(val mainUrl: String) {
    val name: String = "Vidstream"

    private fun getExtractorUrl(id: String): String {
        return "$mainUrl/play?id=$id"
    }

    private fun getDownloadUrl(id: String): String {
        return "$mainUrl/download?id=$id"
    }

    private val normalApis = arrayListOf(MultiQuality())

    // https://gogo-stream.com/streaming.php?id=MTE3NDg5
    suspend fun getUrl(id: String, isCasting: Boolean = false, callback: (ExtractorLink) -> Unit): Boolean {
        try {
            normalApis.apmap { api ->
                val url = api.getExtractorUrl(id)
                val source = api.getSafeUrl(url)
                source?.forEach { callback.invoke(it) }
            }
            val extractorUrl = getExtractorUrl(id)

            /** Stolen from GogoanimeProvider.kt extractor */
            suspendSafeApiCall {
                val link = getDownloadUrl(id)
                println("Generated vidstream download link: $link")
                val page = app.get(link, referer = extractorUrl)

                val pageDoc = Jsoup.parse(page.text)
                val qualityRegex = Regex("(\\d+)P")

                //a[download]
                pageDoc.select(".dowload > a")?.apmap { element ->
                    val href = element.attr("href") ?: return@apmap
                    val qual = if (element.text()
                            .contains("HDP")
                    ) "1080" else qualityRegex.find(element.text())?.destructured?.component1().toString()

                    if (!loadExtractor(href, link, callback)) {
                        callback.invoke(
                            ExtractorLink(
                                this.name,
                                name = this.name,
                                href,
                                page.url,
                                getQualityFromName(qual),
                                element.attr("href").contains(".m3u8")
                            )
                        )
                    }
                }
            }

            with(app.get(extractorUrl)) {
                val document = Jsoup.parse(this.text)
                val primaryLinks = document.select("ul.list-server-items > li.linkserver")
                //val extractedLinksList: MutableList<ExtractorLink> = mutableListOf()

                // All vidstream links passed to extractors
                primaryLinks.distinctBy { it.attr("data-video") }.forEach { element ->
                    val link = element.attr("data-video")
                    //val name = element.text()

                    // Matches vidstream links with extractors
                    extractorApis.filter { !it.requiresReferer || !isCasting }.apmap { api ->
                        if (link.startsWith(api.mainUrl)) {
                            val extractedLinks = api.getSafeUrl(link, extractorUrl)
                            if (extractedLinks?.isNotEmpty() == true) {
                                extractedLinks.forEach {
                                    callback.invoke(it)
                                }
                            }
                        }
                    }
                }
                return true
            }
        } catch (e: Exception) {
            return false
        }
    }
}
