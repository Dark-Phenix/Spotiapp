package com.example.spotiapp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.spotiapp.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class PlaylistView extends LinearLayout {

    private ImageView _iconView;
    private TextView _titleView;
    private ImageView _refreshView;
    private TextView _descriptionView;

    public PlaylistView(Context context) {
        super(context);
        init();
    }

    public PlaylistView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PlaylistView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    protected void init() {
        inflate(getContext(), R.layout.playlist_view, this);
        _iconView = findViewById(R.id.icon);
        _titleView = findViewById(R.id.title);
        _refreshView = findViewById(R.id.refresh);
        _descriptionView = findViewById(R.id.description);
    }

    //int bei icon, weil es eine ID ist die irgendwo liegt
    public void setPlaylist(int iconID, String title)
    {
        _iconView.setImageResource(iconID);
        _titleView.setText(title);

    }

    public ImageView getRefreshView() {
        return _refreshView;
    }

    public TextView getDescriptionView() {
        return _descriptionView;
    }

    public ImageView getPlaylistImage()
    {
        return _iconView;
    }

}
