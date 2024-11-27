package com.example.assignment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultDataSourceFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ExoPlayer player;
    private PlayerView playerView;
    private Handler progressHandler;
    private Handler adProgressHandler;
    private long mainContentResumePosition = 0;
    private MediaSource mainContentSource;
    private boolean isAdPlaying = false;
    private int adIntervalMultiplier = 1;
    private final int[] adResources = {R.raw.ad_5s, R.raw.ad_10s, R.raw.ad_15s, R.raw.ad_20s, R.raw.ad_30s};
    private int adIndex = 0;
    private Spinner sourceSpinner;
    private String streaming_url = "https://pulse-demo.vp.videoplaza.tv/resources/media/sintel_trailer_854x480.mp4";

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.content_player_view);
        sourceSpinner = findViewById(R.id.source_spinner);

        // Initialize ExoPlayer
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Offline", "Online"}
        );

        adapter.setDropDownViewResource(R.layout.custom_spinner_item);
        sourceSpinner.setAdapter(adapter);

        sourceSpinner.setSelection(0);

        sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSource = parent.getItemAtPosition(position).toString();

                if (selectedSource.equals("Online")) {
                    // Use the online URL and configure your player
                    initializePlayer(streaming_url);
                } else {
                    // Use offline content or handle other options
                    initializePlayer();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case where nothing is selected
                Log.d(TAG, "Nothing is selected ");
            }
        });
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer(String onlineUrl) {
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

        // Restart ad scheduling for the main content
        startAdScheduler(mainContentSource);

        // Stop existing ad scheduling (if any)
        if (progressHandler != null) {
            progressHandler.removeCallbacksAndMessages(null);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayer() {
        // Release any existing player instance
        if (player != null) {
            player.release();
        }

        // Create a new player instance
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Load and prepare the offline content (main_content.mp4)
        MediaSource mainContentSource = buildMediaSource(R.raw.main_content);
        player.setMediaSource(mainContentSource);
        player.prepare();
        Log.d(TAG, "Main content started");
        player.play();

        player.addListener(new Player.Listener() {
        });

        // Restart ad scheduling for the main content
        startAdScheduler(mainContentSource);
    }

    @OptIn(markerClass = UnstableApi.class)
    private MediaSource buildMediaSource(int rawResId) {
        String uri = "android.resource://" + getPackageName() + "/" + rawResId;
        return new ProgressiveMediaSource.Factory(
                new DefaultDataSourceFactory(this, Util.getUserAgent(this, "video-player"))
        ).createMediaSource(MediaItem.fromUri(uri));
    }

    private void startAdScheduler(MediaSource mainContentSource) {
        progressHandler = new Handler(Looper.getMainLooper());
        progressHandler.post(new Runnable() {
            @Override
            public void run() {
                if (player.getCurrentPosition() >= adIntervalMultiplier * 30_000) {
                    isAdPlaying = true;
                    mainContentResumePosition = player.getCurrentPosition(); // Save the resume position
                    // Load the ad and play
                    if (adIndex <= adResources.length - 1) {
                        MediaSource adSource = buildMediaSource(adResources[adIndex]);
                        playAd(mainContentSource, adSource);
                    }
                    startAdProgressTracking(getNextAdDuration());
                    adIndex++;
                    adIntervalMultiplier++;
                }
                progressHandler.postDelayed(this, 500);
            }
        });
    }

    @OptIn(markerClass = UnstableApi.class)
    private void playAd(MediaSource mainContentSource, MediaSource adSource) {
        player.setMediaSource(adSource);
        player.prepare();
        player.play();
        Log.d(TAG, "Ad started");
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED && isAdPlaying) {
                    // Resume playback of the main content
                    Log.d(TAG, "Ad completed");
                    playerView.hideController();
                    player.setMediaSource(mainContentSource);
                    player.seekTo(mainContentResumePosition);
                    Log.d(TAG, "Main content resumed");
                    player.prepare();
                    player.play();
                    isAdPlaying = false;
                }
                if (playbackState == Player.STATE_ENDED && !isAdPlaying) {
                    Log.d(TAG, "Content Ended");
                } else if (playbackState == Player.STATE_ENDED) {
                    stopAdProgressTracking();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Player.Listener.super.onPlayerError(error);
                int errorCode = error.errorCode;
                String errorMessage = error.getMessage();
                String detailedErrorMessage = "Error Code: " + errorCode + ", Message: " + errorMessage;
                Log.e(TAG, "Received onPlayerError: " + detailedErrorMessage, error);

                switch (errorCode) {
                    case PlaybackException.ERROR_CODE_IO_UNSPECIFIED:
                        Log.d(TAG, "onPlayerError: ERROR_CODE_IO_UNSPECIFIED");
                        break;

                    case PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND:
                        Log.d(TAG, "onPlayerError: ERROR_CODE_IO_FILE_NOT_FOUND");
                        retryMainContent(mainContentSource);
                        break;

                    case PlaybackException.ERROR_CODE_DECODING_FAILED:
                        Log.d(TAG, "onPlayerError: ERROR_CODE_DECODING_FAILED");
                        break;

                    default:
                        Log.e(TAG, "Unhandled ExoPlayer error: " + detailedErrorMessage, error);
                        break;
                }
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startAdProgressTracking(long nextAdDuration) {
        adProgressHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "startAdProgressTracking");
        adProgressHandler.postDelayed(stopProgressRunnable, nextAdDuration);
        adProgressHandler.post(new Runnable() {
            @Override
            public void run() {
                long currentPosition = player.getCurrentPosition();
                int progressPercentage = (int) ((currentPosition * 100) / nextAdDuration);
                Log.d(TAG, "Ad progress: " + progressPercentage + "%");
                adProgressHandler.postDelayed(this, 100); // Update every 100ms
            }
        });
    }

    private Runnable stopProgressRunnable = new Runnable() {
        @Override
        public void run() {
            stopAdProgressTracking();
        }
    };

    private void stopAdProgressTracking() {
        adProgressHandler.removeCallbacksAndMessages(null);
    }

    public long getNextAdDuration() {
        long duration;
        switch (adIndex) {
            case 0:
                duration = 5_000;
                break;
            case 1:
                duration = 10_000;
                break;
            case 2:
                duration = 15_000;
                break;
            case 3:
                duration = 20_000;
                break;
            case 4:
                duration = 30_000;
                break;
            default:
                duration = 5_000;
        }
        return duration;
    }

    @OptIn(markerClass = UnstableApi.class)
    private void retryMainContent(MediaSource mainContentSource) {
        if (player != null) {
            player.setMediaSource(mainContentSource);
            player.prepare();
            player.seekTo(mainContentResumePosition);
            player.play();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
        if (progressHandler != null) {
            progressHandler.removeCallbacksAndMessages(null);
        }
    }
}