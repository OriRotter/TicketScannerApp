package com.example.ticketscanner;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        spinner = findViewById(R.id.spinner);
        String serverUrl = getString(R.string.server_url);
        // Execute AsyncTask to fetch data from URL
        new FetchDataTask().execute(serverUrl +"/get_shows");
        new FetchDataReturn().execute(serverUrl +"/get_shows_id");
        initViews();
    }

    private void initViews() {
        // Initialize other views or components here if needed
    }


    // AsyncTask to fetch data from URL
    private class FetchDataTask extends AsyncTask<String, Void, List<String>> {

        @Override
        protected List<String> doInBackground(String... urls) {
                String urlString = urls[0];
            List<String> dataList = new ArrayList<>();

            try {
                URL url = new URL(urlString);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                JSONArray jsonArray = new JSONArray(stringBuilder.toString());

                for (int i = 0; i < jsonArray.length(); i++) {
                    String item = jsonArray.getString(i);
                    dataList.add(item);
                }

                bufferedReader.close();
                inputStream.close();
                urlConnection.disconnect();
            } catch (IOException | JSONException ignored) {
            }

            return dataList;
        }

        @Override
    protected void onPostExecute(List<String> dataList) {
        super.onPostExecute(dataList);

        // Populate Spinner with fetched data
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, dataList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
    }
    private class FetchDataReturn extends AsyncTask<String, Void, List<String>> {

        @Override
        protected List<String> doInBackground(String... urls) {
            String urlString = urls[0];
            List<String> dataList = new ArrayList<>();

            try {
                URL url = new URL(urlString);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                JSONArray jsonArray = new JSONArray(stringBuilder.toString());

                for (int i = 0; i < jsonArray.length(); i++) {
                    String item = jsonArray.getString(i);
                    dataList.add(item);
                }

                bufferedReader.close();
                inputStream.close();
                urlConnection.disconnect();
            } catch (IOException | JSONException ignored) {
            }

            return dataList;
        }

        @Override
        protected void onPostExecute(List<String> dataList) {
            super.onPostExecute(dataList);

            // Instead of populating spinner, return the list
            processFetchedData(dataList);
        }
    }

    // Define a method to process the fetched data
    private void processFetchedData(List<String> dataList) {
        // Do whatever you want with the dataList
        // For example, you can pass it to another method or use it directly
        // In this case, I'll just print the list for demonstration
        findViewById(R.id.scanButton).setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, ScanQRActivity.class);
            i.putExtra("show_id", dataList.get((int) spinner.getSelectedItemId()));
            startActivity(i);
            finish();  // ending this activity
        });
    }

}
