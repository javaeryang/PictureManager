package com.fun.picturemanager;


import android.content.*;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;


import java.io.OutputStream;



public class VideoCaptureFragment extends Fragment {


    private VideoView videoView;

    private ImageView framePreview;

    private SeekBar seekBar;


    private Uri videoUri;


    private Bitmap currentFrame;



    private Handler handler =
            new Handler(Looper.getMainLooper());




    ActivityResultLauncher<String> picker =
            registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri->{


                        if(uri!=null)
                        {

                            videoUri=uri;

                            playVideo();

                        }

                    });



    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle b)
    {


        View v =
                inflater.inflate(
                        R.layout.fragment_video_capture,
                        container,
                        false);



        videoView =
                v.findViewById(
                        R.id.videoView);



        framePreview =
                v.findViewById(
                        R.id.framePreview);



        seekBar =
                v.findViewById(
                        R.id.seekBar);



        v.findViewById(R.id.selectVideo)
                .setOnClickListener(
                        x->
                                picker.launch("video/*")
                );



        v.findViewById(R.id.saveFrame)
                .setOnClickListener(
                        x->saveToGallery()
                );




        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {


                    @Override
                    public void onProgressChanged(
                            SeekBar bar,
                            int progress,
                            boolean fromUser)
                    {


                        if(fromUser)
                        {

                            long time =
                                    progress * 1000L;


                            captureFrame(time);

                        }


                    }


                    public void onStartTrackingTouch(
                            SeekBar s){}



                    public void onStopTrackingTouch(
                            SeekBar s){}



                });



        return v;

    }




    private void playVideo()
    {

        videoView.setVideoURI(videoUri);


        videoView.setOnPreparedListener(
                mp->{


                    int duration =
                            mp.getDuration();


                    seekBar.setMax(
                            duration/1000
                    );


                    videoView.start();



                    updateSeek();


                });


    }




    private void updateSeek()
    {


        if(videoView.isPlaying())
        {


            seekBar.setProgress(
                    videoView.getCurrentPosition()/1000
            );


        }


        handler.postDelayed(
                this::updateSeek,
                500
        );

    }





    private void captureFrame(long time)
    {


        if(videoUri==null)
            return;


        try {


            MediaMetadataRetriever retriever =
                    new MediaMetadataRetriever();


            retriever.setDataSource(
                    requireContext(),
                    videoUri);



            currentFrame =
                    retriever.getFrameAtTime(
                            time*1000,
                            MediaMetadataRetriever
                                    .OPTION_CLOSEST);



            retriever.release();



            framePreview
                    .setImageBitmap(
                            currentFrame);



        }
        catch(Exception e)
        {

            e.printStackTrace();

        }


    }




    private void saveToGallery()
    {


        if(currentFrame==null)
            return;



        try {


            ContentValues values =
                    new ContentValues();


            values.put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "video_frame_"
                            +System.currentTimeMillis()
                            +".jpg");


            values.put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/jpeg");



            Uri uri =
                    requireContext()
                            .getContentResolver()
                            .insert(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                    values);



            OutputStream os =
                    requireContext()
                            .getContentResolver()
                            .openOutputStream(uri);



            currentFrame.compress(
                    Bitmap.CompressFormat.JPEG,
                    99,
                    os);



            os.close();



            Toast.makeText(
                            getContext(),
                            "保存成功",
                            Toast.LENGTH_SHORT)
                    .show();


        }
        catch(Exception e)
        {

            e.printStackTrace();

        }


    }


}