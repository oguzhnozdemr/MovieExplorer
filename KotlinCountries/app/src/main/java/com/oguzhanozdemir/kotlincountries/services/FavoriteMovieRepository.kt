package com.oguzhanozdemir.kotlincountries.services

import com.oguzhanozdemir.kotlincountries.model.FavoriteMovie

class FavoriteMovieRepository(private val dao: FavoriteMovieDao) {

    suspend fun addFavorite(movie: FavoriteMovie) {
        dao.insertFavoriMovie(movie)
    }

    suspend fun removeFavorite(userId: String, movieId: Int) {
        dao.removeFavorite(userId, movieId)
    }

    suspend fun getFavorites(userId: String): List<FavoriteMovie> {
        return dao.getFavoritesByUser(userId)
    }

    suspend fun isFavorite(userId: String, movieId: String): Boolean {
        return dao.isFavorite(userId, movieId)
    }
}