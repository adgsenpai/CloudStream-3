package com.adgstudios.cloudstream3.extractors

import com.adgstudios.cloudstream3.app
import com.adgstudios.cloudstream3.utils.ExtractorApi
import com.adgstudios.cloudstream3.utils.ExtractorLink
import com.adgstudios.cloudstream3.utils.Qualities

class StreamTape : ExtractorApi() {
    override var name = "StreamTape"
    override var mainUrl = "https://streamtape.com"
    override val requiresReferer = false

    private val linkRegex =
        Regex("""'robotlink'\)\.innerHTML = '(.+?)'\+ \('(.+?)'\)""")

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        with(app.get(url)) {
            linkRegex.find(this.text)?.let {
                val extractedUrl = "https:${it.groups[1]!!.value + it.groups[2]!!.value.substring(3,)}"
                return listOf(
                    ExtractorLink(
                        name,
                        name,
                        extractedUrl,
                        url,
                        Qualities.Unknown.value,
                    )
                )
            }
        }
        return null
    }
}
