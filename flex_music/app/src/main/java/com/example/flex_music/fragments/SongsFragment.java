package com.example.flex_music.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flex_music.Adapter.SongAdapter;
import com.example.flex_music.R;
import com.example.flex_music.music_player;
import com.example.flex_music.utils.Searchable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.Serializable;
import java.util.ArrayList;

public class SongsFragment extends Fragment implements Searchable {

    private static final int REQUEST_CODE = 101;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayList<Song> allSongs = new ArrayList<>();
    private SongAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_songs, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewSongs);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshSongs);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SongAdapter(allSongs, getContext());
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(this::loadSongs);

        checkPermission();
        return view;
    }

    private void checkPermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { permission }, REQUEST_CODE);
        } else {
            loadSongs();
        }
    }

    private void loadSongs() {
        allSongs.clear();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATE_ADDED
        };

        try (Cursor cursor = requireActivity().getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String title = cursor.getString(0);
                    String path = cursor.getString(1);
                    String artist = cursor.getString(2);
                    long dateAdded = cursor.getLong(3);

                    Song song = new Song(title, path, artist);
                    song.setTimestamp(dateAdded * 1000); // Store as ms
                    allSongs.add(song);
                }
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error loading songs", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setRefreshing(false);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongs();
            } else {
                Toast.makeText(getContext(), "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onSearchQuery(String query) {
        ArrayList<Song> filteredList = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(allSongs);
        } else {
            for (Song song : allSongs) {
                if (song.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(song);
                }
            }
        }
        adapter.updateSongList(filteredList);
    }

    // ✅ Song data model
    public static class Song implements Serializable {
        private String title;
        private String path;
        private String artist;
        private long timestamp;
        private long lastPlayedTime;

        public Song(String title, String path, String artist) {
            this.title = title;
            this.path = path;
            this.artist = artist;
            this.timestamp = 0;
            this.lastPlayedTime = 0;
        }

        public String getTitle() {
            return title;
        }

        public String getPath() {
            return path;
        }

        public String getArtist() {
            return artist != null ? artist : "Unknown Artist";
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        public long getLastPlayedTime() {
            return lastPlayedTime;
        }

        public void setLastPlayedTime(long lastPlayedTime) {
            this.lastPlayedTime = lastPlayedTime;
        }

        public String getFileName() {
            if (path != null) {
                return path.substring(path.lastIndexOf("/") + 1);
            }
            return title;
        }
    }
}
