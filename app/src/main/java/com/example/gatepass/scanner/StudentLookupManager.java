package com.example.gatepass.scanner;

import com.example.gatepass.firebase.FirestoreConstants;
import com.example.gatepass.firebase.FirestoreRepository;

public class StudentLookupManager {

    private final FirestoreRepository repository;

    public StudentLookupManager(FirestoreRepository repository) {
        this.repository = repository;
    }

    public interface StudentLookupListener {
        void onStudentFound(String name, String regNo);
        void onStudentNotFound(String error);
    }

    public void lookupByReg(String regNo, StudentLookupListener listener) {
        repository.getStudentByRegQuery(regNo).get().addOnSuccessListener(snapshots -> {
            if (!snapshots.isEmpty()) {
                String name = snapshots.getDocuments().get(0).getString(FirestoreConstants.FIELD_NAME);
                listener.onStudentFound(name, regNo);
            } else {
                listener.onStudentNotFound("Student ID not found");
            }
        });
    }

    public void lookupByName(String nameQuery, StudentLookupListener listener) {
        repository.getStudentByNameQuery(nameQuery).get().addOnSuccessListener(snapshots -> {
            if (!snapshots.isEmpty()) {
                String name = snapshots.getDocuments().get(0).getString(FirestoreConstants.FIELD_NAME);
                String reg = snapshots.getDocuments().get(0).getString(FirestoreConstants.FIELD_REG_NO);
                listener.onStudentFound(name, reg);
            } else {
                listener.onStudentNotFound("Student name not found");
            }
        });
    }
}