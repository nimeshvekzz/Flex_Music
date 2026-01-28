package com.example.flex_music.fragments;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import com.example.flex_music.R;
import com.example.flex_music.Adapter.ArtistAdapter;
import com.example.flex_music.Adapter.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;

public class ArtistsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArrayList<String> artistList;
    private ArtistAdapter artistAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artists, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewArtists);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        artistList = new ArrayList<>();

        loadArtists();

        artistAdapter = new ArtistAdapter(requireContext(), artistList);
        recyclerView.setAdapter(artistAdapter);

        return view;
    }

    private void loadArtists() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.Audio.Media.ARTIST };

        Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null,
                MediaStore.Audio.Media.ARTIST + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String artist = cursor.getString(0);
                if (artist != null && !artistList.contains(artist)) {
                    artistList.add(artist);
                }
            }
            cursor.close();
        }
    }
}
