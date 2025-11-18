package com.example.photoviewer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private List<Post> postList;
    private List<Post> postListFull;  // 검색을 위한 원본 데이터
    private Context context;
    private boolean isDarkMode;

    public ImageAdapter(List<Post> postList, Context context, boolean isDarkMode) {
        this.postList = postList;
        this.postListFull = new ArrayList<>(postList);  // 복사본 저장
        this.context = context;
        this.isDarkMode = isDarkMode;
    }

    public void setDarkMode(boolean isDarkMode) {
        this.isDarkMode = isDarkMode;
    }

    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ImageViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.imageView.setImageBitmap(post.getImageBitmap());
        holder.titleView.setText(post.getTitle());
        holder.dateView.setText(post.getCreatedDate().substring(0, 10));  // 날짜만 추출

        // 다크모드 적용
        if (isDarkMode) {
            holder.cardView.setCardBackgroundColor(context.getResources().getColor(R.color.surface_dark, null));
            holder.titleView.setTextColor(context.getResources().getColor(R.color.text_primary_dark, null));
            holder.dateView.setTextColor(context.getResources().getColor(R.color.text_secondary_dark, null));
        } else {
            holder.cardView.setCardBackgroundColor(context.getResources().getColor(R.color.surface_light, null));
            holder.titleView.setTextColor(context.getResources().getColor(R.color.text_primary_light, null));
            holder.dateView.setTextColor(context.getResources().getColor(R.color.text_secondary_light, null));
        }

        // 클릭 이벤트 - 상세 화면으로 이동
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailActivity.class);
            intent.putExtra("post_id", post.getId());
            intent.putExtra("title", post.getTitle());
            intent.putExtra("text", post.getText());
            intent.putExtra("date", post.getCreatedDate());
            intent.putExtra("image_url", post.getImageUrl());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // 검색 필터
    public void filter(String query) {
        postList.clear();
        if (query.isEmpty()) {
            postList.addAll(postListFull);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Post post : postListFull) {
                if (post.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                    postList.add(post);
                }
            }
        }
        notifyDataSetChanged();
    }

    // 데이터 업데이트
    public void updateData(List<Post> newPostList) {
        this.postList = newPostList;
        this.postListFull = new ArrayList<>(newPostList);
        notifyDataSetChanged();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imageView;
        TextView titleView;
        TextView dateView;

        public ImageViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            imageView = itemView.findViewById(R.id.imageView);
            titleView = itemView.findViewById(R.id.titleTextView);
            dateView = itemView.findViewById(R.id.dateTextView);
        }
    }
}