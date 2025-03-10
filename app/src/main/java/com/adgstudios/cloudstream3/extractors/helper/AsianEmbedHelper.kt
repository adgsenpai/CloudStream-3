package com.adgstudios.cloudstream3.extractors.helper

import android.util.Log
import com.adgstudios.cloudstream3.apmap
import com.adgstudios.cloudstream3.app
import com.adgstudios.cloudstream3.utils.ExtractorLink
import com.adgstudios.cloudstream3.utils.loadExtractor

class AsianEmbedHelper {
    companion object {
        suspend fun getUrls(url: String, callback: (ExtractorLink) -> Unit) {
            // Fetch links
            val doc = app.get(url).document
            val links = doc.select("div#list-server-more > ul > li.linkserver")
            if (!links.isNullOrEmpty()) {
                links.apmap {
                    val datavid = it.attr("data-video") ?: ""
                    //Log.i("AsianEmbed", "Result => (datavid) ${datavid}")
                    if (datavid.isNotBlank()) {
                        val res = loadExtractor(datavid, url, callback)
                        Log.i("AsianEmbed", "Result => ($res) (datavid) $datavid")
                    }
                }
            }
        }
    }
}