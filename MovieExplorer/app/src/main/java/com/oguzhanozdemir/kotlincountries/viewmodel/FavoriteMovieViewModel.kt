package com.oguzhanozdemir.kotlincountries.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.oguzhanozdemir.kotlincountries.model.FavoriteMovie
import com.oguzhanozdemir.kotlincountries.model.Movie
import com.oguzhanozdemir.kotlincountries.services.FavoriteMovieRepository
import com.oguzhanozdemir.kotlincountries.services.MovieDatabase
import kotlinx.coroutines.launch

class FavoriteMovieViewModel(application: Application) : BaseViewModel(application) {

    val favoriteMovies = MutableLiveData<List<FavoriteMovie>>()
    val loading = MutableLiveData<Boolean>()
    val error = MutableLiveData<Boolean>()
    
    private val dao = MovieDatabase(getApplication()).favoriteMovieDao()
    private val repository = FavoriteMovieRepository(dao)

    fun toggleFavorite(movie: Movie) = viewModelScope.launch {
        try {
            loading.value = true
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val isAlreadyFavorite = repository.isFavorite(userId, movie.id.toString())

            if (isAlreadyFavorite) {
                repository.removeFavorite(userId, movie.id)
                Log.d("FavoriteViewModel", "Removed movie ${movie.title} from favorites")
            } else {
                val favorite = FavoriteMovie(
                    userId = userId,
                    movieId = movie.id,
                    title = movie.title,
                    posterPath = movie.posterPath,
                    overview = movie.overview,
                    releaseDate = movie.releaseDate,
                    voteAverage = movie.voteAverage,
                    genreNames = movie.genreNames
                )
                repository.addFavorite(favorite)
                Log.d("FavoriteViewModel", "Added movie ${movie.title} to favorites")
            }

            // Listeyi güncelle
            getFavoriteMovies()
        } catch (e: Exception) {
            Log.e("FavoriteViewModel", "Error toggling favorite status", e)
            error.value = true
        } finally {
            loading.value = false
        }
    }

    fun getFavoriteMovies() = viewModelScope.launch {
        try {
            loading.value = true
            error.value = false
            
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val favorites = repository.getFavorites(userId)
                Log.d("FavoriteViewModel", "Loaded ${favorites.size} favorite movies")
                favoriteMovies.value = favorites
            } else {
                Log.d("FavoriteViewModel", "No user logged in, returning empty favorites list")
                favoriteMovies.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e("FavoriteViewModel", "Error loading favorite movies", e)
            favoriteMovies.value = emptyList()
            error.value = true
        } finally {
            loading.value = false
        }
    }

    suspend fun isMovieFavorite(movieId: Int): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return repository.isFavorite(userId, movieId.toString())
    }
    
    init {
        // Initialize with default values
        loading.value = false
        error.value = false
    }
}