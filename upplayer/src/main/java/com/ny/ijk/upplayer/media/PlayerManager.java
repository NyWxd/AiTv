package com.ny.ijk.upplayer.media;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ny.ijk.upplayer.R;
import com.ny.ijk.upplayer.utils.UpEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.pragma.DebugLog;

/**
 * https://github.com/xongEr/ijkPlayer-Demo
 * @author yilv
 * copied by niuyuan on 2018/9/1.
 */

public class PlayerManager implements View.OnClickListener{

    /**
     * 可能会剪裁，保持原视频大小，显示在中心，当原视频的大小超过view的大小，超过部分裁剪处理
     */
    public static final String SCALETYPE_FITPARENT = "fitparent";
    /**
     * 可能会剪裁，等比例放大视频，直到填满view为止，超过部分裁剪处理
     */
    public static final String SCALETYPE_FILLPARENT = "fillparent";
    /**
     * 将视频的内容完整居中显示，如果视频大于view，则按比例缩视频直到完全显示在view中
     * */
    public static final String SCALETYPE_WRAPCONTENT = "wrapContent";

     /**
     * 不剪裁，非等比例拉伸画面填满整个view
      * */
    public static final String SCALETYPE_FITXY = "fitXY";
    /**
     * 不剪裁，非等比例拉伸画面到16:9，并完全显示在view中
     * */
    public static final String SCALETYPE_16_9 = "16:9";
    /**
     * 不剪裁，非等比例拉伸画面到4:3，并完全显示在view中
     * */
    public static final String SCALETYPE_4_3 = "4:3";

    /**
     * 状态常量
     */
    private final int STATUS_ERROR = -1;
    private final int STATUS_IDLE = 0;
    private final int STATUS_LOADING = 1;
    private final int STATUS_PLAYING = 2;
    private final int STATUS_PAUSE = 3;
    private final int STATUS_COMPLETED = 4;
    private final int STATUS_PTZ_PLAY_PAUSE = 5;
    private final int STATUS_PTZ_BACK = 6;
    private final int STATUS_PTZ_SEEK_AFTER = 7;
    private final int STATUS_PTZ_SEEK_BEFORE = 8;

    private final Activity activity;
    private final IjkVideoView videoView;
    private final AudioManager audioManager;
    public GestureDetector gestureDetector;

    private boolean playerSupport;
    private boolean isLive = false;//是否是直播
    private boolean fullScreenOnly;
    private boolean portrait;

    private final int mMaxVolume;
    private int screenWidthPixels;
    private int currentPosition;
    private int status = STATUS_IDLE;
    private long pauseTime;
    private String url;
    private List<String> urls;
    private int mCurrentPosition = 0;
    private String mTotalStr = "00:00";
    private String mPositionStr = "00:00";
    private int mProgress = 0;
    private boolean mControlVisiable = true;

    private float brightness = -1;
    private int volume = -1;
    private long newPosition = -1;
    private long defaultRetryTime = 5000;

    //editing播控界面控件
    private RelativeLayout topRl;
    private LinearLayout startLl;
    private LinearLayout bottomLl;
    private ImageView backIv;
    private ImageView startIv;
    private ProgressBar loadingProgressBar;
    private ImageView lastIv;
    private ImageView playPauseIv;
    private ImageView nextIv;
    private TextView currentTv;
    private SeekBar bottomSeekBar;
    private TextView totalTv;

    private View playerContent;

    //手势弹窗
    private Dialog mProgressDialog;
    private ProgressBar mDialogProgressBar;
    private TextView mDialogSeekTimeTv;
    private TextView mDialogTotalTimeTv;
    private ImageView mDialogIconIv;
    private Dialog mVolumeDialog;
    private ProgressBar mDialogVolumeProgressBar;
    private TextView mDialogVolumeTv;
    private Dialog mBrightnessDialog;
    private ProgressBar mDialogBrightnessProgressBar;
    private TextView mDialogBrightnessTv;

    private OrientationEventListener orientationEventListener;
    private PlayerStateListener playerStateListener;
    SharedPreferences mSp;
    private Map<String, Object> mMap;

    public void setPlayerStateListener(PlayerStateListener playerStateListener){
        this.playerStateListener = playerStateListener;
    }

    private OnErrorListener onErrorListener = new OnErrorListener() {
        @Override
        public void onError(int what, int extra) {

        }
    };

    private OnCompleteListener onCompleteListener = new OnCompleteListener() {
        @Override
        public void onComplete() {
            releaseProgressTimer();
            startIv.setImageResource(R.mipmap.up_play_mid);
            playPauseIv.setImageResource(R.mipmap.up_play);
            statusChange(STATUS_COMPLETED);
            stop();
            releaseProgressTimer();
        }
    };

    private OnInfoListener onInfoListener = new OnInfoListener() {
        @Override
        public void onInfo(int what, int extra) {

        }
    };

    private OnControlPanelVisibilityChangeListener onControlPanelVisibilityChangeListener = new OnControlPanelVisibilityChangeListener() {
        @Override
        public void change(boolean isShowing) {

        }
    };

    /**
     * try to play when error(only for live video)
     *@param defaultRetryTime, 0 will stop retry, default is 5000 millisecond
     * */
    public void setDefaultRetryTime(long defaultRetryTime){
        this.defaultRetryTime = defaultRetryTime;
    }

    public PlayerManager(final Activity activity) {
        try {
            IjkMediaPlayer.loadLibrariesOnce(null);
            IjkMediaPlayer.native_profileBegin("libijkplayer.so");
            playerSupport = true;
        }catch (Throwable e){
            Log.e("UpPlayer","loadLibraries error",e);
        }
        this.activity = activity;
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        screenWidthPixels = activity.getResources().getDisplayMetrics().widthPixels;

        videoView = activity.findViewById(R.id.up_player_view);
        videoView.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer iMediaPlayer) {
                statusChange(STATUS_COMPLETED);
                onCompleteListener.onComplete();

                activity.finish();
            }
        });
        videoView.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer iMediaPlayer, int what, int extra) {
                statusChange(STATUS_ERROR);
                onErrorListener.onError(what,extra);
                return true;
            }
        });
        videoView.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer mp, int what, int extra) {
                switch (what){
                    case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                        statusChange(STATUS_LOADING);
                        break;
                    case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                        statusChange(STATUS_PLAYING);
                        break;
                    case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                        //显示下载进度
                        Toast.makeText(activity,"download rate:"+ extra,Toast.LENGTH_LONG).show();
                        break;
                    case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                        statusChange(STATUS_PLAYING);
                        break;
                }
                onInfoListener.onInfo(what,extra);
                return false;
            }
        });

        audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        gestureDetector = new GestureDetector(activity,new PlayGestureListener());

        if (fullScreenOnly){
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        portrait = getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        if (!playerSupport){
            DebugLog.e("","播放器不支持此设备");
        }

        //初始化播控
        initControlView();

        playerContent = activity.findViewById(R.id.player_content);
        playerContent.setClickable(true);
        playerContent.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (gestureDetector.onTouchEvent(motionEvent))
                    return true;
                switch (motionEvent.getAction() & MotionEvent.ACTION_MASK){
                    case MotionEvent.ACTION_UP:
                        dismissmBrightnessDialog();
                        dismissVolumeDialog();
                        dismissProgressDialog();
                        break;
                }
                return false;
            }
        });

        final boolean isPortrait = getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        orientationEventListener = new OrientationEventListener(activity) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation >= 0 && orientation <= 30 || orientation >= 330 || (orientation >= 150 && orientation <= 210)){
                    //竖屏
                    if (isPortrait){
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                        orientationEventListener.disable();
                    }
                }else if ((orientation >= 90 && orientation <= 120) || (orientation >= 240 && orientation <= 300)){
                    if (!isPortrait){
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                        orientationEventListener.disable();
                    }
                }
            }
        };

        orientationEventListener.enable();

        if ( Build.VERSION.SDK_INT >= 19) {
            View decorView = activity.getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
        mSp = PreferenceManager.getDefaultSharedPreferences(activity);

        mMap = new HashMap();
    }

    //editing
    private void initControlView() {
        topRl = activity.findViewById(R.id.top_layout);
        startLl = activity.findViewById(R.id.start_layout);
        bottomLl = activity.findViewById(R.id.bottom_layout);
        loadingProgressBar = activity.findViewById(R.id.loading);
        currentTv = activity.findViewById(R.id.current);
        totalTv = activity.findViewById(R.id.total);

        bottomSeekBar = activity.findViewById(R.id.bottom_seek_progress);
        bottomSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);

        backIv = activity.findViewById(R.id.back);
        startIv = activity.findViewById(R.id.start);
        lastIv = activity.findViewById(R.id.last);
        playPauseIv = activity.findViewById(R.id.play_pause);
        nextIv = activity.findViewById(R.id.next);

        backIv.setOnClickListener(this);
        startIv.setOnClickListener(this);
        lastIv.setOnClickListener(this);
        playPauseIv.setOnClickListener(this);
        nextIv.setOnClickListener(this);

    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEventBusCome(UpEvent event) {
        if (event != null) {
            receiveEvent(event);
        }
    }

    protected void receiveEvent(UpEvent event) {
        switch (event.getCode()) {
            case 0x666:
                Log.e("ViltaX","--0x666--"+getClass().toString());
                mUiHandler.sendEmptyMessage(STATUS_PTZ_PLAY_PAUSE);
                break;
            case 0x999:
                Log.e("ViltaX","--0x999--"+getClass().toString());
                mUiHandler.sendEmptyMessage(STATUS_PTZ_BACK);
                break;
            case 0x555:
                Log.e("ViltaX","--0x777--"+getClass().toString());
                mUiHandler.sendEmptyMessage(STATUS_PTZ_SEEK_BEFORE);
                break;
            case 0x777:
            Log.e("ViltaX","--0x888--"+getClass().toString());
            mUiHandler.sendEmptyMessage(STATUS_PTZ_SEEK_AFTER);
            break;
        }
    }

    private Handler mUiHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case STATUS_PTZ_PLAY_PAUSE:
                    playPauseIv.performClick();
                    break;
                case STATUS_PTZ_BACK:
                    backIv.performClick();
                    break;
                case STATUS_PTZ_SEEK_BEFORE:
                    //云台控制快退
                    onProgressSlide(- 0.1f);
                    progressDismissTimer.start();
                    break;
                case STATUS_PTZ_SEEK_AFTER:
                    //云台控制快进
                    onProgressSlide(0.1f);
                    if (mCurrentPosition + 0.1f < videoView.getDuration()){
                        progressDismissTimer.start();
                    }
                    break;
            }
            return false;
        }
    });

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            currentTv.setText(generateTime((seekBar.getProgress() * getDuration() / 100)));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            videoView.seekTo((seekBar.getProgress() * getDuration() / 100));
        }
    };

    private int getScreenOrientation() {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        //if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && height >width ||
                (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && width > height ){
            switch (rotation){
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        //if the device's natural orientation is landscape or if the device is square
        else{
            switch (rotation){
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }
        return orientation;
    }

    //editing
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.back) {
            activity.finish();

        } else if (i == R.id.play_pause || i == R.id.start) {
            Log.e("upplayer", "---status: " + status);
            if (status == STATUS_PLAYING) {
                startIv.setImageResource(R.mipmap.up_play_mid);
                playPauseIv.setImageResource(R.mipmap.up_play);
                statusChange(STATUS_PAUSE);
                videoView.pause();
            } else if (status == STATUS_PAUSE) {
                startIv.setImageResource(R.mipmap.up_pause_mid);
                playPauseIv.setImageResource(R.mipmap.up_pause);
                statusChange(STATUS_PLAYING);
                videoView.start();
            } else if (status == STATUS_COMPLETED) {
                startIv.setImageResource(R.mipmap.up_pause_mid);
                playPauseIv.setImageResource(R.mipmap.up_pause);
                statusChange(STATUS_PLAYING);
                videoView.release(false);
                videoView.setRender(videoView.RENDER_TEXTURE_VIEW);
                play(urls.get(mCurrentPosition));
            }

        } else if (i == R.id.last) {
            if (mCurrentPosition >= 1) {
                mCurrentPosition--;
                statusChange(STATUS_COMPLETED);
                videoView.release(false);
                videoView.setRender(videoView.RENDER_TEXTURE_VIEW);
                releaseProgressTimer();
                play(urls.get(mCurrentPosition));
                startIv.setImageResource(R.mipmap.up_pause_mid);
                playPauseIv.setImageResource(R.mipmap.up_pause);
            } else if (mCurrentPosition == 0) {
                Toast.makeText(activity, activity.getString(R.string.toast_at_first_video), Toast.LENGTH_LONG).show();
            } else {
                return;
            }


        } else if (i == R.id.next) {
            if (mCurrentPosition < urls.size() - 1) {
                mCurrentPosition++;
                statusChange(STATUS_COMPLETED);
                videoView.release(false);
                videoView.setRender(videoView.RENDER_TEXTURE_VIEW);
                releaseProgressTimer();
                play(urls.get(mCurrentPosition));
                startIv.setImageResource(R.mipmap.up_pause_mid);
                playPauseIv.setImageResource(R.mipmap.up_pause);
            } else if (mCurrentPosition == urls.size() - 1) {
                Toast.makeText(activity, activity.getString(R.string.toast_at_last_video), Toast.LENGTH_LONG).show();
            } else {
                return;
            }


        }

    }

    public void play(List<String> urls, int defaultUrl) {
        this.urls = urls;
        mCurrentPosition = defaultUrl;
        play(urls.get(defaultUrl));
    }

    private void selectRightPlayer(String url) {
        MediaExtractor extractor = new MediaExtractor();
        int frameRate = 30;//默认帧率为30帧
        int rotation = 0;
        try {
            extractor.setDataSource(url);
            int numTracks = extractor.getTrackCount();
            for (int i = 0; i <numTracks ; i++){
                MediaFormat format = extractor.getTrackFormat(i);
                String mine = format.getString(MediaFormat.KEY_MIME);
                if (mine.startsWith("video/")) {
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)){
                        if (mMap.get(MediaFormat.KEY_FRAME_RATE) != null &&
                                ((mMap.get(MediaFormat.KEY_ROTATION)) != null)){
                            frameRate = ((Integer)mMap.get(MediaFormat.KEY_FRAME_RATE)).intValue();
                            rotation = ((Integer)mMap.get(MediaFormat.KEY_ROTATION)).intValue();
                        }
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            extractor.release();
        }
        Log.e("Player","--frameRate--"+frameRate+" fps"+"--rotation:"+rotation);

        int playerType;
        if (frameRate > 60){
            playerType = 2;
        }else {
            playerType = 3;
        }
        SharedPreferences.Editor editor = mSp.edit();
        String key = activity.getString(R.string.pref_key_player);
        editor.putInt(key,playerType);
        editor.commit();
    }

    public class PlayGestureListener extends GestureDetector.SimpleOnGestureListener{
        private boolean firstTouch;
        private boolean volumeControl;
        private boolean toSeek;


        /**
         * 双击
         * */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            firstTouch = true;
            return super.onDown(e);
        }


        /**
         * 滑动
         * */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float mOldX = e1.getX(),mOldY = e1.getY();
            float deltaY = mOldY - e2.getY();
            float deltaX = mOldX - e2.getX();
            Log.e("upplayer","mOldX: "+mOldX+"--mOldY: "+mOldY);
            Log.e("upplayer","deltaX: "+deltaX+"--deltaY: "+deltaY);
            if (firstTouch){
                toSeek = Math.abs(distanceX) >= Math.abs(distanceY);
                volumeControl = mOldX > screenWidthPixels * 0.5f;
                firstTouch = false;
            }
            Log.e("upplayer","toSeek--"+toSeek);
            if (toSeek){
                if (!isLive){
                    onProgressSlide(-deltaX / videoView.getWidth());
                    Log.e("upplayer","percent: "+(-deltaX / videoView.getWidth()));
                }
            }else {
               float percent = deltaY / videoView.getHeight();
                if (volumeControl){
                   onVolumeSlide(percent);
               }else {
                   onBrightnessSlide(percent);
               }
            }

            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            //单击，隐藏或显示控制控件
            controlViewTimer.start();
            if (mControlVisiable){
                setControlViewVisiable(false);
                controlViewTimer.cancel();
            }else {
                setControlViewVisiable(true);
                controlViewTimer.start();
            }
            return true;
        }

    }

    /**
     * is player support this device
     * */
    public boolean isPlayerSupport(){
        return playerSupport;
    }

    /**
     * is playing
     * */
    public boolean isPlaying(){
        return videoView != null ? videoView.isPlaying() : false;
    }

    public void stop(){
        videoView.stopPlayback();
    }

    public int getCurrentPosition(){
        return videoView.getCurrentPosition();
    }

    public IjkVideoView getVideoView(){
        if (videoView != null){
            return videoView;
        }
        return null;
    }

    public int getDuration(){
        return videoView.getDuration();
    }

    public PlayerManager playerInFullScreen(boolean fullScreen){
        if (fullScreen){
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        return this;
    }

    private void statusChange(int newStatus) {
        status = newStatus;
        if (!isLive && newStatus == STATUS_COMPLETED){
            Log.e("upplayer","statusChange STATUS_COMPLETED...");
            if (playerStateListener != null){
                playerStateListener.onComplete();
                releaseProgressTimer();
            }
        }else if (newStatus == STATUS_ERROR){
            Log.e("upplayer","statusChange STATUS_ERROR...");
            if (playerStateListener != null){
                playerStateListener.onError();
                releaseProgressTimer();
            }
        }else if (newStatus == STATUS_LOADING){
            Log.e("upplayer","statusChange STATUS_LOADING...");
            if (playerStateListener != null){
                playerStateListener.onLoading();
            }
        }else if (newStatus == STATUS_PLAYING){
            Log.e("upplayer","statusChange STATUS_PLAYING...");
            if (playerStateListener != null){
                playerStateListener.onPlay();
            }
        }
    }

    private void onPause(){
        pauseTime = System.currentTimeMillis();
        if (status == STATUS_PLAYING){
            videoView.pause();
            if (!isLive){
                currentPosition = videoView.getCurrentPosition();
            }
        }
    }

    public void onResume(){
        pauseTime = 0;
        if (status == STATUS_PLAYING){
            if (isLive){
                videoView.seekTo(0);
            }else {
                if (currentPosition > 0){
                    videoView.seekTo(currentPosition);
                }
            }
            videoView.start();
        }
    }

    private CountDownTimer progressDismissTimer = new CountDownTimer(1000,100) {
        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            if (videoView.isPlaying()){
                dismissProgressDialog();
            }
        }
    };

    private CountDownTimer controlViewTimer = new CountDownTimer(5000,1000) {
        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            //editing 20180910 倒计时结束，隐藏掉控制控件
            setControlViewVisiable(false);
        }
    };

    private void setControlViewVisiable(boolean isVisiable) {
        int visibility;
        if (isVisiable){
            visibility = View.VISIBLE;
            mControlVisiable = true;
        }else {
            visibility = View.GONE;
            mControlVisiable = false;
        }
        startLl.setVisibility(visibility);
        bottomLl.setVisibility(visibility);
    }

    public void onDestroy(){
        orientationEventListener.disable();
        videoView.stopPlayback();
        releaseProgressTimer();
        EventBus.getDefault().unregister(this);
    }

    public void play(String url){
        this.url = url;
        if (playerSupport){
            selectRightPlayer(urls.get(mCurrentPosition));

            videoView.setVideoPath(url);
            videoView.start();
            status = STATUS_PLAYING;

            startProgressTimer();
            //开始播放，倒计时隐藏掉控制控件
            controlViewTimer.start();
        }
    }

    private String generateTime(long time){
        int totalSeconds = (int) (time / 1000);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        return hours > 0 ? String.format("%02d:%02d:%02d",hours,minutes,seconds) :String.format("%02d:%02d",minutes,seconds);
    }

    /**
     * 滑动改变声音大小
     * @param percent
     * */
    private void onVolumeSlide(float percent){
        //隐藏掉控制控件
        setControlViewVisiable(false);

        Log.e("Volume","volume: "+volume+"--percent:"+percent);
        if (volume == -1){
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (volume < 0){
                volume = 0;
            }
        }
        int index = (int) ((percent * mMaxVolume) + volume);
        if (index > mMaxVolume){
            index = mMaxVolume;
        }else if (index < 0){
            index = 0;
        }
        //变更声音
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,index,0);
        //变更进度条
        int i = (int) (index  * 1.0 / mMaxVolume *100);

        //显示调节音量的弹窗
        showVolumeDialog(i);

        String s = i +"%";
        if (i == 0){
            s = "off";
        }
        DebugLog.d("","onVolumeSlide:"+s);
    }

    /**
     * 滑动改变播放进度
     * @param percent
     * */
    private void onProgressSlide(float percent){
        //隐藏掉控制控件
        setControlViewVisiable(false);

        long position = videoView.getCurrentPosition();
        long duration = videoView.getDuration();
        long deltaMax = Math.min(100  * 1000,duration - position);
        long delta = (long)(deltaMax * percent);

        newPosition = delta + position;
        Log.e("upplayer","position: "+position+"--duration:  "+duration+"--deltaMax:  "+deltaMax+"--delta:  "+delta+"--newPosition: "+newPosition);
        if (newPosition > duration){
            newPosition = duration;
        }else if (newPosition < 0){
            newPosition = 0;
            delta = -position;
        }

        int showDelta = (int) (delta / 100);

        //显示出进度调节dialog，根据showDelta正负来确定显示快退还是快进图标
        showProgressDialog(showDelta,(int)(position * 100 / duration));
        Log.e("upplayer","showDelta: "+showDelta);

        if (showDelta != 0){
            String text = showDelta > 0 ? ("+"+showDelta) : "" + showDelta;
            videoView.seekTo((int) newPosition);
            Log.e("upplayer","onProgressSlide:" + text);
        }
    }

    private void showmBrightnessDialog(int brightnessPercent) {
        if (mBrightnessDialog == null){
            View localview = LayoutInflater.from(activity).inflate(R.layout.up_dialog_brightness,null);
            mDialogBrightnessProgressBar = localview.findViewById(R.id.brightness_progressbar);
            mDialogBrightnessTv = localview.findViewById(R.id.tv_brightness);
            mBrightnessDialog = createDialogWithView(localview);
        }

        if (!mBrightnessDialog.isShowing()){
            mBrightnessDialog.show();
        }

        if (brightnessPercent > 100){
            brightnessPercent = 100;
        }else if (brightnessPercent < 0){
            brightnessPercent = 0;
        }

        mDialogBrightnessTv.setText(brightnessPercent + "%");
        mDialogBrightnessProgressBar.setProgress(brightnessPercent);

    }

    private void dismissmBrightnessDialog(){
        if (mBrightnessDialog != null){
            mBrightnessDialog.dismiss();
            //每次结束调整亮度，保存下当前的亮度，便于下次在此基础上调整
            brightness = activity.getWindow().getAttributes().screenBrightness;
        }
    }

    private void showVolumeDialog(int volumePercent) {
        if (mVolumeDialog == null){
            View localview = LayoutInflater.from(activity).inflate(R.layout.up_dialog_volume,null);
            mDialogVolumeProgressBar = localview.findViewById(R.id.volume_progressbar);
            mDialogVolumeTv = localview.findViewById(R.id.tv_volume);
            mVolumeDialog = createDialogWithView(localview);
        }

        if (!mVolumeDialog.isShowing()){
            mVolumeDialog.show();
        }

        if (volumePercent > 100){
            volumePercent = 100;
        }else if (volumePercent < 0){
            volumePercent = 0;
        }

        mDialogVolumeTv.setText(volumePercent + "%");
        mDialogVolumeProgressBar.setProgress(volumePercent);
    }

    private void dismissVolumeDialog(){
        if (mVolumeDialog != null){
            mVolumeDialog.dismiss();
            //每次结束调整音量，保存下当前的音量，便于下次在此基础上调整
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
    }

    private void showProgressDialog(int showDelta, int progress) {
        if (mProgressDialog == null){
            View localview = LayoutInflater.from(activity).inflate(R.layout.up_dialog_progress,null);
            mDialogProgressBar = localview.findViewById(R.id.duration_progressbar);
            mDialogSeekTimeTv = localview.findViewById(R.id.tv_current);
            mDialogTotalTimeTv = localview.findViewById(R.id.tv_duration);
            mDialogIconIv = localview.findViewById(R.id.duration_image_tip);
            mProgressDialog = createDialogWithView(localview);
        }

        if (!mProgressDialog.isShowing()){
            mProgressDialog.show();
        }

        mDialogSeekTimeTv.setText(generateTime((progress * getDuration() / 100)));
        mDialogTotalTimeTv.setText(totalTv.getText());
        mDialogProgressBar.setProgress(progress);
        if (showDelta > 0){
            mDialogIconIv.setBackgroundResource(R.mipmap.up_forward_icon);
        }else {
            mDialogIconIv.setBackgroundResource(R.mipmap.up_backward_icon);
        }
    }

    private void dismissProgressDialog(){
        if (mProgressDialog != null){
            mProgressDialog.dismiss();
        }
    }

    private Dialog createDialogWithView(View localview) {
        Dialog dialog = new Dialog(activity,R.style.up_style_dialog_progress);
        dialog.setContentView(localview);
        Window window = dialog.getWindow();
        window.addFlags(Window.FEATURE_ACTION_BAR);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        window.setLayout(-2,-2);
        WindowManager.LayoutParams localLayoutParams = window.getAttributes();
        window.setAttributes(localLayoutParams);
        return dialog;
    }

    /**
     * 滑动改变亮度
     * @param percent
     * */
    private void onBrightnessSlide(float percent){
        //隐藏掉控制控件
        setControlViewVisiable(false);

        if (brightness < 0){
            brightness = activity.getWindow().getAttributes().screenBrightness;
            if (brightness <= 0.00f){
                brightness = 0.5f;
            }else if (brightness < 0.01f){
                brightness = 0.01f;
            }
        }
        DebugLog.d("","brightness:"+brightness+",percent:"+percent);
        WindowManager.LayoutParams lpa = activity.getWindow().getAttributes();
        lpa.screenBrightness = brightness + percent;
        if (lpa.screenBrightness > 1.0f){
            lpa.screenBrightness = 1.0f;
        }else if (lpa.screenBrightness < 0.01f){
            lpa.screenBrightness = 0.01f;
        }
        activity.getWindow().setAttributes(lpa);
        //显示调节亮度的弹窗
        showmBrightnessDialog((int)((brightness + percent) * 100));
    }

    public void setFullScreenOnly(boolean fullScreenOnly){
        this.fullScreenOnly = fullScreenOnly;
        tryFullScreen(fullScreenOnly);
        if (fullScreenOnly){
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }

    private void tryFullScreen(boolean fullScreen) {
        if (activity instanceof AppCompatActivity){
            ActionBar supportActionBar = ((AppCompatActivity)activity).getSupportActionBar();
            if (supportActionBar != null){
                if (fullScreen){
                    supportActionBar.hide();
                }else {
                    supportActionBar.show();
                }
                setFullScreen(fullScreen);
            }
        }
    }

    private void setFullScreen(boolean fullScreen) {
        if (activity != null){
            WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
            if (fullScreen){
                attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                activity.getWindow().setAttributes(attrs);
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            }else {
                attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
                activity.getWindow().setAttributes(attrs);
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            }
        }
    }

    /**
     * fitParent：可能会剪裁，保持原视频的大小，显示在中心，当原视频的大小超过view的大小，超过部分剪裁处理
     * fillParent：可能会剪裁，等比例放大视频，直到填满view为止，超过view的部分作剪裁处理
     * wrapContent：将视频的内容完整居中显示，如果视频大于view，则按比例所视频直到显示在view中
     * fitXY：不剪裁，费等比拉伸画面填满整个view
     * 16:9：不剪裁，飞等比例拉伸画面到16:9，并完全显示在view中
     * 4:3：不剪裁，费等比例拉伸画面到4:3，并完全显示在view中
     *
     * @param scaleType
     * */
    public void setScaleType(String scaleType){
        if (SCALETYPE_FITPARENT.equals(scaleType)){
            videoView.setAspectRatio(IRenderView.AR_ASPECT_FIT_PARENT);
        }else if (SCALETYPE_FILLPARENT.equals(scaleType)){
            videoView.setAspectRatio(IRenderView.AR_ASPECT_FILL_PARENT);
        }else if (SCALETYPE_WRAPCONTENT.equals(scaleType)){
            videoView.setAspectRatio(IRenderView.AR_ASPECT_WRAP_CONTENT);
        }else if (SCALETYPE_FITXY.equals(scaleType)){
            videoView.setAspectRatio(IRenderView.AR_MATCH_PARENT);
        }else if (SCALETYPE_16_9.equals(scaleType)){
            videoView.setAspectRatio(IRenderView.AR_16_9_FIT_PARENT);
        }else if (SCALETYPE_4_3.equals(scaleType)){
            videoView.setAspectRatio(IRenderView.AR_4_3_FIT_PARENT);
        }
    }

    public void start(){
        videoView.start();
    }

    public void pause(){
        videoView.pause();
    }

    public boolean onBackPressed(){
        if (!fullScreenOnly && getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        return true;
        }
        return false;
    }

    public class ProgressTimerTask extends TimerTask{

        @Override
        public void run() {
            if (status == STATUS_PLAYING || status == STATUS_PAUSE){
                String total = generateTime(getDuration());
                String current = generateTime(getCurrentPosition());
                int position = 0;
                int duration = 0;
                if (!total.equals("00:00")){
                    mTotalStr = total;
                    duration = getDuration();
                }
                if (!current.equals("00:00")){
                    mPositionStr = current;
                    position = getCurrentPosition();
                }
                if (duration != 0 && position != 0){
                    mProgress = getCurrentPosition() * 100 / getDuration();
//                    Log.e("upplayer","--mPositionStr--"+mPositionStr+"--mTotalStr--"+mTotalStr+"--progress--"+mPositionStr);
                }
                mTimeHandler.sendEmptyMessage(UPDATE_TIME_AND_PROGRESS);

            }
        }
    }


    protected static Timer UPDATE_PROGRESS_TIMER;
    protected ProgressTimerTask mProgressTimerTask;

    public void startProgressTimer(){
        UPDATE_PROGRESS_TIMER = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        UPDATE_PROGRESS_TIMER.schedule(mProgressTimerTask,0,300);
    }
    public void releaseProgressTimer(){
        UPDATE_PROGRESS_TIMER = null;
        mProgressTimerTask = null;
        mTotalStr = "00:00";
        mPositionStr = "00:00";
        mProgress = 0;
        mTimeHandler.removeMessages(UPDATE_TIME_AND_PROGRESS);
    }

    private static final int UPDATE_TIME_AND_PROGRESS = 1;
    private android.os.Handler mTimeHandler = new android.os.Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case UPDATE_TIME_AND_PROGRESS:
                    changeTimeAndProgress();
                    break;
            }
        }
    };

    private void changeTimeAndProgress() {
        totalTv.setText(mTotalStr);
        currentTv.setText(mPositionStr);
        bottomSeekBar.setProgress(mProgress);
    }


    class Query{
        private final Activity activity;
        private View view;

        Query(Activity activity) {
            this.activity = activity;
        }

        public Query id(int id) {
            view = activity.findViewById(id);
            return this;
        }

        public Query image(int resId) {
            if (view instanceof ImageView){
                ((ImageView)view).setImageResource(resId);
            }
            return this;
        }

        public Query visible() {
            if (view != null){
                view.setVisibility(View.VISIBLE);
            }
            return this;
        }

        public Query gone() {
            if (view != null){
                view.setVisibility(View.GONE);
            }
            return this;
        }

        public Query invisible() {
            if (view != null){
                view.setVisibility(View.INVISIBLE);
            }
            return this;
        }

        public Query clicked(View.OnClickListener handler) {
            if (view != null){
                view.setOnClickListener(handler);
            }
            return this;
        }

        public Query text(CharSequence text) {
            if (view != null && view instanceof TextView){
                ((TextView)view).setText(text);
            }
            return this;
        }

        public Query visibility(int visible) {
            if (view != null){
                view.setVisibility(visible);
            }
            return this;
        }

        private void size(boolean width, int n, boolean dip) {
            if (view != null){
                ViewGroup.LayoutParams lp = view.getLayoutParams();
                if (n > 0 && dip){
                    n = dip2pixel(activity,n);
                }
                if (width){
                    lp.width = n;
                }else {
                    lp.height = n;
                }
                view.setLayoutParams(lp);
            }
        }

        public void height(int height, boolean dip){
            size(false,height,dip);
        }

        private int dip2pixel(Context context, float n) {
            int value = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,n,context.getResources().getDisplayMetrics());
            return value;
        }

        private float pixel2dip(Context context, float n) {
            Resources resources = context.getResources();
            DisplayMetrics metrics = resources.getDisplayMetrics();
            float dp = n / (metrics.densityDpi / 160f);
            return dp;
        }
    }

    public PlayerManager onError(OnErrorListener onErrorListener){
        this.onErrorListener = onErrorListener;
        return this;
    }

    public PlayerManager onComplete(OnCompleteListener onCompleteListener){
        this.onCompleteListener = onCompleteListener;
        return this;
    }

    public PlayerManager onInfo(OnInfoListener onInfoListener){
        this.onInfoListener = onInfoListener;
        return this;
    }

    public PlayerManager onControlPanelVisiblityChange(OnControlPanelVisibilityChangeListener onControlPanelVisibilityChangeListener){
        this.onControlPanelVisibilityChangeListener = onControlPanelVisibilityChangeListener;
        return this;
    }

    public PlayerManager live(boolean isLive){
        this.isLive = isLive;
        return this;
    }

    public PlayerManager toggleAspectRatio(){
        if (videoView != null){
            videoView.toggleAspectRatio();
        }
        return this;
    }

    public interface PlayerStateListener{
        void onComplete();
        void onError();
        void onLoading();
        void onPlay();
    }

    public interface OnErrorListener{
        void onError(int what, int extra);
    }

    public interface OnCompleteListener{
        void onComplete();
    }

    public  interface OnControlPanelVisibilityChangeListener{
        void change(boolean isShowing);
    }

    public interface OnInfoListener{
        void onInfo(int what, int extra);
    }
}