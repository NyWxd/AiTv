package com.freevisiontech.iqy.aitv;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.ny.ijk.upplayer.media.PlayerManager;
import com.ny.ijk.upplayer.ui.PlayActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String url_1 = "http://hd.yinyuetai.com/uploads/videos/common/410E01536EE4913B861AAAE14384E5E5.flv?sc=c7f63ff8b1851298&br=1101&vid=2113588&aid=273&area=HT&vst=0";
    private String url_2 = "http://he.yinyuetai.com/uploads/videos/common/80910161684BA0A34DE07502D4029DB8.mp4?sc=b5138673890caad4&br=2686&vid=3151952&aid=22&area=HT&vst=0";
    private String url_3 = "http://he.yinyuetai.com/uploads/videos/common/4DD10169F6DF359A0590C808DA7AAC62.mp4?sc=b1a1bd1dcd93d15e&br=3150&vid=3372051&aid=22&area=HT&vst=3";
//    private String url_1 = "/storage/emulated/0/DCIM/fvmobile/VID20190301165551460TimeLapse.mp4";
//    private String url_2 = "/storage/emulated/0/DCIM/fvmobile/VID20190301152631562TimeLapse.mp4";
//    private String url_3 = "/storage/emulated/0/DCIM/fvmobile/VID20190301143809284TimeLapse.mp4";
//    private String url_1 = "http://ivi.bupt.edu.cn/hls/cctv1hd.m3u8";
//    private String url_2 = "http://ivi.bupt.edu.cn/hls/cctv5phd.m3u8";
//    private String url_3 = "http://ivi.bupt.edu.cn/hls/cctv6hd.m3u8";
//    private String url_4 = "/storage/emulated/0/DCIM/fvmobile/VID20181213141537294TimeLapse.mp4";
//    private String url_5 = "/storage/emulated/0/DCIM/fvmobile/VID20181213141828022.mp4";
//    private String url_6 = "/storage/emulated/0/DCIM/fvmobile/VID20181213141903259TimeLapse.mp4";
//    private String url_7 = "/storage/emulated/0/DCIM/fvmobile/VID20181213142031044.mp4";
//    private String url_8 = "/storage/emulated/0/DCIM/fvmobile/VID20181213142057210TimeLapse.mp4";
//    private String url_9 = "/storage/emulated/0/DCIM/fvmobile/test_video1.mp4";
//    private String url_10 = "/storage/emulated/0/DCIM/fvmobile/test_video2.mp4";
//    private String url_11 = "/storage/emulated/0/DCIM/fvmobile/VID20181215163356526.mp4";
    ArrayList<String> urls = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermission();


        urls.add(url_1);
        urls.add(url_2);
        urls.add(url_3);
//        urls.add(url_4);
//        urls.add(url_5);
//        urls.add(url_6);
//        urls.add(url_7);
//        urls.add(url_8);
//        urls.add(url_9);
//        urls.add(url_10);

        Intent intent = new Intent(new Intent(MainActivity.this, PlayActivity.class));
        intent.putStringArrayListExtra("urls",urls);

        intent.putExtra("position",0);
        startActivity(intent);
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //校验是否已具有模糊定位权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        1);
            }
        }
    }

    private void initVideo() {

//        urls.add(url_1);
//        urls.add(url_2);
//        urls.add(url_3);
//        urls.add(url_4);
//        urls.add(url_5);
//
//        mPlayerManager = new PlayerManager(PlayActivity.this);
//        mPlayerManager.setFullScreenOnly(true);
//        mPlayerManager.live(false);
//        mPlayerManager.setScaleType(PlayerManager.SCALETYPE_WRAPCONTENT);
//        mPlayerManager.playerInFullScreen(true);
//        mPlayerManager.play(urls,1);

    }

}
