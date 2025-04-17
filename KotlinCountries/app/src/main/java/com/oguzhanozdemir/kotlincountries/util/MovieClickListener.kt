package com.oguzhanozdemir.kotlincountries.util

import android.view.View
import com.oguzhanozdemir.kotlincountries.model.Movie

interface MovieClickListener {

    fun onMovieClicked(v: View,movie: Movie)  // UUID kullanmak yerine Int türü kullanıldı
}