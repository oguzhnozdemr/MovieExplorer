package com.oguzhanozdemir.kotlincountries.services
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oguzhanozdemir.kotlincountries.model.Movie

@Dao
interface MovieDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg movies: Movie): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: Movie): Long
    
    @Query("INSERT OR REPLACE INTO movie (id, title, posterPath, overview, releaseDate, voteAverage, genreNames) VALUES (:id, :title, :posterPath, COALESCE(:overview, ''), COALESCE(:releaseDate, ''), :voteAverage, COALESCE(:genreNames, ''))")
    suspend fun safeInsertMovie(
        id: Int, 
        title: String, 
        posterPath: String?, 
        overview: String?, 
        releaseDate: String?, 
        voteAverage: Float, 
        genreNames: String?
    ): Long
    

    @Query("INSERT OR REPLACE INTO movie (id, title, posterPath, overview, releaseDate, voteAverage, genreNames) VALUES (:id, :title, null, '', '', :voteAverage, '')")
    suspend fun emergencyInsertMovie(
        id: Int,
        title: String,
        voteAverage: Float
    ): Long

    @Query("SELECT * FROM movie")
    suspend fun getAllMovies(): List<Movie>

    @Query("SELECT * FROM movie WHERE id = :movieId")
    suspend fun getMovie(movieId: Int): Movie
    
    @Query("SELECT * FROM movie WHERE id = :movieId LIMIT 1")
    suspend fun getMovieSafely(movieId: Int): Movie?

    @Query("DELETE FROM movie")
    suspend fun deleteAll()
    

    @Query("SELECT COUNT(*) FROM movie WHERE id = :movieId")
    suspend fun movieExists(movieId: Int): Int
} 