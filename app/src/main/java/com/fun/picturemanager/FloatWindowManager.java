package com.fun.picturemanager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FloatWindowManager {

    private static final String CMD_FILE_PATH = "/data/local/tmp/camera_cmd.txt";
    private static FloatWindowManager instance;

    private List<File> cachedImageFiles = new ArrayList<>();
    private int currentIndex = 0;

    private int currentWidth = 1920;
    private int currentHeight = 1080;
    private int currentRotationAngle = 0;
    private boolean currentEnable = false;
    private boolean lastAA = false;

    private TextView statusTextView;
    private TextView configTextView;
    private Toast currentToast;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private FloatWindowManager() {}

    public static synchronized FloatWindowManager getInstance() {
        if (instance == null) {
            instance = new FloatWindowManager();
        }
        return instance;
    }

    public void setParams(int width, int height, int rotationAngle, boolean enable, boolean lastAA) {
        this.currentWidth = width > 0 ? width : 1920;
        this.currentHeight = height > 0 ? height : 1080;
        this.currentRotationAngle = rotationAngle;
        this.currentEnable = enable;
        this.lastAA = lastAA;
        updateConfigUI();
    }

    public void setStatusTextView(TextView textView) {
        this.statusTextView = textView;
        updateStatusUI();
    }

    public void setConfigTextView(TextView textView) {
        this.configTextView = textView;
        updateConfigUI();
    }

    private void updateConfigUI() {
        String text = "分辨率: " + currentWidth + "x" + currentHeight + " | 旋转: " + currentRotationAngle + "°";
        handler.post(() -> {
            if (configTextView != null) {
                configTextView.setText(text);
            }
        });
    }

    private void showToast(Context context, String msg) {
        if (context == null) return;
        handler.post(() -> {
            try {
                if (currentToast != null) {
                    currentToast.cancel();
                }
                currentToast = Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT);
                currentToast.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void prepareAndSaveSelectedImages(Context context, List<Uri> uris) {
        if (context == null || uris == null || uris.isEmpty()) {
            if (context != null) {
                showToast(context, "未选择任何图片");
            }
            return;
        }

        updateStatusText("正在解析图片 (" + uris.size() + "张)...");

        List<File> files = new ArrayList<>();
        File cacheDir = new File(context.getFilesDir(), "float_cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        } else {
            File[] oldFiles = cacheDir.listFiles();
            if (oldFiles != null) {
                for (File f : oldFiles) {
                    f.delete();
                }
            }
        }

        for (int i = 0; i < uris.size(); i++) {
            Uri uri = uris.get(i);
            File targetFile = new File(cacheDir, "cache_" + i + ".jpg");
            if (copyUriToFile(context, uri, targetFile)) {
                files.add(targetFile);
            }
        }

        this.cachedImageFiles = files;
        this.currentIndex = 0;

        if (files.isEmpty()) {
            showToast(context, "解析选择图片失败");
            updateStatusText("未选择图片");
            return;
        }

        handler.post(() -> {
            updateStatusUI();
            processSaveImage(context, cachedImageFiles.get(0));
        });
    }

    private boolean copyUriToFile(Context context, Uri uri, File destFile) {
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(destFile)) {
            if (is == null) return false;
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void onNextClicked(Context context) {
        if (cachedImageFiles == null || cachedImageFiles.isEmpty()) {
            showToast(context, "请先点击[选择多张图片]");
            return;
        }

        if (currentIndex + 1 < cachedImageFiles.size()) {
            currentIndex++;
            updateStatusUI();
            processSaveImage(context, cachedImageFiles.get(currentIndex));
        } else {
            showToast(context, "没有下一个了");
            updateStatusText("没有下一个了 (" + cachedImageFiles.size() + "/" + cachedImageFiles.size() + ")");
        }
    }

    private void updateStatusUI() {
        if (cachedImageFiles == null || cachedImageFiles.isEmpty()) {
            updateStatusText("未选择图片");
        } else {
            updateStatusText("进度: " + (currentIndex + 1) + "/" + cachedImageFiles.size());
        }
    }

    private void updateStatusText(String text) {
        handler.post(() -> {
            if (statusTextView != null) {
                statusTextView.setText(text);
            }
        });
    }

    public void processSaveImage(Context context, File imageFile) {
        if (context == null || imageFile == null || !imageFile.exists()) return;

        new Thread(() -> {
            try {
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap == null) {
                    handler.post(() -> Toast.makeText(context, "解码图片失败", Toast.LENGTH_SHORT).show());
                    return;
                }

                int angle = currentRotationAngle;
                if (angle != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(angle);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }

                int w = currentWidth;
                int h = currentHeight;
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, w, h, true);

                String tmpName = lastAA ? "/bb.jpg" : "/aa.jpg";
                lastAA = !lastAA;

                File localFile = new File(context.getFilesDir(), tmpName);
                FileOutputStream fos = new FileOutputStream(localFile);
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                fos.flush();
                fos.close();

                String rootPath = "/data/local/tmp" + tmpName;

                rootCopy(localFile.getAbsolutePath(), rootPath);

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type", "image");
                jsonObject.put("imgPath", rootPath);
                jsonObject.put("width", w);
                jsonObject.put("height", h);
                jsonObject.put("enable", currentEnable);

                rootWriteJson(jsonObject.toString());

                int currentNum = currentIndex + 1;
                int totalNum = cachedImageFiles.size();
                String fileName = tmpName.substring(1);
                String msg = "已保存 (" + currentNum + "/" + totalNum + "): " + rootPath;

                handler.post(() -> {
                    updateStatusText("进度: " + currentNum + "/" + totalNum + " (" + fileName + " ✅)");
                    showToast(context, msg);
                });

            } catch (Exception e) {
                e.printStackTrace();
                String err = e.getMessage();
                showToast(context, "保存失败: " + err);
            }
        }).start();
    }

    private void rootCopy(String src, String dst) {
        try {
            String cmd = "cp " + src + " " + dst + ";chmod 666 " + dst;
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rootWriteJson(String json) {
        try {
            String base64 = Base64.encodeToString(json.getBytes("UTF-8"), Base64.NO_WRAP);
            String cmd = "echo " + base64 + " | base64 -d > " + CMD_FILE_PATH + ";chmod 666 " + CMD_FILE_PATH;
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
