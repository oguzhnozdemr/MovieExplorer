package com.oguzhanozdemir.kotlincountries.services
import com.oguzhanozdemir.kotlincountries.model.Movie
import com.oguzhanozdemir.kotlincountries.model.MovieResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieAPI {



    //Base -> "https://api.themoviedb.org/3/"
    //Ext -> "movie/popular"
    @GET("movie/popular")

    fun getPopularMovies(
        @Query("api_key") apiKey: String = "537fb921c9ff556260576accbc62c2e2"
    ): Single<MovieResponse>

    // Türe göre
    @GET("discover/movie")
    fun getMoviesByGenre(
        @Query("api_key") apiKey: String = "537fb921c9ff556260576accbc62c2e2",
        @Query("with_genres") genreId: Int // 18, Drama türü için ID
    ): Single<MovieResponse>
    
    // Film arama
    @GET("search/movie")
    fun searchMovies(
        @Query("api_key") apiKey: String = "537fb921c9ff556260576accbc62c2e2",
        @Query("query") searchQuery: String
    ): Single<MovieResponse>
    
    // Film detayları
    @GET("movie/{movie_id}")
    fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String = "537fb921c9ff556260576accbc62c2e2"
    ): Single<Movie>
}