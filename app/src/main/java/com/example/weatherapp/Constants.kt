package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {
    const val APP_ID: String="15b510b01782360bf2a84ea4ace5f16d"
    const val BASE_URL: String="http://api.openweathermap.org/data/"
    const val METRIC_UNIT : String="metric"
    const val WEATHER_RESPONSE_DATA = "weather_response_data"
    const val PREFRENCE_NAME="WeatherAppPrefrence"
    // whether we have internet connection or not.
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // if its newer version
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            //if its null return false
            val network=connectivityManager.activeNetwork ?: return false
            val activeNetwork=connectivityManager.getNetworkCapabilities(network) ?: return false
            return when{
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->true
                else ->false
            }

        }else { //for older version
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }
}