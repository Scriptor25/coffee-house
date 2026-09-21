package dev.scriptor.context

import dev.scriptor.*
import dev.scriptor.server.Provider
import dev.scriptor.server.jvm.annotation.Context
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.logging.Logger

@Context
class TmdbContext {

    @JsonSerializable
    data class ConfigurationImages(
        @all:JsonProperty("base_url")
        val baseUrl: String,
        @all:JsonProperty("secure_base_url")
        val secureBaseUrl: String,
        @all:JsonProperty("backdrop_sizes")
        val backdropSizes: List<String>,
        @all:JsonProperty("logo_sizes")
        val logoSizes: List<String>,
        @all:JsonProperty("poster_sizes")
        val posterSizes: List<String>,
        @all:JsonProperty("profile_sizes")
        val profileSizes: List<String>,
        @all:JsonProperty("still_sizes")
        val stillSizes: List<String>,
    )

    @JsonSerializable
    data class Configuration(
        @all:JsonProperty("change_keys")
        val changeKeys: List<String>,
        @all:JsonProperty
        val images: ConfigurationImages,
    )

    @JsonSerializable
    data class ImageReference(
        @all:JsonProperty("aspect_ratio")
        val aspectRatio: Double,
        @all:JsonProperty("file_path")
        val filePath: String? = null,
        @all:JsonProperty
        val height: Int,
        @all:JsonProperty("iso_639_1")
        val iso6391: String,
        @all:JsonProperty("vote_average")
        val voteAverage: Double,
        @all:JsonProperty("vote_count")
        val voteCount: Int,
        @all:JsonProperty
        val width: Int,
    )

    @JsonSerializable
    data class ImageReferences(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val logos: List<ImageReference> = listOf(),
        @all:JsonProperty
        val posters: List<ImageReference> = listOf(),
        @all:JsonProperty
        val backdrops: List<ImageReference> = listOf(),
    )

    @JsonSerializable
    data class AlternativeTitle(
        @all:JsonProperty("iso_3166_1")
        val iso31661: String,
        @all:JsonProperty
        val title: String,
        @all:JsonProperty
        val type: String,
    )

    @JsonSerializable
    data class NamedReference(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val name: String,
    )

    @JsonSerializable
    data class LanguageReference(
        @all:JsonProperty("english_name")
        val englishName: String,
        @all:JsonProperty("iso_639_1")
        val iso6391: String,
        @all:JsonProperty
        val name: String,
    )

    @JsonSerializable
    data class CountryReference(
        @all:JsonProperty("iso_3166_1")
        val iso31661: String,
        @all:JsonProperty
        val name: String,
    )

    @JsonSerializable
    data class CompanyReference(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty("logo_path")
        val logoPath: String? = null,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty("origin_country")
        val originCountry: String,
    )

    @JsonSerializable
    data class AggregateCreditsCastRole(
        @all:JsonProperty
        val character: String,
        @all:JsonProperty("credit_id")
        val creditId: String,
        @all:JsonProperty("episode_count")
        val episodeCount: Int,
    )

    @JsonSerializable
    data class AggregateCreditsCast(
        @all:JsonProperty
        val adult: Boolean = true,
        @all:JsonProperty
        val gender: Int,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty("known_for_department")
        val knownForDepartment: String,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val order: Int,
        @all:JsonProperty("original_name")
        val originalName: String,
        @all:JsonProperty
        val popularity: Double,
        @all:JsonProperty("profile_path")
        val profilePath: String? = null,
        @all:JsonProperty
        val roles: List<AggregateCreditsCastRole>,
        @all:JsonProperty("total_episode_count")
        val totalEpisodeCount: Int,
    )

    @JsonSerializable
    data class AggregateCreditsCrewJob(
        @all:JsonProperty("credit_id")
        val creditId: String,
        @all:JsonProperty("episode_count")
        val episodeCount: Int,
        @all:JsonProperty
        val job: String,
    )

    @JsonSerializable
    data class AggregateCreditsCrew(
        @all:JsonProperty
        val adult: Boolean = true,
        @all:JsonProperty
        val department: String,
        @all:JsonProperty
        val gender: Int,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val jobs: List<AggregateCreditsCrewJob>,
        @all:JsonProperty("known_for_department")
        val knownForDepartment: String,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty("original_name")
        val originalName: String,
        @all:JsonProperty
        val popularity: Double,
        @all:JsonProperty("profile_path")
        val profilePath: String? = null,
        @all:JsonProperty("total_episode_count")
        val totalEpisodeCount: Int,
    )

    @JsonSerializable
    data class AggregateCredits(
        @all:JsonProperty
        val cast: List<AggregateCreditsCast>,
        @all:JsonProperty
        val crew: List<AggregateCreditsCrew>,
        @all:JsonProperty
        val id: Int,
    )

    @JsonSerializable
    data class Translation<Data>(
        @all:JsonProperty
        val data: Data,
        @all:JsonProperty("english_name")
        val englishName: String,
        @all:JsonProperty("iso_3166_1")
        val iso31661: String,
        @all:JsonProperty("iso_639_1")
        val iso6391: String,
        @all:JsonProperty
        val name: String,
    )

    @JsonSerializable
    data class Translations<Data>(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val translations: List<Translation<Data>>,
    )

    @JsonSerializable
    data class VideoReference(
        @all:JsonProperty
        val id: String,
        @all:JsonProperty("iso_3166_1")
        val iso31661: String,
        @all:JsonProperty("iso_639_1")
        val iso6391: String,
        @all:JsonProperty
        val key: String,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val official: Boolean,
        @all:JsonProperty("published_at")
        val publishedAt: String,
        @all:JsonProperty
        val site: String,
        @all:JsonProperty
        val size: Int,
        @all:JsonProperty
        val type: String,
    )

    @JsonSerializable
    data class VideoReferences(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val results: List<VideoReference>,
    )

    @JsonSerializable
    data class CastReference(
        @all:JsonProperty
        val adult: Boolean = true,
        @all:JsonProperty("cast_id")
        val castId: Int = 0,
        @all:JsonProperty
        val character: String,
        @all:JsonProperty("credit_id")
        val creditId: String,
        @all:JsonProperty
        val gender: Int,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty("known_for_department")
        val knownForDepartment: String,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val order: Int,
        @all:JsonProperty("original_name")
        val originalName: String,
        @all:JsonProperty
        val popularity: Double,
        @all:JsonProperty("profile_path")
        val profilePath: String? = null,
    )

    @JsonSerializable
    data class CrewReference(
        @all:JsonProperty
        val adult: Boolean = true,
        @all:JsonProperty("credit_id")
        val creditId: String,
        @all:JsonProperty
        val department: String,
        @all:JsonProperty
        val gender: Int,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val job: String,
        @all:JsonProperty("known_for_department")
        val knownForDepartment: String,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty("original_name")
        val originalName: String,
        @all:JsonProperty
        val popularity: Double,
        @all:JsonProperty("profile_path")
        val profilePath: String? = null,
    )

    @JsonSerializable
    data class CreditDetailsMediaSeason(
        @all:JsonProperty("air_date")
        val airDate: String,
        @all:JsonProperty("episode_count")
        val episodeCount: Int,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val overview: String,
        @all:JsonProperty("poster_path")
        val posterPath: String? = null,
        @all:JsonProperty("season_number")
        val seasonNumber: Int,
        @all:JsonProperty("show_id")
        val showId: Int,
    )

    @JsonSerializable
    data class CreditDetailsMedia(
        @all:JsonProperty
        val adult: Boolean = true,
        @all:JsonProperty("backdrop_path")
        val backdropPath: String? = null,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty("original_language")
        val originalLanguage: String,
        @all:JsonProperty("original_name")
        val originalName: String,
        @all:JsonProperty
        val overview: String,
        @all:JsonProperty("poster_path")
        val posterPath: String? = null,
        @all:JsonProperty("media_type")
        val mediaType: String,
        @all:JsonProperty("genre_ids")
        val genreIds: List<Int>,
        @all:JsonProperty
        val popularity: Double,
        @all:JsonProperty("first_air_date")
        val firstAirDate: String,
        @all:JsonProperty("vote_average")
        val voteAverage: Double,
        @all:JsonProperty("vote_count")
        val voteCount: Int,
        @all:JsonProperty("origin_country")
        val originCountry: List<String> = listOf(),
        @all:JsonProperty
        val character: String,
        @all:JsonProperty
        val episodes: List<Any>,
        @all:JsonProperty
        val seasons: List<CreditDetailsMediaSeason>,
    )

    @JsonSerializable
    data class CreditDetailsPerson(
        @all:JsonProperty
        val adult: Boolean = true,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty("original_name")
        val originalName: String,
        @all:JsonProperty("media_type")
        val mediaType: String,
        @all:JsonProperty
        val popularity: Double,
        @all:JsonProperty
        val gender: Int,
        @all:JsonProperty("known_for_department")
        val knownForDepartment: String,
        @all:JsonProperty("profile_path")
        val profilePath: String? = null,
    )

    @JsonSerializable
    data class CreditDetails(
        @all:JsonProperty("credit_type")
        val creditType: String,
        @all:JsonProperty
        val department: String,
        @all:JsonProperty
        val id: String,
        @all:JsonProperty
        val job: String,
        @all:JsonProperty
        val media: CreditDetailsMedia,
        @all:JsonProperty("media_type")
        val mediaType: String,
        @all:JsonProperty
        val person: CreditDetailsPerson,
    )

    @JsonSerializable
    data class MovieCollection(
        @all:JsonProperty("backdrop_path")
        val backdropPath: String? = null,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty("poster_path")
        val posterPath: String? = null,
    )

    @JsonSerializable
    data class MovieDetails(
        @all:JsonProperty
        val adult: Boolean = false,
        @all:JsonProperty("backdrop_path")
        val backdropPath: String? = null,
        @all:JsonProperty("belongs_to_collection")
        val belongsToCollection: MovieCollection? = null,
        @all:JsonProperty
        val budget: Int = -1,
        @all:JsonProperty
        val genres: List<NamedReference> = listOf(),
        @all:JsonProperty
        val homepage: String = "",
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty("imdb_id")
        val imdbId: String? = null,
        @all:JsonProperty("origin_country")
        val originCountry: List<String> = listOf(),
        @all:JsonProperty("original_language")
        val originalLanguage: String,
        @all:JsonProperty("original_title")
        val originalTitle: String,
        @all:JsonProperty
        val overview: String,
        @all:JsonProperty
        val popularity: Double,
        @all:JsonProperty("poster_path")
        val posterPath: String? = null,
        @all:JsonProperty("production_companies")
        val productionCompanies: List<CompanyReference>,
        @all:JsonProperty("production_countries")
        val productionCountries: List<CountryReference>,
        @all:JsonProperty("release_date")
        val releaseDate: String,
        @all:JsonProperty
        val revenue: Int,
        @all:JsonProperty
        val runtime: Int,
        @all:JsonProperty
        val softcore: Boolean,
        @all:JsonProperty("spoken_languages")
        val spokenLanguages: List<LanguageReference>,
        @all:JsonProperty
        val status: String,
        @all:JsonProperty
        val tagline: String,
        @all:JsonProperty
        val title: String,
        @all:JsonProperty
        val video: Boolean,
        @all:JsonProperty("vote_average")
        val voteAverage: Double,
        @all:JsonProperty("vote_count")
        val voteCount: Int,
    )

    @JsonSerializable
    data class MovieAlternativeTitles(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val titles: List<AlternativeTitle>,
    )

    @JsonSerializable
    data class MovieCredits(
        @all:JsonProperty
        val cast: List<CastReference>,
        @all:JsonProperty
        val crew: List<CrewReference>,
        @all:JsonProperty
        val id: Int,
    )

    @JsonSerializable
    data class MovieExternalIds(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty("imdb_id")
        val imdbId: String,
        @all:JsonProperty("wikidata_id")
        val wikidataId: String,
        @all:JsonProperty("facebook_id")
        val facebookId: String,
        @all:JsonProperty("instagram_id")
        val instagramId: String,
        @all:JsonProperty("twitter_id")
        val twitterId: String,
    )

    @JsonSerializable
    data class MovieKeywords(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val keywords: List<NamedReference>,
    )

    @JsonSerializable
    data class MovieRecommendation(
        @all:JsonProperty
        val adult: Boolean = false,
        @all:JsonProperty("backdrop_path")
        val backdropPath: String? = null,
        @all:JsonProperty("genre_ids")
        val genreIds: List<Int>,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty("original_language")
        val originalLanguage: String,
        @all:JsonProperty("original_title")
        val originalTitle: String,
        @all:JsonProperty
        val overview: String,
        @all:JsonProperty
        val popularity: Double,
        @all:JsonProperty("poster_path")
        val posterPath: String? = null,
        @all:JsonProperty("release_date")
        val releaseDate: String,
        @all:JsonProperty
        val title: String,
        @all:JsonProperty
        val video: Boolean,
        @all:JsonProperty("vote_average")
        val voteAverage: Double,
        @all:JsonProperty("vote_count")
        val voteCount: Int,
    )

    @JsonSerializable
    data class MovieRecommendations(
        @all:JsonProperty
        val page: Int,
        @all:JsonProperty
        val results: List<MovieRecommendation>,
        @all:JsonProperty("total_pages")
        val totalPages: Int,
        @all:JsonProperty("total_results")
        val totalResults: Int,
    )

    @JsonSerializable
    data class MovieTranslationData(
        @all:JsonProperty
        val homepage: String = "",
        @all:JsonProperty
        val overview: String,
        @all:JsonProperty
        val runtime: Int,
        @all:JsonProperty
        val tagline: String,
        @all:JsonProperty
        val title: String,
    )

    typealias MovieTranslations = Translations<MovieTranslationData>

    @JsonSerializable
    data class ShowDetailsCreatedBy(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty("credit_id")
        val creditId: String,
        @all:JsonProperty
        val gender: Int,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty("profile_path")
        val profilePath: String? = null,
    )

    @JsonSerializable
    data class ShowDetailsLastEpisodeToAir(
        @all:JsonProperty("air_date")
        val airDate: String,
        @all:JsonProperty("episode_number")
        val episodeNumber: Int,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val overview: String,
        @all:JsonProperty("production_code")
        val productionCode: String,
        @all:JsonProperty
        val runtime: Int,
        @all:JsonProperty("show_id")
        val showId: Int,
        @all:JsonProperty("still_path")
        val stillPath: String? = null,
        @all:JsonProperty("vote_average")
        val voteAverage: Double,
        @all:JsonProperty("vote_count")
        val voteCount: Int,
        @all:JsonProperty("season_number")
        val seasonNumber: Int,
    )

    @JsonSerializable
    data class ShowDetailsSeason(
        @all:JsonProperty("air_date")
        val airDate: String,
        @all:JsonProperty("episode_count")
        val episodeCount: Int,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val overview: String,
        @all:JsonProperty("poster_path")
        val posterPath: String? = null,
        @all:JsonProperty("season_number")
        val seasonNumber: Int,
        @all:JsonProperty("vote_average")
        val voteAverage: Double,
    )

    @JsonSerializable
    data class ShowDetails(
        @all:JsonProperty
        val adult: Boolean = false,
        @all:JsonProperty("backdrop_path")
        val backdropPath: String? = null,
        @all:JsonProperty("created_by")
        val createdBy: List<ShowDetailsCreatedBy>,
        @all:JsonProperty("episode_runtime")
        val episodeRuntime: List<Int> = listOf(),
        @all:JsonProperty("first_air_date")
        val firstAirDate: String,
        @all:JsonProperty
        val genres: List<NamedReference> = listOf(),
        @all:JsonProperty
        val homepage: String = "",
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty("in_production")
        val inProduction: Boolean,
        @all:JsonProperty
        val languages: List<String>,
        @all:JsonProperty("last_air_date")
        val lastAirDate: String,
        @all:JsonProperty("last_episode_to_air")
        val lastEpisodeToAir: ShowDetailsLastEpisodeToAir,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty("next_episode_to_air")
        val nextEpisodeToAir: String? = null,
        @all:JsonProperty
        val networks: List<CompanyReference>,
        @all:JsonProperty("number_of_episodes")
        val numberOfEpisodes: Int,
        @all:JsonProperty("number_of_seasons")
        val numberOfSeasons: Int,
        @all:JsonProperty("origin_country")
        val originCountry: List<String> = listOf(),
        @all:JsonProperty("original_language")
        val originalLanguage: String,
        @all:JsonProperty("original_name")
        val originalName: String,
        @all:JsonProperty
        val overview: String,
        @all:JsonProperty
        val popularity: Double,
        @all:JsonProperty("poster_path")
        val posterPath: String? = null,
        @all:JsonProperty("production_companies")
        val productionCompanies: List<CompanyReference>,
        @all:JsonProperty("production_countries")
        val productionCountries: List<CountryReference>,
        @all:JsonProperty
        val seasons: List<ShowDetailsSeason>,
        @all:JsonProperty("spoken_languages")
        val spokenLanguages: List<LanguageReference>,
        @all:JsonProperty
        val status: String,
        @all:JsonProperty
        val tagline: String,
        @all:JsonProperty
        val type: String,
        @all:JsonProperty("vote_average")
        val voteAverage: Double,
        @all:JsonProperty("vote_count")
        val voteCount: Int,
    )

    @JsonSerializable
    data class ShowAlternativeTitles(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val results: List<AlternativeTitle>,
    )

    @JsonSerializable
    data class ShowContentRating(
        @all:JsonProperty
        val descriptors: List<Any>,
        @all:JsonProperty("iso_3166_1")
        val iso31661: String,
        @all:JsonProperty
        val rating: String,
    )

    @JsonSerializable
    data class ShowContentRatings(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val results: List<ShowContentRating>,
    )

    @JsonSerializable
    data class ShowEpisodeGroup(
        @all:JsonProperty
        val description: String,
        @all:JsonProperty("episode_count")
        val episodeCount: Int,
        @all:JsonProperty("group_count")
        val groupCount: Int,
        @all:JsonProperty
        val id: String,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val network: CompanyReference,
        @all:JsonProperty
        val type: Int,
    )

    @JsonSerializable
    data class ShowEpisodeGroups(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val results: List<ShowEpisodeGroup>,
    )

    @JsonSerializable
    data class ShowExternalIds(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty("imdb_id")
        val imdbId: String,
        @all:JsonProperty("freebase_mid")
        val freebaseMId: String,
        @all:JsonProperty("freebase_id")
        val freebaseId: String,
        @all:JsonProperty("tvdb_id")
        val tvdbId: Int,
        @all:JsonProperty("tvrage_id")
        val tvrageId: Int,
        @all:JsonProperty("wikidata_id")
        val wikidataId: String,
        @all:JsonProperty("facebook_id")
        val facebookId: String,
        @all:JsonProperty("instagram_id")
        val instagramId: String,
        @all:JsonProperty("twitter_id")
        val twitterId: String,
    )

    @JsonSerializable
    data class ShowKeywords(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val results: List<NamedReference>,
    )

    @JsonSerializable
    data class ShowRecommendation(
        @all:JsonProperty
        val adult: Boolean = false,
        @all:JsonProperty("backdrop_path")
        val backdropPath: String? = null,
        @all:JsonProperty("first_air_date")
        val firstAirDate: String,
        @all:JsonProperty("genre_ids")
        val genreIds: List<Int>,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty("original_language")
        val originalLanguage: String,
        @all:JsonProperty("original_name")
        val originalName: String,
        @all:JsonProperty("origin_country")
        val originCountry: List<String> = listOf(),
        @all:JsonProperty
        val overview: String,
        @all:JsonProperty
        val popularity: Double,
        @all:JsonProperty("poster_path")
        val posterPath: String? = null,
        @all:JsonProperty("vote_average")
        val voteAverage: Double,
        @all:JsonProperty("vote_count")
        val voteCount: Int,
    )

    @JsonSerializable
    data class ShowRecommendations(
        @all:JsonProperty
        val page: Int,
        @all:JsonProperty
        val results: List<ShowRecommendation>,
        @all:JsonProperty("total_pages")
        val totalPages: Int,
        @all:JsonProperty("total_results")
        val totalResults: Int,
    )

    @JsonSerializable
    data class ShowSimilar(
        @all:JsonProperty
        val page: Int,
        @all:JsonProperty
        val results: List<ShowRecommendation>,
        @all:JsonProperty("total_pages")
        val totalPages: Int,
        @all:JsonProperty("total_results")
        val totalResults: Int,
    )

    @JsonSerializable
    data class ShowTranslationData(
        @all:JsonProperty
        val homepage: String = "",
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val overview: String,
        @all:JsonProperty
        val tagline: String,
    )

    typealias ShowTranslations = Translations<ShowTranslationData>

    @JsonSerializable
    data class SeasonDetailsEpisode(
        @all:JsonProperty("air_date")
        val airDate: String? = null,
        @all:JsonProperty
        val crew: List<CrewReference>,
        @all:JsonProperty("episode_number")
        val episodeNumber: Int,
        @all:JsonProperty("episode_type")
        val episodeType: String,
        @all:JsonProperty("guest_stars")
        val guestStars: List<CastReference>,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val overview: String,
        @all:JsonProperty("production_code")
        val productionCode: String,
        @all:JsonProperty
        val runtime: Int,
        @all:JsonProperty("season_number")
        val seasonNumber: Int,
        @all:JsonProperty("show_id")
        val showId: Int,
        @all:JsonProperty("still_path")
        val stillPath: String? = null,
        @all:JsonProperty("vote_average")
        val voteAverage: Double,
        @all:JsonProperty("vote_count")
        val voteCount: Int,
    )

    @JsonSerializable
    data class SeasonDetails(
        @all:JsonProperty("_id")
        val internalId: String,
        @all:JsonProperty("air_date")
        val airDate: String,
        @all:JsonProperty
        val episodes: List<SeasonDetailsEpisode>,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val networks: List<CompanyReference>,
        @all:JsonProperty
        val overview: String,
        @all:JsonProperty("poster_path")
        val posterPath: String? = null,
        @all:JsonProperty("season_number")
        val seasonNumber: Int,
        @all:JsonProperty("vote_average")
        val voteAverage: Double,
    )

    @JsonSerializable
    data class SeasonExternalIds(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty("freebase_mid")
        val freebaseMId: String,
        @all:JsonProperty("freebase_id")
        val freebaseId: String,
        @all:JsonProperty("tvdb_id")
        val tvdbId: Int,
        @all:JsonProperty("tvrage_id")
        val tvrageId: String,
        @all:JsonProperty("wikidata_id")
        val wikidataId: String,
    )

    @JsonSerializable
    data class SeasonTranslationData(
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val overview: String,
    )

    typealias SeasonTranslations = Translations<SeasonTranslationData>

    @JsonSerializable
    data class EpisodeDetails(
        @all:JsonProperty("air_date")
        val airDate: String? = null,
        @all:JsonProperty
        val crew: List<CrewReference>,
        @all:JsonProperty("episode_number")
        val episodeNumber: Int,
        @all:JsonProperty("guest_stars")
        val guestStars: List<CastReference>,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val overview: String,
        @all:JsonProperty("production_code")
        val productionCode: String,
        @all:JsonProperty
        val runtime: Int,
        @all:JsonProperty("season_number")
        val seasonNumber: Int,
        @all:JsonProperty("still_path")
        val stillPath: String?,
        @all:JsonProperty("vote_average")
        val voteAverage: Double,
        @all:JsonProperty("vote_count")
        val voteCount: Int,
    )

    @JsonSerializable
    data class EpisodeCredits(
        @all:JsonProperty
        val cast: List<CastReference>,
        @all:JsonProperty
        val crew: List<CrewReference>,
        @all:JsonProperty
        val guestStars: List<CastReference>,
        @all:JsonProperty
        val id: Int,
    )

    @JsonSerializable
    data class EpisodeExternalIds(
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty("imdb_id")
        val imdbId: String,
        @all:JsonProperty("freebase_mid")
        val freebaseMId: String,
        @all:JsonProperty("freebase_id")
        val freebaseId: String,
        @all:JsonProperty("tvdb_id")
        val tvdbId: Int,
        @all:JsonProperty("tvrage_id")
        val tvrageId: Int,
        @all:JsonProperty("wikidata_id")
        val wikidataId: String,
    )

    @JsonSerializable
    data class EpisodeTranslationData(
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val overview: String,
    )

    typealias EpisodeTranslations = Translations<EpisodeTranslationData>

    @JsonSerializable
    data class EpisodeGroupEpisode(
        @all:JsonProperty("air_date")
        val airDate: String,
        @all:JsonProperty("episode_number")
        val episodeNumber: Int,
        @all:JsonProperty
        val id: Int,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val order: Int,
        @all:JsonProperty
        val overview: String,
        @all:JsonProperty("production_code")
        val productionCode: String,
        @all:JsonProperty
        val runtime: String,
        @all:JsonProperty("season_number")
        val seasonNumber: Int,
        @all:JsonProperty("show_id")
        val showId: Int,
        @all:JsonProperty("still_path")
        val stillPath: String,
        @all:JsonProperty("vote_average")
        val voteAverage: Double,
        @all:JsonProperty("vote_count")
        val voteCount: Int,
    )

    @JsonSerializable
    data class EpisodeGroup(
        @all:JsonProperty
        val episodes: List<EpisodeGroupEpisode>,
        @all:JsonProperty
        val id: String,
        @all:JsonProperty
        val locked: Boolean,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val order: Int,
    )

    @JsonSerializable
    data class EpisodeGroupDetails(
        @all:JsonProperty
        val description: String,
        @all:JsonProperty("episode_count")
        val episodeCount: Int,
        @all:JsonProperty("group_count")
        val groupCount: Int,
        @all:JsonProperty
        val groups: List<EpisodeGroup>,
        @all:JsonProperty
        val id: String,
        @all:JsonProperty
        val name: String,
        @all:JsonProperty
        val network: CompanyReference,
        @all:JsonProperty
        val type: Int,
    )

    private val cache = mutableMapOf<String, Any?>()

    context(provider: Provider, log: Logger)
    private inline fun <reified T> getData(
        resource: String,
        params: Map<String, Any?> = mapOf(),
    ): T? {
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

        val uri = "https://api.themoviedb.org/3/${resource}${query}"

        if (uri in cache) {
            return cache[uri] as? T
        }

        val token: String = provider.getT("tmdb-token")
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

        if (json is JsonObjectNode) {
            val success = json["success"]
            val statusCode = json["status_code"]
            val statusMessage = json["status_message"]

            if (
                success is JsonBooleanNode &&
                statusCode is JsonNumberNode &&
                statusMessage is JsonStringNode
            ) {
                log.warning("at $resource: (success=$success, status_code=$statusCode, status_message=$statusMessage)")

                cache[uri] = null
                return null
            }
        }

        val value = json.fromJson<T>()
        cache[uri] = value
        return value
    }

    context(_: Provider, _: Logger)
    private inline fun <reified T> getMovieData(
        resource: String?,
        movieId: Int,
        params: Map<String, Any?> = mapOf(),
    ): T? {
        val resource =
            if (resource == null) "movie/$movieId"
            else "movie/$movieId/$resource"
        return getData(resource, params)
    }

    context(_: Provider, _: Logger)
    private inline fun <reified T> getShowData(
        resource: String?,
        showId: Int,
        params: Map<String, Any?> = mapOf(),
    ): T? {
        val resource =
            if (resource == null) "tv/$showId"
            else "tv/$showId/$resource"
        return getData(resource, params)
    }

    context(_: Provider, _: Logger)
    private inline fun <reified T> getSeasonData(
        resource: String?,
        showId: Int,
        seasonNumber: Int,
        params: Map<String, Any?> = mapOf(),
    ): T? {
        val resource =
            if (resource == null) "tv/$showId/season/$seasonNumber"
            else "tv/$showId/season/$seasonNumber/$resource"
        return getData(resource, params)
    }

    context(_: Provider, _: Logger)
    private inline fun <reified T> getEpisodeData(
        resource: String?,
        showId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
        params: Map<String, Any?> = mapOf(),
    ): T? {
        val resource =
            if (resource == null) "tv/$showId/season/$seasonNumber/episode/$episodeNumber"
            else "tv/$showId/season/$seasonNumber/episode/$episodeNumber/$resource"
        return getData(resource, params)
    }

    context(_: Provider, _: Logger)
    fun getConfiguration(): Configuration? {
        return getData("configuration")
    }

    context(_: Provider, _: Logger)
    fun getMovieDetails(
        movieId: Int,
        language: String? = null,
    ): MovieDetails? {
        return getMovieData(
            null,
            movieId,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getMovieAlternativeTitles(movieId: Int): MovieAlternativeTitles? {
        return getMovieData("alternative_titles", movieId)
    }

    context(_: Provider, _: Logger)
    fun getMovieCredits(
        movieId: Int,
        language: String? = null,
    ): MovieCredits? {
        return getMovieData(
            "credits",
            movieId,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getMovieExternalIds(movieId: Int): MovieExternalIds? {
        return getMovieData("external_ids", movieId)
    }

    context(_: Provider, _: Logger)
    fun getMovieImages(
        movieId: Int,
        language: String? = null,
    ): ImageReferences? {
        return getMovieData(
            "images",
            movieId,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getMovieKeywords(movieId: Int): MovieKeywords? {
        return getMovieData("keywords", movieId)
    }

    context(_: Provider, _: Logger)
    fun getMovieRecommendations(
        movieId: Int,
        language: String? = null,
        page: Int = 1,
    ): MovieRecommendations? {
        return getMovieData(
            "recommendations",
            movieId,
            mapOf(
                "language" to language,
                "page" to page,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getMovieSimilar(
        movieId: Int,
        language: String? = null,
        page: Int = 1,
    ): MovieRecommendations? {
        return getMovieData(
            "similar",
            movieId,
            mapOf(
                "language" to language,
                "page" to page,
            )
        )
    }

    context(_: Provider, _: Logger)
    fun getMovieTranslations(movieId: Int): MovieTranslations? {
        return getMovieData("translations", movieId)
    }

    context(_: Provider, _: Logger)
    fun getMovieVideos(
        movieId: Int,
        language: String? = null,
    ): VideoReferences? {
        return getMovieData(
            "videos",
            movieId,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getShowDetails(
        showId: Int,
        language: String? = null,
    ): ShowDetails? {
        return getShowData(
            null,
            showId,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getShowAggregateCredits(
        showId: Int,
        language: String? = null,
    ): AggregateCredits? {
        return getShowData(
            "aggregate_credits",
            showId,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getShowAlternativeTitles(showId: Int): ShowAlternativeTitles? {
        return getShowData("alternative_titles", showId)
    }

    context(_: Provider, _: Logger)
    fun getShowContentRatings(showId: Int): ShowContentRatings? {
        return getShowData("content_ratings", showId)
    }

    context(_: Provider, _: Logger)
    fun getShowEpisodeGroups(showId: Int): ShowEpisodeGroups? {
        return getShowData("episode_groups", showId)
    }

    context(_: Provider, _: Logger)
    fun getShowExternalIds(showId: Int): ShowExternalIds? {
        return getShowData("external_ids", showId)
    }

    context(_: Provider, _: Logger)
    fun getShowImages(
        showId: Int,
        language: String? = null,
    ): ImageReferences? {
        return getShowData(
            "images",
            showId,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getShowKeywords(showId: Int): ShowKeywords? {
        return getShowData("keywords", showId)
    }

    context(_: Provider, _: Logger)
    fun getShowRecommendations(
        showId: Int,
        language: String? = null,
        page: Int = 1,
    ): ShowRecommendations? {
        return getShowData(
            "recommendations",
            showId,
            mapOf(
                "language" to language,
                "page" to page,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getShowSimilar(
        showId: Int,
        language: String? = null,
        page: Int = 1,
    ): ShowSimilar? {
        return getShowData(
            "similar",
            showId,
            mapOf(
                "language" to language,
                "page" to page,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getShowTranslations(showId: Int): ShowTranslations? {
        return getShowData("translations", showId)
    }

    context(_: Provider, _: Logger)
    fun getShowVideos(
        showId: Int,
        language: String? = null,
    ): VideoReferences? {
        return getShowData(
            "videos",
            showId,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getSeasonDetails(
        showId: Int,
        seasonNumber: Int,
        language: String? = null,
    ): SeasonDetails? {
        return getSeasonData(
            null,
            showId,
            seasonNumber,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getSeasonAggregateCredits(
        showId: Int,
        seasonNumber: Int,
        language: String? = null,
    ): AggregateCredits? {
        return getSeasonData(
            "aggregate_credits",
            showId,
            seasonNumber,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getSeasonExternalIds(
        showId: Int,
        seasonNumber: Int,
    ): SeasonExternalIds? {
        return getSeasonData(
            "external_ids",
            showId,
            seasonNumber,
        )
    }

    context(_: Provider, _: Logger)
    fun getSeasonImages(
        showId: Int,
        seasonNumber: Int,
        language: String? = null,
    ): ImageReferences? {
        return getSeasonData(
            "images",
            showId,
            seasonNumber,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getSeasonTranslations(
        showId: Int,
        seasonNumber: Int,
    ): SeasonTranslations? {
        return getSeasonData(
            "translations",
            showId,
            seasonNumber,
        )
    }

    context(_: Provider, _: Logger)
    fun getSeasonVideos(
        showId: Int,
        seasonNumber: Int,
        language: String? = null,
    ): VideoReferences? {
        return getSeasonData(
            "videos",
            showId,
            seasonNumber,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getEpisodeDetails(
        showId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
        language: String? = null,
    ): EpisodeDetails? {
        return getEpisodeData(
            null,
            showId,
            seasonNumber,
            episodeNumber,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getEpisodeCredits(
        showId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
        language: String? = null,
    ): EpisodeCredits? {
        return getEpisodeData(
            "credits",
            showId,
            seasonNumber,
            episodeNumber,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getEpisodeExternalIds(
        showId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    ): EpisodeExternalIds? {
        return getEpisodeData(
            "external_ids",
            showId,
            seasonNumber,
            episodeNumber,
        )
    }

    context(_: Provider, _: Logger)
    fun getEpisodeImages(
        showId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
        language: String? = null,
    ): ImageReferences? {
        return getEpisodeData(
            "images",
            showId,
            seasonNumber,
            episodeNumber,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getEpisodeTranslations(
        showId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
    ): EpisodeTranslations? {
        return getEpisodeData(
            "translations",
            showId,
            seasonNumber,
            episodeNumber,
        )
    }

    context(_: Provider, _: Logger)
    fun getEpisodeVideos(
        showId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
        language: String? = null,
    ): VideoReferences? {
        return getEpisodeData(
            "videos",
            showId,
            seasonNumber,
            episodeNumber,
            mapOf(
                "language" to language,
            ),
        )
    }

    context(_: Provider, _: Logger)
    fun getEpisodeGroupDetails(groupId: String): EpisodeGroupDetails? {
        return getData("episode_group/$groupId")
    }
}
