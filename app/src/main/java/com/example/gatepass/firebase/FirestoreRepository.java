package com.example.gatepass.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import java.util.Map;

public class FirestoreRepository {

    private final FirebaseFirestore db;

    public FirestoreRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // --- Users / Guards ---

    public Query getGuardsQuery() {
        return db.collection(FirestoreConstants.COL_USERS)
                .whereEqualTo(FirestoreConstants.FIELD_ROLE, FirestoreConstants.ROLE_SECURITY);
    }

    public Task<DocumentReference> addUser(Map<String, Object> userData) {
        return db.collection(FirestoreConstants.COL_USERS).add(userData);
    }

    public Task<Void> updateUser(String userId, Map<String, Object> updates) {
        return db.collection(FirestoreConstants.COL_USERS).document(userId).update(updates);
    }

    public Task<Void> deleteUser(String userId) {
        return db.collection(FirestoreConstants.COL_USERS).document(userId).delete();
    }

    public Query getLoginQuery(String username, String role) {
        // Note: Password matching will be handled in LoginActivity after migration to hashing
        return db.collection(FirestoreConstants.COL_USERS)
                .whereEqualTo(FirestoreConstants.FIELD_USERNAME, username)
                .whereEqualTo(FirestoreConstants.FIELD_ROLE, role);
    }

    // --- Visitors ---

    public Query getRecentVisitorsQuery(long startOfToday) {
        return db.collection(FirestoreConstants.COL_VISITORS)
                .whereGreaterThanOrEqualTo(FirestoreConstants.FIELD_TIMESTAMP, startOfToday)
                .orderBy(FirestoreConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING);
    }

    public Query getVisitorsByStatusQuery(String status) {
        return db.collection(FirestoreConstants.COL_VISITORS)
                .whereEqualTo(FirestoreConstants.FIELD_STATUS, status);
    }

    public Task<DocumentReference> saveVisitor(Map<String, Object> visitorData) {
        return db.collection(FirestoreConstants.COL_VISITORS).add(visitorData);
    }

    public Task<Void> performCheckoutTransaction(Transaction.Function<Void> transactionFunction) {
        return db.runTransaction(transactionFunction);
    }
    
    public DocumentReference getVisitorReference(String documentId) {
        return db.collection(FirestoreConstants.COL_VISITORS).document(documentId);
    }

    public Query getVisitorSearchQuery(String field, String query) {
        return db.collection(FirestoreConstants.COL_VISITORS)
                .whereGreaterThanOrEqualTo(field, query)
                .whereLessThanOrEqualTo(field, query + "\uf8ff")
                .limit(10);
    }

    // --- Blacklist ---

    public Query getBlacklistSearchQuery(String query) {
        return db.collection(FirestoreConstants.COL_BLACKLIST)
                .whereGreaterThanOrEqualTo(FirestoreConstants.FIELD_NAME_LOWER, query)
                .whereLessThanOrEqualTo(FirestoreConstants.FIELD_NAME_LOWER, query + "\uf8ff");
    }

    public Query getBlacklistAllQuery() {
        return db.collection(FirestoreConstants.COL_BLACKLIST)
                .orderBy(FirestoreConstants.FIELD_TIMESTAMP, Query.Direction.DESCENDING);
    }

    public Task<DocumentReference> addToBlacklist(Map<String, Object> data) {
        return db.collection(FirestoreConstants.COL_BLACKLIST).add(data);
    }

    public Task<Void> removeFromBlacklist(String docId) {
        return db.collection(FirestoreConstants.COL_BLACKLIST).document(docId).delete();
    }

    // --- Students ---

    public Query getStudentByRegQuery(String regNo) {
        return db.collection(FirestoreConstants.COL_STUDENTS)
                .whereEqualTo(FirestoreConstants.FIELD_REG_NO, regNo)
                .limit(1);
    }

    public Query getStudentByNameQuery(String nameQuery) {
        return db.collection(FirestoreConstants.COL_STUDENTS)
                .whereGreaterThanOrEqualTo(FirestoreConstants.FIELD_NAME, nameQuery)
                .whereLessThanOrEqualTo(FirestoreConstants.FIELD_NAME, nameQuery + "\uf8ff")
                .limit(1);
    }
}