package com.artbating.solly;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.os.EnvironmentCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
import static android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;

/**
 * Created by kim on 2017. 3. 21..
 */

public class MusicService extends Service {
    static final String ACTION_PLAY = "PLAY";
    static final String ACTION_PAUSE = "PAUSE";
    static final String ACTION_REVERSE = "REVERSE";
    static final String ACTION_FORWARD = "FORWARD";
    static final String ACTION_CLOSE = "CLOSE";
    static final String ACTION_STARTACTIVITY = "START";
    TelephonyManager mTelMan;
    static int playing = 0;
    static boolean notinull = false;
    AudioManager mAudioManager = null;
    private DatabaseReference mDatabase;
    long pauseTime =0;
    private ArrayList<String> list = new ArrayList<>();
    static ArrayList<String> fileArrayList = new ArrayList<>();
    static ArrayList<String> lrcfileArrayList = new ArrayList<>();
    static ArrayList<Uri> uriArrayList = new ArrayList<>();
    static ArrayList<Uri> lrclist = new ArrayList<>();
    static ArrayList<byte[]> bytes = new ArrayList<>();
    static int firstStrart= 0;
    MediaPlayer mediaPlayer;
    static int duration = 0;
    static int curpos = 0;
    static boolean isplay = false;
    static boolean shuffle = false;
    boolean notiStartPause = true;
    Handler handler = new Handler();
    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
    static boolean destory = false;
    int loopingcount = 0;
    int a = 0;
    private TimerTask mTask;
    private Timer mTimer;

// instantiate it within the onCreate method

    @Subscribe
    public void backSeek(BackgroundSeek backgroundSeek) {
        destory = false;
        startPlayProgressUpdater();
    }
    @Subscribe
    public void playlistSelect(final PlayListSelect playListSelect){
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                playing=playListSelect.getPosition();
                if (mediaPlayer.isPlaying()) {
                    isplay = false;
                    mediaPlayer.stop();
                }
                initMedia();
                noti();
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mediaPlayer.start();
                        if (!isplay) {
                            startPlayProgressUpdater();
                        }
                        isplay = true;
                    }
                });
                handler.removeCallbacks(this);
            }
        },300);

    }
    @Subscribe
    public void FinishLoad(ClickInt clickInt) {
        mediaPlayer.seekTo(clickInt.getClick());
        curpos = clickInt.getClick();
    }
    //메인액티비티 종료 이벤트 버스
    @Subscribe
    public void activitiyDes(DestroyBus destroyBus) {
        destory = destroyBus.isDestroy();
        //메인 액티비티가 종료되었을시 noti가살아있으면 백그라운드 유지하고 죽어있으면 앱전체를 종료하면서 캐시데이터를 삭제한다.
        Log.d("stop1", String.valueOf(notinull));
        if (!notinull) {
            Log.d("stop", String.valueOf(notinull));
            try {
                trimCache(this);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            stopSelf();
        }
    }

    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    //한곡 반복이벤트 받기
    @Subscribe
    public void looping(Looping looping) {
        loopingcount = looping.isLooping();
        if (looping.isLooping() == 0) {
            mediaPlayer.setLooping(false);
        } else if (looping.isLooping() == 1) {
            mediaPlayer.setLooping(false);
        } else if (looping.isLooping() == 2) {
            mediaPlayer.setLooping(true);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("create", "create");
        BusProvider.getInstance().register(this);
        //앱최초 실행시에만 미디어를 세팅한다.
        if (firstStrart ==1){
            initMedia();
        }
    }
    //백그라운드에서 메인 액비티로 현재 음악 재생시간을 넘겨주기 위한 메소드 스태틱메소드를 통하여 공유한다.
    public void startPlayProgressUpdater() {
        MusicService.curpos = mediaPlayer.getCurrentPosition();
        MusicService.duration = mediaPlayer.getDuration();
        if (mediaPlayer.isPlaying()) {
            Runnable notification = new Runnable() {
                public void run() {
                    if (destory) {

                        if (!notinull) {
                            handler.removeCallbacks(this);
                        }
                    } else {

                        startPlayProgressUpdater();
                    }
                }
            };
            handler.postDelayed(notification, 500);
        } else {
            MusicService.duration = mediaPlayer.getDuration();
            MusicService.curpos = mediaPlayer.getCurrentPosition();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.print(intent);
        if (intent != null) {
            String action = intent.getAction();
            if (!TextUtils.isEmpty(action)) {
                if (action.equals(ACTION_PLAY)) {
                    //플레이 버튼이 눌렷을때
                    String playpause = intent.getStringExtra(ACTION_PLAY);
                    if (!TextUtils.isEmpty(playpause)) {
                        //메인액티비티에서 눌럿을시
                        if (playpause.equals(ACTION_PLAY)) {
                            notiStartPause = true;
                            //플레이를 시작하면서 오디오 포커스를 단말기에 요청하고 노티가 실행된 구분변수 불리언 형태를 true로 세팅한다.
                            //노티를 실행하고 버튼클릭이벤트 실행 .. 메인액티비티 클릭이벤트와 동일하게 백그라운드에서 미디어플레이어 세팅
                            if (!mediaPlayer.isPlaying()) {
                                requsetAudioFocus();
                                mediaPlayer.start();
                            }
                            noti();
                            buttonClick(0);
                        } else {
                            //플레이어가 실행되고 있엇다면 멈춤
                            notiStartPause = false;
                            buttonClick(0);
                            noti();
                        }
                    } else {
                        //노티에서 플레이 눌럿을시
                        if (notiStartPause) {
                            buttonClick(0);
                            notiStartPause = false;
                            noti();
                            BusProvider.getInstance().post(new NotiPlayStop(1));
                        } else {
                            notiStartPause = true;
                            if (!mediaPlayer.isPlaying()) {
                                requsetAudioFocus();
                            }

                            buttonClick(0);
                            noti();
                            BusProvider.getInstance().post(new NotiPlayStop(0));
                        }
                    }
                } else if (action.equals(ACTION_REVERSE)) {
                    notiStartPause = true;
                    buttonClick(-1);
                    BusProvider.getInstance().post(new Position(playing));
                    noti();
                } else if (action.equals(ACTION_FORWARD)) {
                    notiStartPause = true;
                    buttonClick(1);
                    BusProvider.getInstance().post(new Position(playing));
                    noti();
                } else if (action.equals(ACTION_CLOSE)) {
                    //액티비티가 죽었다면 백그라운드도 종료시키고 액티비티가 살아있다면 노티만 없애버린다.
                    if (!destory) {
                        notiStartPause = false;
                        BusProvider.getInstance().post(new NotiPlayStop(1));
                        mediaPlayer.pause();
                        stopForeground(true);
                        NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
                        nMgr.cancel(1010);
                        notinull = false;
                        isplay =false;
                    } else {
                        stopSelf();
                    }
                }
            }
        }
        return MusicService.START_STICKY;
    }

    @Override
    public void onDestroy() {

        try {
            if (mediaPlayer.isPlaying()) {

                handler.removeCallbacks(null);
                mediaPlayer.stop();
                mediaPlayer.release();
            }
            mediaPlayer.release();
            android.os.Process.killProcess(android.os.Process.myPid());
        }catch (Exception ignored){
            Log.d("destroy", "destroy2");
        }
        super.onDestroy();
    }

    private void buttonClick(int a) {
        if (a == 0) {
            if (!isplay) {
                try {
                    mediaPlayer.start();
                    requsetAudioFocus();
                    startPlayProgressUpdater();
                    isplay = true;
                } catch (IllegalStateException e) {
                    mediaPlayer.pause();
                }
            } else {
                isplay = false;
                mediaPlayer.pause();
            }
        } else if (a == -1) {
            if (!shuffle){
                if (playing >= 1) {
                    int seconds = (int) (((long) mediaPlayer.getCurrentPosition() % (1000 * 60 * 60)) % (1000 * 60) / 1000);
                    if (seconds < 3) {
                        playing--;
                        if (mediaPlayer.isPlaying()) {
                            isplay = false;
                            mediaPlayer.stop();
                        }
                        initMedia();
                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mediaPlayer.start();
                                if (!isplay){
                                    startPlayProgressUpdater();
                                }
                                isplay = true;
                            }
                        });
                    } else {
                        mediaPlayer.seekTo(0);
                    }
                } else {
                    int seconds = (int) (((long) mediaPlayer.getCurrentPosition() % (1000 * 60 * 60)) % (1000 * 60) / 1000);
                    if (seconds < 3) {
                        playing= uriArrayList.size()-1;
                        if (mediaPlayer.isPlaying()) {
                            isplay = false;
                            mediaPlayer.stop();
                        }
                        initMedia();
                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mediaPlayer.start();
                                if (!isplay){
                                    startPlayProgressUpdater();
                                }
                                isplay = true;
                            }
                        });
                    } else {
                        mediaPlayer.seekTo(0);
                    }
                }
            } else {
                int seconds = (int) (((long) mediaPlayer.getCurrentPosition() % (1000 * 60 * 60)) % (1000 * 60) / 1000);
                if (seconds < 3) {
                    playing = shuffle();
                    if (mediaPlayer.isPlaying()) {
                        isplay = false;
                        mediaPlayer.stop();
                    }
                    initMedia();
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mediaPlayer.start();
                            if (!isplay){
                                startPlayProgressUpdater();
                            }
                            isplay = true;
                        }
                    });
                } else {
                    mediaPlayer.seekTo(0);
                }
            }
        } else if (a == 1) {
            if (!shuffle){
                if (playing <= uriArrayList.size()-2) {
                    playing++;
                    if (mediaPlayer.isPlaying()) {
                        isplay = false;
                        mediaPlayer.stop();
                    }
                    initMedia();
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mediaPlayer.start();
                            if (!isplay){
                                startPlayProgressUpdater();
                            }
                            isplay = true;
                        }
                    });
                } else {
                    playing=0;
                    if (mediaPlayer.isPlaying()) {
                        isplay = false;
                        mediaPlayer.stop();
                    }
                    initMedia();
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mediaPlayer.start();
                            if (!isplay){
                                startPlayProgressUpdater();
                            }
                            isplay = true;
                        }
                    });
                }
            } else {
                playing = shuffle();
                if (mediaPlayer.isPlaying()) {
                    isplay = false;
                    mediaPlayer.stop();
                }
                initMedia();
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mediaPlayer.start();
                        if (!isplay){
                            startPlayProgressUpdater();
                        }
                        isplay = true;
                    }
                });
            }
        }
    }
    public static int shuffle(){
        Random rand = new Random();
        int min, max;
        min =0;
        max = uriArrayList.size();
        return rand.nextInt(max - min);
    }
    //오디오 포커스 변화에 따른 미디어플레이어 세팅
    AudioManager.OnAudioFocusChangeListener afChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        //다른앱애서 오디오 포커스 요청이 왔을때 음악을 멈춘다.
                        //이때 타이머 테스크를통하여 오디오 포커스가 잃은지 10초 이상이 흘르면 미디어자체를 해지시킨다.
                        try {
                            if (mediaPlayer.isPlaying()) {
                                mediaPlayer.pause();
                                buttonClick(0);
                                notiStartPause = false;
                                noti();
                                pauseTime = System.currentTimeMillis();
                                BusProvider.getInstance().post(new NotiPlayStop(1));
                                mTask = new TimerTask() {
                                    @Override
                                    public void run() {

                                        mediaPlayer.stop();
                                        mediaPlayer.release();
                                        mediaPlayer = null;
                                    }
                                };
                                mTimer = new Timer();
                                mTimer.schedule(mTask,10000);

                            }
                        } catch (Exception ignored){}
                    } else if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT) {
                        //오디오 포커스를 잠시동안 잃을때 퍼즈시킨다.
                        if (mediaPlayer.isPlaying()) {

                            mediaPlayer.pause();
                        }
                        // Pause playback
                    } else if (focusChange == AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        //알람같은것이 울렷을때 볼륨을 살짝 줄엿다가 다시높힌다.
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.setVolume(0.1f,0.1f);
                        }
                        // Lower the volume, keep playing
                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        //오디오 포커스를 가져왔을때 노티가 죽어잇다면 다시새로 미디어를 세팅하고 미디어가 실행중이아니라면 다시 재생시킨다.
                        if (notinull){
                            if (mediaPlayer ==null){

                                initMedia();
                            }else if (!mediaPlayer.isPlaying()){
                                if (notiStartPause){
                                    if (mTimer !=null){

                                        mTimer.cancel();
                                    }

                                    mediaPlayer.seekTo(curpos);
                                    mediaPlayer.start();
                                    mediaPlayer.setVolume(1f,1f);
                                    notiStartPause= true;
                                    noti();
                                    buttonClick(0);
                                }
                            } else if (mediaPlayer.isPlaying()){
                                mediaPlayer.setVolume(1f,1f);
                            }
                        }

                        // Your app has been granted audio focus again
                        // Raise volume to normal, restart playback if necessary
                    }
                }
            };
//오디오포커스를 요청한다.
    void requsetAudioFocus() {
        if (mTimer !=null){

            mTimer.cancel();
        }
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }
//미디어 세팅
    public void initMedia() {
        firstStrart = 0;
        try {
            duration = 0;
            curpos = 0;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.reset();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(getApplicationContext(), uriArrayList.get(playing));
            mediaPlayer.prepare();
            duration = mediaPlayer.getDuration();
            curpos = mediaPlayer.getCurrentPosition();
            requsetAudioFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                Log.d("ready", "ready");

            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {

                return false;
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (!shuffle){
                    if (playing == uriArrayList.size()-1) {
                        if (loopingcount == 1) {
                            playing = 0;
                            initMedia();
                            mediaPlayer.start();
                            noti();
                            BusProvider.getInstance().post(new Position(playing));
                        } else {
                            mediaPlayer.seekTo(0);

                            notiStartPause=false;
                            buttonClick(0);
                            noti();
                            BusProvider.getInstance().post(new NotiPlayStop(1));
                            isplay =false;
                        }
                    } else {
                        playing++;
                        initMedia();
                        BusProvider.getInstance().post(new Position(playing));
                        mediaPlayer.start();
                        noti();
                    }
                } else {
                    playing= shuffle();
                    initMedia();
                    BusProvider.getInstance().post(new Position(playing));
                    mediaPlayer.start();
                    noti();
                }
            }
        });


    }
//노티피케이션 세팅
    public void noti() {
        notinull = true;
        RemoteViews remoteViews = new RemoteViews(getPackageName(),
                R.layout.notilayout);
        Notification notification = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.notilogoicon)
                .setContentTitle("Solly").setCustomContentView(remoteViews).build();
        pendingIntent(remoteViews);
        try{
            remoteViews.setImageViewBitmap(R.id.albumcover, getbitmapUri(uriArrayList.get(playing)));
            remoteViews.setTextViewText(R.id.artist, artist(uriArrayList.get(playing)));
            remoteViews.setTextViewText(R.id.songname, songName(uriArrayList.get(playing)));
        } catch (NullPointerException e){
            remoteViews.setImageViewResource(R.id.albumcover, R.drawable.nocover);
            remoteViews.setTextViewText(R.id.artist, "알수없는 음악가");
            remoteViews.setTextViewText(R.id.songname, "Unknown");
        }

        if (!notiStartPause) {
            remoteViews.setImageViewResource(R.id.playstop, R.drawable.playbt);
        } else {
            remoteViews.setImageViewResource(R.id.playstop, R.drawable.pause);
        }
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        startForeground(1010, notification);
    }
//노피피케이션에 들어갈 각각의 컨텐츠들 세팅
    public Bitmap getbitmapUri(Uri uri) {
        mediaMetadataRetriever.setDataSource(getApplicationContext(), uri);
        byte[] a = mediaMetadataRetriever.getEmbeddedPicture();
        return BitmapFactory.decodeByteArray(a, 0, a.length);
    }

    public String artist(Uri uri) {
        mediaMetadataRetriever.setDataSource(getApplicationContext(), uri);
        return mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
    }

    public String songName(Uri uri) {
        mediaMetadataRetriever.setDataSource(getApplicationContext(), uri);
        return mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
    }
//노피케이션 버튼 클릭 이벤트
    public void pendingIntent(RemoteViews v) {
        Intent startstop = new Intent(this, MusicService.class);
        startstop.setAction(MusicService.ACTION_PLAY);
        startstop.setPackage("com.artbating.solly");
        PendingIntent pStartStop = PendingIntent.getService(getApplicationContext(), 1, startstop, PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.playstop, pStartStop);


        Intent reverse = new Intent(this, MusicService.class);
        reverse.setAction(MusicService.ACTION_REVERSE);
        reverse.setPackage("com.artbating.solly");
        PendingIntent pReverse = PendingIntent.getService(getApplicationContext(), 1, reverse, PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.reverse, pReverse);

        Intent forward = new Intent(this, MusicService.class);
        forward.setAction(MusicService.ACTION_FORWARD);
        forward.setPackage("com.artbating.solly");
        PendingIntent pForward = PendingIntent.getService(getApplicationContext(), 1, forward, PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.forward, pForward);

        Intent close = new Intent(this, MusicService.class);
        close.setAction(MusicService.ACTION_CLOSE);
        close.setPackage("com.artbating.solly");
        PendingIntent pClose = PendingIntent.getService(getApplicationContext(), 1, close, PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.close, pClose);

        Intent startactivity = new Intent(this, MainActivity.class);
        startactivity.setPackage("com.artbating.solly");
        startactivity.setAction(ACTION_STARTACTIVITY);
        PendingIntent pStartAct = PendingIntent.getActivity(getApplicationContext(), 1, startactivity, PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.albumcover, pStartAct);

        PendingIntent pStartAct2 = PendingIntent.getActivity(getApplicationContext(), 1, startactivity, PendingIntent.FLAG_UPDATE_CURRENT);
        v.setOnClickPendingIntent(R.id.content, pStartAct2);
    }


    @Override
    public boolean stopService(Intent name) {
        Log.d("stop", "stop");
        //백그라운드가 죽을시 노티를 없애고 미디어플레이러를 해지시키며 백그라운드를 종료시킨다.
        NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancel(1010);
        mediaPlayer.stop();
        mediaPlayer.release();
        return super.stopService(name);
    }


}
