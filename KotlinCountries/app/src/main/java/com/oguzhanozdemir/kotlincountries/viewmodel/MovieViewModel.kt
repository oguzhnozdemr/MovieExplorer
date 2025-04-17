package com.oguzhanozdemir.kotlincountries.viewmodel

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.oguzhanozdemir.kotlincountries.model.FavoriteMovie
import com.oguzhanozdemir.kotlincountries.model.Movie
import com.oguzhanozdemir.kotlincountries.model.Note
import com.oguzhanozdemir.kotlincountries.services.MovieAPIService
import com.oguzhanozdemir.kotlincountries.services.MovieDatabase
import com.oguzhanozdemir.kotlincountries.services.MovieNotesService
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.observers.DisposableSingleObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch


class MovieViewModel(application: Application): BaseViewModel(application) {

    val movieLiveData = MutableLiveData<Movie>()
    private val movieAPIService = MovieAPIService()
    private val disposable = CompositeDisposable()
    private val notesService = MovieNotesService()
    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> = _notes
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun getDataFromRoom(uuid: Int){
        Log.d("MovieViewModel", "Film ID ile veri çekiliyor: $uuid")

        launch{
            try {
                val dao = MovieDatabase(getApplication()).movieDao()
                val movie = dao.getMovieSafely(uuid)
                
                if (movie != null) {

                    val genreList = if (movie.genreNames?.isNotEmpty() == true) {
                        movie.genreNames!!.split(",").map {
                            // Basit bir şekilde ad bilgisini kullanıyoruz, ID önemsiz
                            com.oguzhanozdemir.kotlincountries.model.Genre(0, it.trim())
                        }
                    } else {
                        emptyList()
                    }
                    
                    try {

                        val movieWithGenres = Movie(
                            id = movie.id,
                            title = movie.title,
                            posterPath = movie.posterPath,
                            overview = movie.overview ?: "",
                            releaseDate = movie.releaseDate ?: "",
                            voteAverage = movie.voteAverage,
                            genres = genreList,
                            genreNames = movie.genreNames
                        )
                        
                        movieLiveData.value = movieWithGenres
                    } catch (e: Exception) {
                        Log.e("MovieViewModel", "Film nesnesi oluşturulurken hata oluştu", e)
                        // Hata durumunda API'den çekelim
                        getMovieDetailsFromAPI(uuid)
                    }
                } else {
                    Log.d("MovieViewModel", "Film SQLite'da bulunamadı, API'den alınacak")
                    // SQLite'da film bulunamadıysa API'den çekelim
                    getMovieDetailsFromAPI(uuid)
                }
            } catch(e: Exception) {
                Log.e("MovieViewModel", "Film veritabanından çekerken hata oluştu", e)
                // Hata durumunda API'dan çekelim
                getMovieDetailsFromAPI(uuid)
            }
        }
    }
    
    private fun getMovieDetailsFromAPI(movieId: Int) {
        if (movieId <= 0) {
            Log.e("MovieViewModel", "Geçersiz film ID: $movieId")
            return
        }
        
        Log.d("MovieViewModel", "Film detayları API'dan alınıyor: $movieId")
        
        disposable.add(
            movieAPIService.getMovieDetails(movieId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<Movie>() {
                    override fun onSuccess(movie: Movie) {
                        Log.d("MovieViewModel", "Film API'dan başarıyla alındı: ${movie.title}")
                        
                        // API'den gelen türleri genreNames alanına kaydet
                        if (movie.genres.isNotEmpty()) {
                            movie.genreNames = movie.genres.joinToString(",") { it.name?.ifEmpty { "Unknown" } ?: "Unknown" }
                            Log.d("MovieViewModel", "Film türleri: ${movie.genreNames}")
                        } else {
                            // Genres boş ise, boş string ata (null olmasını engelle)
                            movie.genreNames = ""
                        }

                        // Update LiveData and save to database
                        movieLiveData.value = movie
                        saveMovieToDatabase(movie)
                    }

                    override fun onError(e: Throwable) {
                        Log.e("MovieViewModel", "Film API'dan alınırken hata oluştu", e)
                        movieLiveData.value = null // Film bulunamadığını bildir
                    }
                })
        )
    }
    
    private fun saveMovieToDatabase(movie: Movie) {
        launch {
            try {
            val dao = MovieDatabase(getApplication()).movieDao()
                
                try {
                    // Make sure genreNames is never null
                    val safeGenreNames = movie.genreNames?.ifEmpty { "" } ?: ""
                    
                    // Use the safer method to insert movie
                    val result = dao.safeInsertMovie(
                        id = movie.id,
                        title = movie.title,
                        posterPath = movie.posterPath,
                        overview = movie.overview ?: "",
                        releaseDate = movie.releaseDate ?: "",
                        voteAverage = movie.voteAverage,
                        genreNames = safeGenreNames
                    )
                    Log.d("MovieViewModel", "Movie saved to SQLite: ${movie.title}, result=$result")
                    
                } catch (e: Exception) {
                    Log.e("MovieViewModel", "Normal insert failed, trying emergency insert", e)
                    
                    // If that fails, try the emergency insert as a fallback
                    try {
                        val result = dao.emergencyInsertMovie(
                            id = movie.id,
                            title = movie.title,
                            voteAverage = movie.voteAverage
                        )
                        Log.d("MovieViewModel", "Movie saved with emergency method: ${movie.title}, result=$result")
                    } catch (e2: Exception) {
                        Log.e("MovieViewModel", "Emergency insert also failed", e2)
                        throw e2
                    }
                }
            } catch (e: Exception) {
                Log.e("MovieViewModel", "Failed to save movie to SQLite", e)
                e.printStackTrace()
            }
        }
    }

    suspend fun addFavoriteMovie(movie: Movie){

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val dao = MovieDatabase(getApplication()).favoriteMovieDao()
        val isFavorite = userId?.let { dao.isFavorite(it,movie.id.toString()) }

        if(isFavorite == true){
            dao.removeFavorite(userId,movie.id)
        }else {
            val favorite = userId?.let {
                FavoriteMovie(
                    userId = it,
                    movieId = movie.id,
                    title = movie.title,
                    posterPath = movie.posterPath,
                    overview = movie.overview,
                    releaseDate = movie.releaseDate,
                    voteAverage = movie.voteAverage,
                    genreNames = movie.genreNames
                )
            }
            if (favorite != null) {
                dao.insertFavoriMovie(favorite)
            } else {
                Log.w("FavoriteInsert", "favorite nesnesi null, ekleme yapılmadı.")
            }

        }

    }

    suspend fun isMovieFavorite(movieId: Int): Boolean {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        val dao = MovieDatabase(getApplication()).favoriteMovieDao()
        return dao.isFavorite(userId, movieId.toString())
    }
    /**
     * Filme ait notları yükler
     */
    fun loadMovieNotes(movieId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val notes = notesService.getNotesForMovie(movieId.toString())
                _notes.value = notes
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load notes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun addNote(movieId: String, content: String, onSuccess: () -> Unit) {
        if (content.isBlank()) {
            _errorMessage.value = "Note content cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            try {
                if (content.isBlank()) {
                    _errorMessage.value = "Not içeriği boş olamaz."
                    return@launch
                }
                val success = notesService.addNote(movieId, content)
                if (success) {
                    // Notu ekledikten sonra notları yeniden yükle
                    loadMovieNotes(movieId)
                    onSuccess()
                } else {
                    _errorMessage.value = "Failed to add note"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add note: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    /**
     * Notu beğenir veya beğeniyi geri alır
     */
    fun toggleLikeNote(noteId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            _errorMessage.value = "Please sign in to like notes"
            return
        }

        viewModelScope.launch {
            try {
                val success = notesService.toggleLikeNote(noteId)
                if (success) {
                    // Note like toggled successfully, reload notes
                    val currentMovieId = _notes.value?.firstOrNull()?.movieId
                    currentMovieId?.let { loadMovieNotes(it) }
                } else {
                    _errorMessage.value = "Failed to toggle note like"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to toggle note like: ${e.message}"
            }
        }
    }


    fun deleteNot(noteId: String){
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Pass noteId instead of currentUser.toString()
                val success = notesService.deleteNote(noteId)
                if(success){
                    val currentMovieId = _notes.value?.firstOrNull()?.movieId
                    currentMovieId?.let { loadMovieNotes(it) }
                } else {
                    _errorMessage.value = "You can only delete your own notes"
                }
            } catch (e: Exception){
                _errorMessage.value = "Failed to delete note: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}