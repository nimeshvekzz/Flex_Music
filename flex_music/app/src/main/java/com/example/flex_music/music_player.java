package com.example.flex_music;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.example.flex_music.fragments.SongsFragment;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class music_player extends AppCompatActivity implements MediaService.OnServiceStateChangedListener {

    // UI Components
    private ImageView imageViewAlbum;
    private TextView textViewSongTitle, textViewArtistName, textViewCurrentTime, textViewTotalTime;
    private SeekBar seekBar;
    private ImageButton buttonPlayPause, buttonNext, buttonPrev, buttonLoop, buttonShuffle, buttonWishlist;

    // Media Service
    private MediaService mediaService;
    private boolean isBound = false;
    private final Handler handler = new Handler();
    private ArrayList<SongsFragment.Song> songs;
    private int currentIndex = 0;
    private boolean isLooping = false;
    private boolean isShuffling = false;

    private SharedPreferences prefs;

    private final Runnable updateSeekBarTask = new Runnable() {
        @Override
        public void run() {
            if (isBound && mediaService.isPlaying() && mediaService.getMediaPlayer() != null) {
                try {
                    int currentPosition = mediaService.getMediaPlayer().getCurrentPosition();
                    seekBar.setProgress(currentPosition);
                    textViewCurrentTime.setText(formatTime(currentPosition));
                } catch (IllegalStateException e) {
                    // Ignore during preparation
                }
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate
                .setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        initializeViews();
        handleIntent(getIntent());

        if (songs == null || songs.isEmpty()) {
            Toast.makeText(this, "No songs to play!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setButtonListeners();
        setupSeekBar();
        restorePlaybackState();
        startMediaService();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
        if (isBound) {
            mediaService.setSongs(songs);
            mediaService.playSong(currentIndex);
            updateUI();
        }
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            ArrayList<SongsFragment.Song> newSongs = (ArrayList<SongsFragment.Song>) intent
                    .getSerializableExtra("songList");
            int newIndex = intent.getIntExtra("songIndex", -1);

            if (newSongs != null) {
                songs = newSongs;
            }
            if (newIndex != -1) {
                currentIndex = newIndex;
            }
        }
    }

    private void initializeViews() {
        imageViewAlbum = findViewById(R.id.imageViewAlbum);
        textViewSongTitle = findViewById(R.id.textViewSongTitle);
        textViewArtistName = findViewById(R.id.textViewArtistName);
        textViewCurrentTime = findViewById(R.id.textViewCurrentTime);
        textViewTotalTime = findViewById(R.id.textViewTotalTime);
        seekBar = findViewById(R.id.seekBar);
        buttonPlayPause = findViewById(R.id.buttonPlayPause);
        buttonNext = findViewById(R.id.buttonNext);
        buttonPrev = findViewById(R.id.buttonPrev);
        buttonLoop = findViewById(R.id.buttonLoop);
        buttonShuffle = findViewById(R.id.buttonShuffle);
        buttonWishlist = findViewById(R.id.buttonWishlist);
    }

    private void setButtonListeners() {
        buttonPlayPause.setOnClickListener(v -> togglePlayPause());
        buttonNext.setOnClickListener(v -> playNext());
        buttonPrev.setOnClickListener(v -> playPrevious());
        buttonLoop.setOnClickListener(v -> toggleLoop());
        buttonShuffle.setOnClickListener(v -> toggleShuffle());
        buttonWishlist.setOnClickListener(v -> toggleFavorite());
        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
    }

    private void setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isBound && mediaService.getMediaPlayer() != null)
                    mediaService.getMediaPlayer().seekTo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void startMediaService() {
        Intent serviceIntent = new Intent(this, MediaService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaService.MediaBinder binder = (MediaService.MediaBinder) service;
            mediaService = binder.getService();
            mediaService.addListener(music_player.this);
            isBound = true;

            mediaService.setSongs(songs);

            // Only play if it's a DIFFERENT song or if nothing is playing
            if (!mediaService.isPlaying() || mediaService.getCurrentIndex() != currentIndex) {
                mediaService.playSong(currentIndex);
            } else {
                // If same song, just update current duration and progress
                if (mediaService.getMediaPlayer() != null) {
                    try {
                        int duration = mediaService.getMediaPlayer().getDuration();
                        if (duration > 0) {
                            seekBar.setMax(duration);
                            textViewTotalTime.setText(formatTime(duration));
                        }
                    } catch (Exception e) {
                    }
                }
            }

            updateUI();
            handler.removeCallbacks(updateSeekBarTask);
            handler.post(updateSeekBarTask);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (isBound && mediaService != null) {
                mediaService.removeListener(music_player.this);
            }
            isBound = false;
        }
    };

    @Override
    public void onStateChanged() {
        updateUI();
    }

    private void updateUI() {
        if (!isBound || songs == null || currentIndex < 0)
            return;
        SongsFragment.Song song = songs.get(currentIndex);
        textViewSongTitle.setText(song.getTitle());
        textViewArtistName.setText(song.getArtist());

        buttonPlayPause.setImageResource(mediaService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);

        if (mediaService.getMediaPlayer() != null) {
            try {
                int duration = mediaService.getMediaPlayer().getDuration();
                if (duration > 0) {
                    seekBar.setMax(duration);
                    textViewTotalTime.setText(formatTime(duration));
                }
            } catch (IllegalStateException e) {
                // MediaPlayer is likely preparing, duration not available yet
            }
        }

        updateWishlistIcon();
        updateToggleButtonStates();
    }

    private void togglePlayPause() {
        if (isBound) {
            mediaService.playPause();
            updateUI();
        }
    }

    private void playNext() {
        if (isBound) {
            mediaService.playNext();
            currentIndex = mediaService.getCurrentIndex();
            updateUI();
            saveToRecentlyPlayed(songs.get(currentIndex));
        }
    }

    private void playPrevious() {
        if (isBound) {
            mediaService.playPrevious();
            currentIndex = mediaService.getCurrentIndex();
            updateUI();
            saveToRecentlyPlayed(songs.get(currentIndex));
        }
    }

    private void toggleLoop() {
        isLooping = !isLooping;
        buttonLoop.setAlpha(isLooping ? 1.0f : 0.5f);
        prefs.edit().putBoolean("loop_mode", isLooping).apply();
    }

    private void toggleShuffle() {
        isShuffling = !isShuffling;
        buttonShuffle.setAlpha(isShuffling ? 1.0f : 0.5f);
        prefs.edit().putBoolean("shuffle_mode", isShuffling).apply();
    }

    private void toggleFavorite() {
        if (isBound) {
            SongsFragment.Song song = songs.get(currentIndex);
            mediaService.toggleFavorite(song);
            updateWishlistIcon();
            Toast.makeText(this, mediaService.isFavorite(song) ? "Added to Favorites" : "Removed from Favorites",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateWishlistIcon() {
        if (isBound) {
            SongsFragment.Song song = songs.get(currentIndex);
            boolean isFav = mediaService.isFavorite(song);
            buttonWishlist.setImageResource(isFav ? R.drawable.whishlist : R.drawable.b_whishlist);
            buttonWishlist.setColorFilter(isFav ? 0xFFFF0000 : 0xFFFFFFFF); // Red for fav, white for not
        }
    }

    private void updateToggleButtonStates() {
        isShuffling = prefs.getBoolean("shuffle_mode", false);
        isLooping = prefs.getBoolean("loop_mode", false);
        buttonShuffle.setAlpha(isShuffling ? 1.0f : 0.5f);
        buttonLoop.setAlpha(isLooping ? 1.0f : 0.5f);
    }

    private void saveToRecentlyPlayed(SongsFragment.Song song) {
        // Logic remains in Activity as it involves complex serialization that's fine
        // here
        // But we could move it to service too if requested.
        // For now, let's keep it simple.
        String json = prefs.getString("recently_played", "");
        ArrayList<SongsFragment.Song> recentSongs;
        Gson gson = new Gson();
        if (!json.isEmpty()) {
            recentSongs = gson.fromJson(json, new com.google.gson.reflect.TypeToken<ArrayList<SongsFragment.Song>>() {
            }.getType());
        } else {
            recentSongs = new ArrayList<>();
        }

        recentSongs.removeIf(s -> s.getPath().equals(song.getPath()));
        song.setTimestamp(System.currentTimeMillis());
        recentSongs.add(0, song);

        long twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        recentSongs.removeIf(s -> s.getTimestamp() < twentyFourHoursAgo);

        if (recentSongs.size() > 50)
            recentSongs = new ArrayList<>(recentSongs.subList(0, 50));
        prefs.edit().putString("recently_played", gson.toJson(recentSongs)).apply();
    }

    private void savePlaybackState() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("current_song_index", currentIndex);
        editor.putBoolean("shuffle_mode", isShuffling);
        editor.putBoolean("loop_mode", isLooping);
        editor.apply();
    }

    private void restorePlaybackState() {
        currentIndex = prefs.getInt("current_song_index", currentIndex);
        isShuffling = prefs.getBoolean("shuffle_mode", false);
        isLooping = prefs.getBoolean("loop_mode", false);
    }

    @SuppressLint("DefaultLocale")
    private String formatTime(int millis) {
        int minutes = (int) TimeUnit.MILLISECONDS.toMinutes(millis);
        int seconds = (int) (TimeUnit.MILLISECONDS.toSeconds(millis) % 60);
        return String.format("%02d : %02d", minutes, seconds);
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePlaybackState();
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
        handler.removeCallbacks(updateSeekBarTask);
    }
}
