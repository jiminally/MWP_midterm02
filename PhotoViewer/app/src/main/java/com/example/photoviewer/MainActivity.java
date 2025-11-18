package com.example.photoviewer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Build;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    TextView textView;
    String site_url = "http://10.0.2.2:8000";
    CloadImage taskDownload;

    // UI 컴포넌트
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private SearchView searchView;
    private Button btnViewToggle;
    private Button btnDarkMode;
    private LinearLayout mainLayout;
    private LinearLayout topBar;

    // Upload 관련
    private Bitmap selectedImage = null;
    private AlertDialog uploadDialog;

    // RecyclerView 관련
    private ImageAdapter adapter;
    private List<Post> postList = new ArrayList<>();
    private boolean isGridView = false;

    // 다크모드
    private boolean isDarkMode = false;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // SharedPreferences 초기화
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);

        textView = findViewById(R.id.textView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerView);
        searchView = findViewById(R.id.searchView);
        btnViewToggle = findViewById(R.id.btn_view_toggle);
        btnDarkMode = findViewById(R.id.btn_dark_mode);
        mainLayout = findViewById(R.id.mainLayout);
        topBar = findViewById(R.id.topBar);

        // 다크모드 적용
        applyTheme();

        // RecyclerView 설정
        adapter = new ImageAdapter(postList, this, isDarkMode);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Pull to Refresh 설정
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadData();
        });

        // 검색 기능
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return false;
            }
        });

        // Grid/List 전환 버튼
        btnViewToggle.setOnClickListener(v -> {
            isGridView = !isGridView;
            if (isGridView) {
                recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
                btnViewToggle.setText("☰ 리스트 전환");
            } else {
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                btnViewToggle.setText("⊞ 그리드 전환");
            }
        });

        // 다크모드 토글 버튼
        btnDarkMode.setOnClickListener(v -> {
            isDarkMode = !isDarkMode;
            prefs.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply();
            applyTheme();
            adapter.setDarkMode(isDarkMode);
            adapter.notifyDataSetChanged();
        });
    }

    private void applyTheme() {
        if (isDarkMode) {
            // 다크모드 색상 적용
            mainLayout.setBackgroundColor(getResources().getColor(R.color.background_dark, null));
            topBar.setBackgroundColor(getResources().getColor(R.color.surface_dark, null));
            swipeRefreshLayout.setBackgroundColor(getResources().getColor(R.color.background_dark, null));
            searchView.setBackgroundColor(getResources().getColor(R.color.surface_dark, null));
            textView.setTextColor(getResources().getColor(R.color.text_primary_dark, null));
            textView.setBackgroundColor(getResources().getColor(R.color.surface_dark, null));
            btnDarkMode.setText("☀️ 라이트모드");

            // 상태바 색상 변경
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.background_dark, null));
                getWindow().getDecorView().setSystemUiVisibility(0); // 다크 아이콘
            }
        } else {
            // 라이트모드 색상 적용
            mainLayout.setBackgroundColor(getResources().getColor(R.color.background_light, null));
            topBar.setBackgroundColor(getResources().getColor(R.color.surface_light, null));
            swipeRefreshLayout.setBackgroundColor(getResources().getColor(R.color.background_light, null));
            searchView.setBackgroundColor(getResources().getColor(R.color.surface_light, null));
            textView.setTextColor(getResources().getColor(R.color.text_primary_light, null));
            textView.setBackgroundColor(getResources().getColor(R.color.surface_light, null));
            btnDarkMode.setText("🌙 다크모드");

            // 상태바 색상 변경
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.white, null));
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // DetailActivity에서 돌아왔을 때 테마 다시 적용
        isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        applyTheme();
        adapter.setDarkMode(isDarkMode);
        adapter.notifyDataSetChanged();
        loadData();
    }

    public void onClickDownload(View v) {
        loadData();
    }

    private void loadData() {
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api_root/Post/");
    }

    public void onClickUpload(View v) {
        showUploadDialog();
    }

    private void showUploadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_upload, null);
        builder.setView(dialogView);

        ImageView imagePreview = dialogView.findViewById(R.id.imagePreview);
        Button btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);
        EditText editTitle = dialogView.findViewById(R.id.editTitle);
        EditText editText = dialogView.findViewById(R.id.editText);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnUpload = dialogView.findViewById(R.id.btnUpload);

        uploadDialog = builder.create();

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        btnCancel.setOnClickListener(v -> {
            selectedImage = null;
            uploadDialog.dismiss();
        });

        btnUpload.setOnClickListener(v -> {
            String title = editTitle.getText().toString().trim();
            String text = editText.getText().toString().trim();

            if (selectedImage == null) {
                Toast.makeText(MainActivity.this, "이미지를 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            if (title.isEmpty()) {
                Toast.makeText(MainActivity.this, "제목을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            if (text.isEmpty()) {
                Toast.makeText(MainActivity.this, "내용을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            new UploadPost().execute(title, text);
            uploadDialog.dismiss();
        });

        uploadDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                if (uploadDialog != null && uploadDialog.isShowing()) {
                    ImageView imagePreview = uploadDialog.findViewById(R.id.imagePreview);
                    if (imagePreview != null) {
                        imagePreview.setImageBitmap(selectedImage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "이미지 로드 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class CloadImage extends AsyncTask<String, Integer, List<Post>> {
        @Override
        protected List<Post> doInBackground(String... urls) {
            List<Post> posts = new ArrayList<>();

            try {
                String apiUrl = urls[0];
                String token = "9f6ee41ab339f5cd9eed5fe933277f987bfc95fa";

                URL urlAPI = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();

                    String strJson = result.toString();
                    JSONArray aryJson = new JSONArray(strJson);

                    for (int i = 0; i < aryJson.length(); i++) {
                        JSONObject postJson = aryJson.getJSONObject(i);

                        int id = i;
                        String title = postJson.getString("title");
                        String text = postJson.getString("text");
                        String author = postJson.getString("author");
                        String createdDate = postJson.getString("created_date");
                        String imageUrl = postJson.getString("image");

                        Bitmap imageBitmap = null;
                        if (!imageUrl.equals("")) {
                            URL myImageUrl = new URL(imageUrl);
                            HttpURLConnection imgConn = (HttpURLConnection) myImageUrl.openConnection();
                            InputStream imgStream = imgConn.getInputStream();
                            imageBitmap = BitmapFactory.decodeStream(imgStream);
                            imgStream.close();
                        }

                        Post post = new Post(id, title, text, author, createdDate, imageUrl, imageBitmap);
                        posts.add(post);
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return posts;
        }

        @Override
        protected void onPostExecute(List<Post> posts) {
            swipeRefreshLayout.setRefreshing(false);

            if (posts.isEmpty()) {
                textView.setText("불러올 이미지가 없습니다.");
            } else {
                textView.setText("게시물 " + posts.size() + "개");
                postList.clear();
                postList.addAll(posts);
                adapter.updateData(postList);
            }
        }
    }

    private class UploadPost extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String title = params[0];
            String text = params[1];
            String token = "9f6ee41ab339f5cd9eed5fe933277f987bfc95fa";

            try {
                String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
                String LINE_FEED = "\r\n";

                URL url = new URL(site_url + "/api_root/Post/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());

                outputStream.writeBytes("--" + boundary + LINE_FEED);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"author\"" + LINE_FEED);
                outputStream.writeBytes(LINE_FEED);
                outputStream.writeBytes("1" + LINE_FEED);

                outputStream.writeBytes("--" + boundary + LINE_FEED);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"title\"" + LINE_FEED);
                outputStream.writeBytes(LINE_FEED);
                outputStream.writeBytes(title + LINE_FEED);

                outputStream.writeBytes("--" + boundary + LINE_FEED);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"text\"" + LINE_FEED);
                outputStream.writeBytes(LINE_FEED);
                outputStream.writeBytes(text + LINE_FEED);

                outputStream.writeBytes("--" + boundary + LINE_FEED);
                outputStream.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"upload.jpg\"" + LINE_FEED);
                outputStream.writeBytes("Content-Type: image/jpeg" + LINE_FEED);
                outputStream.writeBytes(LINE_FEED);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                selectedImage.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                outputStream.write(imageBytes);
                outputStream.writeBytes(LINE_FEED);

                outputStream.writeBytes("--" + boundary + "--" + LINE_FEED);
                outputStream.flush();
                outputStream.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    return "success";
                } else {
                    return "failed: " + responseCode;
                }

            } catch (Exception e) {
                e.printStackTrace();
                return "error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.equals("success")) {
                Toast.makeText(MainActivity.this, "업로드 성공!", Toast.LENGTH_LONG).show();
                selectedImage = null;
                loadData();
            } else {
                Toast.makeText(MainActivity.this, "업로드 실패: " + result, Toast.LENGTH_LONG).show();
            }
        }
    }
}