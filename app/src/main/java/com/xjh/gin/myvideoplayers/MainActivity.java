package com.xjh.gin.myvideoplayers;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private CustomVideoView videoView;
    private LinearLayout controllerLayout;
    private ImageView play_controller_img,screen_img,volume_img;
    private TextView time_current_tv,time_total_tv;
    private SeekBar play_seek,volume_seek;
    public static final int UPDATEUI = 1;
    private int screen_width,screen_height;
    private RelativeLayout videoLayout;
    private AudioManager mAudioManager;
    private boolean isFullScreen = false;
    private boolean isAdjust = false;//是否误触
    private int threshold=54;//误触的临界值
    private float StartX=0,StartY=0;
    private float mBrightness;//当前亮度
    private ImageView operation_bg,operation_percent;
    private FrameLayout progress_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initEvent();
        //videolocal();
        videoonline();
        UIHandler.sendEmptyMessage(UPDATEUI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //停止UI刷新
        UIHandler.removeMessages(UPDATEUI);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //停止UI刷新
        UIHandler.removeMessages(UPDATEUI);
    }

    private void initView() {
        videoView = findViewById(R.id.id_VideoView);
        controllerLayout = findViewById(R.id.controllerbar_layout);
        play_controller_img=findViewById(R.id.pause_img);
        screen_img=findViewById(R.id.screen_img);
        time_current_tv=findViewById(R.id.time_current_tv);
        time_total_tv=findViewById(R.id.time_total_tv);
        play_seek=findViewById(R.id.play_seek);
        volume_seek=findViewById(R.id.volume_seek);
        videoLayout=findViewById(R.id.videoLayout);
        volume_img=findViewById(R.id.volume_img);
        screen_width=getResources().getDisplayMetrics().widthPixels;
        screen_height=getResources().getDisplayMetrics().heightPixels;
        mAudioManager=(AudioManager)getSystemService(AUDIO_SERVICE);
        operation_bg=findViewById(R.id.operation_bg);
        operation_percent=findViewById(R.id.operation_percent);
        progress_layout=findViewById(R.id.progress_layout);
        int streamMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);//最大音量
        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volume_seek.setMax(streamMaxVolume);
        volume_seek.setProgress(streamVolume);
    }

    private void initEvent() {
        play_controller_img.setOnClickListener(this);
        screen_img.setOnClickListener(this);
        play_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTextViewWithTimeFormat(time_current_tv,progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                UIHandler.removeMessages(UPDATEUI);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                videoView.seekTo(progress);
                UIHandler.sendEmptyMessage(UPDATEUI);
            }
        });
        volume_seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,progress,0);//设置当前音量
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //控制VideoView的手势
        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();
                switch(event.getAction()){
                    case MotionEvent.ACTION_DOWN://手指落下屏幕的那一刻（只会调用一次）
                        StartX=x;
                        StartY=y;
                        break;
                    case MotionEvent.ACTION_MOVE://手指在屏幕移动（调用多次）
                        float detlaX=x-StartX;
                        float detlaY=y-StartY;//手指滑动的偏移量
                        float absdetlaX=Math.abs(detlaX);//偏移量的绝对值
                        float absdetlaY=Math.abs(detlaY);
                        if(absdetlaX>threshold&&absdetlaY>threshold){
                            if(absdetlaX<absdetlaY){
                                isAdjust=true;
                            }
                            else{
                                isAdjust=false;
                            }
                        }
                        else if(absdetlaX<threshold&&absdetlaY>threshold){
                            isAdjust=true;
                        }
                        else if(absdetlaX>threshold&&absdetlaY<threshold){
                            isAdjust=true;
                        }
                        if(isAdjust){
                            if(x<screen_width/2){
                                changeBrightness(-detlaY);
                            }
                            else{
                                changeVolume(-detlaY);
                            }
                        }
                        StartX=x;
                        StartY=y;
                        break;
                    case MotionEvent.ACTION_UP://手指离开屏幕的那一刻（只会调用一次）
                        progress_layout.setVisibility(View.GONE);
                        break;
                }
                return true;
            }
        });
    }

    //调节音量
    private void changeVolume(float detlaY){
        int max=mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current=mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int addvolume=(int)((detlaY/screen_height)*max*3);
        int volume=Math.max(0,current+addvolume);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,volume,0);
        volume_seek.setProgress(volume);

        //图片
        operation_bg.setImageResource(R.drawable.video_voice_bg);
        ViewGroup.LayoutParams layoutParams = operation_percent.getLayoutParams();
        layoutParams.width= (int) (DensityUtil.dip2px(this,94)*(float)volume/max);
        operation_percent.setLayoutParams(layoutParams);
        if(progress_layout.getVisibility()==View.GONE){
            progress_layout.setVisibility(View.VISIBLE);
        }

    }

    //调节亮度
    private void changeBrightness(float detlaY){
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        mBrightness=attributes.screenBrightness;
        float addBrightness=detlaY/screen_height/3;
        mBrightness+=addBrightness;
        if(mBrightness>1.0f){
            mBrightness=1.0f;
        }
        if(mBrightness<0.01f){
            mBrightness=0.01f;
        }
        attributes.screenBrightness=mBrightness;
        getWindow().setAttributes(attributes);
        //图片
        operation_bg.setImageResource(R.drawable.video_brightness_bg);
        ViewGroup.LayoutParams layoutParams = operation_percent.getLayoutParams();
        layoutParams.width= (int) (DensityUtil.dip2px(this,94)*mBrightness)*3;
        operation_percent.setLayoutParams(layoutParams);
        if(progress_layout.getVisibility()==View.GONE){
            progress_layout.setVisibility(View.VISIBLE);
        }
    }

    //本地视频播放
    private void videolocal() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/生活大爆炸S04E02.mkv";
        videoView.setVideoPath(path);
        videoplay();
    }

    //网络播放
    private void videoonline() {
        videoView.setVideoURI(Uri.parse("http://221.228.226.23/11/w/y/z/v/wyzvolcrbuqljdfxvscczsxkgztvcm/hc.yinyuetai.com/7FF1015C39741F10D082A0C66C0A896D.mp4"));
        videoplay();
    }

    private void videoplay() {
        MediaController controller = new MediaController(this);
        controller.setVisibility(View.INVISIBLE);//取消自带的按钮
        videoView.setMediaController(controller);
        controller.setMediaPlayer(videoView);
        videoView.start();//自动播放视频
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.pause_img:
                if(videoView.isPlaying()){
                    play_controller_img.setImageResource(R.drawable.play_btn_style);
                    //暂停播放
                    videoView.pause();
                    //停止UI刷新
                    UIHandler.removeMessages(UPDATEUI);
                }
                else{
                    play_controller_img.setImageResource(R.drawable.pause_btn_style);
                    //播放
                    videoView.start();
                    //恢复UI刷新
                    UIHandler.sendEmptyMessage(UPDATEUI);
                }
                break;
            case R.id.screen_img:
                if(isFullScreen){
                    isFullScreen=false;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//切换竖屏
                }
                else{
                    isFullScreen=true;
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//切换横屏
                }
                break;
        }
    }

    //实时刷新UI
    private Handler UIHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what==UPDATEUI){
                int currentPosition = videoView.getCurrentPosition();//获取当前视频的播放时间
                int totalduration = videoView.getDuration();//获取视频播放的总时间
                updateTextViewWithTimeFormat(time_current_tv,currentPosition);
                updateTextViewWithTimeFormat(time_total_tv,totalduration);

                play_seek.setMax(totalduration);
                play_seek.setProgress(currentPosition);
                UIHandler.sendEmptyMessageDelayed(UPDATEUI,500);//自己给自己刷新
            }
        }
    };


    //格式化时间
    private void updateTextViewWithTimeFormat(TextView textView,int millisecond){
        int second = millisecond/1000;//传入是毫秒
        int hh = second/3600;
        int mm=second%3600/60;
        int ss=second%60;
        String str = null;
        if(hh!=0){
            str=String.format("%02d:%02d:%02d",hh,mm,ss);
        }
        else{
            str=String.format("%02d:%02d",mm,ss);
        }
        textView.setText(str);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE){//当屏幕方向为横屏的时候
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
            volume_img.setVisibility(View.VISIBLE);
            volume_seek.setVisibility(View.VISIBLE);
            isFullScreen=true;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);//移除半屏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        }
        else{
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT,DensityUtil.dip2px(this,240));
            volume_img.setVisibility(View.GONE);
            volume_seek.setVisibility(View.GONE);
            isFullScreen=false;
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);//设置半屏
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);//移除全屏
        }
    }

    private void setVideoViewScale(int width,int height){
        ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
        layoutParams.width=width;
        layoutParams.height=height;
        videoView.setLayoutParams(layoutParams);
        ViewGroup.LayoutParams ViewlayoutParams = videoLayout.getLayoutParams();
        ViewlayoutParams.width=width;
        ViewlayoutParams.height=height;
        videoLayout.setLayoutParams(ViewlayoutParams);
    }
}