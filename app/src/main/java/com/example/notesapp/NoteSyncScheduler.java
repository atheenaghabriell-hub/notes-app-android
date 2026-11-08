package com.example.notesapp;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Data;

public final class NoteSyncScheduler {

    private NoteSyncScheduler() {
    }

    public static String workName(String userId) {
        return "notes-sync-" + userId;
    }

    public static void enqueue(Context context, String userId) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        Data input = new Data.Builder()
                .putString(NoteSyncWorker.KEY_USER_ID, userId)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(NoteSyncWorker.class)
                .setConstraints(constraints)
                .setInputData(input)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                workName(userId), ExistingWorkPolicy.APPEND_OR_REPLACE, request);
    }
}
