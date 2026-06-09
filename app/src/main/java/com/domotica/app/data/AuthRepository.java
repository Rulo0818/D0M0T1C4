package com.domotica.app.data;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import com.domotica.app.util.Constants;

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {
    private FirebaseManager firebase = FirebaseManager.getInstance();

    public Task<AuthResult> login(String email, String password) {
        return firebase.getAuth().signInWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> register(String email, String password) {
        return firebase.getAuth().createUserWithEmailAndPassword(email, password);
    }

    public void saveUserToDatabase(FirebaseUser user, String displayName) {
        Map<String, Object> userData = new HashMap<>();
        userData.put(Constants.EMAIL, user.getEmail());
        userData.put(Constants.DISPLAY_NAME, displayName);
        userData.put(Constants.CREATED_AT, System.currentTimeMillis());
        firebase.getUsersRef().child(user.getUid()).setValue(userData);
    }

    public FirebaseUser getCurrentUser() {
        return firebase.getAuth().getCurrentUser();
    }

    public void logout() {
        firebase.getAuth().signOut();
    }
}
