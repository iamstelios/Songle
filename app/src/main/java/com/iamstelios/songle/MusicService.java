package com.iamstelios.songle;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

/**
 * MusicService is used to handle the background music of the game.
 * <p>It extends Service so that the music keeps playing between activities.</p>
 */
public class MusicService extends Service {
    private final IBinder mBinder = new ServiceBinder();
    MediaPlayer mPlayer;
    private int length = 0;

    public MusicService() { }

    public class ServiceBinder extends Binder {
        MusicService getService()
        {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent arg0){return mBinder;}

    @Override
    public void onCreate (){
        super.onCreate();
        //Initialize the player, with the background music found  in the raw resources
        mPlayer = MediaPlayer.create(this, R.raw.background_music);

        if(mPlayer!= null)
        {
            //Set the background music to loop indefinitely
            mPlayer.setLooping(true);
            mPlayer.setVolume(100,100);
        }

        mPlayer.setOnErrorListener(new OnErrorListener() {

            public boolean onError(MediaPlayer mp, int what, int
                    extra){

                onErrorHandler(mPlayer, what, extra);
                return true;
            }
        });
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId)
    {
        if(mPlayer==null){
            onCreate();
        }
        mPlayer.start();
        return START_STICKY;
    }

    /**
     * Pauses the background music
     */
    public void pauseMusic()
    {
        if(mPlayer==null){
            return;
        }
        if(mPlayer.isPlaying())
        {
            mPlayer.pause();
            length=mPlayer.getCurrentPosition();

        }
    }
    /**
     * Resumes the background music
     */
    public void resumeMusic()
    {
        if(mPlayer==null){
            onCreate();
        }
        if(mPlayer.isPlaying()==false)
        {
            mPlayer.seekTo(length);
            mPlayer.start();
        }
    }

    @Override
    public void onDestroy ()
    {
        super.onDestroy();
        if(mPlayer != null)
        {
            try{
                mPlayer.stop();
                mPlayer.release();
            }finally {
                mPlayer = null;
            }
        }
    }

    /**
     * Handles errors occuring with the MediaPlayer by stopping and releasing it.
     * <p><b>Note:</b> mPlayer is assigned to <b>null</b></p>
     */
    public boolean onErrorHandler(MediaPlayer mp, int what, int extra) {

        Toast.makeText(this, "music player failed", Toast.LENGTH_SHORT).show();
        if(mPlayer != null)
        {
            try{
                mPlayer.stop();
                mPlayer.release();
            }finally {
                mPlayer = null;
            }
        }
        return false;
    }
}
