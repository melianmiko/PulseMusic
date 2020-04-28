package com.hardcodecoder.pulsemusic.adapters;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.hardcodecoder.pulsemusic.GlideApp;
import com.hardcodecoder.pulsemusic.GlideConstantArtifacts;
import com.hardcodecoder.pulsemusic.R;
import com.hardcodecoder.pulsemusic.helper.MediaArtHelper;
import com.hardcodecoder.pulsemusic.interfaces.ItemTouchHelperAdapter;
import com.hardcodecoder.pulsemusic.interfaces.ItemTouchHelperViewHolder;
import com.hardcodecoder.pulsemusic.interfaces.PlaylistItemListener;
import com.hardcodecoder.pulsemusic.interfaces.SimpleGestureCallback;
import com.hardcodecoder.pulsemusic.model.MusicModel;

import java.util.Collections;
import java.util.List;

public class PlaylistDataAdapter extends RecyclerView.Adapter<PlaylistDataAdapter.PlaylistDataSVH> implements ItemTouchHelperAdapter {


    private LayoutInflater mInflater;
    private List<MusicModel> mPlaylistTracks;
    private SimpleGestureCallback mCallback;
    private PlaylistItemListener mListener;
    private int lastPosition = -1;
    private MusicModel deletedItem;
    private int deletedIndex;


    public PlaylistDataAdapter(List<MusicModel> playlistTracks, LayoutInflater inflater, PlaylistItemListener mListener, @Nullable SimpleGestureCallback callback) {
        this.mPlaylistTracks = playlistTracks;
        this.mInflater = inflater;
        this.mListener = mListener;
        this.mCallback = callback;
    }

    public void addItems(final List<MusicModel> list) {
        int startIndex = mPlaylistTracks.size();
        mPlaylistTracks.addAll(list);
        notifyItemRangeInserted(startIndex, list.size());
    }


    public void restoreItem() {
        mPlaylistTracks.add(deletedIndex, deletedItem);
        notifyItemInserted(deletedIndex);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        Collections.swap(mPlaylistTracks, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        if (null != mCallback)
            mCallback.onItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemDismiss(int position) {
        deletedItem = mPlaylistTracks.remove(position);
        deletedIndex = position;
        notifyItemRemoved(position);
        if (null != mCallback)
            mCallback.onItemDismissed(position);
    }

    @NonNull
    @Override
    public PlaylistDataSVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PlaylistDataSVH(mInflater.inflate(R.layout.rv_playlist_item, parent, false), mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistDataSVH holder, int position) {
        holder.updateView(mPlaylistTracks.get(position));
        holder.itemView.startAnimation(AnimationUtils.loadAnimation(holder.itemView.getContext(),
                (position > lastPosition) ? R.anim.up_from_bottom : R.anim.down_from_top));
        lastPosition = position;
    }

    @Override
    public int getItemCount() {
        return mPlaylistTracks.size();
    }

    static class PlaylistDataSVH extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {

        private TextView title;
        private ImageView art;

        PlaylistDataSVH(@NonNull View itemView, PlaylistItemListener listener) {
            super(itemView);
            title = itemView.findViewById(R.id.library_item_tv1);
            art = itemView.findViewById(R.id.library_item_iv1);
            itemView.setOnClickListener(v -> listener.onItemClick(getAdapterPosition()));
            //noinspection AndroidLintClickableViewAccessibility
            itemView.findViewById(R.id.btn_handle)
                    .setOnTouchListener((v, event) -> {
                        if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_UP)
                            listener.onStartDrag(this);
                        return true;
                    });
        }

        void updateView(MusicModel md) {
            title.setText(md.getSongName());
            GlideApp
                    .with(itemView.getContext())
                    .load(md.getAlbumArtUrl())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            MediaArtHelper.getMediaArtDrawableAsync(itemView.getContext(), md.getAlbumId(),
                                    MediaArtHelper.RoundingRadius.RADIUS_4dp,
                                    drawable -> art.setImageDrawable(drawable));
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .transform(GlideConstantArtifacts.getRadius8dp())
                    .into(art);
        }

        @Override
        public void onItemSelected() {
            itemView.setBackground(itemView.getContext().getDrawable(R.drawable.active_item_background));
        }

        @Override
        public void onItemClear() {
            itemView.setBackground(itemView.getContext().getDrawable(android.R.color.transparent));
        }
    }
}
