package com.example.weatherapp
// retrofit is used to establish connection between internet and download the file.
//import androidx.core.app.ActivityCompat
//import androidx.databinding.DataBindingUtil

import retrofit2.*
//import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.example.weatherapp.databinding.ActivityMainBinding
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.jar.Manifest

//Retrofit is the class through which your API interfaces are turned into callable objects.
class MainActivity : AppCompatActivity() {

    // Binding Object
    private lateinit var binding: ActivityMainBinding
    // this one is required to get location that is latitude and longitude of user.
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mProgressDialog:Dialog  ?=  null
    // A global variable for the SharedPreferences // to store previous data into it
    private lateinit var mSharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Initialize the SharedPreferences variable (it requires a NAME and a MODE,
        // which we will define in the constants.kt file

        mSharedPreferences=getSharedPreferences(Constants.PREFRENCE_NAME,Context.MODE_PRIVATE)
        binding = DataBindingUtil.setContentView(this@MainActivity, R.layout.activity_main)
        if (!isLocationEnabled()) {
            Toast.makeText(
                this, "Your location provider is turned off . Please turn it on!!",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            //// Asking the location permission on runtime.
            Dexter.withActivity(this)
                .withPermissions(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ).withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            //add request location data
                            requestLocationData()
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "you have denied location permission. Please enable them as it is mandatory",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }

                    //Method called whenever Android asks the application to inform the user of the need for the requested permissions
                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
        }
    }
    //            A function to request the current location. Using the fused location provider client.   }
    @SuppressLint("MissingPermission")
    //above is used as mFusedLocationClient is not specifically requesting for permission.
     private fun requestLocationData(){
        val mLocationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()!!
        )
    }
    /**
     * inflate menu_main.xml file and add functionality for the clicked item
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menumain, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.action_refresh -> {
                // getLocationWeatherDetails()
                requestLocationData()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
    private  val mLocationCallback =object :LocationCallback() {
         override fun onLocationResult(locationResult: LocationResult){
            val mLastLocation: Location =locationResult.lastLocation
             val latitude=mLastLocation.latitude
             Log.i("Current Latitude ","$latitude")

             val longitude=mLastLocation.longitude
             Log.i("Current Longitude is","$longitude")
             getLocationWeatherDetails(latitude,longitude)
             // This will ensure that the data will be refreshed while displaying the LOADING animation
             mFusedLocationClient.removeLocationUpdates(this);
         }
     }
      private fun getLocationWeatherDetails(latitude :Double, longitude :Double){
         if(Constants.isNetworkAvailable(this@MainActivity)){
             /**
              * Add the built-in converter factory first. This prevents overriding its
              * behavior but also ensures correct behavior when using converters that consume all types.
              */
             //Converter can transform data into right format i.e, GSON format here
             val retrofit :Retrofit=Retrofit.Builder()
                                            .baseUrl(Constants.BASE_URL)
                 /** Add converter factory for serialization and deserialization of objects. */
                 /**
                  * Create an instance using a default {@link Gson} instance for conversion. Encoding to JSON and
                  * decoding from JSON (when no charset is specified by a header) will use UTF-8.
                  */
                                            .addConverterFactory(GsonConverterFactory.create())
                                             /** Create the Retrofit instances. */
                                            .build()
             //WE map service interface in which we declare the end point and api type i.e
             // GET, POST and so on along with the request parameter which are required.
              val service :WeatherService= retrofit
                  .create<WeatherService>(WeatherService::class.java)
             /** An invocation of a Retrofit method that sends a request to a web-server and returns a response.
              * Here we pass the required param in the service
              */
             val listCall: Call<WeatherResponse> = service.getWeather(
                 latitude,
                 longitude,
                 Constants.METRIC_UNIT,
                 Constants.APP_ID
             )
             showCustomProgressDialog()

             listCall.enqueue(object : Callback<WeatherResponse>{
                 @SuppressLint("SetTextI18n")
                 //Successfull HTTP response
                 override fun onResponse(
                     call: Call<WeatherResponse>,
                     response: Response<WeatherResponse>
                 ) {
                     if(response.isSuccessful){

                         // body will be whole JSON object
                         val weatherList: WeatherResponse?= response.body()
                         //Store data using shared prefrences
                         val weatherResponseJsonString=Gson().toJson(weatherList)
                         val editor =mSharedPreferences.edit()
                         editor.clear()
                         // weatherResponseJSONString = We are storing weather_response_data as a String
                         // at position = WEATHER_RESPONSE_DATA
                         editor.putString(Constants.WEATHER_RESPONSE_DATA, weatherResponseJsonString)

                         // Commit the changes
                         editor.apply()
                         editor.commit()
                         // Populate values received from weatherList (Response) into the UI

                             // setupUI(weatherList)
                             setupUI()




                         Log.i("Response Result", "$weatherList")
                     }else{

                         when(response.code()){
                             400-> {
                                 Log.e("Error 400","Bad Connection")
                             }
                             404-> {
                                 Log.e("Error 404","NOT FOUND")
                             }else ->{
                                 Log.e("Error","Generic Error")
                             }
                         }
                     }
                     hideProgressDialog()
                 }
                 //Invoked when a network or unexpected exception occurred during the HTTP
                 override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                     Log.e("Errorrrr!!", t.message.toString())
                     //hideProgressDialog()
                 }

             })
         }else{
             Toast.makeText(this@MainActivity,
                 "NO internet connection available",Toast.LENGTH_SHORT).show()
         }
      }
      //A function used to show the alert dialog when the permissions are denied and need to allow it from settings app info.
      private fun showRationalDialogForPermissions() {
            AlertDialog.Builder(this)
                .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
                .setPositiveButton(
                    "GO TO SETTINGS"
                ) { _, _ ->
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        //below is used to open settings for this particular app. Here, it links to our app
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }.show()
      }

    // to check whether the location is enabled
    private fun isLocationEnabled(): Boolean{
        // this provides access to system location services.
        val locationManager:LocationManager= getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    private fun showCustomProgressDialog(){
        mProgressDialog=Dialog(this)
        /*Set the screen content from a layout resource. The resource will be inflated ,adding all top views to screen*/
        mProgressDialog!!.setContentView(R.layout.dialogcustomprogress)
         // start the dialog and display it on screen
        mProgressDialog!!.show()
    }
    private fun hideProgressDialog(){
        if(mProgressDialog!=null){
            //closes the progress dialog
            mProgressDialog!!.dismiss()
        }
    }
    //@SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupUI(){
        // Get list from our SharedPreferences
        // In case if hte received data will be EMPTY, then we will use our default value: ""
        val weatherResponseJsonString=mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA,"")
        if(!weatherResponseJsonString.isNullOrEmpty()){
            // Get DATA back from the SharedPreferences
            val weatherList=Gson().fromJson(weatherResponseJsonString,WeatherResponse::class.java)
            // For loop to get the required data. And all are populated in the UI.
            for(i in weatherList.weather.indices){
                Log.i("Weather name",weatherList.weather.toString())
                binding.tvMain.text = weatherList.weather[i].main
                binding.tvMainDescription.text = weatherList.weather[i].description
                binding.tvHumidity.text = weatherList.main.humidity.toString() + " per cent"
                binding.tvMin.text = weatherList.main.temp_min.toString() + " min"
                binding.tvMax.text = weatherList.main.temp_max.toString() + " max"
                binding.tvSpeed.text = weatherList.wind.speed.toString()
                binding.tvName.text = weatherList.name
                binding.tvCountry.text = weatherList.sys.country
                binding.tvSunriseTime.text = unixTime(weatherList.sys.sunrise.toLong())
                binding.tvSunsetTime.text = unixTime(weatherList.sys.sunset.toLong())
                // Note: Locale.getDefault() can return country code

                binding.tvTemp.text =
                    weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
                // Reference for Icons List (Codes): https://openweathermap.org/weather-conditions
                when (weatherList.weather[i].icon) {

                    // Clear Sky
                    "01d" -> binding.ivMain.setImageResource(R.drawable.sunny)
                    "01n" -> binding.ivMain.setImageResource(R.drawable.cloud)

                    // Few Clouds
                    "02d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "02n" -> binding.ivMain.setImageResource(R.drawable.cloud)

                    // Scattered Clouds
                    "03d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "03n" -> binding.ivMain.setImageResource(R.drawable.cloud)

                    // Broken Clouds
                    "04d" -> binding.ivMain.setImageResource(R.drawable.cloud)
                    "04n" -> binding.ivMain.setImageResource(R.drawable.cloud)

                    // Rain
                    "10d" -> binding.ivMain.setImageResource(R.drawable.rain)
                    "10n" -> binding.ivMain.setImageResource(R.drawable.cloud)

                    // Thunderstorm
                    "11d" -> binding.ivMain.setImageResource(R.drawable.storm)
                    "11n" -> binding.ivMain.setImageResource(R.drawable.rain)

                    // Snow
                    "13d" -> binding.ivMain.setImageResource(R.drawable.snowflake)
                    "13n" -> binding.ivMain.setImageResource(R.drawable.snowflake)

                }

            }
        }


    }
    /**
     * Function is used to get the temperature unit value.
     */
    private fun getUnit(value:String):String{
        var value="°C"
        if("US" == value || "MM"== value || "LR"==value){
            value="°F"
        }
        return  value
    }
    /**
     * The function is used to get the formatted time based on the Format and the LOCALE we pass to it.
     * Reference: Epoch & Unix Timestamp Conversion Tools - https://www.epochconverter.com/
     */
    private fun unixTime(timex: Long) :String{
        // We are going to pass the time as a Long Value:
        // i.e. 1632462389 = (Friday, September 24, 2021 5:46:24 AM)

        // ... * 1000L will help in converting time from milliseconds
        val date = Date(timex * 1000L)

        // Reference: How to set 24-hours format for date on java?
        // https://stackoverflow.com/questions/8907509/how-to-set-24-hours-format-for-date-on-java

        // hh:mm:ss = 12 Hour Format
        // HH:mm:ss = 24 Hour Format

        // Using Locale.US is a safeguard, as received data might be corrupt
        val sdf=SimpleDateFormat("HH:mm",Locale.US)
        sdf.timeZone= TimeZone.getDefault()
        // It will return final formatted and converted time/date
        return sdf.format(date)
    }
}