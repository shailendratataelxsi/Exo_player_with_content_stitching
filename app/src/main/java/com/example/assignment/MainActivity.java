package com.example.assignment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int READ_STORAGE_PERMISSION_REQUEST_CODE = 100;
    private ExoPlayer player;
    private PlayerView playerView;
    private Handler adHandler;
    private MediaSource mainContentSource;
    private long mainContentResumePosition = 0;
    private boolean isAdPlaying = false;
    private String streaming_url = "https://pulse-demo.vp.videoplaza.tv/resources/media/sintel_trailer_854x480.mp4";
    private final String mainContent = "/sdcard/Download/tad/main_content.mp4";
    private final String[] adResources = {
            "/sdcard/Download/tad/ad_5s.mp4",
            "/sdcard/Download/tad/ad_10s.mp4",
            "/sdcard/Download/tad/ad_15s.mp4",
            "/sdcard/Download/tad/ad_20s.mp4",
            "/sdcard/Download/tad/ad_30s.mp4"
    };
    private int adIndex = 0;
    private int adIntervalMultiplier = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.content_player_view);
        Spinner sourceSpinner = findViewById(R.id.source_spinner);

        // Check for storage permission
        if (!isReadStoragePermissionGranted()) {
            requestReadStoragePermission();
        }

        // Setup spinner for content selection
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Offline", "Online"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceSpinner.setAdapter(adapter);

        sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position == 0) { // Main Content
                    initializePlayer(mainContent);
                } else {
                    initializeOnlinePlayer(streaming_url);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "No source selected");
            }
        });
    }
    @OptIn(markerClass = UnstableApi.class)
    private void initializeOnlinePlayer(String onlineUrl) {
        // Release any existing player instance
        if (player != null) {
            player.release();
        }

        // Create a new player instance
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        mainContentSource = new ProgressiveMediaSource.Factory(new DefaultDataSource.Factory(this)).createMediaSource(MediaItem.fromUri(onlineUrl));
        player.setMediaSource(mainContentSource);
        player.prepare();
        Log.d(TAG, "Main content started");
        player.play();
    }
    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer(String contentPath) {
        releasePlayer();
        if (!isFileAvailable(contentPath)) {
            Toast.makeText(this, "Main content file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaSource mainContentSource = buildMediaSourceFromFilePath(contentPath);
        player.setMediaSource(mainContentSource);
        player.prepare();
        Log.d(TAG, "Content started: Offline content playing.");
        player.play();
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    Log.d(TAG, "Playback resumed or started");
                } else {
                    Log.d(TAG, "Playback paused or stopped");
                }
            }
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED && !isAdPlaying) {
                    Log.d(TAG, "Content ended: Offline content playback completed.");
                }
            }
        });
        scheduleAds(mainContentSource);
    }

    @OptIn(markerClass = UnstableApi.class)
    private MediaSource buildMediaSourceFromFilePath(String filePath) {
        return new ProgressiveMediaSource.Factory(
                new DefaultDataSource.Factory(this)
        ).createMediaSource(MediaItem.fromUri("file://" + filePath));
    }

    private void scheduleAds(MediaSource mainContentSource) {
        adHandler = new Handler(Looper.getMainLooper());
        adHandler.post(new Runnable() {
            @Override
            public void run() {
                if (player.getCurrentPosition() >= adIntervalMultiplier * 30_000) {
                    playAd(mainContentSource);
                    adIntervalMultiplier++;
                }
                adHandler.postDelayed(this, 500);
            }
        });
    }
    @OptIn(markerClass = UnstableApi.class)
    private void startAdProgressTracking() {
        adHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isAdPlaying && player != null) {
                    long adDuration = player.getDuration();
                    long adPosition = player.getCurrentPosition();

                    if (adDuration > 0) {
                        int progressPercentage = (int) ((adPosition * 100) / adDuration);

                        // Log or update UI with the ad progress percentage
                        Log.d(TAG, "Ad progress: " + progressPercentage + "%");

                        // Example: Trigger an action at specific progress percentages
                        if (progressPercentage == 50) {
                            Log.d(TAG, "Ad is halfway done!");
                        } else if (progressPercentage == 90) {
                            Log.d(TAG, "Ad is about to finish!");
                        }
                    }

                    // Continue tracking ad progress
                    adHandler.postDelayed(this, 500);
                }
            }
        });
    }
    @OptIn(markerClass = UnstableApi.class)
    private void playAd(MediaSource mainContentSource) {
        if (adIndex >= adResources.length) {
            Log.d(TAG, "No more ads to play");
            return;
        }

        String adFilePath = adResources[adIndex];
        if (!isFileAvailable(adFilePath)) {
            Log.e(TAG, "Ad file not found: " + adFilePath);
            Toast.makeText(this, "Ad file not found: " + adFilePath, Toast.LENGTH_SHORT).show();
            adIndex++;
            return;
        }

        MediaSource adSource = buildMediaSourceFromFilePath(adFilePath);
        mainContentResumePosition = player.getCurrentPosition();
        isAdPlaying = true;
        Log.d(TAG, "Ad started");
        player.setMediaSource(adSource);
        player.prepare();
        player.play();

        // Start tracking the ad progress
        startAdProgressTracking();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED && isAdPlaying) {
                    Log.d(TAG, "Ad ended");
                    resumeMainContent(mainContentSource);
                }
            }
        });

        adIndex++;
    }

    @OptIn(markerClass = UnstableApi.class)
    private void resumeMainContent(MediaSource mainContentSource) {
        Log.d(TAG, "Resuming main content after ad.");
        isAdPlaying = false;
        player.setMediaSource(mainContentSource);
        player.seekTo(mainContentResumePosition);
        player.prepare();
        player.play();
    }

    private boolean isFileAvailable(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    private boolean isReadStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestReadStoragePermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                READ_STORAGE_PERMISSION_REQUEST_CODE
        );
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
        if (adHandler != null) {
            adHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }
}