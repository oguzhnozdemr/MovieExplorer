package com.oguzhanozdemir.kotlincountries.services

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.oguzhanozdemir.kotlincountries.model.FavoriteMovie
import com.oguzhanozdemir.kotlincountries.model.Movie
import java.io.File

// Increase database version to 6
@Database(entities = [Movie::class,FavoriteMovie::class], version = 8)
abstract class MovieDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDAO
    abstract fun favoriteMovieDao(): FavoriteMovieDao
    // Singleton
    companion object {
        @Volatile private var instance: MovieDatabase? = null
        private val lock = Any()
        
        // Migration from 5 to 6 - add a callback to log database creation
        val MIGRATION_5_6 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // This is just to force recreation with version 6
                database.execSQL("CREATE TABLE IF NOT EXISTS movie_temp AS SELECT * FROM movie")
                database.execSQL("DROP TABLE movie")
                database.execSQL("ALTER TABLE movie_temp RENAME TO movie")
                
                // Make sure isFavorite column exists and has proper default
                try {
                    database.execSQL("ALTER TABLE movie ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // Column might already exist, which is fine
                    Log.d("MovieDatabase", "isFavorite column already exists")
                }
            }
        }


        operator fun invoke(context: Context) = instance ?: synchronized(lock) {
            instance ?: makeDatabase(context).also {
                instance = it
            }
        }

        private fun makeDatabase(context: Context): MovieDatabase {
            // Don't delete the database - we need to persist favorites
            Log.d("MovieDatabase", "Creating new database instance")
            
            return Room.databaseBuilder(
                context.applicationContext,
                MovieDatabase::class.java, "moviedatabase"
            )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Log.d("MovieDatabase", "Database created fresh")
                    // Pre-create the essential tables and columns
                    try {
                        // Make sure isFavorite column exists 
                        db.execSQL("ALTER TABLE movie ADD COLUMN IF NOT EXISTS isFavorite INTEGER NOT NULL DEFAULT 0")
                    } catch (e: Exception) {
                        Log.e("MovieDatabase", "Error in onCreate callback", e)
                    }
                }
            })
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}