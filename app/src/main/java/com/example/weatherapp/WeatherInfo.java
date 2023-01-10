package com.example.weatherapp;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class WeatherInfo extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;

    String currentCity;
    EditText getCity;
    TextView tvResult;

    Button getWeather;
    Button   getLocation;

    Location currentLocation;

    private final String url = "http://api.openweathermap.org/data/2.5/weather";
    private final String api_key = "6c2fb500bf750c2cc65d35fda157535e";
    DecimalFormat df = new DecimalFormat("#.##");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_info);

        getCity = findViewById(R.id.getCity);
        tvResult = findViewById(R.id.tvResult);

        getLocation = findViewById(R.id.getLocation);
        getWeather = findViewById(R.id.getWeather);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(hasLocationPermission()) {
                    getCurrentLocation();
                } else {
                    if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        showCustomDialog("Location Permission", "this function needs location to work", "OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                multiplePermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
                            }
                        }, "Cancel", null);
                    }
                    else {
                        multiplePermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
                    }
                }
            }
        });

        getWeather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getWeatherDetails();
            }
        });
    }


    // Using geoCoder to parse latitude and longtitude to city name

    private void getCityName(double lat, double lon)
        {
            Log.d("demo", "geocoder active");
            Geocoder geocoder = new Geocoder(WeatherInfo.this, Locale.getDefault());
            List<Address> addresses = null;

            try
            {
                addresses = geocoder.getFromLocation(lat, lon, 1);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            if (addresses != null)
            {
                Log.d("demo", "geocoder set");
                currentCity = addresses.get(0).getLocality();
                getCity.setText(addresses.get(0).getLocality());
            }
        }


    // Request current user location from Google API

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {

        ProgressDialog progressDialog = new ProgressDialog(this);

        progressDialog.setMessage("Getting data..");
        progressDialog.show();

        CurrentLocationRequest currentLocationRequest = new CurrentLocationRequest.Builder()
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(5000)
                .setMaxUpdateAgeMillis(100)
                .build();

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(currentLocationRequest, cancellationTokenSource.getToken()).addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if(task.isSuccessful()) {
                    Location location = task.getResult();
                    Log.d("demo", "onComplete: " + location);
                    currentLocation = location;
                    getWeatherDetails();

                    progressDialog.dismiss();
                }else {
                    task.getException().printStackTrace();
                }
            }
        });
    }




    //custom prompt

    void showCustomDialog(String title, String message,
                          String posBtn, DialogInterface.OnClickListener posListener,
                          String negBtn, DialogInterface.OnClickListener negListener)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(posBtn, posListener)
                .setNegativeButton(negBtn, negListener);
        builder.create().show();
    }

    //check fine and coarse location permission

    private boolean hasLocationPermission(){
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Sends user to app settings page to configure permissions

    private ActivityResultLauncher<String[]> multiplePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
        @Override
        public void onActivityResult(Map<String, Boolean> result) {
            boolean finePermissionAllowed = false;
            if(result.get(Manifest.permission.ACCESS_FINE_LOCATION) != null) {
                finePermissionAllowed = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                if(finePermissionAllowed)
                {
                    getCurrentLocation();
                }
                else {
                    if(!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        showCustomDialog("Location Permission", "This function needs permission", "Launch settings", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:"+ BuildConfig.APPLICATION_ID));
                                startActivity(intent);
                            }
                        }, "Cancel", null);
                    }
                }
            }
        }
    });



    // To return to main activity
    public void StartMain(View v) {
        Intent i = new Intent(this,MainActivity.class);
        startActivity(i);
    }


    //Send request to API with specified city as parameter
    public void getWeatherDetails() {
        String tempUrl = "";
        String city = getCity.getText().toString().trim();
//        if(city.equals("")) {
//            tvResult.setText("city cannot be empty");
//        }
        if (!city.equalsIgnoreCase("")) {
            tempUrl = url + "?q=" + city + "&appid=" + api_key;
        }
        else if (!currentLocation.equals("")){
            tempUrl = url + "?lat=" + currentLocation.getLatitude() + "&lon=" + currentLocation.getLongitude() + "&appid=" + api_key;
        }
        else {
            tvResult.setText("City or Location cannot be empty");
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, tempUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("response: ", response);

                String output = "";
                try {
                    JSONObject jsonRes = new JSONObject(response);
                    JSONArray jsonArray = jsonRes.getJSONArray("weather");
                    JSONObject jsonObjectWeather = jsonArray.getJSONObject(0);
                    String description  = jsonObjectWeather.getString("description");
                    JSONObject jsonObjectMain = jsonRes.getJSONObject("main");
                    double temp = jsonObjectMain.getDouble("temp") - 273.15;
                    double feelslike = jsonObjectMain.getDouble("feels_like") - 273.15;
                    float pressure = jsonObjectMain.getInt("pressure");
                    int humidity = jsonObjectMain.getInt("humidity");
                    JSONObject jsonObjectWind = jsonRes.getJSONObject("wind");
                    String wind = jsonObjectWind.getString("speed");
                    JSONObject jsonObjectCloud = jsonRes.getJSONObject("clouds");
                    String clouds = jsonObjectCloud.getString("all");
                    JSONObject jsonObjectSys = jsonRes.getJSONObject("sys");
                    String countryName = jsonObjectSys.getString("country");
                    String cityName = jsonRes.getString("name");

                    output += "Current weather of " + cityName + " (" + countryName + ")"
                            + "\n Temp: " + df.format(temp) + " °C"
                            + "\n Feels Like: " + df.format(feelslike) + " °C"
                            + "\n Humidity: " + humidity + "%"
                            + "\n Description: " + description
                            + "\n Wind Speed: " + wind + "m/s (meters per second)"
                            + "\n Cloudiness: " + clouds + "%"
                            + "\n Pressure: " + pressure + " hPa";
                    tvResult.setText(output);
                    getCity.setText("");

                }catch (JSONException e) { e.printStackTrace(); }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), error.toString().trim(), Toast.LENGTH_LONG).show();
            }
        });

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
    }



}