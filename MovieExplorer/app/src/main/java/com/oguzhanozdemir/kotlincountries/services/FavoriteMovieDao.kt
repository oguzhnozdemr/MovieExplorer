package com.oguzhanozdemir.kotlincountries.services

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oguzhanozdemir.kotlincountries.model.FavoriteMovie
@Dao
interface FavoriteMovieDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriMovie(movie: FavoriteMovie)

    @Query("DELETE FROM favorites WHERE user_id =:userId AND movie_id =:movieId ")
    suspend fun removeFavorite(userId: String,movieId:Int)


    @Query("SELECT * FROM favorites WHERE user_id = :userId")
    suspend fun getFavoritesByUser(userId: String): List<FavoriteMovie>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE user_id = :userId AND movie_id = :movieId)")
    suspend fun isFavorite(userId: String, movieId: kotlin.String): Boolean
}