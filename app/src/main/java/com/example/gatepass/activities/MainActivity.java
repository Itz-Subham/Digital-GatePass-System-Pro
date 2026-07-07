package com.example.gatepass.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.gatepass.scanner.AadhaarScannerManager;
import com.example.gatepass.scanner.BlurDetector;
import com.example.gatepass.utils.Constants;
import com.example.gatepass.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity {

    private PreviewView viewFinder;
    private TextView resultTextView;
    private Button captureBtn;
    private View scanFrame;
    private FloatingActionButton torchBtn;
    private ImageView btnBack;

    private TextRecognizer recognizer;
    private Camera camera;
    private boolean isTorchOn = false;
    private boolean isProcessing = false;

    private AadhaarScannerManager scannerManager;
    private ExecutorService analysisExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        
        if (!checkLogin()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        scannerManager = new AadhaarScannerManager();
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        analysisExecutor = Executors.newSingleThreadExecutor();

        initializeUI();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }
    }

    private void initializeUI() {
        viewFinder = findViewById(R.id.viewFinder);
        resultTextView = findViewById(R.id.scanResultText);
        captureBtn = findViewById(R.id.captureBtn);
        scanFrame = findViewById(R.id.scan_frame);
        torchBtn = findViewById(R.id.torchBtn);
        btnBack = findViewById(R.id.btnBack);

        torchBtn.setOnClickListener(v -> toggleTorch());
        btnBack.setOnClickListener(v -> finish());

        captureBtn.setOnClickListener(v -> {
            String id = scannerManager.getLastConfirmedId();
            if (!id.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, GatePassFormActivity.class);
                intent.putExtra(Constants.EXTRA_VISITOR_NAME, scannerManager.getLastConfirmedName());
                intent.putExtra(Constants.EXTRA_VISITOR_ID, id);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Point at Aadhaar Card", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleTorch() {
        if (camera != null) {
            isTorchOn = !isTorchOn;
            camera.getCameraControl().enableTorch(isTorchOn);
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
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build();
                imageAnalysis.setAnalyzer(analysisExecutor, this::processImageProxy);

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageProxy(ImageProxy imageProxy) {
        if (isProcessing || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        if (BlurDetector.calculateBlurScore(imageProxy) < Constants.BLUR_THRESHOLD) {
            updateUI("Status: Too Blurry", Color.RED);
            imageProxy.close();
            return;
        }

        isProcessing = true;
        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        recognizer.process(image)
                .addOnSuccessListener(visionText -> scannerManager.processText(visionText, new AadhaarScannerManager.ScannerListener() {
                    @Override
                    public void onStabilityRequired() {
                        updateUI("Verifying Stability...", Color.YELLOW);
                    }

                    @Override
                    public void onVerified(String name, String id) {
                        updateUI("VERIFIED\nNAME: " + name + "\nID: " + scannerManager.formatAadhaar(id), Color.GREEN);
                    }

                    @Override
                    public void onScanning(String partialInfo) {
                        updateUI(partialInfo, Color.YELLOW);
                    }
                }))
                .addOnCompleteListener(task -> {
                    isProcessing = false;
                    imageProxy.close();
                });
    }

    private void updateUI(String text, int color) {
        runOnUiThread(() -> {
            resultTextView.setText(text);
            resultTextView.setTextColor(color);
            
            int alpha = (color == Color.GREEN) ? 80 : 45;
            scanFrame.setBackgroundColor(Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)));
            
            if (color == Color.GREEN) {
                scanFrame.animate().scaleX(1.05f).scaleY(1.05f).setDuration(150).withEndAction(() -> 
                    scanFrame.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                ).start();
            }
        });
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to scan.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recognizer != null) recognizer.close();
        if (analysisExecutor != null) analysisExecutor.shutdown();
    }
}