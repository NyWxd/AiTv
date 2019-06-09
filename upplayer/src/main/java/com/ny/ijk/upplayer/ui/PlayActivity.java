package com.ny.ijk.upplayer.ui;

import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.ny.ijk.upplayer.R;
import com.ny.ijk.upplayer.media.IRenderView;
import com.ny.ijk.upplayer.media.IjkVideoView;
import com.ny.ijk.upplayer.media.PlayerManager;

import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IMediaPlayer;

public class PlayActivity extends AppCompatActivity {

    private IjkVideoView mVideoView;
    private PlayerManager mPlayerManager;
    private int mPosition;
    List<String> urls = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_content);

        //这是普通的仅播放
//        initView();
        //这是带控制的播放
        urls = getIntent().getStringArrayListExtra("urls");
        mPosition = getIntent().getIntExtra("position",0);
        initVideo();
    }

    //把使用播放器的Activity的onTouch事件传递给PlayerManager
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mPlayerManager.gestureDetector.onTouchEvent(event)){
            return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 初始化PlayerManager
     * 可左半屏滑动控制亮度，右半屏控制音量，双击切换比例 （无提示）
     * */
    private void initVideo() {
        if (urls != null && urls.size() > 0){
            mPlayerManager = new PlayerManager(this);
//            mPlayerManager.setFullScreenOnly(true);
            mPlayerManager.live(false);
            mPlayerManager.setScaleType(PlayerManager.SCALETYPE_FITPARENT);
//            mPlayerManager.playerInFullScreen(true);
            mPlayerManager.play(urls,mPosition);
        }


    }

    private void initView() {
        mVideoView = findViewById(R.id.up_player_view);
        mVideoView.setAspectRatio(IRenderView.AR_ASPECT_FIT_PARENT);
//        mVideoView.setVideoURI(Uri.parse(url_2));
        mVideoView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayerManager.pause();
    }

    @Override
    protected void onResume() {
        mPlayerManager.onResume();
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        if (hasFocus && Build.VERSION.SDK_INT >= 19) {
//            View decorView = getWindow().getDecorView();
//            decorView.setSystemUiVisibility(
//                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                            | View.SYSTEM_UI_FLAG_FULLSCREEN
//                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
//        }
//    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayerManager.onDestroy();
    }
}
