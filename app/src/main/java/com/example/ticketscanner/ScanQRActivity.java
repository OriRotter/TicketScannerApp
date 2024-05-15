package com.example.ticketscanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class ScanQRActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private Camera mCamera;
    private ImageButton mFlashlightButton;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private MultiFormatReader multiFormatReader;
    private boolean isScanningEnabled = true;
    private static final int REQUEST_SUCCESS_ACTIVITY = 1;
    private boolean isFlashlightOn = false;
    private String serverUrl;
    private String show_id = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan_qr);
        show_id = String.valueOf(getIntent().getStringExtra("show_id"));
        serverUrl = getString(R.string.server_url);
        mFlashlightButton = findViewById(R.id.flashlight_button);
        mFlashlightButton.setOnClickListener(v -> toggleFlashlight());
        mSurfaceView = findViewById(R.id.camera_preview);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            initializeCamera();
        }
        mSurfaceView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // When user taps on the SurfaceView, try to focus
                autoFocus();
            }
            return true;
        });

        // Initialize MultiFormatReader for QR code scanning
        multiFormatReader = new MultiFormatReader();
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        multiFormatReader.setHints(hints);
    }

    private void autoFocus() {
        if (mCamera != null) {
            mCamera.autoFocus((success, camera) -> {
                // Auto focus complete
                // Focus is successful
                // Focus failed
            });
        }
    }

    // Method to handle button click to send text to the server
    public void sendTextToServer(View view) {
        // Retrieve text from input field
        EditText editText = findViewById(R.id.input_text);
        String searchText = editText.getText().toString();

        // Call method to send HTTPS POST request with the search text
        sendSearchRequest(searchText);
    }

    // Method to send HTTP POST request with the search text
    private void sendSearchRequest(String searchText) {
        // Assuming you have similar logic for sending HTTPS POST request as for QR code
        // Modify this accordingly to send the search request and handle the response
        // You can reuse the existing AsyncTask or create a new one for sending search requests
        new SendSearchRequest().execute(searchText, show_id);
    }

    private class SendSearchRequest extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String search = params[0];
            String response = "";

            try {
                URL url = new URL(serverUrl + "/search");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);

                // Build query parameters
                Map<String, String> parameters = new HashMap<>();
                parameters.put("search", search);
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
            try {
                JSONArray responseArray = new JSONArray(result);

                // Check if responseArray has at least two elements
                if (responseArray.length() >= 2) {
                    JSONArray ordersArray = responseArray.getJSONArray(0);
                    JSONArray ticketsArray = responseArray.getJSONArray(1);

                    if (ordersArray.length() > 0) {
                        for (int i = 0; i < ordersArray.length(); i++) {
                            JSONArray innerArray = ordersArray.getJSONArray(i);

                            ordersArray.put(i, innerArray.get(0) + "|| Name: " + innerArray.get(1) + " | Phone number: " + innerArray.get(2) + " | email: " + innerArray.get(3));
                        }
                        // If orders list is not empty, open DetailsActivity with orders list
                        Intent intent = new Intent(ScanQRActivity.this, DetailsActivity.class);
                        intent.putExtra("details", ordersArray.toString());
                        intent.putExtra("show_id", show_id);
                        startActivity(intent);
                    } else if (ticketsArray.length() > 0) {
                        // If orders list is not empty, open DetailsActivity with orders list
                        Intent intent = new Intent(ScanQRActivity.this, TicketsDetailsActivity.class);
                        intent.putExtra("show_id", show_id);
                        intent.putExtra("tickets", ticketsArray.toString());
                        startActivity(intent);
                    } else {
                        // If both lists are empty, display a toast
                        Toast.makeText(ScanQRActivity.this, "No tickets found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // If responseArray doesn't have at least two elements, display a toast
                    Toast.makeText(ScanQRActivity.this, "Invalid server response", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(ScanQRActivity.this, "Error parsing server response", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeCamera() {
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
    protected void onResume() {
        super.onResume();
        initializeCamera();
    }
    private void toggleFlashlight() {
        if (isFlashlightOn) {
            turnOffFlashlight();
        } else {
            turnOnFlashlight();
        }
    }

    private void turnOnFlashlight() {
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(parameters);
            isFlashlightOn = true;
            mFlashlightButton.setImageResource(R.drawable.flashlight_on_check);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to turn on flashlight.", Toast.LENGTH_SHORT).show();
        }
    }

    private void turnOffFlashlight() {
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameters);
            isFlashlightOn = false;
            mFlashlightButton.setImageResource(R.drawable.flashlight_off_check);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to turn off flashlight.", Toast.LENGTH_SHORT).show();
        }
    }

    public void goBackToMainActivity(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Optional: finish the current activity to prevent going back to it when pressing the back button
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        try {
            mCamera = Camera.open();

            // Set the camera parameters for preview size to match the SurfaceView size
            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size bestSize = getBestPreviewSize(parameters);
            if (bestSize != null) {
                parameters.setPreviewSize(bestSize.width, bestSize.height);
                mCamera.setParameters(parameters);
            }
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
        } catch (Exception ignored) {
        }
    }

    private Camera.Size getBestPreviewSize(Camera.Parameters parameters) {
        Camera.Size bestSize = null;
        int surfaceViewWidth = mSurfaceView.getWidth();
        int surfaceViewHeight = mSurfaceView.getHeight();
        float surfaceViewRatio = (float) surfaceViewWidth / surfaceViewHeight;
        List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();
        if (supportedSizes != null) {
            float minDiff = Float.MAX_VALUE;
            for (Camera.Size size : supportedSizes) {
                float ratio = (float) size.width / size.height;
                float diff = Math.abs(ratio - surfaceViewRatio);
                if (diff < minDiff) {
                    bestSize = size;
                    minDiff = diff;
                }
            }
        }
        return bestSize;
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        // Not needed for this example
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera.Size size = camera.getParameters().getPreviewSize();
        if (size != null) {
            try {
                int width = size.width;
                int height = size.height;
                new Rect(0, 0, width, height);
                PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false);
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

                // Use ZXing to decode the QR code
                Result result = multiFormatReader.decode(binaryBitmap);

                // Handle the scanned QR code result
                handleQRCodeResult(result.getText());

            } catch (Exception ignored) {
            }
        }
    }

    private void handleQRCodeResult(String qrCodeData) {
        if (isScanningEnabled)
        {
            if (qrCodeData.isEmpty()) {
                // Display a message for empty QR code
                Toast.makeText(this, "Empty QR code scanned", Toast.LENGTH_SHORT).show();
            } else {
                // Call your empty function here
                emptyFunction(qrCodeData);
            }
        }
    }

    private void emptyFunction(String qrCodeData) {
        isScanningEnabled = false;
        // Send HTTPS POST request to server
        new SendPostRequest().execute(qrCodeData);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, initialize the camera again
                initializeCamera();
                surfaceCreated(mSurfaceHolder);
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    private class SendPostRequest extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String qrCodeData = params[0];
            String response = "";

            try {
                URL url = new URL(serverUrl+"/use");
                HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setDoOutput(true);

                // Build query parameters
                Map<String, String> parameters = new HashMap<>();
                parameters.put("hash", qrCodeData);
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
                case "200": {
                    // If server response is 200, start the success activity
                    Intent intent = new Intent(ScanQRActivity.this, VActivity.class);
                    startActivityForResult(intent, REQUEST_SUCCESS_ACTIVITY);

                    break;
                }
                case "401": {
                    // If server response is 200, start the success activity
                    Intent intent = new Intent(ScanQRActivity.this, XActivity.class);
                    startActivityForResult(intent, REQUEST_SUCCESS_ACTIVITY);

                    break;
                }
                case "404": {
                    // If server response is 200, start the success activity
                    Intent intent = new Intent(ScanQRActivity.this, NotFoundActivity.class);
                    startActivityForResult(intent, REQUEST_SUCCESS_ACTIVITY);

                    break;
                }
                default:
                    // If server response is not 200, display toast with error message
                    Toast.makeText(ScanQRActivity.this, "Server Error: " + result, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SUCCESS_ACTIVITY) {
            if (resultCode == RESULT_CANCELED) {
                // SuccessActivity finished successfully
                isScanningEnabled = true;
            }
        }
    }
}