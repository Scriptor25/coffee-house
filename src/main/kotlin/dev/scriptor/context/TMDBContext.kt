package dev.scriptor.context

import dev.scriptor.parseJson
import dev.scriptor.server.Provider
import dev.scriptor.server.jvm.annotation.Context
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Context
class TMDBContext {

    data class MovieCollection(
        val id: Int,
        val name: String,
        /** poster_path */
        val posterPath: String,
        /** backdrop_path */
        val backdropPath: String,
    )

    data class MovieGenre(
        val id: Int,
        val name: String,
    )

    data class MovieCompany(
        val id: Int,
        /** logo_path */
        val logoPath: String,
        val name: String,
        /** origin_country */
        val originCountry: String,
    )

    data class MovieCountry(
        /** iso_3166_1 */
        val iso31661: String,
        val name: String,
    )

    data class MovieLanguage(
        /** english_name */
        val englishName: String,
        /** iso_639_1 */
        val iso6391: String,
        val name: String,
    )

    data class MovieDetails(
        val adult: Boolean,
        /** backdrop_path */
        val backdropPath: String,
        /** belongs_to_collection */
        val belongsToCollection: MovieCollection,
        val budget: Int,
        val genres: List<MovieGenre>,
        val homepage: String,
        val id: Int,
        /** imdb_id */
        val imdbId: String,
        /** origin_country */
        val originCountry: List<String>,
        /** original_language */
        val originalLanguage: String,
        /** original_title */
        val originalTitle: String,
        val overview: String,
        val popularity: Double,
        /** poster_path */
        val posterPath: String,
        /** production_companies */
        val productionCompanies: List<MovieCompany>,
        /** production_countries */
        val productionCountries: List<MovieCountry>,
        /** release_date */
        val releaseDate: String,
        val revenue: Int,
        val runtime: Int,
        val softcore: Boolean,
        /** spoken_languages */
        val spokenLanguages: List<MovieLanguage>,
        val status: String,
        val tagline: String,
        val title: String,
        val video: Boolean,
        /** vote_average */
        val voteAverage: Double,
        /** vote_count */
        val voteCount: Int,
    )

    data class MovieAlternativeTitle(
        /** iso_3166_1 */
        val iso31661: String,
        val title: String,
        val type: String,
    )

    data class MovieAlternativeTitles(
        val id: Int,
        val titles: List<MovieAlternativeTitle>,
    )

    data class MovieCastMember(
        val adult: Boolean,
        val gender: Int,
        val id: Int,
        /** know_for_department */
        val knownForDepartment: String,
        val name: String,
        /** original_name */
        val originalName: String,
        val popularity: Double,
        /** profile_path */
        val profilePath: String,
        /** cast_id */
        val castId: Int,
        val character: String,
        /** credit_id */
        val creditId: String,
        val order: Int,
    )

    data class MovieCrewMember(
        val adult: Boolean,
        val gender: Int,
        val id: Int,
        /** known_for_department */
        val knownForDepartment: String,
        val name: String,
        /** original_name */
        val originalName: String,
        val popularity: Double,
        /** profile_path */
        val profilePath: String,
        /** credit_id */
        val creditId: String,
        val department: String,
        val job: String,
    )

    data class MovieCredits(
        val id: Int,
        val cast: List<MovieCastMember>,
        val crew: List<MovieCrewMember>,
    )

    data class MovieExternalIds(
        val id: Int,
        /** imdb_id */
        val imdbId: String,
        /** wikidata_id */
        val wikidataId: String,
        /** facebook_id */
        val facebookId: String,
        /** instagram_id */
        val instagramId: String,
        /** twitter_id */
        val twitterId: String,
    )

    data class MovieImage(
        /** aspect_ratio */
        val aspectRatio: Double,
        val height: Int,
        /** iso_639_1 */
        val iso6391: String,
        /** file_path */
        val filePath: String,
        /** vote_average */
        val voteAverage: Double,
        /** vote_count */
        val voteCount: Int,
        val width: Int,
    )

    data class MovieImages(
        val backdrops: List<MovieImage>,
        val id: Int,
        val logos: List<MovieImage>,
        val posters: List<MovieImage>,
    )

    data class MovieKeyword(
        val id: Int,
        val name: String,
    )

    data class MovieKeywords(
        val id: Int,
        val keywords: List<MovieKeyword>,
    )

    data class MovieRecommendation(
        val adult: Boolean,
        /** backdrop_path */
        val backdropPath: String,
        /** genre_ids */
        val genreIds: List<Int>,
        val id: Int,
        /** original_language */
        val originalLanguage: String,
        /** original_title */
        val originalTitle: String,
        val overview: String,
        val popularity: Double,
        /** poster_path */
        val posterPath: String,
        /** release_date */
        val releaseDate: String,
        val title: String,
        val video: Boolean,
        /** vote_average */
        val voteAverage: Double,
        /** vote_count */
        val voteCount: Int,
    )

    data class MovieRecommendations(
        val page: Int,
        val results: List<MovieRecommendation>,
        /** total_pages */
        val totalPages: Int,
        /** total_results */
        val totalResults: Int,
    )

    data class MovieTranslationData(
        val homepage: String,
        val overview: String,
        val runtime: Int,
        val tagline: String,
        val title: String,
    )

    data class MovieTranslation(
        /** iso_3166_1 */
        val iso31661: String,
        /** iso_639_1 */
        val iso6391: String,
        val name: String,
        /** english_name */
        val englishName: String,
        val data: MovieTranslationData,
    )

    data class MovieTranslations(
        val id: Int,
        val translations: List<MovieTranslation>,
    )

    data class MovieVideo(
        val id: Int,
        /** iso_3166_1 */
        val iso31661: String,
        /** iso_639_1 */
        val iso6391: String,
        val key: String,
        val name: String,
        val official: Boolean,
        /** published_at */
        val publishedAt: String,
        val site: String,
        val size: Int,
        val type: String,
    )

    data class MovieVideos(
        val id: Int,
        val results: List<MovieVideo>,
    )

    context(provider: Provider)
    inline fun <reified T> getMovieData(resource: String, movieId: Int, params: Map<String, String?> = emptyMap()): T {
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

        return provider(json)
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
