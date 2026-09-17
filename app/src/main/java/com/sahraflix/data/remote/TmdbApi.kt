package com.sahraflix.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("discover/movie")
    suspend fun discoverMovies(@Query("page") page: Int, @Query("api_key") apiKey: String): Response<TmdbPageDto<TmdbMovieDto>>

    @GET("discover/tv")
    suspend fun discoverTv(@Query("page") page: Int, @Query("api_key") apiKey: String): Response<TmdbPageDto<TmdbTvDto>>

    @GET("search/multi")
    suspend fun searchMulti(@Query("query") query: String, @Query("page") page: Int, @Query("api_key") apiKey: String): Response<TmdbPageDto<TmdbSearchDto>>

    @GET("movie/{id}")
    suspend fun movie(@Path("id") id: Int, @Query("append_to_response") append: String, @Query("api_key") apiKey: String): Response<TmdbMovieDto>

    @GET("tv/{id}")
    suspend fun tv(@Path("id") id: Int, @Query("append_to_response") append: String, @Query("api_key") apiKey: String): Response<TmdbTvDto>

    @GET("tv/{id}/season/{season}")
    suspend fun season(@Path("id") id: Int, @Path("season") season: Int, @Query("api_key") apiKey: String): Response<TmdbSeasonDto>

    @GET("tv/{id}/season/{season}/episode/{episode}")
    suspend fun episode(@Path("id") id: Int, @Path("season") season: Int, @Path("episode") episode: Int, @Query("api_key") apiKey: String): Response<TmdbEpisodeDto>
}

data class TmdbPageDto<T>(val page: Int, val total_pages: Int, val results: List<T> = emptyList())
data class TmdbMovieDto(val id: Int, val title: String?, val overview: String?, val poster_path: String?, val backdrop_path: String?, val release_date: String?, val vote_average: Double?, val runtime: Int? = null, val genres: List<TmdbGenreDto> = emptyList(), val credits: TmdbCreditsDto? = null)
data class TmdbTvDto(val id: Int, val name: String?, val overview: String?, val poster_path: String?, val backdrop_path: String?, val first_air_date: String?, val vote_average: Double?, val seasons: List<TmdbSeasonDto> = emptyList(), val genres: List<TmdbGenreDto> = emptyList(), val credits: TmdbCreditsDto? = null)
data class TmdbSeasonDto(val id: Int, val season_number: Int, val name: String?, val overview: String?, val poster_path: String?, val episode_count: Int, val episodes: List<TmdbEpisodeDto> = emptyList())
data class TmdbEpisodeDto(val id: Int, val episode_number: Int, val season_number: Int, val name: String?, val overview: String?, val still_path: String?, val air_date: String?, val vote_average: Double?)
data class TmdbGenreDto(val id: Int, val name: String)
data class TmdbCreditsDto(val cast: List<TmdbCastDto> = emptyList(), val crew: List<TmdbCrewDto> = emptyList())
data class TmdbCastDto(val id: Int, val name: String, val character: String?, val profile_path: String?)
data class TmdbCrewDto(val id: Int, val name: String, val department: String?, val job: String?, val profile_path: String?)
data class TmdbSearchDto(val id: Int, val media_type: String?, val title: String?, val name: String?, val overview: String?, val poster_path: String?, val backdrop_path: String?, val release_date: String?, val first_air_date: String?, val vote_average: Double?)
