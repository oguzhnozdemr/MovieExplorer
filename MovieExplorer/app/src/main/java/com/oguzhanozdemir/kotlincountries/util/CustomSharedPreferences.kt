package com.oguzhanozdemir.kotlincountries.util

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit

class CustomSharedPreferences {

    companion object{
        private var PREFERENCES_TIME = "preferences_time"
        private var sharedPreferences: SharedPreferences? = null

        @Volatile private var instance : CustomSharedPreferences? = null
        private val lock = Any()

        operator fun invoke(context: Context) : CustomSharedPreferences = instance ?: synchronized(
            lock){
            instance ?: makeCustomSharedPreferences(context).also{
                instance = it
            }
        }

        private fun makeCustomSharedPreferences(context: Context) : CustomSharedPreferences {

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return CustomSharedPreferences()

        }
    }

    fun saveTime(time: Long){
        sharedPreferences?.edit(true){
            putLong(PREFERENCES_TIME,time)

        }
    }

    fun getTime() = sharedPreferences?.getLong(PREFERENCES_TIME,0)

}