package com.example.gatepass.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.gatepass.utils.Constants;
import com.example.gatepass.qr.QrUtils;
import com.example.gatepass.R;
import com.google.android.material.button.MaterialButton;

public class DisplayQrActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!checkLogin()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_display_qr);

        TextView tvStudentName = findViewById(R.id.tvStudentName);
        TextView tvStudentReg = findViewById(R.id.tvStudentReg);
        TextView tvEntryTime = findViewById(R.id.tvEntryTime);
        TextView tvDocId = findViewById(R.id.tvDocId);
        ImageView qrImageView = findViewById(R.id.qrImageView);
        MaterialButton btnFinish = findViewById(R.id.btnFinish);

        String studentName = getIntent().getStringExtra(Constants.EXTRA_STUDENT_NAME);
        String studentReg = getIntent().getStringExtra(Constants.EXTRA_STUDENT_REG);
        String entryTime = getIntent().getStringExtra(Constants.EXTRA_ENTRY_TIME);
        String documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);

        if (studentName != null) tvStudentName.setText(studentName);
        if (studentReg != null) tvStudentReg.setText("Reg: " + studentReg);
        if (entryTime != null) tvEntryTime.setText("Entry: " + entryTime);
        if (documentId != null) {
            String trimmedId = documentId.trim();
            tvDocId.setText("ID: " + trimmedId);
            String finalUrl = Constants.BASE_QR_URL + trimmedId;
            
            Bitmap qrBitmap = QrUtils.generateQrCode(finalUrl, 512);
            if (qrBitmap != null) {
                qrImageView.setImageBitmap(qrBitmap);
            }
        }

        btnFinish.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}