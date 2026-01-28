package com.example.flex_music;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.os.Handler;
import android.content.ComponentName;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.example.flex_music.Adapter.ViewPagerAdapter;
import com.example.flex_music.fragments.ArtistsFragment;
import com.example.flex_music.fragments.FavoritesFragment;
import com.example.flex_music.fragments.RecentlyPlayedFragment;
import com.example.flex_music.fragments.RecentlyAddedFragment;
import com.example.flex_music.fragments.SongsFragment;
import com.example.flex_music.utils.Searchable;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity implements MediaService.OnServiceStateChangedListener {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private SearchView searchView;
    private ImageView settingsIcon;

    // Mini Player components
    private androidx.cardview.widget.CardView miniPlayerCard;
    private ImageView miniPlayerAlbumArt, miniPlayerPlayPause, miniPlayerWishlist;
    private TextView miniPlayerSongTitle, miniPlayerArtistName;

    private MediaService mediaService;
    private boolean isBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchView = findViewById(R.id.searchView);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        settingsIcon = findViewById(R.id.settingsIcon);

        // Initialize Mini Player
        miniPlayerCard = findViewById(R.id.miniPlayerCard);
        miniPlayerAlbumArt = findViewById(R.id.miniPlayerAlbumArt);
        miniPlayerPlayPause = findViewById(R.id.miniPlayerPlayPause);
        miniPlayerWishlist = findViewById(R.id.miniPlayerWishlist);
        miniPlayerSongTitle = findViewById(R.id.miniPlayerSongTitle);
        miniPlayerArtistName = findViewById(R.id.miniPlayerArtistName);

        setupViewPager(viewPager);
        tabLayout.setupWithViewPager(viewPager);

        startMediaService();

        settingsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AppAppearanceActivity.class);
            startActivity(intent);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchInCurrentTab(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchInCurrentTab(newText);
                return true;
            }

            private void searchInCurrentTab(String query) {
                int position = viewPager.getCurrentItem();
                Fragment fragment = getSupportFragmentManager()
                        .findFragmentByTag("android:switcher:" + R.id.viewPager + ":" + position);

                if (fragment instanceof Searchable) {
                    ((Searchable) fragment).onSearchQuery(query);
                }
            }
        });
    }

    private void startMediaService() {
        Intent serviceIntent = new Intent(this, MediaService.class);
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private final android.content.ServiceConnection serviceConnection = new android.content.ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaService.MediaBinder binder = (MediaService.MediaBinder) service;
            mediaService = binder.getService();
            mediaService.addListener(MainActivity.this);
            isBound = true;
            updateMiniPlayerUI();
            setupMiniPlayerListeners();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    public void onStateChanged() {
        updateMiniPlayerUI();
    }

    private void updateMiniPlayerUI() {
        if (!isBound || mediaService == null || mediaService.getMediaPlayer() == null) {
            miniPlayerCard.setVisibility(android.view.View.GONE);
            return;
        }

        SongsFragment.Song currentSong = mediaService.getCurrentSong();
        if (currentSong == null) {
            miniPlayerCard.setVisibility(android.view.View.GONE);
            return;
        }

        miniPlayerCard.setVisibility(android.view.View.VISIBLE);
        miniPlayerSongTitle.setText(currentSong.getTitle());
        miniPlayerArtistName.setText(currentSong.getArtist());
        miniPlayerPlayPause.setImageResource(mediaService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        boolean isFav = mediaService.isFavorite(currentSong);
        miniPlayerWishlist.setImageResource(isFav ? R.drawable.whishlist : R.drawable.b_whishlist);
        miniPlayerWishlist.setColorFilter(isFav ? 0xFFFF0000 : 0xFFFFFFFF);
    }

    private void setupMiniPlayerListeners() {
        miniPlayerPlayPause.setOnClickListener(v -> {
            if (isBound)
                mediaService.playPause();
        });

        miniPlayerWishlist.setOnClickListener(v -> {
            if (isBound) {
                mediaService.toggleFavorite(mediaService.getCurrentSong());
                updateMiniPlayerUI();
            }
        });

        miniPlayerCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, music_player.class);
            // We need to pass the current state to activity if it's not already playing
            startActivity(intent);
        });
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new SongsFragment(), "Songs");
        adapter.addFragment(new ArtistsFragment(), "Artists");
        adapter.addFragment(new FavoritesFragment(), "Favorites");
        adapter.addFragment(new RecentlyPlayedFragment(), "Recent");
        adapter.addFragment(new RecentlyAddedFragment(), "Added");
        viewPager.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            if (mediaService != null) {
                mediaService.removeListener(this);
            }
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
