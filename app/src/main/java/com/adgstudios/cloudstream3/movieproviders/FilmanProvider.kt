package com.adgstudios.cloudstream3.movieproviders

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.readValue
import com.adgstudios.cloudstream3.*
import com.adgstudios.cloudstream3.utils.ExtractorLink
import com.adgstudios.cloudstream3.utils.loadExtractor
import org.jsoup.Jsoup
import org.jsoup.select.Elements

class FilmanProvider : MainAPI() {
    override var mainUrl = "https://filman.cc"
    override var name = "filman.cc"
    override val lang = "pl"
    override val hasMainPage = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries
    )

    override suspend fun getMainPage(): HomePageResponse {
        val response = app.get(mainUrl).text
        val document = Jsoup.parse(response)
        val lists = document.select(".item-list,.series-list")
        val categories = ArrayList<HomePageList>()
        for (l in lists) {
            val title = l.parent().select("h3").text()
            val items = l.select(".poster").map { i ->
                val name = i.select("a[href]").attr("title")
                val href = i.select("a[href]").attr("href")
                val poster = i.select("img[src]").attr("src")
                val year = l.select(".film_year").text().toIntOrNull()
                if (l.hasClass("series-list")) TvSeriesSearchResponse(
                    name,
                    href,
                    this.name,
                    TvType.TvSeries,
                    poster,
                    year,
                    null
                ) else MovieSearchResponse(
                    name,
                    href,
                    this.name,
                    TvType.Movie,
                    poster,
                    year
                )
            }
            categories.add(HomePageList(title, items))
        }
        return HomePageResponse(categories)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/wyszukiwarka?phrase=$query"
        val response = app.get(url).text
        val document = Jsoup.parse(response)
        val lists = document.select("#advanced-search > div")
        val movies = lists[1].select(".item")
        val series = lists[3].select(".item")
        if (movies.isEmpty() && series.isEmpty()) return ArrayList()
        fun getVideos(type: TvType, items: Elements): List<SearchResponse> {
            return items.map { i ->
                val href = i.attr("href")
                val img = i.selectFirst("> img").attr("src").replace("/thumb/", "/big/")
                val name = i.selectFirst(".title").text()
                if (type === TvType.TvSeries) {
                    TvSeriesSearchResponse(
                        name,
                        href,
                        this.name,
                        type,
                        img,
                        null,
                        null
                    )
                } else {
                    MovieSearchResponse(name, href, this.name, type, img, null)
                }
            }
        }
        return getVideos(TvType.Movie, movies) + getVideos(TvType.TvSeries, series)
    }

    override suspend fun load(url: String): LoadResponse {
        val response = app.get(url).text
        val document = Jsoup.parse(response)
        var title = document.select("span[itemprop=title]").text()
        val data = document.select("#links").outerHtml()
        val posterUrl = document.select("#single-poster > img").attr("src")
        val year = document.select(".info > ul li")[1].text().toIntOrNull()
        val plot = document.select(".description").text()
        val episodesElements = document.select("#episode-list a[href]")
        if (episodesElements.isEmpty()) {
            return MovieLoadResponse(title, url, name, TvType.Movie, data, posterUrl, year, plot)
        }
        title = document.selectFirst(".info").parent().select("h2").text()
        val episodes = episodesElements.mapNotNull { episode ->
            val e = episode.text()
            val regex = Regex("""\[s(\d{1,3})e(\d{1,3})]""").find(e) ?: return@mapNotNull null
            val eid = regex.groups
            Episode(
                episode.attr("href"),
                e.split("]")[1].trim(),
                eid[1]?.value?.toInt(),
                eid[2]?.value?.toInt(),
            )
        }.toMutableList()

        return TvSeriesLoadResponse(
            title,
            url,
            name,
            TvType.TvSeries,
            episodes,
            posterUrl,
            year,
            plot
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = if (data.startsWith("http"))
            app.get(data).document.select("#links").first()
        else Jsoup.parse(data)

        document.select(".link-to-video")?.apmap { item ->
            val decoded = base64Decode(item.select("a").attr("data-iframe"))
            val link = mapper.readValue<LinkElement>(decoded).src
            loadExtractor(link, null, callback)
        }
        return true
    }
}

data class LinkElement(
    @JsonProperty("src") val src: String
)
