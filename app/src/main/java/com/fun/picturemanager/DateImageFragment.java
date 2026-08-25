package com.fun.picturemanager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DateImageFragment extends Fragment {

    private RecyclerView recyclerView;
    private DateImageAdapter adapter;
    private List<DateItem> dateItems = new ArrayList<>();
    private String currentEditingDate;
    private boolean isAscending = true;

    private final ActivityResultLauncher<String> singlePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && currentEditingDate != null) {
                    String localPath = copyUriToInternalStorage(getContext(), uri, currentEditingDate);
                    if (localPath != null) {
                        saveDateImage(currentEditingDate, localPath);
                    } else {
                        Toast.makeText(getContext(), "复制图片到本地失败", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<String> multiPicker =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    batchUpdateImages(uris);
                }
            });

    private String copyUriToInternalStorage(Context context, Uri uri, String fileName) {
        if (context == null) return null;
        try {
            File outputDir = new File(context.getFilesDir(), "date_images");
            if (!outputDir.exists()) outputDir.mkdirs();
            File outputFile = new File(outputDir, fileName + ".jpg");

            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return null;

            OutputStream os = new FileOutputStream(outputFile);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = is.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }
            os.close();
            is.close();
            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_date_image, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        view.findViewById(R.id.sortBtn).setOnClickListener(v -> {
            isAscending = !isAscending;
            ((Button)v).setText(isAscending ? "排序: 升序" : "排序: 降序");
            sortItems(isAscending);
        });

        view.findViewById(R.id.batchDeleteBtn).setOnClickListener(v -> {
            batchDelete();
        });

        view.findViewById(R.id.batchUpdateBtn).setOnClickListener(v -> {
            multiPicker.launch("image/*");
        });

        loadDates();
    }

    private void batchDelete() {
        Context context = getContext();
        if (context == null) return;
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            for (DateItem item : dateItems) {
                if (item.selected && item.imagePath != null) {
                    db.dateImageDao().deleteByDate(item.date);
                }
            }
            loadDates();
        }).start();
    }

    private void batchUpdateImages(List<Uri> uris) {
        Context context = getContext();
        if (context == null) return;
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            int uriIndex = 0;
            for (DateItem item : dateItems) {
                if (item.selected) {
                    if (uriIndex < uris.size()) {
                        String localPath = copyUriToInternalStorage(context, uris.get(uriIndex), item.date);
                        if (localPath != null) {
                            db.dateImageDao().insert(new DateImage(item.date, localPath));
                        }
                        uriIndex++;
                    } else {
                        // If we have fewer images than selected dates, we stop updating
                        break;
                    }
                }
            }
            loadDates();
        }).start();
    }

    private void loadDates() {
        Context context = getContext();
        if (context == null) return;
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            List<DateImage> savedImages = db.dateImageDao().getAll();
            Map<String, String> imageMap = new HashMap<>();
            for (DateImage di : savedImages) {
                imageMap.put(di.date, di.imagePath);
            }

            dateItems.clear();
            Calendar calendar = Calendar.getInstance();
            int currentMonth = calendar.get(Calendar.MONTH);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            
            // Loop until the end of the month
            while (calendar.get(Calendar.MONTH) == currentMonth) {
                String dateStr = sdf.format(calendar.getTime());
                dateItems.add(new DateItem(dateStr, imageMap.get(dateStr)));
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (!isAscending) {
                Collections.reverse(dateItems);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter = new DateImageAdapter(dateItems);
                    recyclerView.setAdapter(adapter);
                });
            }
        }).start();
    }

    private void saveDateImage(String date, String path) {
        Context context = getContext();
        if (context == null) return;
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            db.dateImageDao().insert(new DateImage(date, path));
            loadDates(); // Reload
        }).start();
    }

    private void deleteDateImage(String date) {
        Context context = getContext();
        if (context == null) return;
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(context);
            db.dateImageDao().deleteByDate(date);
            loadDates();
        }).start();
    }

    private void showPreview(String path) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_preview);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        ImageView previewImage = dialog.findViewById(R.id.previewImage);
        
        try {
            if (path.startsWith("/")) {
                previewImage.setImageURI(Uri.fromFile(new File(path)));
            } else {
                previewImage.setImageURI(Uri.parse(path));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        previewImage.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private class DateItem {
        String date;
        String imagePath;
        boolean selected;

        DateItem(String date, String imagePath) {
            this.date = date;
            this.imagePath = imagePath;
        }
    }

    private class DateImageAdapter extends RecyclerView.Adapter<DateImageAdapter.ViewHolder> {
        private final List<DateItem> items;

        DateImageAdapter(List<DateItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date_image, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DateItem item = items.get(position);
            holder.dateText.setText(item.date);
            holder.checkBox.setChecked(item.selected);
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> item.selected = isChecked);

            if (item.imagePath != null) {
                try {
                    if (item.imagePath.startsWith("/")) {
                        holder.imageView.setImageURI(Uri.fromFile(new File(item.imagePath)));
                    } else {
                        holder.imageView.setImageURI(Uri.parse(item.imagePath));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    holder.imageView.setImageDrawable(null);
                }
            } else {
                holder.imageView.setImageDrawable(null);
            }

            holder.imageView.setOnClickListener(v -> {
                if (item.imagePath != null) {
                    showPreview(item.imagePath);
                }
            });

            holder.changeBtn.setOnClickListener(v -> {
                currentEditingDate = item.date;
                singlePicker.launch("image/*");
            });

            holder.deleteBtn.setOnClickListener(v -> {
                deleteDateImage(item.date);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView dateText;
            ImageView imageView;
            Button changeBtn, deleteBtn;
            CheckBox checkBox;

            ViewHolder(View itemView) {
                super(itemView);
                dateText = itemView.findViewById(R.id.dateText);
                imageView = itemView.findViewById(R.id.dateImage);
                changeBtn = itemView.findViewById(R.id.changeBtn);
                deleteBtn = itemView.findViewById(R.id.deleteBtn);
                checkBox = itemView.findViewById(R.id.checkBox);
            }
        }
    }
    
    // Sorting functionality
    public void sortItems(boolean ascending) {
        if (ascending) {
            Collections.sort(dateItems, (o1, o2) -> o1.date.compareTo(o2.date));
        } else {
            Collections.sort(dateItems, (o1, o2) -> o2.date.compareTo(o1.date));
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
