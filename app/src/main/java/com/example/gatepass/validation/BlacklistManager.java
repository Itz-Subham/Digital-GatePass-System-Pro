package com.example.gatepass.validation;

import com.example.gatepass.firebase.FirestoreConstants;
import com.example.gatepass.firebase.FirestoreRepository;

import java.util.HashMap;
import java.util.Map;

public class BlacklistManager {

    private final FirestoreRepository repository;

    public BlacklistManager(FirestoreRepository repository) {
        this.repository = repository;
    }

    public interface BlacklistCheckListener {
        void onBlacklisted(String name);
        void onNotBlacklisted();
    }

    public void checkBlacklist(String name, BlacklistCheckListener listener) {
        if (name == null || name.isEmpty()) {
            listener.onNotBlacklisted();
            return;
        }

        repository.getBlacklistSearchQuery(name.toLowerCase()).get().addOnSuccessListener(snapshots -> {
            if (!snapshots.isEmpty()) {
                listener.onBlacklisted(name);
            } else {
                listener.onNotBlacklisted();
            }
        });
    }

    public void addToBlacklist(String name, String visitorIdHash, final OnSuccessListener successListener) {
        Map<String, Object> data = new HashMap<>();
        data.put(FirestoreConstants.FIELD_NAME, name);
        data.put(FirestoreConstants.FIELD_NAME_LOWER, name.toLowerCase());
        data.put(FirestoreConstants.FIELD_ID, visitorIdHash);
        data.put(FirestoreConstants.FIELD_TIMESTAMP, System.currentTimeMillis());

        repository.addToBlacklist(data).addOnSuccessListener(documentReference -> {
            if (successListener != null) successListener.onSuccess();
        });
    }

    public interface OnSuccessListener {
        void onSuccess();
    }
}