package com.example.photoviewer;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DetailActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    private ImageView detailImageView;
    private TextView detailTitle, detailText, detailDate;
    private Button btnDelete;
    private ImageButton btnBack;
    private LinearLayout detailLayout;
    private LinearLayout detailTopBar;
    private View detailDivider;

    private int postId;
    private String imageUrl;
    private String token = "9f6ee41ab339f5cd9eed5fe933277f987bfc95fa";
    private String site_url = "http://10.0.2.2:8000";

    private boolean isDarkMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // SharedPreferences에서 다크모드 설정 읽기
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);

        detailImageView = findViewById(R.id.detailImageView);
        detailTitle = findViewById(R.id.detailTitle);
        detailText = findViewById(R.id.detailText);
        detailDate = findViewById(R.id.detailDate);
        btnDelete = findViewById(R.id.btnDelete);
        btnBack = findViewById(R.id.btnBack);
        detailLayout = findViewById(R.id.detailLayout);
        detailTopBar = findViewById(R.id.detailTopBar);
        detailDivider = findViewById(R.id.detailDivider);

        // 다크모드 적용
        applyTheme();

        // Intent에서 데이터 받기
        postId = getIntent().getIntExtra("post_id", -1);
        String title = getIntent().getStringExtra("title");
        String text = getIntent().getStringExtra("text");
        String date = getIntent().getStringExtra("date");
        imageUrl = getIntent().getStringExtra("image_url");

        Log.d("DetailActivity", "Post ID: " + postId);
        Log.d("DetailActivity", "Image URL: " + imageUrl);

        // 데이터 표시
        detailTitle.setText(title);
        detailText.setText(text);
        detailDate.setText(date.substring(0, 10));

        // 이미지 로드
        new LoadImageTask().execute(imageUrl);

        // 뒤로가기 버튼
        btnBack.setOnClickListener(v -> finish());

        // 삭제 버튼
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    private void applyTheme() {
        if (isDarkMode) {
            // 다크모드 색상 적용
            detailLayout.setBackgroundColor(getResources().getColor(R.color.background_dark, null));
            detailTopBar.setBackgroundColor(getResources().getColor(R.color.surface_dark, null));
            detailDivider.setBackgroundColor(getResources().getColor(R.color.divider_dark, null));
            detailTitle.setTextColor(getResources().getColor(R.color.text_primary_dark, null));
            detailText.setTextColor(getResources().getColor(R.color.text_primary_dark, null));
            detailDate.setTextColor(getResources().getColor(R.color.text_secondary_dark, null));

            // 상태바 색상 변경
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.background_dark, null));
                getWindow().getDecorView().setSystemUiVisibility(0);
            }
        } else {
            // 라이트모드 색상 적용
            detailLayout.setBackgroundColor(getResources().getColor(R.color.background_light, null));
            detailTopBar.setBackgroundColor(getResources().getColor(R.color.surface_light, null));
            detailDivider.setBackgroundColor(getResources().getColor(R.color.divider_light, null));
            detailTitle.setTextColor(getResources().getColor(R.color.text_primary_light, null));
            detailText.setTextColor(getResources().getColor(R.color.text_primary_light, null));
            detailDate.setTextColor(getResources().getColor(R.color.text_secondary_light, null));

            // 상태바 색상 변경
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.white, null));
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("게시물 삭제")
                .setMessage("정말로 이 게시물을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    // imageUrl에서 실제 post ID 추출
                    String realPostId = extractPostIdFromUrl(imageUrl);
                    Log.d("DetailActivity", "Extracted Post ID from URL: " + realPostId);
                    new DeletePostTask().execute(realPostId);
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // URL에서 실제 post ID 추출
    private String extractPostIdFromUrl(String imageUrl) {
        // 예: http://10.0.2.2:8000/media/post/123/image.jpg -> 123 추출
        try {
            String[] parts = imageUrl.split("/");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("post") && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
        } catch (Exception e) {
            Log.e("DetailActivity", "Error extracting post ID: " + e.getMessage());
        }
        return String.valueOf(postId);
    }

    // 이미지 로드 AsyncTask
    private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                InputStream is = conn.getInputStream();
                return BitmapFactory.decodeStream(is);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                detailImageView.setImageBitmap(bitmap);
            }
        }
    }

    // 삭제 AsyncTask
    private class DeletePostTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... ids) {
            try {
                String postIdStr = ids[0];
                String deleteUrl = site_url + "/api_root/Post/" + postIdStr + "/";

                Log.d("DELETE", "Delete URL: " + deleteUrl);

                URL url = new URL(deleteUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                Log.d("DELETE", "Response Code: " + responseCode);

                // 에러 응답 읽기
                if (responseCode != HttpURLConnection.HTTP_NO_CONTENT &&
                        responseCode != HttpURLConnection.HTTP_OK) {
                    InputStream errorStream = conn.getErrorStream();
                    if (errorStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        StringBuilder errorResponse = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            errorResponse.append(line);
                        }
                        Log.e("DELETE", "Error Response: " + errorResponse.toString());
                        return "failed:" + responseCode + ":" + errorResponse.toString();
                    }
                }

                if (responseCode == HttpURLConnection.HTTP_NO_CONTENT ||
                        responseCode == HttpURLConnection.HTTP_OK) {
                    return "success";
                } else {
                    return "failed:" + responseCode;
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("DELETE", "Exception: " + e.getMessage());
                return "error:" + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("DELETE", "Result: " + result);

            if (result.equals("success")) {
                Toast.makeText(DetailActivity.this, "삭제되었습니다", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(DetailActivity.this, "삭제 실패: " + result, Toast.LENGTH_LONG).show();
            }
        }
    }
}