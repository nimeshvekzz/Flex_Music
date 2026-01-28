package com.example.flex_music;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import android.support.v4.media.session.MediaSessionCompat;

import com.example.flex_music.fragments.SongsFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private static final String CHANNEL_ID = "flex_music_channel";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer mediaPlayer;
    private ArrayList<SongsFragment.Song> songs;
    private int currentIndex = -1;
    private final IBinder binder = new MediaBinder();
    private boolean isPaused = false;
    private ArrayList<SongsFragment.Song> favoriteSongs;
    private final Gson gson = new Gson();
    private MediaSessionCompat mediaSession;
    private final List<OnServiceStateChangedListener> listeners = new ArrayList<>();

    public interface OnServiceStateChangedListener {
        void onStateChanged();
    }

    public void addListener(OnServiceStateChangedListener listener) {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(OnServiceStateChangedListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (OnServiceStateChangedListener listener : listeners) {
            listener.onStateChanged();
        }
    }

    public class MediaBinder extends Binder {
        public MediaService getService() {
            return MediaService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initMediaPlayer();
        loadFavorites();
        mediaSession = new MediaSessionCompat(this, "FlexMusicSession");
    }

    private void loadFavorites() {
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(this);
        String favJson = prefs.getString("favorites", "");
        favoriteSongs = favJson.isEmpty() ? new ArrayList<>()
                : gson.fromJson(favJson, new TypeToken<ArrayList<SongsFragment.Song>>() {
                }.getType());
    }

    private void saveFavorites() {
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(this);
        prefs.edit().putString("favorites", gson.toJson(favoriteSongs)).apply();
    }

    public boolean isFavorite(SongsFragment.Song song) {
        if (song == null)
            return false;
        for (SongsFragment.Song s : favoriteSongs) {
            if (s.getPath().equals(song.getPath()))
                return true;
        }
        return false;
    }

    public void toggleFavorite(SongsFragment.Song song) {
        if (song == null)
            return;
        if (isFavorite(song)) {
            favoriteSongs.removeIf(s -> s.getPath().equals(song.getPath()));
        } else {
            favoriteSongs.add(song);
        }
        saveFavorites();
    }

    private void initMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case "PLAY":
                    playPause();
                    break;
                case "NEXT":
                    playNext();
                    break;
                case "PREV":
                    playPrevious();
                    break;
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setSongs(ArrayList<SongsFragment.Song> songList) {
        this.songs = songList;
    }

    public void playSong(int index) {
        if (songs == null || index < 0 || index >= songs.size())
            return;
        currentIndex = index;
        SongsFragment.Song song = songs.get(currentIndex);

        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.prepareAsync();
            updateRecentlyPlayed(song);
            notifyListeners();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPaused = true;
            updateNotification("Paused");
            stopForeground(false); // Allow notification to be dismissed
            notifyListeners();
        } else {
            mediaPlayer.start();
            isPaused = false;
            updateNotification("Playing");
            // Re-start foreground if needed
            startForeground(NOTIFICATION_ID, createNotification("Playing"));
            notifyListeners();
        }
    }

    public void playNext() {
        if (songs != null && currentIndex < songs.size() - 1) {
            playSong(currentIndex + 1);
        } else if (songs != null) {
            playSong(0);
        }
    }

    public void playPrevious() {
        if (songs != null && currentIndex > 0) {
            playSong(currentIndex - 1);
        } else if (songs != null) {
            playSong(songs.size() - 1);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        isPaused = false;
        Notification notification = createNotification("Playing");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        playNext();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Flex Music Playback", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null)
                manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String status) {
        SongsFragment.Song song = (songs != null && currentIndex >= 0) ? songs.get(currentIndex) : null;
        String title = (song != null) ? song.getTitle() : "Flex Music";
        String artist = (song != null) ? song.getArtist() : "Unknown Artist";

        Intent intent = new Intent(this, music_player.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Action Intents
        Intent prevIntent = new Intent(this, NotificationReceiver.class).setAction("PREV");
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent playIntent = new Intent(this, NotificationReceiver.class).setAction("PLAY");
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, NotificationReceiver.class).setAction("NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        int playPauseIcon = isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(title)
                .setContentText(artist)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo))
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_previous, "Previous", prevPendingIntent)
                .addAction(playPauseIcon, "Play/Pause", playPendingIntent)
                .addAction(R.drawable.ic_next, "Next", nextPendingIntent)
                .setStyle(new MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession.getSessionToken()))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(isPlaying())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }

    private void updateNotification(String status) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(status));
        }
    }

    public SongsFragment.Song getCurrentSong() {
        if (songs != null && currentIndex >= 0 && currentIndex < songs.size()) {
            return songs.get(currentIndex);
        }
        return null;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
        super.onDestroy();
    }

    private void updateRecentlyPlayed(SongsFragment.Song song) {
        if (song == null)
            return;

        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager
                .getDefaultSharedPreferences(this);
        String json = prefs.getString("recently_played", "");
        ArrayList<SongsFragment.Song> recentSongs;

        if (!json.isEmpty()) {
            recentSongs = gson.fromJson(json, new TypeToken<ArrayList<SongsFragment.Song>>() {
            }.getType());
        } else {
            recentSongs = new ArrayList<>();
        }

        // Update timestamps
        long now = System.currentTimeMillis();
        song.setLastPlayedTime(now);

        // Remove if already exists (deduplicate)
        recentSongs.removeIf(s -> s.getPath().equals(song.getPath()));

        // Add to top
        recentSongs.add(0, song);

        // Limit to 50 items for performance
        if (recentSongs.size() > 50) {
            recentSongs = new ArrayList<>(recentSongs.subList(0, 50));
        }

        prefs.edit().putString("recently_played", gson.toJson(recentSongs)).apply();
    }
}