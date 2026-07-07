package com.example.gatepass.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.gatepass.utils.Constants;
import com.example.gatepass.qr.QrUtils;
import com.example.gatepass.R;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CheckoutScannerActivity extends BaseActivity {

    private PreviewView viewFinder;
    private BarcodeScanner scanner;
    private boolean isProcessing = false;
    private ExecutorService analysisExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!checkLogin()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_checkout_scanner);

        viewFinder = findViewById(R.id.viewFinder);
        MaterialButton btnManualCheckout = findViewById(R.id.btnManualCheckout);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnManualCheckout.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManualSelectionActivity.class);
            intent.putExtra("ACTION", ManualSelectionActivity.ACTION_CHECKOUT);
            startActivity(intent);
        });

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        scanner = BarcodeScanning.getClient(options);
        analysisExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(analysisExecutor, this::processImageProxy);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CheckoutScanner", "Camera failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageProxy(ImageProxy imageProxy) {
        if (isProcessing || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String rawValue = barcode.getRawValue();
                        if (rawValue != null) {
                            String docId = QrUtils.extractDocId(rawValue);
                            if (docId != null && !docId.isEmpty()) {
                                showSuccessFeedback();
                                new Handler(Looper.getMainLooper()).postDelayed(() -> navigateToDetail(docId), 200);
                                break;
                            } else {
                                Toast.makeText(this, "Invalid Gate Pass QR", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void showSuccessFeedback() {
        View overlay = findViewById(R.id.vFeedbackOverlay);
        if (overlay != null) {
            overlay.setVisibility(View.VISIBLE);
            overlay.setAlpha(0.6f);
            overlay.animate().alpha(0f).setDuration(400)
                    .withEndAction(() -> overlay.setVisibility(View.GONE)).start();
        }

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(100);
            }
        }
    }

    private void navigateToDetail(String docId) {
        if (isProcessing || docId == null || docId.isEmpty()) return;
        isProcessing = true;

        try {
            Intent intent = new Intent(this, CheckoutDetailActivity.class);
            intent.putExtra(Constants.EXTRA_DOCUMENT_ID, docId.trim());
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e("CheckoutScanner", "Navigation failed", e);
            isProcessing = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanner != null) scanner.close();
        if (analysisExecutor != null) analysisExecutor.shutdown();
    }
}