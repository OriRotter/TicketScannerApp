package com.example.ticketscanner;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


public class DetailsActivity extends AppCompatActivity {
    private String serverUrl;
    private String show_id = "";
    private String password = "";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        show_id = String.valueOf(getIntent().getStringExtra("show_id"));
        serverUrl = getString(R.string.server_url);
        password = getString(R.string.password);
        // Get list data passed from ScanQRActivity
        String listData = getIntent().getStringExtra("details");

        // Parse the list data and display it in the ListView
        displayListData(listData);
    }

    private void displayListData(String listData) {
        if (listData != null) {
            try {
                JSONArray jsonArray = new JSONArray(listData);
                String[] listItems = new String[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    listItems[i] = jsonArray.getString(i);
                }

                // Set up ListView to display list data
                ListView listView = findViewById(R.id.listView);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
                listView.setAdapter(adapter);

                // Handle item click on ListView
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    // Get the selected item
                    String selectedItem = (String) parent.getItemAtPosition(position);

                    // Extract the first character
                    char firstChar = selectedItem.charAt(0);

                    sendSearchRequest(String.valueOf(firstChar));
                });
            } catch (JSONException ignored) {
            }
        } else {
            // Handle the case when listData is null
            Toast.makeText(this, "No data received", Toast.LENGTH_SHORT).show();
        }
    }
    private void sendSearchRequest(String searchText) {
        // Assuming you have similar logic for sending HTTPS POST request as for QR code
        // Modify this accordingly to send the search request and handle the response
        // You can reuse the existing AsyncTask or create a new one for sending search requests
        new SendSearchRequest().execute(searchText, show_id);
    }


    private class SendSearchRequest extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String order_number = params[0];
            String response = "";

            try {
                URL url = new URL(serverUrl + "/order_number");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);

                // Build query parameters
                Map<String, String> parameters = new HashMap<>();
                parameters.put("password", password);
                parameters.put("order_number", order_number);
                parameters.put("showID", show_id);

                // Write parameters to request body
                StringBuilder postData = new StringBuilder();
                for (Map.Entry<String, String> param : parameters.entrySet()) {
                    if (postData.length() != 0) postData.append('&');
                    postData.append(param.getKey()).append('=').append(param.getValue());
                }
                byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);
                conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                    wr.write(postDataBytes);
                }

                // Read response from server
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                StringBuilder stringBuilder = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                response = stringBuilder.toString();

                reader.close();
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            try
            {
                JSONArray ticketsArray = new JSONArray(result);
                // If orders list is not empty, open DetailsActivity with orders list
                Intent intent = new Intent(DetailsActivity.this, TicketsDetailsActivity.class);
                intent.putExtra("show_id", show_id);
                intent.putExtra("tickets", ticketsArray.toString());
                startActivity(intent);
        } catch(JSONException e)
        {
            e.printStackTrace();
            Toast.makeText(DetailsActivity.this, "Error parsing server response", Toast.LENGTH_SHORT).show();
        }
    }
    }
}

