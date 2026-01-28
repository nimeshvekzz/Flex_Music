package com.example.flex_music.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flex_music.Adapter.SongAdapter;
import com.example.flex_music.R;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private ArrayList<SongsFragment.Song> favoritesList;
    private SharedPreferences prefs;
    private Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewFavorites);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        loadFavorites();
        adapter = new SongAdapter(favoritesList, getContext());
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
        if (adapter != null) {
            adapter.updateSongList(favoritesList);
        }
    }

    private void loadFavorites() {
        String json = prefs.getString("favorites", "");
        if (!json.isEmpty()) {
            favoritesList = gson.fromJson(json, new TypeToken<ArrayList<SongsFragment.Song>>() {
            }.getType());
        } else {
            favoritesList = new ArrayList<>();
        }
    }
}
