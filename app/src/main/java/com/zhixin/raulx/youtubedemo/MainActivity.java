package com.zhixin.raulx.youtubedemo;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements YouTubeVideoView.Callback, MediaPlayer.OnPreparedListener, TextureView.SurfaceTextureListener{

    private TextureView mVideoView;
    private MediaPlayer mMediaPlayer;
    private YouTubeVideoView mYouTubeVideoView;
    private ListView mProgramListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgramListView = (ListView) findViewById(R.id.list_view);
        mProgramListView.setAdapter(ArrayAdapter.createFromResource(this, R.array.program_list, android.R.layout.simple_list_item_1));
        mProgramListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playVideo();
            }
        });
        mVideoView = (TextureView) findViewById(R.id.video_view);
        mVideoView.setSurfaceTextureListener(this);
        mMediaPlayer = MediaPlayer.create(this,R.raw.test_video);
        mMediaPlayer.setOnPreparedListener(this);
        mYouTubeVideoView = (YouTubeVideoView) findViewById(R.id.youtube_view);
        mYouTubeVideoView.setCallback(this);
    }

    private void playVideo(){
        mYouTubeVideoView.show();

        if (mMediaPlayer.isPlaying())
            return;

        mMediaPlayer.start();
    }


    @Override
    public void onVideoClick(){
        Toast.makeText(this, "click to pause/start", Toast.LENGTH_SHORT).show();
        if(mMediaPlayer.isPlaying())
            mMediaPlayer.pause();
        else
            mMediaPlayer.start();
    }

    @Override
    public void onVideoViewHide(){
        Toast.makeText(this, "video view hide", Toast.LENGTH_SHORT).show();
        mMediaPlayer.pause();
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayer.setLooping(true);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mMediaPlayer.setSurface(new Surface(surface));
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        finish();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        mMediaPlayer.release();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mYouTubeVideoView.getNowStateScale() == 1f){
            mYouTubeVideoView.goMin();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
