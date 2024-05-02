package com.example.ticketscanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.ticketscanner.R;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.ResultPoint;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayOutputStream;

public class ScanQRActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private View mScanningBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);
        System.out.println( String.valueOf(getIntent().getStringExtra("info")));
        Log.d("hello", String.valueOf(getIntent().getStringExtra("info")));

        mSurfaceView = findViewById(R.id.camera_preview);
        mScanningBox = findViewById(R.id.scanning_box);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            initializeCamera();
        }
    }

    private void initializeCamera() {
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
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
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to open camera.", Toast.LENGTH_SHORT).show();
        }
    }

    private Camera.Size getBestPreviewSize(Camera.Parameters parameters) {
        Camera.Size bestSize = null;
        List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();
        if (supportedSizes != null) {
            int surfaceViewWidth = mSurfaceView.getWidth();
            int surfaceViewHeight = mSurfaceView.getHeight();
            float surfaceViewRatio = (float) surfaceViewWidth / surfaceViewHeight;
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
                Rect rect = new Rect(0, 0, width, height);
                YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                if (!yuvImage.compressToJpeg(rect, 100, outputStream)) {
                    return;
                }
                byte[] jpegData = outputStream.toByteArray();

                // Convert JPEG to BinaryBitmap
                Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
                hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
                        new PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)));

                // Use ZXing to decode the QR code
                MultiFormatReader reader = new MultiFormatReader();
                Result result = reader.decode(binaryBitmap, hints);

                // Handle the scanned QR code result
                handleQRCodeResult(result.getText());

            } catch (Exception e) {
                Log.e("QRCodeScanner", "Error decoding QR code", e);
            }
        }
    }

    private void handleQRCodeResult(String qrCodeData) {
        // Call your empty function here
        // For example:
        emptyFunction();
    }

    private void emptyFunction() {
        // Your empty function implementation goes here
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
