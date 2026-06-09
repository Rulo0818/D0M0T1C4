package com.domotica.app.data;

import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.domotica.app.util.Constants;

public class DeviceRepository {
    private FirebaseManager firebase = FirebaseManager.getInstance();

    public void getLinkedDeviceId(String uid, @NonNull final OnDeviceIdCallback callback) {
        firebase.getUsersRef().child(uid).child(Constants.DEVICE_ID)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String deviceId = snapshot.getValue(String.class);
                    callback.onResult(deviceId);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    callback.onError(error.toException());
                }
            });
    }

    public void setLed(String deviceId, int status, OnWriteCallback callback) {
        firebase.getDevicesRef().child(deviceId)
            .child(Constants.LED)
            .setValue(status)
            .addOnCompleteListener(task -> {
                if (callback != null) {
                    callback.onComplete(task.isSuccessful(), task.getException());
                }
            });
    }

    public interface OnWriteCallback {
        void onComplete(boolean success, Exception error);
    }

    public DatabaseReference getDeviceRef(String deviceId) {
        return firebase.getDevicesRef().child(deviceId);
    }

    public void saveDeviceBinding(String uid, String deviceId) {
        firebase.getUsersRef().child(uid)
            .child(Constants.DEVICE_ID).setValue(deviceId);
    }

    public interface OnDeviceIdCallback {
        void onResult(String deviceId);
        void onError(Exception e);
    }
}
