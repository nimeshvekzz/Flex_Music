package com.example.flex_music.fragments;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.flex_music.R;
import com.example.flex_music.Adapter.SongAdapter;
import com.example.flex_music.fragments.SongsFragment.Song;

import java.util.ArrayList;

public class RecentlyAddedFragment extends Fragment {

    private static final int REQUEST_CODE = 102;
    private RecyclerView recyclerView;
    private ArrayList<Song> recentlyAddedSongs = new ArrayList<>();
    private SongAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recently_played, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewRecentlyPlayed);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new SongAdapter(recentlyAddedSongs, getContext());
        recyclerView.setAdapter(adapter);

        checkPermission();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecentlyAdded();
    }

    private void checkPermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { permission }, REQUEST_CODE);
        } else {
            loadRecentlyAdded();
        }
    }

    private void loadRecentlyAdded() {
        recentlyAddedSongs.clear();
        ContentResolver contentResolver = requireActivity().getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.ARTIST
        };

        // Filter for songs added in last 24 hours
        long twentyFourHoursAgo = (System.currentTimeMillis() / 1000) - (24 * 60 * 60);
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
                MediaStore.Audio.Media.DATE_ADDED + " >= ?";
        String[] selectionArgs = { String.valueOf(twentyFourHoursAgo) };
        String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)) {
            if (cursor != null) {
                int titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int pathIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                int artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);

                while (cursor.moveToNext()) {
                    String title = cursor.getString(titleIdx);
                    String path = cursor.getString(pathIdx);
                    String artist = cursor.getString(artistIdx);
                    recentlyAddedSongs.add(new Song(title, path, artist));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (adapter != null) {
            adapter.updateSongList(recentlyAddedSongs);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadRecentlyAdded();
        }
    }
}
