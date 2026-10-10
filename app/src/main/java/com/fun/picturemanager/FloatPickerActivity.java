package com.fun.picturemanager;

import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class FloatPickerActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> multiPicker =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    new Thread(() -> {
                        FloatWindowManager.getInstance().prepareAndSaveSelectedImages(FloatPickerActivity.this, uris);
                        runOnUiThread(this::finish);
                    }).start();
                } else {
                    finish();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        multiPicker.launch("image/*");
    }
}
