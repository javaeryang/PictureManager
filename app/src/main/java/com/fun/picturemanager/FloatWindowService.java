package com.fun.picturemanager;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class FloatWindowService extends Service {

    private static boolean running = false;
    private WindowManager windowManager;
    private View floatView;
    private WindowManager.LayoutParams windowLayoutParams;

    public static boolean isRunning() {
        return running;
    }

    public static void startService(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, FloatWindowService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stopService(Context context) {
        if (context == null) return;
        Intent intent = new Intent(context, FloatWindowService.class);
        context.stopService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundServiceNotification();
        }

        initFloatWindow();
    }

    private void startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "float_window_channel";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "悬浮窗服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }

            Notification notification = new NotificationCompat.Builder(this, channelId)
                    .setContentTitle("图片悬浮窗正常运行中")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(1001, notification);
            }
        }
    }

    private void initFloatWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatView = LayoutInflater.from(this).inflate(R.layout.layout_float_window, null);

        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        windowLayoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        windowLayoutParams.gravity = Gravity.TOP | Gravity.START;
        windowLayoutParams.x = 50;
        windowLayoutParams.y = 200;

        View floatHeader = floatView.findViewById(R.id.floatHeader);
        TextView floatCloseBtn = floatView.findViewById(R.id.floatCloseBtn);
        TextView floatConfigText = floatView.findViewById(R.id.floatConfigText);
        TextView floatStatusText = floatView.findViewById(R.id.floatStatusText);
        Button floatSelectBtn = floatView.findViewById(R.id.floatSelectBtn);
        Button floatNextBtn = floatView.findViewById(R.id.floatNextBtn);

        FloatWindowManager.getInstance().setConfigTextView(floatConfigText);
        FloatWindowManager.getInstance().setStatusTextView(floatStatusText);

        floatHeader.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = windowLayoutParams.x;
                        initialY = windowLayoutParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        windowLayoutParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        windowLayoutParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatView, windowLayoutParams);
                        return true;
                }
                return false;
            }
        });

        floatCloseBtn.setOnClickListener(v -> stopSelf());

        floatSelectBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, FloatPickerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        floatNextBtn.setOnClickListener(v -> {
            FloatWindowManager.getInstance().onNextClicked(this);
        });

        windowManager.addView(floatView, windowLayoutParams);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        running = false;
        if (windowManager != null && floatView != null) {
            try {
                windowManager.removeView(floatView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
