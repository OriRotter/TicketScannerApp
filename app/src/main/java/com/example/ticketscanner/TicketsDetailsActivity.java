package com.example.ticketscanner;

import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class TicketsDetailsActivity extends AppCompatActivity {
    private String serverUrl;
    private String show_id = "";
    private String password = "";

    private List<List<String>> tickets;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_tickets);
        show_id = String.valueOf(getIntent().getStringExtra("show_id"));
        serverUrl = getString(R.string.server_url);
        password = getString(R.string.password);

        // Retrieve the JSON string of tickets from the intent
        String jsonTickets = getIntent().getStringExtra("tickets");

        try {
            // Parse the JSON string into a JSONArray
            JSONArray jsonArray = new JSONArray(jsonTickets);

            // Convert the JSONArray to a list of lists of strings
            tickets = JsonUtils.jsonArrayToList(jsonArray);

            // Display the list of tickets
            displayListData();

        } catch (JSONException e) {
            // Handle JSON parsing error
            Toast.makeText(this, "Error parsing JSON data", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayListData() {
        // Set up ListView to display list data
        ListView listView = findViewById(R.id.listView);

        // Create custom adapter and set it to the ListView
        TicketsListAdapter adapter = new TicketsListAdapter(this, R.layout.list_item_ticket, tickets);
        listView.setAdapter(adapter);
    }

    public void changeIt(String hash, Button buttonTicketStatus)
    {
        new TicketsDetailsActivity.SendPostRequest(buttonTicketStatus).execute(hash);
    }
    private class SendPostRequest extends AsyncTask<String, Void, String> {
        private final Button mButton; // Store button reference

        public SendPostRequest(Button button) {
            mButton = button;
        }


        @Override
        protected String doInBackground(String... params) {
            String hash = params[0];
            String response = "";

            try {
                URL url = new URL(serverUrl+"/change");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);

                // Build query parameters
                Map<String, String> parameters = new HashMap<>();
                parameters.put("password", password);
                parameters.put("hash", hash);
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
            } catch (Exception ignored) {
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            // Display toast with server response
            switch (result) {
                case "200":
                    // If server response is 200, change to green
                    mButton.setBackgroundColor(ContextCompat.getColor(this.mButton.getContext(), R.color.red));
                    mButton.setText("סרוק");
                    break;
                case "300":
                    mButton.setBackgroundColor(ContextCompat.getColor(this.mButton.getContext(), R.color.green));
                    mButton.setText("פתוח");

                    break;
                case "404":
                    Toast.makeText(TicketsDetailsActivity.this, "Server Error: " + result, Toast.LENGTH_SHORT).show();

                    break;
                default:
                    // If server response is not 200, display toast with error message
                    Toast.makeText(TicketsDetailsActivity.this, "Server Error: " + result, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}
