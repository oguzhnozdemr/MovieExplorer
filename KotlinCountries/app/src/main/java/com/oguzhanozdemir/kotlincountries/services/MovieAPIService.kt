package com.oguzhanozdemir.kotlincountries.services

import com.oguzhanozdemir.kotlincountries.model.Movie
import io.reactivex.rxjava3.core.Single
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class MovieAPIService {
    //Base -> https://raw.githubusercontent.com/
    //Ext -> atilsamancioglu/IA19-DataSetCountries/master/countrydataset.json

    private val BASE_URL = "https://api.themoviedb.org/3/"
    private val api = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
        .build()
        .create(MovieAPI::class.java)

    // Popüler filmleri çek
    fun getPopularMovieData(): Single<List<Movie>> {
        return api.getPopularMovies()
            .map { response -> response.results }
    }
    
    // Belirli bir türdeki filmleri çek
    fun getMoviesByGenre(genreId: Int): Single<List<Movie>> {
        return api.getMoviesByGenre(genreId = genreId)
            .map { response -> response.results }
    }
    
    // Film ara
    fun searchMovies(query: String): Single<List<Movie>> {
        return api.searchMovies(searchQuery = query)
            .map { response -> response.results }
    }
    
    // Film detaylarını getir
    fun getMovieDetails(movieId: Int): Single<Movie> {
        return api.getMovieDetails(movieId = movieId)
    }
}