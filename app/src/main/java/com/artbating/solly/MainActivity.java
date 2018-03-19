package com.artbating.solly;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.squareup.otto.Subscribe;
import com.zl.reik.dilatingdotsprogressbar.DilatingDotsProgressBar;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.AbstractTag;
import org.jaudiotagger.tag.id3.ID3Tags;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.lyrics3.Lyrics3v2;
import org.jaudiotagger.tag.lyrics3.Lyrics3v2Fields;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import jp.wasabeef.glide.transformations.BlurTransformation;

import static android.support.v4.view.ViewPager.SCROLL_STATE_DRAGGING;


/**
 * Created by chunghoen on 2017-03-18.
 */

public class MainActivity extends AppCompatActivity {
    private ImageView buttonPlayStop, menu,menu2,shuffle;
    private static final int TIME_DELAY = 2000;
    private static long back_pressed;
    private SeekBar seekBar, topseek;
    private TextView maxtime, currenttime, subject, topartist, topsong,artist;
    private RelativeLayout nfctag, toolbar,hide,empty,topClick,loopingClick,shuffleClick,forward,reverse;
    private ImageView topcover, topplaystop, topforward, looping,empty2;
    private final Handler handler = new Handler();
    private String starttime = "0:00";
    private MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
    private byte[] art;
    int playing = 0;
    static int DOWNSTATE =0;
    static int DOWNPROGRESS =0;
    static boolean DOWNBOOLEAN = false;
    int notiinit = 0;
    ViewPager pager;
    ViewPagerAdapter pagerAdapter;
    ArrayList<SongData> songDataArrayList = new ArrayList<>();
    MarketAdapter adapter;
    LyricView lyricView;
    ImageView lyricback;
    TextView lyricvisible;
    LetterSpacingTextView appname;
    RecyclerView recyclerView;
    Typeface typeface;
    static int arraynull =1;
    private ArrayList<Uri> fileArrayList = new ArrayList<>();
    private ArrayList<Uri> lrcuriArrayList = new ArrayList<>();


    private FrameLayout topFrame;
    private TimerTask mTask;
    private Timer mTimer;


    TextView blink;
    Dialog mdialog;
    boolean seekplay = true;
    int loopcount = 0;
    int bottomsheetexpand = 0;
    private BottomSheetBehavior mBottomSheetBehavior;
    private WABottomSheetBehavior mBottomSheetBehavior2;
    LinearLayout bottomSheet;
    boolean destroy = false;

    //플레이 리스트 셀렉트 이벤트 버스
    @Subscribe
    public void playlistSelect(PlayListSelect playListSelect) {
        playing = playListSelect.getPosition();
        pager.setCurrentItem(playing,true);
        initViews(MusicService.uriArrayList.get(playing));
        final Handler handler = new Handler();
        //0.2초간 딜레이를 주는이유는 미디어세팅하는데 어느정도 시간을 주기 위해서 안줘도 상관없는데 좀더 부드럽게 진행가능하다
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("aaa233","aaa"+MusicService.isplay);
                if (!MusicService.isplay) {
                    buttonClick(0);
                    Log.d("aaa233333","aaadd");
                }
                handler.removeCallbacks(this);
            }
        },200);

    }

    @Subscribe
    public void setPosition(Position position) {
        //재생관련 버튼 클릭을 캐치하여 뷰 세팅
        Log.d("pospos","pospos");
        int pos = position.getPosition();
        initViews(MusicService.uriArrayList.get(pos));
        pager.setCurrentItem(pos,true);
    }

    @Subscribe
    public void destroy(DestroyBus destroyBus) {
        destroy = destroyBus.isDestroy();
    }

    //노티에서 플레이 스탭 눌럿을시에 메인 화면에서도 플레이스탑 아이콘 동일하게 세팅하는 이벤트 버스
    @Subscribe
    public void PlayStopNoti(NotiPlayStop notiPlayStop) {
        if (notiPlayStop.getPlaystop() == 0) {
            seekplay = false;
            startPlayProgressUpdater();
            topplaystop.setImageResource(R.drawable.pause);
            buttonPlayStop.setImageResource(R.drawable.pause);
        } else {
            seekplay = true;
            topplaystop.setImageResource(R.drawable.playbt);
            buttonPlayStop.setImageResource(R.drawable.playbt);
        }
    }

    //nfc다운로드 관련 이벤트버스
    @Subscribe
    public void downLoadProgress(DownProgress downProgress) {
        //다운로드가 완료되면 뷰를 다시 세팅한다.
        if (downProgress.getComplete() == NFCActivity.DONWSIZE) {

            if (songDataArrayList.size() != 0) {
                Log.d("music", "music2");
                recyclernotify();
                pagerAdapter.notifyDataSetChanged();
                pager.setAdapter(null);
                pager.setAdapter(pagerAdapter);
            } else {
                Log.d("music", "music3");
                initViews(MusicService.uriArrayList.get(MusicService.playing));
                setRecyclerView();
                pagerAdapter.notifyDataSetChanged();
                pager.setAdapter(pagerAdapter);

            }

        }

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BusProvider.getInstance().register(this);
        setContentView(R.layout.mainlayout);
        BusProvider.getInstance().post(new DestroyBus(false));
        mdialog = new Dialog(MainActivity.this);
        mdialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mdialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mdialog.setContentView(R.layout.loading);
        DilatingDotsProgressBar progress = (DilatingDotsProgressBar) mdialog.findViewById(R.id.progress1);
        Intent intent = getIntent();
        String action = intent.getAction();
        init();
        //메인 뷰를 실행하는 루트에 따라서 뷰 세팅 설정을 달리한다. 노티에서 클릭해서 들어왓을시 앱이 그냥 실행되서 순차적으로 들어왓을시를 구분한다.
        if (!TextUtils.isEmpty(action)) {
            if (action.equals(MusicService.ACTION_STARTACTIVITY)) {

                if (MusicService.isplay) {
                    notiinit =0;
                    BusProvider.getInstance().post(new BackgroundSeek(true));
                    initViews(MusicService.uriArrayList.get(MusicService.playing));
                    buttonPlayStop.setImageResource(R.drawable.pause);
                    seekplay = false;
                    startPlayProgressUpdater();
                    topplaystop.setImageResource(R.drawable.pause);
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    topFrame.setVisibility(View.GONE);
                    setRecyclerView();
                    pager.setAdapter(pagerAdapter);
                    pager.setCurrentItem(MusicService.playing);
                } else {
                    initViews(MusicService.uriArrayList.get(MusicService.playing));
                    setRecyclerView();
                    pager.setAdapter(pagerAdapter);
                }
            }
        } else {
            if (!MusicService.notinull){

                getMyAudio();
                try {
                    initViews(MusicService.uriArrayList.get(MusicService.playing));
                    setRecyclerView();
                    pager.setAdapter(pagerAdapter);
                } catch (IndexOutOfBoundsException e) {
                    initViews(null);
                }
            } else {
                try {
                    initViews(MusicService.uriArrayList.get(MusicService.playing));
                    setRecyclerView();
                    pager.setAdapter(pagerAdapter);
                } catch (IndexOutOfBoundsException e) {
                    initViews(null);
                }
            }
        }
    }
    //이미지 슬라이드시 과부하를 막기위해 타임체크를하고 0.5초가 지나면 미디어를 플레이어에 올린다.
    //노티 이닛 변수는 최초 실행시와 현재 노래 재생이 되고있는지등에 따라 구분하기위한 변수이다.
    private void slideTimeCheck(final int position){
        if (mTimer!=null){
            mTimer.cancel();
        }
        mTask = new TimerTask() {
            @Override
            public void run() {
                if (notiinit !=0){
                    BusProvider.getInstance().post(new PlayListSelect(position));
                }
                notiinit =1;
            }
        };

        mTimer = new Timer();

        mTimer.schedule(mTask,500);

    }
    private void init(){
        appname = (LetterSpacingTextView) findViewById(R.id.appname);
        appname.setText("보유 앨범");
        appname.setLetterSpacing(3f);
        typeface = Typeface.createFromAsset(getApplicationContext().getAssets(),"나눔고딕Bold.ttf");
        appname.setTypeface(typeface);
        nfctag = (RelativeLayout) findViewById(R.id.nfctag);
        empty2 = (ImageView) findViewById(R.id.empty2);
        empty = (RelativeLayout) findViewById(R.id.empty);
        recyclerView = (RecyclerView) findViewById(R.id.marketitem);
        toolbar = (RelativeLayout) findViewById(R.id.toolbar);
        bottomSheet = (LinearLayout) findViewById(R.id.bottom_sheet);
        looping = (ImageView) bottomSheet.findViewById(R.id.looping);
        loopingClick = (RelativeLayout) bottomSheet.findViewById(R.id.loopingclick);
        topFrame = (FrameLayout) bottomSheet.findViewById(R.id.topframe);
        topcover = (ImageView) bottomSheet.findViewById(R.id.topcover);
        topartist = (TextView) bottomSheet.findViewById(R.id.topartist);
        topsong = (TextView) bottomSheet.findViewById(R.id.topsong);

        topplaystop = (ImageView) bottomSheet.findViewById(R.id.playstoptop);
        topforward = (ImageView) bottomSheet.findViewById(R.id.forwardtop);
        topClick = (RelativeLayout) bottomSheet.findViewById(R.id.topclick);
        menu = (ImageView) bottomSheet.findViewById(R.id.menu);
        buttonPlayStop = (ImageView) bottomSheet.findViewById(R.id.playbt);
        maxtime = (TextView) bottomSheet.findViewById(R.id.mediamaxtime);
        seekBar = (SeekBar) bottomSheet.findViewById(R.id.seekbar);
        lyricView = (LyricView) bottomSheet.findViewById(R.id.custom_lyric_view);
        lyricback = (ImageView) bottomSheet.findViewById(R.id.lyricback);
        lyricvisible = (TextView) bottomSheet.findViewById(R.id.lyricvisiblebt);
        topseek = (SeekBar) bottomSheet.findViewById(R.id.topseek);
        hide = (RelativeLayout) bottomSheet.findViewById(R.id.hide);
        currenttime = (TextView) bottomSheet.findViewById(R.id.mediacurrnettime);
        View include = bottomSheet.findViewById(R.id.sub);
        subject = (TextView) include.findViewById(R.id.subject);
        artist = (TextView) include.findViewById(R.id.artist);
        pager = (ViewPager) bottomSheet.findViewById(R.id.coverpager);
        forward = (RelativeLayout) bottomSheet.findViewById(R.id.forward);
        reverse = (RelativeLayout) bottomSheet.findViewById(R.id.reverse);
        menu2 = (ImageView) bottomSheet.findViewById(R.id.menu2);
        shuffle = (ImageView) bottomSheet.findViewById(R.id.shuffle);
        shuffleClick = (RelativeLayout) bottomSheet.findViewById(R.id.shuffleclick);
        //increaseClickArea(bottomSheet,seekBar);
        //뷰페이저리스너는 intview에 넣을경우 계속해서 더해지기때문에 중복방지를 위해 inti에 넣음
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                //이미지 슬라이드 체크후 다음곡 전곡으로 이동
                slideTimeCheck(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //이미지 슬라이딩 도중 바텀시트가 내려가는것을 막음
                if (state == SCROLL_STATE_DRAGGING){
                    mBottomSheetBehavior2.setAllowUserDragging(false);
                } else {
                    mBottomSheetBehavior2.setAllowUserDragging(true);
                    notiinit =1;
                }
            }
        });
    }
    private void initViews(Uri uri) {
        if (MusicService.uriArrayList.size()==0 || MusicService.uriArrayList ==null){
            recyclerView.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
            Glide.with(getApplicationContext()).load(R.drawable.markeyempty1)
                    .into(empty2);
            arraynull =0;
        }
        nfctag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NFCActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                if (MusicService.isplay) {
                    buttonClick(0);
                }
            }
        });
        mBottomSheetBehavior = WABottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior2 = (WABottomSheetBehavior) mBottomSheetBehavior;
        mBottomSheetBehavior2.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                //ㅁ바텀시트의 스크롤 정도에따라 하단 미니플레이어 알파값을 조절하여 뷰에서 사라지고 보이고 하는 토글형식의 애니메이션
                if (slideOffset >= 0.5f && slideOffset < 1) {
                    topFrame.setAlpha(slideOffset - 1);
                    topFrame.setVisibility(View.VISIBLE);
                } else if (slideOffset == 1) {
                    bottomsheetexpand = 1;
                    topFrame.setVisibility(View.GONE);
                } else if (slideOffset > 0 && slideOffset <= 0.5f) {
                    topFrame.setAlpha(1 - slideOffset);
                    topFrame.setVisibility(View.VISIBLE);
                } else if (slideOffset == 0) {
                    bottomsheetexpand = 0;
                    topFrame.setVisibility(View.VISIBLE);
                }
            }
        });
        //가사뷰에 비해이비어를 넣어준 이유는 가사뷰의 스크롤할때 바텀시트가 내려가지 못하도록 세팅
        lyricView.setBehavior(mBottomSheetBehavior2);
        topplaystop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClick(0);
            }
        });

        topforward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClick(1);
            }
        });

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PlayListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        menu2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PlayListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        topClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        if (uri!=null){
            shuffleClick.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //셔플 기능
                    if (!MusicService.shuffle){
                        shuffle.setSelected(true);
                        MusicService.shuffle= true;
                    } else {
                        shuffle.setSelected(false);
                        MusicService.shuffle= false;
                    }
                }
            });
            loopingClick.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //곡 반복재생 설정 한곡 전체 취소
                    if (loopcount == 0) {
                        loopcount = 1;
                        BusProvider.getInstance().post(new Looping(1));
                        looping.setImageResource(R.drawable.replay2);
                    } else if (loopcount == 1) {
                        loopcount = 2;
                        BusProvider.getInstance().post(new Looping(2));
                        looping.setImageResource(R.drawable.replay3);
                    } else if (loopcount == 2) {
                        loopcount = 0;
                        BusProvider.getInstance().post(new Looping(0));
                        looping.setImageResource(R.drawable.replay);
                    }
                }
            });
        }
        //버튼 클릭 이벤트를 버튼클릭 메소드의 int형 인자를 넘겨주어 클릭이벤트 구분
        buttonPlayStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClick(0);
            }
        });

        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClick(1);
            }
        });

        reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClick(-1);

            }
        });
        buttonPlayStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonClick(0);
            }
        });


        currenttime.setText(starttime);
        //mp3파일의 음원정보 세팅 앨범커버,아티스트,제목 등등
        try {
            metaRetriver.setDataSource(getApplicationContext(), uri);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        try {
            if (uri !=null){
                lyricback.setVisibility(View.GONE);
                artist.setVisibility(View.VISIBLE);
                topartist.setVisibility(View.VISIBLE);
                art = metaRetriver.getEmbeddedPicture();
                if (art !=null){
                    Glide.with(getApplicationContext()).load(art)
                            .into(topcover);
                } else {
                    Glide.with(getApplicationContext()).load(R.drawable.nocover)
                            .into(topcover);
                }
                String title = metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                String nullTitle = "알 수 없는 음악";
                String songArtist  = metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                String nullSongArtist = "알 수 없는 음악가";
                if (title !=null){
                    subject.setText(title);
                    topartist.setText(title);
                } else {
                    subject.setText(nullTitle);
                    topartist.setText(nullTitle);
                }
                if (songArtist!=null){
                    artist.setText(songArtist);
                    topsong.setText(songArtist);
                } else {
                    artist.setText(nullSongArtist);
                    topsong.setText(nullSongArtist);
                }
                Glide.with(getApplicationContext()).load(art)
                        .bitmapTransform(new BlurTransformation(getApplicationContext()))
                        .into(lyricback);

            } else {
                subject.setText("앨범을 추가해 주세요");
                artist.setVisibility(View.GONE);
                topartist.setVisibility(View.GONE);
                topsong.setText("재생 중인 곡이 없습니다");
                lyricback.setVisibility(View.VISIBLE);
                Glide.with(getApplicationContext()).load(R.drawable.nocover)
                        .into(lyricback);
                Glide.with(getApplicationContext()).load(R.drawable.nocover)
                        .into(topcover);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //가사 보였다 안보이게하는 토글버튼 이벤트
        lyricvisible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lyricView.getVisibility() == View.GONE) {
                    lyricView.setVisibility(View.VISIBLE);
                    lyricback.setVisibility(View.VISIBLE);
                } else {
                    lyricView.setVisibility(View.GONE);
                    lyricback.setVisibility(View.GONE);

                }
            }
        });
        //가사 배열에서 가사 파일을 불러와 가사뷰에 세팅
        try {
            Uri lrcuri = MusicService.lrclist.get(playing);
            File file = new File(lrcuri.getPath());
            lyricView.setLyricFile(file, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        topseek.getThumb().mutate().setAlpha(0);
        //그지같은 헬쥐 때문에 하단 플레이서 뷰 디스플레이 가로세로의 비율에 맞게 직접 세팅
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            int padding = (int) convertDpToPixel(12);
            int height = (int) convertDpToPixel(5);
            int heightframe = (int) convertDpToPixel(57);
            seekBar.setPadding(0,padding,0,padding);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,height);
            RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,heightframe);
            topFrame.setLayoutParams(layoutParams1);
            mBottomSheetBehavior.setPeekHeight(heightframe);
            topseek.setLayoutParams(layoutParams);
        }
        //시크바 곡 시간 정보 세팅
        topseek.setMax(MusicService.duration);
        seekBar.setMax(MusicService.duration);
        maxtime.setText(milliSecondsToTimer((long) MusicService.duration));
        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBottomSheetBehavior.getState()==BottomSheetBehavior.STATE_EXPANDED){
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });
        subject.setSelected(true);
        subject.setHorizontallyScrolling(true);
        topseek.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        //시크바 이동 이벤트를 캐치하여 노래 타이밍 이동
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(seekBar.getProgress());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d("clickint", String.valueOf(seekBar.getProgress()));
                BusProvider.getInstance().post(new ClickInt(seekBar.getProgress()));

                //seekBar.setProgress(seekBar.getProgress());
            }
        });

    }

    public void recyclernotify() {
        songDataArrayList.clear();
        recyclerdata();
        adapter.notifyDataSetChanged();
    }

    //마켓부분 mp3파일 데이터정보를 통해 뷰세팅
    public void recyclerdata() {
        if (MusicService.uriArrayList.size() != 0) {
            MusicService.bytes.clear();
            Log.d("sizeis",MusicService.uriArrayList.size()+"");
            for (int i = 0; i < MusicService.uriArrayList.size(); i++) {
                SongData songData = new SongData();
                MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                try{
                    metadataRetriever.setDataSource(getApplicationContext(), MusicService.uriArrayList.get(i));
                    songData.setArtist(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
                    songData.setCover(metadataRetriever.getEmbeddedPicture());
                    songData.setTitle(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
                    String time = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    art = metadataRetriever.getEmbeddedPicture();
                    if (time == null){
                        songData.setDuration(milliSecondsToTimer(0));
                    } else {
                        songData.setDuration(milliSecondsToTimer(Long.parseLong(time)));
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
                songDataArrayList.add(songData);
                MusicService.bytes.add(art);

            }
            pagerAdapter = new ViewPagerAdapter(getApplicationContext(),MusicService.bytes);
            pagerAdapter.notifyDataSetChanged();
        }
    }

    public void setRecyclerView() {
        recyclerdata();
        adapter = new MarketAdapter(getApplicationContext(), songDataArrayList);
        recyclerView.setLayoutManager(new GridLayoutManager(getApplicationContext(), 2));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(getApplicationContext(), R.dimen.item_offset);
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setAdapter(adapter);
        recyclerView.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
    }

    public void getMyAudio() {
        try {
            String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
            sdPath += "/Solly/";
            File directory = new File(sdPath);

//특정 directory 내 파일 목록 가져오기
            File[] files = directory.listFiles();

            for (File file : files) {

//파일이 directory 가 아닌 file 일때
                if (!file.isFile()) {
//file 명 console 창에 뿌려주기
                    File[] files1 = file.listFiles();
                    for (File file1 : files1) {
                        if (file1.isFile()) {
                            String filename = file1.getName();
                            int pos = filename.lastIndexOf(".");

                            String ext = filename.substring(pos + 1);
                            Uri uri = Uri.fromFile(file1);
                            if (ext.equals("lrc")) {
                                MusicService.lrclist.add(uri);
                            } else {
                                MusicService.uriArrayList.add(uri);
                                try {
                                    metaRetriver.setDataSource(getApplicationContext(), uri);
                                } catch (IllegalArgumentException ignored) {
                                }
                                art = metaRetriver.getEmbeddedPicture();
                                MusicService.bytes.add(art);
                            }
                        }
                    }
                }
                startService(new Intent(this, MusicService.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        pagerAdapter = new ViewPagerAdapter(getApplicationContext(),MusicService.bytes);
    }
    //1초단위 스레드를 통한 타임바 시간 현재 시간 세팅
    public void startPlayProgressUpdater() {
        lyricView.setCurrentTimeMillis(MusicService.curpos);
        seekBar.setProgress(MusicService.curpos);
        seekBar.setMax(MusicService.duration);
        topseek.setProgress(MusicService.curpos);
        topseek.setMax(MusicService.duration);
        currenttime.setText(milliSecondsToTimer((long) MusicService.curpos));
        maxtime.setText(milliSecondsToTimer((long) MusicService.duration));
        Log.d("playplay", String.valueOf(MusicService.curpos));
        if (!seekplay) {
            Runnable notification = new Runnable() {
                public void run() {

                    startPlayProgressUpdater();
                }
            };
            handler.postDelayed(notification, 1000);
        } else {
            seekBar.setProgress(MusicService.curpos);
        }
    }
    //디피를 픽셀로 바꾸는 메소드
    public float convertDpToPixel(float dp){
        Resources resources = getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    //버튼 클릭 이벤트
    private void buttonClick(int a) {
        if (MusicService.uriArrayList.size() != 0) {
            if (a == 0) {
                //플레이 스탑 버튼 눌럿을시 노래가 재생되고있으면 멈추고 아니면 재생시킨다
                notiinit =1;
                //BusProvider.getInstance().post(new ClickInt(0));
                Intent intent = new Intent(this, MusicService.class);
                intent.setAction(MusicService.ACTION_PLAY);
                intent.setPackage("com.artbating.sollymusic");

                if (!MusicService.isplay) {
                    try {
                        intent.putExtra(MusicService.ACTION_PLAY, MusicService.ACTION_PLAY);
                        startService(intent);
                        seekplay = false;
                        topplaystop.setImageResource(R.drawable.pause);
                        buttonPlayStop.setImageResource(R.drawable.pause);
                        startPlayProgressUpdater();
                    } catch (IllegalStateException e) {
                        topplaystop.setImageResource(R.drawable.playbt);
                        buttonPlayStop.setImageResource(R.drawable.playbt);
                    }
                } else {
                    intent.putExtra(MusicService.ACTION_PLAY, MusicService.ACTION_PAUSE);
                    startService(intent);
                    seekplay = true;
                    topplaystop.setImageResource(R.drawable.playbt);
                    buttonPlayStop.setImageResource(R.drawable.playbt);
                }
            } else if (a == -1) {
                //전곡으로 이동 전곡으로 이동 눌럿을시 현재진행 시간이 3초이내면 전곡으로 바로넘어가고 3초후라면 전곡으로 넘어가지않고 현재곡의 처음부분으로 이동한다.
                Intent intent = new Intent(this, MusicService.class);
                intent.setAction(MusicService.ACTION_REVERSE);
                intent.setPackage("com.artbating.sollymusic");
                startService(intent);
               if (!MusicService.shuffle){
                   if (playing >= 1) {
                       int seconds = (int) (((long) MusicService.curpos % (1000 * 60 * 60)) % (1000 * 60) / 1000);
                       if (seconds < 3 || currenttime.getText().toString().equals(starttime)) {
                           playing--;
                           if (!MusicService.isplay) {
                               seekplay = false;
                               startPlayProgressUpdater();
                               topplaystop.setImageResource(R.drawable.pause);
                               buttonPlayStop.setImageResource(R.drawable.pause);
                           }


                       } else {
                           seekBar.setProgress(0);
                       }
                   } else {
                       int seconds = (int) (((long) MusicService.curpos % (1000 * 60 * 60)) % (1000 * 60) / 1000);
                       if (seconds < 3 || currenttime.getText().toString().equals(starttime)) {
                           if (!MusicService.isplay) {
                               seekplay = false;
                               startPlayProgressUpdater();
                               topplaystop.setImageResource(R.drawable.pause);
                               buttonPlayStop.setImageResource(R.drawable.pause);
                           }
                           playing = MusicService.uriArrayList.size() - 1;

                       } else {
                           seekBar.setProgress(0);
                       }

                   }
               } else {
                   int seconds = (int) (((long) MusicService.curpos % (1000 * 60 * 60)) % (1000 * 60) / 1000);
                   if (seconds < 3 || currenttime.getText().toString().equals(starttime)) {
                       playing = MusicService.shuffle();
                       if (!MusicService.isplay) {
                           seekplay = false;
                           startPlayProgressUpdater();
                           topplaystop.setImageResource(R.drawable.pause);
                           buttonPlayStop.setImageResource(R.drawable.pause);
                       }


                   } else {
                       seekBar.setProgress(0);
                   }
               }
            } else if (a == 1) {
                //다음곡으로 이동 셔플기능이 켜져있다면 랜덤하게 재생
                Intent intent = new Intent(this, MusicService.class);
                intent.setAction(MusicService.ACTION_FORWARD);
                intent.setPackage("com.artbating.sollymusic");
                startService(intent);
                if (!MusicService.shuffle){
                    if (playing <= MusicService.uriArrayList.size() - 2) {
                        playing++;
                        if (!MusicService.isplay) {
                            seekplay = false;
                            startPlayProgressUpdater();
                        }
                        topplaystop.setImageResource(R.drawable.pause);
                        buttonPlayStop.setImageResource(R.drawable.pause);
                    } else {
                        playing = 0;
                        if (!MusicService.isplay) {
                            Log.d("false", "false");
                            seekplay = false;
                            startPlayProgressUpdater();
                        }
                        Log.d("false", "false");

                        topplaystop.setImageResource(R.drawable.pause);
                        buttonPlayStop.setImageResource(R.drawable.pause);

                    }
                } else {
                    playing=MusicService.shuffle();
                    if (!MusicService.isplay) {
                        seekplay = false;
                        startPlayProgressUpdater();
                    }
                    topplaystop.setImageResource(R.drawable.pause);
                    buttonPlayStop.setImageResource(R.drawable.pause);
                }
            }
        }
    }
    //유닉스스탬프 형태의 시간정보를 보편적인 시간정보로 바꿈
    public String milliSecondsToTimer(long milliseconds) {
        String finalTimerString = "";
        String secondsString = "";

        // Convert total duration into time
        int hours = (int) (milliseconds / (1000 * 60 * 60));
        int minutes = (int) (milliseconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliseconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        // Add hours if there
        if (hours > 0) {
            finalTimerString = hours + ":";
        }

        // Prepending 0 to seconds if it is one digit
        if (seconds < 10) {
            secondsString = "0" + seconds;
        } else {
            secondsString = "" + seconds;
        }

        finalTimerString = finalTimerString + minutes + ":" + secondsString;

        // return timer string
        return finalTimerString;
    }

    @Override
    protected void onDestroy() {
        BusProvider.getInstance().post(new DestroyBus(true));
        handler.removeCallbacks(null);
        seekplay = true;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (back_pressed + TIME_DELAY > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            if (bottomsheetexpand == 1) {
                back_pressed = 0;
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                Toast.makeText(getBaseContext(), "뒤로가기를 한번 더 누르시면 종료됩니다.",
                        Toast.LENGTH_SHORT).show();
                back_pressed = System.currentTimeMillis();
            }
        }

    }

}
