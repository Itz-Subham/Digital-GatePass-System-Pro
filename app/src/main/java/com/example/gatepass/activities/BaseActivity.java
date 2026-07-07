package com.example.gatepass.activities;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gatepass.firebase.FirestoreRepository;
import com.example.gatepass.utils.SessionManager;

public abstract class BaseActivity extends AppCompatActivity {

    protected SessionManager sessionManager;
    protected FirestoreRepository repository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        repository = new FirestoreRepository();
    }

    protected boolean checkLogin() {
        return sessionManager.isLoggedIn();
    }

    protected String getUserRole() {
        return sessionManager.getRole();
    }
}
