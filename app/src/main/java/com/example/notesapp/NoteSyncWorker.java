package com.example.notesapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class NoteSyncWorker extends Worker {

    public static final String KEY_USER_ID = "user_id";

    public NoteSyncWorker(@NonNull Context context, @NonNull WorkerParameters parameters) {
        super(context, parameters);
    }

    @NonNull
    @Override
    public Result doWork() {
        String userId = getInputData().getString(KEY_USER_ID);
        if (userId == null || userId.isEmpty()) {
            return Result.failure();
        }
        return NoteSyncManager.sync(getApplicationContext(), userId)
                ? Result.success() : Result.retry();
    }
}
