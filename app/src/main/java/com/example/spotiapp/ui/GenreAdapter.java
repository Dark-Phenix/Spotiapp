package com.example.spotiapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.GenreViewHolder> {
    private List<String> genres;
    private Set<String> selectedGenres;

    public GenreAdapter(List<String> genres, Set<String> selectedGenres) {
        this.genres = new ArrayList<>(genres);
        this.selectedGenres = selectedGenres;
    }

    public void filterList(List<String> filtered) {
        genres = new ArrayList<>(filtered);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GenreViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
        return new GenreViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GenreViewHolder holder, int position) {
        String genre = genres.get(position);
        CheckedTextView textView = (CheckedTextView) holder.itemView;
        textView.setText(genre);
        textView.setChecked(selectedGenres.contains(genre));

        textView.setOnClickListener(v -> {
            boolean nowChecked = !textView.isChecked();
            textView.setChecked(nowChecked);
            if (nowChecked) {
                selectedGenres.add(genre);
            } else {
                selectedGenres.remove(genre);
            }
        });
    }

    @Override
    public int getItemCount() {
        return genres.size();
    }

    static class GenreViewHolder extends RecyclerView.ViewHolder {
        public GenreViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
