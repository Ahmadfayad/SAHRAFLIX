package com.sahraflix.data.remote

import com.sahraflix.domain.model.TmdbCastMember
import com.sahraflix.domain.model.TmdbCrewMember
import com.sahraflix.domain.model.TmdbEpisode
import com.sahraflix.domain.model.TmdbMovie
import com.sahraflix.domain.model.TmdbSeason
import com.sahraflix.domain.model.TmdbTvShow
import kotlinx.coroutines.delay
import javax.inject.Inject

class TmdbClient @Inject constructor(
    private val api: TmdbApi,
    private val apiKey: String
) {
    suspend fun discoverMovies(page: Int): TmdbPageDto<TmdbMovieDto> =
        request { api.discoverMovies(page, requireApiKey()) }

    suspend fun discoverTv(page: Int): TmdbPageDto<TmdbTvDto> =
        request { api.discoverTv(page, requireApiKey()) }

    suspend fun movie(id: Int): TmdbMovie = request { api.movie(id, "credits", requireApiKey()) }.let { it.toDomain() }
    suspend fun tv(id: Int): TmdbTvShow = request { api.tv(id, "credits", requireApiKey()) }.let { it.toDomain() }
    suspend fun season(id: Int, number: Int): TmdbSeason = request { api.season(id, number, requireApiKey()) }.toDomain()
    suspend fun episode(id: Int, season: Int, episode: Int): TmdbEpisode = request { api.episode(id, season, episode, requireApiKey()) }.toDomain()

    suspend fun search(query: String, page: Int = 1): TmdbPageDto<TmdbSearchDto> =
        request { api.searchMulti(query, page, requireApiKey()) }

    private fun requireApiKey(): String = apiKey.trim().takeIf { it.isNotEmpty() }
        ?: error("TMDB API key is not configured")

    private suspend fun <T> request(call: suspend () -> retrofit2.Response<T>): T {
        repeat(MAX_ATTEMPTS) { attempt ->
            val response = call()
            if (response.isSuccessful) return response.body() ?: error("TMDB returned an empty response")
            if (response.code() != 429 || attempt == MAX_ATTEMPTS - 1) {
                error("TMDB request failed: ${response.code()}")
            }
            delay((attempt + 1) * RETRY_DELAY_MS)
        }
        error("TMDB request exhausted retries")
    }

    private fun image(path: String?): String? = path?.let { IMAGE_BASE_URL + it }
    private fun year(date: String?): Int? = date?.take(4)?.toIntOrNull()

    private fun TmdbMovieDto.toDomain() = TmdbMovie(id, title.orEmpty(), overview, image(poster_path), image(backdrop_path), year(release_date), vote_average, runtime, genres.map { it.name }, credits?.cast.orEmpty().map { TmdbCastMember(it.id, it.name, it.character, image(it.profile_path)) }, credits?.crew.orEmpty().map { TmdbCrewMember(it.id, it.name, it.department, it.job, image(it.profile_path)) })
    private fun TmdbTvDto.toDomain() = TmdbTvShow(id, name.orEmpty(), overview, image(poster_path), image(backdrop_path), year(first_air_date), vote_average, genres.map { it.name }, seasons.map { it.toSummary() }, credits?.cast.orEmpty().map { TmdbCastMember(it.id, it.name, it.character, image(it.profile_path)) }, credits?.crew.orEmpty().map { TmdbCrewMember(it.id, it.name, it.department, it.job, image(it.profile_path)) })
    private fun TmdbSeasonDto.toDomain() = TmdbSeason(id, season_number, name.orEmpty(), overview, image(poster_path), episode_count, episodes.map { it.toDomain() })
    private fun TmdbSeasonDto.toSummary() = TmdbSeason(id, season_number, name.orEmpty(), overview, image(poster_path), episode_count)
    private fun TmdbEpisodeDto.toDomain() = TmdbEpisode(id, episode_number, season_number, name.orEmpty(), overview, image(still_path), air_date, vote_average)

    private companion object {
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w780"
        const val MAX_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 500L
    }
}
