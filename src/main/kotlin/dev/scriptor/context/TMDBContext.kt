package dev.scriptor.context

import dev.scriptor.JsonProperty
import dev.scriptor.JsonSerializable
import dev.scriptor.cast
import dev.scriptor.parseJson
import dev.scriptor.server.Provider
import dev.scriptor.server.jvm.annotation.Context
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.reflect.typeOf

@Context
class TMDBContext {

    @JsonSerializable
    data class MovieCollection(
        val id: Int,
        val name: String,
        @JsonProperty("poster_path")
        val posterPath: String?,
        @JsonProperty("backdrop_path")
        val backdropPath: String?,
    )

    @JsonSerializable
    data class MovieGenre(
        val id: Int,
        val name: String,
    )

    @JsonSerializable
    data class MovieCompany(
        val id: Int,
        @JsonProperty("logo_path")
        val logoPath: String?,
        val name: String,
        @JsonProperty("origin_country")
        val originCountry: String,
    )

    @JsonSerializable
    data class MovieCountry(
        @JsonProperty("iso_3166_1")
        val iso31661: String,
        val name: String,
    )

    @JsonSerializable
    data class MovieLanguage(
        @JsonProperty("english_name")
        val englishName: String,
        @JsonProperty("iso_639_1")
        val iso6391: String,
        val name: String,
    )

    @JsonSerializable
    data class MovieDetails(
        val adult: Boolean,
        @JsonProperty("backdrop_path")
        val backdropPath: String?,
        @JsonProperty("belongs_to_collection")
        val belongsToCollection: MovieCollection?,
        val budget: Int,
        val genres: List<MovieGenre>,
        val homepage: String,
        val id: Int,
        @JsonProperty("imdb_id")
        val imdbId: String,
        @JsonProperty("origin_country")
        val originCountry: List<String>,
        @JsonProperty("original_language")
        val originalLanguage: String,
        @JsonProperty("original_title")
        val originalTitle: String,
        val overview: String,
        val popularity: Double,
        @JsonProperty("poster_path")
        val posterPath: String?,
        @JsonProperty("production_companies")
        val productionCompanies: List<MovieCompany>,
        @JsonProperty("production_countries")
        val productionCountries: List<MovieCountry>,
        @JsonProperty("release_date")
        val releaseDate: String,
        val revenue: Int,
        val runtime: Int,
        val softcore: Boolean,
        @JsonProperty("spoken_languages")
        val spokenLanguages: List<MovieLanguage>,
        val status: String,
        val tagline: String,
        val title: String,
        val video: Boolean,
        @JsonProperty("vote_average")
        val voteAverage: Double,
        @JsonProperty("vote_count")
        val voteCount: Int,
    )

    @JsonSerializable
    data class MovieAlternativeTitle(
        @JsonProperty("iso_3166_1")
        val iso31661: String,
        val title: String,
        val type: String,
    )

    @JsonSerializable
    data class MovieAlternativeTitles(
        val id: Int,
        val titles: List<MovieAlternativeTitle>,
    )

    @JsonSerializable
    data class MovieCastMember(
        val adult: Boolean,
        val gender: Int,
        val id: Int,
        @JsonProperty("know_for_department")
        val knownForDepartment: String,
        val name: String,
        @JsonProperty("original_name")
        val originalName: String,
        val popularity: Double,
        @JsonProperty("profile_path")
        val profilePath: String?,
        @JsonProperty("cast_id")
        val castId: Int,
        val character: String,
        @JsonProperty("credit_id")
        val creditId: String,
        val order: Int,
    )

    @JsonSerializable
    data class MovieCrewMember(
        val adult: Boolean,
        val gender: Int,
        val id: Int,
        @JsonProperty("known_for_department")
        val knownForDepartment: String,
        val name: String,
        @JsonProperty("original_name")
        val originalName: String,
        val popularity: Double,
        @JsonProperty("profile_path")
        val profilePath: String?,
        @JsonProperty("credit_id")
        val creditId: String,
        val department: String,
        val job: String,
    )

    @JsonSerializable
    data class MovieCredits(
        val id: Int,
        val cast: List<MovieCastMember>,
        val crew: List<MovieCrewMember>,
    )

    @JsonSerializable
    data class MovieExternalIds(
        val id: Int,
        @JsonProperty("imdb_id")
        val imdbId: String,
        @JsonProperty("wikidata_id")
        val wikidataId: String,
        @JsonProperty("facebook_id")
        val facebookId: String,
        @JsonProperty("instagram_id")
        val instagramId: String,
        @JsonProperty("twitter_id")
        val twitterId: String,
    )

    @JsonSerializable
    data class MovieImage(
        @JsonProperty("aspect_ratio")
        val aspectRatio: Double,
        val height: Int,
        @JsonProperty("iso_639_1")
        val iso6391: String,
        @JsonProperty("file_path")
        val filePath: String?,
        @JsonProperty("vote_average")
        val voteAverage: Double,
        @JsonProperty("vote_count")
        val voteCount: Int,
        val width: Int,
    )

    @JsonSerializable
    data class MovieImages(
        val backdrops: List<MovieImage>,
        val id: Int,
        val logos: List<MovieImage>,
        val posters: List<MovieImage>,
    )

    @JsonSerializable
    data class MovieKeyword(
        val id: Int,
        val name: String,
    )

    @JsonSerializable
    data class MovieKeywords(
        val id: Int,
        val keywords: List<MovieKeyword>,
    )

    @JsonSerializable
    data class MovieRecommendation(
        val adult: Boolean,
        @JsonProperty("backdrop_path")
        val backdropPath: String?,
        @JsonProperty("genre_ids")
        val genreIds: List<Int>,
        val id: Int,
        @JsonProperty("original_language")
        val originalLanguage: String,
        @JsonProperty("original_title")
        val originalTitle: String,
        val overview: String,
        val popularity: Double,
        @JsonProperty("poster_path")
        val posterPath: String?,
        @JsonProperty("release_date")
        val releaseDate: String,
        val title: String,
        val video: Boolean,
        @JsonProperty("vote_average")
        val voteAverage: Double,
        @JsonProperty("vote_count")
        val voteCount: Int,
    )

    @JsonSerializable
    data class MovieRecommendations(
        val page: Int,
        val results: List<MovieRecommendation>,
        @JsonProperty("total_pages")
        val totalPages: Int,
        @JsonProperty("total_results")
        val totalResults: Int,
    )

    @JsonSerializable
    data class MovieTranslationData(
        val homepage: String,
        val overview: String,
        val runtime: Int,
        val tagline: String,
        val title: String,
    )

    @JsonSerializable
    data class MovieTranslation(
        @JsonProperty("iso_3166_1")
        val iso31661: String,
        @JsonProperty("iso_639_1")
        val iso6391: String,
        val name: String,
        @JsonProperty("english_name")
        val englishName: String,
        val data: MovieTranslationData,
    )

    @JsonSerializable
    data class MovieTranslations(
        val id: Int,
        val translations: List<MovieTranslation>,
    )

    @JsonSerializable
    data class MovieVideo(
        val id: Int,
        @JsonProperty("iso_3166_1")
        val iso31661: String,
        @JsonProperty("iso_639_1")
        val iso6391: String,
        val key: String,
        val name: String,
        val official: Boolean,
        @JsonProperty("published_at")
        val publishedAt: String,
        val site: String,
        val size: Int,
        val type: String,
    )

    @JsonSerializable
    data class MovieVideos(
        val id: Int,
        val results: List<MovieVideo>,
    )

    private val cache = mutableMapOf<String, Any?>()

    context(provider: Provider)
    private inline fun <reified T> getMovieData(
        resource: String,
        movieId: Int,
        params: Map<String, String?> = emptyMap()
    ): T {
        val params = params.toSortedMap()

        val query = buildString {
            var first = true
            for ((key, value) in params) {
                if (value == null) continue
                if (first) {
                    first = false
                    append("?")
                } else {
                    append("&")
                }
                append(key)
                append("=")
                append(value)
            }
        }

        val uri = "https://api.themoviedb.org/3/movie/${movieId}${resource}${query}"

        val cached = cache[uri]
        if (cached != null) {
            return cached as T
        }

        val token = provider.getNamedT<String>("tmdb-token")
            ?: error("missing tmdb-token")

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(uri))
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $token")
            .build()

        val response = client.send(
            request,
            HttpResponse.BodyHandlers.ofString(),
        )

        val text = response.body()
        val json = parseJson(text)

        val value = json.cast<T>(
            mapOf(
                typeOf<Int>() to { it.cast<Number>().toInt() },
                typeOf<Double>() to { it.cast<Number>().toDouble() },
            ),
        )
        cache[uri] = value
        return value
    }

    /**
     * https://api.themoviedb.org/3/movie/{movie_id}?language={language}
     */
    context(_: Provider)
    fun getMovieDetails(movieId: Int, language: String = "en-US"): MovieDetails {
        return getMovieData(
            "",
            movieId,
            mapOf(
                "language" to language,
            ),
        )
    }

    /**
     * https://api.themoviedb.org/3/movie/{movie_id}/alternative_titles?country={country}
     */
    context(_: Provider)
    fun getMovieAlternativeTitles(movieId: Int, country: String? = null): MovieAlternativeTitles {
        return getMovieData(
            "/alternative_titles",
            movieId,
            mapOf(
                "country" to country,
            ),
        )
    }

    /**
     * https://api.themoviedb.org/3/movie/{movie_id}/credits?language={language}
     */
    context(_: Provider)
    fun getMovieCredits(movieId: Int, language: String = "en-US"): MovieCredits {
        return getMovieData(
            "/credits",
            movieId,
            mapOf(
                "language" to language,
            ),
        )
    }

    /**
     * https://api.themoviedb.org/3/movie/{movie_id}/external_ids
     */
    context(_: Provider)
    fun getMovieExternalIds(movieId: Int): MovieExternalIds {
        return getMovieData("/external_ids", movieId)
    }

    /**
     * https://api.themoviedb.org/3/movie/{movie_id}/images?include_image_language={include_image_language}&language={language}
     */
    context(_: Provider)
    fun getMovieImages(movieId: Int, includeImageLanguage: String? = null, language: String? = null): MovieImages {
        return getMovieData(
            "/images",
            movieId,
            mapOf(
                "include_image_language" to includeImageLanguage,
                "language" to language,
            ),
        )
    }

    /**
     * https://api.themoviedb.org/3/movie/{movie_id}/keywords
     */
    context(_: Provider)
    fun getMovieKeywords(movieId: Int): MovieKeywords {
        return getMovieData("/keywords", movieId)
    }

    /**
     * https://api.themoviedb.org/3/movie/{movie_id}/recommendations?language={language}&page={page}
     */
    context(_: Provider)
    fun getMovieRecommendations(movieId: Int, language: String = "en-US", page: Int = 1): MovieRecommendations {
        return getMovieData(
            "/recommendations",
            movieId,
            mapOf(
                "language" to language,
                "page" to "$page",
            ),
        )
    }

    /**
     * https://api.themoviedb.org/3/movie/{movie_id}/similar?language={language}&page={page}
     */
    context(_: Provider)
    fun getMovieSimilar(movieId: Int, language: String = "en-US", page: Int = 1): MovieRecommendations {
        return getMovieData(
            "/similar",
            movieId,
            mapOf(
                "language" to language,
                "page" to "$page",
            )
        )
    }

    /**
     * https://api.themoviedb.org/3/movie/{movie_id}/translations
     */
    context(_: Provider)
    fun getMovieTranslations(movieId: Int): MovieTranslations {
        return getMovieData(
            "/translations",
            movieId,
        )
    }

    /**
     * https://api.themoviedb.org/3/movie/{movie_id}/videos?language={language}
     */
    context(_: Provider)
    fun getMovieVideos(movieId: Int, language: String = "en-US"): MovieVideos {
        return getMovieData(
            "/videos",
            movieId,
            mapOf(
                "language" to language,
            ),
        )
    }
}
