package com.example.ticketscanner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
public class VActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        ImageView imageView = findViewById(R.id.imageView);
        imageView.setOnClickListener(v -> {
            // When the "V" sign is clicked, return to the scanning activity
            Intent intent = new Intent(VActivity.this, ScanQRActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            setResult(RESULT_OK);
            startActivity(intent);
            finish();
        });
    }
}
