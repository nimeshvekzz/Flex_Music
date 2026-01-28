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

/**
 * Fragment to display songs that were played in the last 24 hours.
 * Data is persisted via SharedPreferences.
 */
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
        // Inflate the common recently_played layout
        View view = inflater.inflate(R.layout.fragment_recently_played, container, false);

        // UI Initialization
        recyclerView = view.findViewById(R.id.recyclerViewRecentlyPlayed);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Preference setup to retrieve history
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

        // Initial data load
        loadRecentlyPlayedSongs();
        adapter = new SongAdapter(recentlyPlayedList, requireContext());
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh when returning to screen to show newly played songs
        loadRecentlyPlayedSongs();
        if (adapter != null) {
            adapter.updateSongList(recentlyPlayedList);
        }
    }

    /**
     * Loads the recently played song list from SharedPreferences and filters by
     * time.
     */
    private void loadRecentlyPlayedSongs() {
        String json = prefs.getString("recently_played", "");
        recentlyPlayedList.clear();

        if (!json.isEmpty()) {
            // Deserialize JSON list of Songs
            Type type = new TypeToken<ArrayList<SongsFragment.Song>>() {
            }.getType();
            ArrayList<SongsFragment.Song> allSongs = gson.fromJson(json, type);

            long now = System.currentTimeMillis();
            long twentyFourHoursAgo = now - (24 * 60 * 60 * 1000);

            // Only add songs played within the last 24 hours
            if (allSongs != null) {
                for (SongsFragment.Song song : allSongs) {
                    if (song.getLastPlayedTime() >= twentyFourHoursAgo) {
                        recentlyPlayedList.add(song);
                    }
                }
            }
        }
    }
}
