package com.example.lab_control_v2.utils;

import android.content.Context;
import android.media.MediaPlayer;

import com.example.lab_control_v2.R;

public class SoundPlayer {

    public static void playPop(Context context){
        MediaPlayer popSound = MediaPlayer.create(context, R.raw.duckquack);
        popSound.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){

            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.release();
            }
        });
        popSound.start();
    }
}