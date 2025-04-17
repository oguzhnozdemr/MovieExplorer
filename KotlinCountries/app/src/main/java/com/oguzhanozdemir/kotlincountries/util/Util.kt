package com.oguzhanozdemir.kotlincountries.util

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.oguzhanozdemir.kotlincountries.R
import java.text.SimpleDateFormat
import java.util.Locale

fun ImageView.downloadFromUrl(uri : String?, progressDrawable: CircularProgressDrawable) {

    val options = RequestOptions()
        .placeholder(progressDrawable)
        .error(R.mipmap.ic_launcher_round)

    Glide.with(context)
        .setDefaultRequestOptions(options)
        .load(uri)
        .into(this)

}

fun placeholderProgressBar(context: Context) : CircularProgressDrawable {
    return CircularProgressDrawable(context).apply {
        strokeWidth = 8f
        centerRadius = 40f
        start()
    }
}

fun formatDate(dateString: String): String {
    if (dateString.isBlank()) return ""
    
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

fun Fragment.showError(message: String) {
    view?.let {
        Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
    } ?: Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

@BindingAdapter("android:downloadUrl")
fun downloadImage(view: ImageView, url: String?) {
    view.downloadFromUrl(url, placeholderProgressBar(view.context))
}
