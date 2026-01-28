package com.example.flex_music.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
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

import java.lang.reflect.Type;
import java.util.ArrayList;

public class RecentlyPlayedFragment extends Fragment {

    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private ArrayList<SongsFragment.Song> recentlyPlayedList = new ArrayList<>();
    private SharedPreferences prefs;
    private Gson gson = new Gson();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recently_played, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewRecentlyPlayed);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        loadRecentlyPlayedSongs();
        adapter = new SongAdapter(recentlyPlayedList, getContext());
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecentlyPlayedSongs();
        if (adapter != null) {
            adapter.updateSongList(recentlyPlayedList);
        }
    }

    private void loadRecentlyPlayedSongs() {
        String json = prefs.getString("recently_played", "");
        recentlyPlayedList.clear();

        if (!json.isEmpty()) {
            Type type = new TypeToken<ArrayList<SongsFragment.Song>>() {
            }.getType();
            ArrayList<SongsFragment.Song> allSongs = gson.fromJson(json, type);

            long now = System.currentTimeMillis();
            long twentyFourHoursAgo = now - (24 * 60 * 60 * 1000);

            for (SongsFragment.Song song : allSongs) {
                if (song.getLastPlayedTime() >= twentyFourHoursAgo) {
                    recentlyPlayedList.add(song);
                }
            }
        }
    }
}
