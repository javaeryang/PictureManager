package com.fun.picturemanager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.fun.picturemanager.databinding.FragmentFirstBinding;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FirstFragment extends Fragment {

    private static final String CMD_FILE_PATH = "/data/local/tmp/camera_cmd.txt";

    private FragmentFirstBinding binding;

    private ImageView imageView;


    private Bitmap bitmap;


    private float rotate = 90;


    private EditText widthEdit;
    private EditText heightEdit;

    private Switch enableSwitch;

    private Uri imgUri;


    private boolean lastAA = false;

    private String currentType = "image";
    private Uri selectedVideoUri;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String> videoPicker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedVideoUri = uri;
                            currentType = "video";
                            updateTypeUI();
                            Toast.makeText(getContext(), "已选择视频，已切换为视频模式", Toast.LENGTH_SHORT).show();
                        }
                    });

    ActivityResultLauncher<String> picker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {

                        if(uri!=null)
                        {
                            currentType = "image";
                            updateTypeUI();
                            loadImage(uri);
                            rotateImage();
                        }

                    });

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        imageView = binding.imageView;


        widthEdit = binding.widthEdit;


        heightEdit = binding.heightEdit;

        enableSwitch =
                binding.enableSwitch;

        String[] resolutions = {"自定义", "1280x720", "1920x1080"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, resolutions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.resolutionSpinner.setAdapter(adapter);

        binding.resolutionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = resolutions[position];
                if ("1280x720".equals(selected)) {
                    widthEdit.setText("1280");
                    heightEdit.setText("720");
                } else if ("1920x1080".equals(selected)) {
                    widthEdit.setText("1920");
                    heightEdit.setText("1080");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        preloadConfig();

        binding.typeSwitchBtn.setOnClickListener(v -> {
            if ("image".equals(currentType)) {
                currentType = "video";
            } else {
                currentType = "image";
            }
            updateTypeUI();
            Toast.makeText(getContext(), "已切换类型为: " + ("video".equals(currentType) ? "视频" : "图片"), Toast.LENGTH_SHORT).show();
        });

        binding.selectBtn
                .setOnClickListener(
                        x-> picker.launch("image/*")
                );

        binding.selectVideoBtn.setOnClickListener(
                x -> videoPicker.launch("video/*")
        );

        binding.frontRotate.setOnClickListener(view1 -> {
            loadImage(imgUri);
            rotateImage(90);
        });
        binding.backendRotate.setOnClickListener(view1 -> {
            loadImage(imgUri);
            rotateImage(270);
        });

        binding.rotateBtn
                .setOnClickListener(
                        x-> rotateImage(rotate)
                );


        binding.saveBtn
                .setOnClickListener(
                        x->saveImage()
                );

        binding.dateManageBtn.setOnClickListener(v ->
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.DateImageFragment)
        );

        binding.autoGetBtn.setOnClickListener(v -> autoGetTodayImage());

        binding.buttonFirst.setOnClickListener(v ->
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_VideoCaptureFragment)
        );
    }

    private void autoGetTodayImage() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(Calendar.getInstance().getTime());

        new Thread(() -> {
            DateImage di = AppDatabase.getDatabase(requireContext()).dateImageDao().getByDate(today);
            handler.post(() -> {
                if (di != null && di.imagePath != null) {
                    loadImage(Uri.parse(di.imagePath));
                    Toast.makeText(getContext(), "已自动加载今日图片", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "今日未设置图片，请从相册选取", Toast.LENGTH_SHORT).show();
                    picker.launch("image/*");
                }
            });
        }).start();
    }

    private void loadImage(Uri uri)
    {

        try {

            imgUri = uri;

            InputStream is;
            if (uri.getScheme() == null || "file".equals(uri.getScheme())) {
                String path = uri.getPath();
                if (path != null) {
                    is = new FileInputStream(new File(path));
                } else {
                    is = requireContext().getContentResolver().openInputStream(uri);
                }
            } else {
                is = requireContext().getContentResolver().openInputStream(uri);
            }

            if (is == null) return;

            bitmap =
                    BitmapFactory.decodeStream(is);


            imageView.setImageBitmap(bitmap);
            is.close();

        }catch(Exception e)
        {
            e.printStackTrace();
        }

    }


    private void updateTypeUI() {
        if (binding == null) return;
        if ("video".equals(currentType)) {
            binding.typeSwitchBtn.setText("当前类型：视频 (点击切换)");
        } else {
            binding.typeSwitchBtn.setText("当前类型：图片 (点击切换)");
        }
    }

    private void preloadConfig()
    {

        new Thread(() -> {

            String json = readFile(CMD_FILE_PATH);

            if(json==null)
                return;

            try {

                JSONObject obj = new JSONObject(json);

                String type = obj.optString("type", "image");
                boolean enable = obj.optBoolean("enable", false);
                String imgPath = obj.optString("imgPath", null);
                String videoPath = obj.optString("videoPath", null);
                int width = obj.optInt("width", 0);
                int height = obj.optInt("height", 0);

                handler.post(() -> applyConfig(type, enable, imgPath, videoPath, width, height));

            }catch(Exception e)
            {
                e.printStackTrace();
            }

        }).start();

    }


    private void applyConfig(
            String type,
            boolean enable,
            String imgPath,
            String videoPath,
            int width,
            int height)
    {

        if(binding==null)
            return;

        if ("video".equals(type)) {
            currentType = "video";
        } else {
            currentType = "image";
        }
        updateTypeUI();

        enableSwitch.setChecked(enable);

        if(width>0)
            widthEdit.setText(String.valueOf(width));

        if(height>0)
            heightEdit.setText(String.valueOf(height));

        if("image".equals(currentType) && imgPath!=null && !imgPath.isEmpty())
            loadImageFromFile(imgPath);

    }


    private String readFile(String path)
    {

        File f = new File(path);

        if(!f.exists() || !f.canRead())
            return null;

        try (FileInputStream fis = new FileInputStream(f)) {

            byte[] data = new byte[(int) f.length()];

            int n = fis.read(data);

            return n<=0 ? null : new String(data, 0, n, "UTF-8");

        }catch(Exception e)
        {
            return null;
        }

    }


    private void loadImageFromFile(String path)
    {

        File f = new File(path);

        if(!f.exists() || !f.canRead())
            return;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;

        try (FileInputStream fis = new FileInputStream(f)) {
            BitmapFactory.decodeStream(fis, null, opts);
        }catch(Exception e)
        {
            return;
        }

        int sample = 1;
        int maxDim = 1920;

        while(opts.outWidth / sample > maxDim
                || opts.outHeight / sample > maxDim)
        {
            sample *= 2;
        }

        opts.inJustDecodeBounds = false;
        opts.inSampleSize = sample;

        Bitmap bmp = null;

        try (FileInputStream fis = new FileInputStream(f)) {
            bmp = BitmapFactory.decodeStream(fis, null, opts);
        }catch(Exception e)
        {
            e.printStackTrace();
        }

        if(bmp==null)
            return;

        bitmap = bmp;

        imgUri = Uri.fromFile(f);

        imageView.setImageBitmap(bitmap);

    }




    private void rotateImage()
    {
        rotateImage(270);

    }
    private void rotateImage(float rotate)
    {

        if(bitmap==null)
            return;

        Matrix matrix=new Matrix();

        matrix.postRotate(rotate);


        bitmap =
                Bitmap.createBitmap(
                        bitmap,
                        0,
                        0,
                        bitmap.getWidth(),
                        bitmap.getHeight(),
                        matrix,
                        true);



        imageView.setImageBitmap(bitmap);

    }





    private void saveImage()
    {
        if ("video".equals(currentType)) {
            saveVideoConfig();
        } else {
            saveImageConfig();
        }
    }

    private void saveVideoConfig() {
        int w = 1920;
        int h = 1080;
        try {
            w = Integer.parseInt(widthEdit.getText().toString());
            h = Integer.parseInt(heightEdit.getText().toString());
        } catch (Exception ignored) {}

        String rootVideoPath = "/data/local/tmp/test.mp4";

        if (selectedVideoUri != null) {
            try {
                String localPath = requireContext().getFilesDir().getAbsolutePath() + "/test.mp4";
                File localFile = new File(localPath);
                if (copyUriToFile(selectedVideoUri, localFile)) {
                    rootCopy(localPath, rootVideoPath);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "video");
            jsonObject.put("videoPath", rootVideoPath);
            jsonObject.put("width", w);
            jsonObject.put("height", h);
            jsonObject.put("fps", 30);
            jsonObject.put("loop", true);
            jsonObject.put("queueSize", 30);
            jsonObject.put("enable", enableSwitch.isChecked());

            rootWriteJson(jsonObject.toString());

            Toast.makeText(
                            getContext(),
                            "保存视频配置成功:\n" + rootVideoPath,
                            Toast.LENGTH_LONG)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageConfig()
    {


        if(bitmap==null)
        {
            Toast.makeText(getContext(), "请先选择图片", Toast.LENGTH_SHORT).show();
            return;
        }


        int w =
                Integer.parseInt(
                        widthEdit.getText().toString());


        int h =
                Integer.parseInt(
                        heightEdit.getText().toString());



        Bitmap result =
                Bitmap.createScaledBitmap(
                        bitmap,
                        w,
                        h,
                        true);



        String tmpName;


        if(lastAA)
        {
            tmpName= "/bb.jpg";
        }
        else
        {
            tmpName= "/aa.jpg";
        }


        lastAA=!lastAA;



        try {

            String path = requireContext().getFilesDir().getAbsolutePath() + tmpName;
            String rootPath =
                    "/data/local/tmp"
                            + tmpName;

            FileOutputStream fos =
                    new FileOutputStream(path);


            result.compress(
                    Bitmap.CompressFormat.JPEG,
                    95,
                    fos);



            fos.flush();
            fos.close();



            // root复制
            rootCopy(
                    path,
                    rootPath
            );



            // 写配置文件
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "image");
            jsonObject.put("imgPath", rootPath);
            jsonObject.put("width", w);
            jsonObject.put("height", h);
            jsonObject.put("enable", enableSwitch.isChecked());
            rootWriteJson(jsonObject.toString());



            Toast.makeText(
                            getContext(),
                            "保存图片配置成功:"
                                    +rootPath,
                            Toast.LENGTH_LONG)
                    .show();


        }catch(Exception e)
        {
            e.printStackTrace();
        }

    }

    private boolean copyUriToFile(Uri uri, File destFile) {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri);
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

    private void rootCopy(
            String src,
            String dst)
    {

        try {


            String cmd =
                    "cp "
                            +src
                            +" "
                            +dst
                            +";chmod 666 "
                            +dst;



            Process p =
                    Runtime.getRuntime()
                            .exec(new String[]{
                                    "su",
                                    "-c",
                                    cmd
                            });



            p.waitFor();



        }catch(Exception e)
        {

            e.printStackTrace();

        }


    }

    private void rootWriteJson(String json)
    {


        try {


            // base64编码避免shell特殊字符
            String base64 =
                    Base64
                            .encodeToString(
                                    json.getBytes("UTF-8"),
                                    Base64.NO_WRAP
                            );



            String cmd =
                    "echo "
                            + base64
                            + " | base64 -d > " + CMD_FILE_PATH + ";"
                            +"chmod 666 " + CMD_FILE_PATH;



            Process p =
                    Runtime.getRuntime()
                            .exec(new String[]{
                                    "su",
                                    "-c",
                                    cmd
                            });



            p.waitFor();



        }
        catch(Exception e)
        {

            e.printStackTrace();

        }

    }


    private void writeCommand(String path)
    {


        try {


            FileOutputStream fos =
                    new FileOutputStream(
                            CMD_FILE_PATH);


            fos.write(
                    path.getBytes()
            );


            fos.flush();

            fos.close();



        }catch(Exception e)
        {

            e.printStackTrace();

        }

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}