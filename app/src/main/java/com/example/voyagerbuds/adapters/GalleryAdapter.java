package com.example.voyagerbuds.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voyagerbuds.R;
import com.example.voyagerbuds.models.GalleryItem;

import java.io.File;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_ITEM = 1;

    private Context context;
    private List<Object> items;
    private boolean isSelectionMode = false;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(GalleryItem item, int position);

        void onItemLongClick(GalleryItem item, int position);
    }

    public GalleryAdapter(Context context, List<Object> items, OnItemClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    public void updateItems(List<Object> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean selectionMode) {
        isSelectionMode = selectionMode;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) {
            return TYPE_HEADER;
        } else {
            return TYPE_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_gallery_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_gallery_image, parent, false);
            return new ImageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((String) items.get(position));
        } else {
            ((ImageViewHolder) holder).bind((GalleryItem) items.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public GridLayoutManager.SpanSizeLookup getSpanSizeLookup(final int spanCount) {
        return new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return getItemViewType(position) == TYPE_HEADER ? spanCount : 1;
            }
        };
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_header_date);
        }

        void bind(String date) {
            tvDate.setText(date);
        }
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imgGalleryItem;
        View viewSelectionOverlay;
        ImageView imgCheck;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imgGalleryItem = itemView.findViewById(R.id.img_gallery_item);
            viewSelectionOverlay = itemView.findViewById(R.id.view_selection_overlay);
            imgCheck = itemView.findViewById(R.id.img_check);
        }

        void bind(GalleryItem item) {
            String path = item.getImagePath();
            imgGalleryItem.setTag(path);

            // Use Glide for efficient loading with proper content URI support
            Uri uri;
            if (path.startsWith("content://") || path.startsWith("file://")) {
                uri = Uri.parse(path);
            } else {
                uri = Uri.fromFile(new File(path));
            }

            com.bumptech.glide.Glide.with(context)
                    .load(uri)
                    .thumbnail(0.1f)
                    .centerCrop()
                    .placeholder(R.drawable.ic_photo)
                    .error(R.drawable.ic_photo)
                    .skipMemoryCache(false)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .into(imgGalleryItem);

            if (isSelectionMode) {
                if (item.isSelected()) {
                    viewSelectionOverlay.setVisibility(View.VISIBLE);
                    imgCheck.setVisibility(View.VISIBLE);
                } else {
                    viewSelectionOverlay.setVisibility(View.GONE);
                    imgCheck.setVisibility(View.GONE);
                }
            } else {
                viewSelectionOverlay.setVisibility(View.GONE);
                imgCheck.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onItemClick(item, getAdapterPosition());
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null)
                    listener.onItemLongClick(item, getAdapterPosition());
                return true;
            });
        }
    }
}
