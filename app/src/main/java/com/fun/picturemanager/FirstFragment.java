package com.fun.picturemanager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Objects;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    private ImageView imageView;


    private Bitmap bitmap;


    private float rotate = 90;


    private EditText widthEdit;
    private EditText heightEdit;

    private Switch enableSwitch;

    private Uri imgUri;


    private boolean lastAA = false;



    ActivityResultLauncher<String> picker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {

                        if(uri!=null)
                        {
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



        binding.selectBtn
                .setOnClickListener(
                        x-> picker.launch("image/*")
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

        binding.buttonFirst.setOnClickListener(v ->
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_VideoCaptureFragment)
        );
    }

    private void loadImage(Uri uri)
    {

        try {

            imgUri = uri;

            InputStream is =
                    requireContext()
                            .getContentResolver()
                            .openInputStream(uri);


            bitmap =
                    BitmapFactory.decodeStream(is);


            imageView.setImageBitmap(bitmap);

        }catch(Exception e)
        {
            e.printStackTrace();
        }

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


        if(bitmap==null)
            return;



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
            jsonObject.put("enable", enableSwitch.isChecked());
            jsonObject.put("imgPath", rootPath);
            jsonObject.put("width", w);
            jsonObject.put("height", h);
            rootWriteJson(jsonObject.toString());



            Toast.makeText(
                            getContext(),
                            "保存成功:"
                                    +rootPath,
                            Toast.LENGTH_LONG)
                    .show();



            Toast.makeText(
                            getContext(),
                            "保存:"+path,
                            Toast.LENGTH_LONG)
                    .show();



        }catch(Exception e)
        {
            e.printStackTrace();
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
                    android.util.Base64
                            .encodeToString(
                                    json.getBytes("UTF-8"),
                                    android.util.Base64.NO_WRAP
                            );



            String cmd =
                    "echo "
                            + base64
                            + " | base64 -d > /data/local/tmp/camera_cmd.txt;"
                            +"chmod 666 /data/local/tmp/camera_cmd.txt";



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
                            "/data/local/tmp/camera_cmd.txt");


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