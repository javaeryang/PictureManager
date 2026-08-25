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

    private Button prevFrame, nextFrame, batchExport;

    private ProgressBar exportProgressBar;
    private TextView exportStatusText;
    private TextView videoInfoText;
    private TickMarkView tickMarkView;


    private Uri videoUri;


    private Bitmap currentFrame;


    private double fps = 30.0;
    private int totalFrames = 0;
    private long currentFrameIndex = 0;



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

        prevFrame = v.findViewById(R.id.prevFrame);
        nextFrame = v.findViewById(R.id.nextFrame);
        batchExport = v.findViewById(R.id.batchExport);

        exportProgressBar = v.findViewById(R.id.exportProgressBar);
        exportStatusText = v.findViewById(R.id.exportStatusText);
        videoInfoText = v.findViewById(R.id.videoInfoText);
        tickMarkView = v.findViewById(R.id.tickMarkView);



        v.findViewById(R.id.selectVideo)
                .setOnClickListener(
                        x->
                                picker.launch("video/*")
                );



        v.findViewById(R.id.saveFrame)
                .setOnClickListener(
                        x->saveToGallery()
                );


        prevFrame.setOnClickListener(v1 -> stepFrame(-1));
        nextFrame.setOnClickListener(v1 -> stepFrame(1));
        batchExport.setOnClickListener(v1 -> batchExport( (int)fps ));




        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    private Runnable captureRunnable;

                    @Override
                    public void onProgressChanged(
                            SeekBar bar,
                            int progress,
                            boolean fromUser)
                    {


                        if(fromUser)
                        {
                            // 1. Snapping Logic
                            int snappedProgress = progress;
                            int framePerSec = (int) fps;
                            if (framePerSec > 0) {
                                int nearestSecondFrame = Math.round((float) progress / framePerSec) * framePerSec;
                                // Snap if within 15% of a second
                                if (Math.abs(progress - nearestSecondFrame) < framePerSec * 0.15f) {
                                    snappedProgress = nearestSecondFrame;
                                    bar.setProgress(snappedProgress);
                                }
                            }

                            currentFrameIndex = snappedProgress;

                            // 2. Debounced Rendering
                            handler.removeCallbacks(captureRunnable);
                            captureRunnable = () -> captureFrameAtIndex(currentFrameIndex);
                            handler.postDelayed(captureRunnable, 100);

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


                    extractMetadata();


                    seekBar.setMax(
                            totalFrames > 0 ? totalFrames - 1 : 0
                    );


                    videoView.start();



                    updateSeek();


                });


    }


    private void extractMetadata() {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(requireContext(), videoUri);

            // 1. Try to get FPS
            String fpsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
            if (fpsStr != null) {
                fps = Double.parseDouble(fpsStr);
            } else {
                fps = 30.0; // Default
            }

            // 2. Try to get total frames (API 28+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                String frameCount = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT);
                if (frameCount != null) {
                    totalFrames = Integer.parseInt(frameCount);
                }
            }

            // 3. Fallback for total frames using duration
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (durationStr != null) {
                long durationMs = Long.parseLong(durationStr);
                if (totalFrames <= 0) {
                    totalFrames = (int) (durationMs * fps / 1000);
                } else {
                    // If we have totalFrames, we can refine FPS
                    fps = totalFrames * 1000.0 / durationMs;
                }
            }

            // Update UI
            String info = String.format("FPS: %.2f | 总帧数: %d", fps, totalFrames);
            handler.post(() -> {
                videoInfoText.setText(info);
                tickMarkView.setVideoData(totalFrames, fps);
            });

        } catch (Exception e) {
            e.printStackTrace();
            handler.post(() -> videoInfoText.setText("无法获取视频信息"));
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {}
        }
    }


    private void stepFrame(int delta) {
        currentFrameIndex += delta;
        if (currentFrameIndex < 0) currentFrameIndex = 0;
        if (currentFrameIndex >= totalFrames) currentFrameIndex = totalFrames - 1;

        seekBar.setProgress((int) currentFrameIndex);
        captureFrameAtIndex(currentFrameIndex);
    }




    private void updateSeek()
    {


        if(videoView.isPlaying())
        {

            int positionMs = videoView.getCurrentPosition();
            currentFrameIndex = (long) (positionMs * fps / 1000.0);

            seekBar.setProgress((int) currentFrameIndex);


        }


        handler.postDelayed(
                this::updateSeek,
                100
        );

    }





    private void captureFrameAtIndex(long index) {
        if (videoUri == null) return;

        new Thread(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(requireContext(), videoUri);

                Bitmap bitmap;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    bitmap = retriever.getFrameAtIndex((int) index);
                } else {
                    // Fallback: calculate time from index
                    long timeUs = (long) (index * 1000000.0 / fps);
                    bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST);
                }

                if (bitmap != null) {
                    currentFrame = bitmap;
                    handler.post(() -> framePreview.setImageBitmap(currentFrame));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    retriever.release();
                } catch (Exception ignored) {}
            }
        }).start();
    }


    private void batchExport(int interval) {
        if (videoUri == null || totalFrames <= 0) return;

        // UI Setup
        exportProgressBar.setVisibility(View.VISIBLE);
        exportStatusText.setVisibility(View.VISIBLE);
        exportProgressBar.setProgress(0);
        batchExport.setEnabled(false);

        int totalToExport = 0;
        for (int i = 0; i < totalFrames; i += interval) totalToExport++;
        exportProgressBar.setMax(totalToExport);

        final int totalTasks = totalToExport;

        new Thread(() -> {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(requireContext(), videoUri);
                int count = 0;
                int currentTaskIndex = 0;
                for (int i = 0; i < totalFrames; i += interval) {
                    Bitmap bitmap;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        bitmap = retriever.getFrameAtIndex(i);
                    } else {
                        long timeUs = (long) (i * 1000000.0 / fps);
                        bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST);
                    }

                    if (bitmap != null) {
                        if (saveBitmapToGallery(bitmap, "batch_" + i)) {
                            count++;
                        }
                    }

                    currentTaskIndex++;
                    int progress = currentTaskIndex;
                    handler.post(() -> {
                        exportProgressBar.setProgress(progress);
                        exportStatusText.setText("正在导出: " + progress + "/" + totalTasks);
                    });
                }
                int finalCount = count;
                handler.post(() -> {
                    Toast.makeText(getContext(), "批量保存完成，成功: " + finalCount + "/" + totalTasks, Toast.LENGTH_LONG).show();
                    resetExportUI();
                });
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    Toast.makeText(getContext(), "批量保存出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetExportUI();
                });
            } finally {
                try {
                    retriever.release();
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void resetExportUI() {
        exportProgressBar.setVisibility(View.GONE);
        exportStatusText.setVisibility(View.GONE);
        batchExport.setEnabled(true);
    }


    private boolean saveBitmapToGallery(Bitmap bmp, String nameSuffix) {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "video_frame_" + nameSuffix + "_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            Uri uri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
                    return bmp.compress(Bitmap.CompressFormat.JPEG, 95, os);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    private void saveToGallery()
    {


        if(currentFrame==null)
            return;

        if (saveBitmapToGallery(currentFrame, "single")) {
            Toast.makeText(getContext(), "保存成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "保存失败", Toast.LENGTH_SHORT).show();
        }
    }


}