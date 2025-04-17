package com.oguzhanozdemir.kotlincountries.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.oguzhanozdemir.kotlincountries.model.Movie

import com.oguzhanozdemir.kotlincountries.services.MovieAPIService
import com.oguzhanozdemir.kotlincountries.services.MovieDatabase
import com.oguzhanozdemir.kotlincountries.util.CustomSharedPreferences
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.observers.DisposableSingleObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.launch
import java.security.KeyStore.TrustedCertificateEntry

class FeedViewModel(application: Application) : BaseViewModel(application){

    private val movieAPIService = MovieAPIService()
    private val disposable = CompositeDisposable()
    private var refreshTime =10*60*1000*1000*1000L
    private var customPreferences = CustomSharedPreferences(getApplication())
    private val _selectedGenreId = MutableLiveData<Int>()
    private val _isSearchActive = MutableLiveData<Boolean>()

    val selectedGenreId: LiveData<Int> get() = _selectedGenreId
    val isSearchActive: LiveData<Boolean> get() = _isSearchActive
    val movies = MutableLiveData<List<Movie>>()
    val countryError = MutableLiveData<Boolean>()
    val countryLoading = MutableLiveData<Boolean>()

    // Başlangıçta Drama (18) türünü seç
    init {
        _selectedGenreId.value = 18 // Default genre ID
        _isSearchActive.value = false
    }

    // Veriyi yenile
    fun refhreshData(){
        val updateTime = customPreferences.getTime()
        val selectedId = _selectedGenreId.value ?: 18 // Eğer null ise varsayılan olarak 18 (Drama) kullan
        
        // Tür değişmeden önce ilk açılışta cache kontrolü yapılabilir
        if(updateTime != null && updateTime != 0L && System.nanoTime() - updateTime < refreshTime) {
            getDataFromSQLite()
        } else {

            // API'den veri getir
            getDataFromAPI(selectedId)
        }
    }




    private fun getDataFromSQLite(){
        countryLoading.value = true
        val selectedId = _selectedGenreId.value ?: 18

        launch {
            val allMovies = MovieDatabase(getApplication()).movieDao().getAllMovies()
            showMovies(allMovies)
            //Toast.makeText(getApplication(), "Movies From SQLite - Genre ID: $selectedId", Toast.LENGTH_LONG).show()
        }
    }

    // API'den veri getir
    private fun getDataFromAPI(genreId: Int) {
        countryLoading.value = true

        disposable.add(
            movieAPIService.getMoviesByGenre(genreId) // Seçilen genre ID kullanılıyor
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<Movie>>() {
                    override fun onSuccess(t: List<Movie>) {
                        storeInSQLite(t)
                        //Toast.makeText(getApplication(), "Movies From API - Genre ID: $genreId", Toast.LENGTH_LONG).show()
                    }

                    override fun onError(e: Throwable) {
                        countryLoading.value = false
                        countryError.value = true
                        e.printStackTrace()
                    }
                })
        )
    }

    // Film ara
    fun searchMovies(query: String) {
        if (query.isEmpty()) {
            // Arama boşsa, normal görünüme dön
            _isSearchActive.value = false
            val selectedId = _selectedGenreId.value ?: 18
            getDataFromAPI(selectedId)
            return
        }
        
        _isSearchActive.value = true
        countryLoading.value = true
        
        disposable.add(
            movieAPIService.searchMovies(query)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<List<Movie>>() {
                    override fun onSuccess(t: List<Movie>) {
                        // Arama sonuçlarını doğrudan göster, cache'e kaydetme
                        showMovies(t)
                    }

                    override fun onError(e: Throwable) {
                        countryLoading.value = false
                        countryError.value = true
                        e.printStackTrace()
                    }
                })
        )
    }

    // Filmleri göster
    private fun showMovies(movieList: List<Movie>) {
        movies.value = movieList
        countryError.value = false
        countryLoading.value = false
    }

    // SQLite'ye veri kaydet
    private fun storeInSQLite(list: List<Movie>) {
        launch {
            try {
                val dao = MovieDatabase(getApplication()).movieDao()
                
                try {
                    dao.deleteAll()
                    
                    // Process each movie to ensure non-null values
                    val processedList = list.map { movie ->
                        // Process genres and set genreNames if present
                        if (movie.genres.isNotEmpty()) {
                            movie.genreNames = movie.genres.joinToString(",") { it.name?.ifEmpty { "Unknown" } ?: "Unknown" }
                        } else {
                            movie.genreNames = ""
                        }
                        movie
                    }
                    
                    // Insert each movie individually to handle errors per movie
                    val insertedIds = mutableListOf<Long>()
                    
                    for (movie in processedList) {
                        try {
                            val id = dao.safeInsertMovie(
                                id = movie.id,
                                title = movie.title,
                                posterPath = movie.posterPath,
                                overview = movie.overview ?: "",
                                releaseDate = movie.releaseDate ?: "",
                                voteAverage = movie.voteAverage,
                                genreNames = movie.genreNames ?: ""
                            )
                            insertedIds.add(id)
                        } catch (e: Exception) {
                            // If normal insert fails, try emergency insert
                            try {
                                val id = dao.emergencyInsertMovie(
                                    id = movie.id,
                                    title = movie.title,
                                    voteAverage = movie.voteAverage
                                )
                                insertedIds.add(id)
                            } catch (e2: Exception) {
                                // Just log and continue with next movie
                                e2.printStackTrace()
                            }
                        }
                    }
                    
                    // Update IDs if necessary
                    if (insertedIds.size == processedList.size) {
                        for (i in processedList.indices) {
                            processedList[i].id = insertedIds[i].toInt()
                        }
                    }
                    
                    showMovies(processedList)
                } catch (e: Exception) {
                    // If bulk operations failed, still try to show the movies
                    e.printStackTrace()
                    showMovies(list)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                countryError.value = true
                countryLoading.value = false
            }
        }
        customPreferences.saveTime(System.nanoTime())
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }

    // Genre ID'yi ayarla ve verileri yenile
    fun setGenreId(genreId: Int) {
        // Arama aktifse iptal et
        _isSearchActive.value = false
        
        // Tür değiştiğinde her zaman API'den veri çek
        _selectedGenreId.value = genreId
        getDataFromAPI(genreId) // Tür değiştiğinde doğrudan API'den ver al
    }
}