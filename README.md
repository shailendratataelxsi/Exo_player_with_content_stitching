# README #

# **Module Overview**

# Video Player with Content Stitching using ExoPlayer

This project demonstrates a basic video player implementation using **ExoPlayer** in Android. The player supports playback of at least two videos that are "stitched" together, including basic playback controls (play/pause), and a seamless transition between the videos. The application also simulates a mid-roll ad insertion by transitioning to an advertisement video at a specific timestamp and returning to the main content once the ad is complete.

### Dependency: Add Required Video Files

To ensure the project runs as expected, you must add two required video files to the `res/raw` folder in your Android project:

1. `main_content.mp4`
2. `ad_5s.mp4`
3. `ad_10s.mp4`
4. `ad_15s.mp4`
5. `ad_20.mp4`
6. `ad_30.mp4`

## Core Features

1. **Video Player Implementation**
    - Implements a basic video player using **ExoPlayer**.
    - Supports playback of at least two videos that will be stitched together.
    - Basic playback controls (play/pause).
    - Smooth transitions between videos.

2. **Content Stitching**
    - Plays the main content video.
    - At a predetermined timestamp (e.g., 30 seconds), seamlessly transitions to a second video (simulating an ad).
    - After the ad completes, it transitions back to the main content video.
    - Demonstrates **mid-roll insertion**.

3. **Error Handling**
    - Captures and logs playback errors such as network issues or unsupported formats.
    - Implements retry mechanisms for a better user experience.

4. **Technical Requirements**
    - Java is used for the implementation.
    - Basic error handling is included.
    - Smooth transitions between Stream and Ads.

## Technical Details

1. **Technologies Used**
    - **Programming Language:** Java
    - **UI Design:** XML layouts
    - **Media Framework:** ExoPlayer

2. **Dependencies**
To integrate ExoPlayer, add the following dependencies in your `build.gradle` file:
```gradle
implementation "androidx.media3:media3-exoplayer:1.3.1"
implementation "androidx.media3:media3-ui:1.3.1"


