package com.artbating.solly;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import pl.bclogic.pulsator4droid.library.PulsatorLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by kim on 2017. 3. 26..
 */

public class NFCActivity extends AppCompatActivity {
    static int A = 0;
    ImageView signal, tag4, tag3;
    static int DONWSIZE = 0;
    EditText keyvalue;
    Button get;
    DownloadlrcTask downloadlrcTask;
    DownloadTask downloadTask;
    TextView g2;
    final Handler handler = new Handler();
    NfcAdapter mNfcAdapter; // NFC 어댑터
    PendingIntent mPendingIntent; // 수신받은 데이터가 저장된 인텐트
    IntentFilter[] mIntentFilters; // 인텐트 필터
    String[][] mNFCTechLists;
    VerticalProgressBar verticalProgressBar;
    DownViewPagerAdapter viewPagerAdapter;
    RelativeLayout complete,cancel;
    ViewPager pager;
    ArrayList<DownInfo> downInfoArrayList  = new ArrayList<>();
    LetterSpacingTextView title;
    Typeface typeface;
    boolean stateTag = false;
    //다운로드가 완료되면 백그라운드 태스크를 종료시킨다.
    @Subscribe
    public void TaskCancel(DownComplete downComplete) {
        downloadTask.cancel(true);
    }
    // 백그라운드에서 다운로드 상태에따라 gif를 세팅한다. 다운로드 상태는 메인액티비티에 전역변수로 세팅해놓고 그값을 유지시키며 갱신한다. 이 액티비티를 종료했다 다시들어와도 유지시키기 위해서
    @Subscribe
    public void setState(DownState downState) {
        MainActivity.DOWNSTATE = downState.getState();
        setGif();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BusProvider.getInstance().register(this);
        setContentView(R.layout.nfctaglayout);
        cancel = (RelativeLayout) findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.DOWNSTATE ==4){
                    MainActivity.DOWNSTATE=0;
                }
                finish();
            }
        });
        title = (LetterSpacingTextView) findViewById(R.id.title);
        title.setLetterSpacing(3f);
        title.setText("앨범 추가");
        typeface = Typeface.createFromAsset(getApplicationContext().getAssets(),"나눔고딕Bold.ttf");
        title.setTypeface(typeface);
        signal = (ImageView) findViewById(R.id.signal);
        verticalProgressBar = (VerticalProgressBar) findViewById(R.id.vertical);
        complete = (RelativeLayout) findViewById(R.id.complete);
        pager = (ViewPager) findViewById(R.id.pager);
        g2 = (TextView) findViewById(R.id.g2);
        setGif();
        // NFC 어댑터를 구한다
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        // NFC 어댑터가 null 이라면 칩이 존재하지 않는 것으로 간주

        if (mNfcAdapter == null) {
            Toast.makeText(getApplicationContext(), "TAG 실패", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        // NFC 데이터 활성화에 필요한 인텐트 필터를 생성
        IntentFilter iFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            iFilter.addDataType("*/*");
            mIntentFilters = new IntentFilter[]{iFilter};
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "TAG 실패", Toast.LENGTH_SHORT).show();
        }
        mNFCTechLists = new String[][]{new String[]{NfcF.class.getName()}};

    }
    //메인액티비티에 다운로드 상태를 세팅함
    public void stateProgress() {
        if (MainActivity.DOWNBOOLEAN) {
            Runnable notification = new Runnable() {
                public void run() {
                    verticalProgressBar.setProgress(MainActivity.DOWNPROGRESS);
                    stateProgress();
                }
            };
            handler.postDelayed(notification, 10);
        }
    }
    //메인액티비티의 다운로드 상태 구분 변수를 통해 gif 세팅
    public void setGif() {
        if (MainActivity.DOWNSTATE == 0) {
            Glide.with(getApplicationContext()).load(R.drawable.nfcstay)
                    .into(signal);
        } else if (MainActivity.DOWNSTATE == 1) {
            Glide.with(getApplicationContext()).load(R.drawable.nfccontact).into(signal);
        } else if (MainActivity.DOWNSTATE == 2) {
            signal.setVisibility(View.GONE);
            stateProgress();
        } else if (MainActivity.DOWNSTATE == 3) {
            verticalProgressBar.setVisibility(View.GONE);
            signal.setVisibility(View.VISIBLE);
            Glide.with(getApplicationContext()).load(R.drawable.nfccomplete).into(signal);
        } else if (MainActivity.DOWNSTATE ==4){
            g2.setVisibility(View.GONE);
            downInfoArrayList.clear();
            complete.setVisibility(View.VISIBLE);
            signal.setVisibility(View.GONE);
            verticalProgressBar.setVisibility(View.GONE);
                if (MainActivity.arraynull==0){
                    for (int i =0; i<MusicService.uriArrayList.size(); i++) {
                        Uri uri = MusicService.uriArrayList.get(i);
                        DownInfo downInfo = new DownInfo();
                        MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
                        metaRetriver.setDataSource(getApplicationContext(), uri);
                        downInfo.setCover(metaRetriver.getEmbeddedPicture());
                        downInfo.setAlbum(metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
                        downInfo.setArtist(metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
                        String a = metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
                        try {
                            StringBuffer sb = new StringBuffer(a);
                            sb.insert(4, ".");
                            sb.insert(7, ".");
                            downInfo.setDate(sb.toString());
                        } catch (NullPointerException ignored){}
                        downInfoArrayList.add(downInfo);
                    }
                    MainActivity.arraynull =1;
                    Log.d("pospos","aaaaaaa");
                } else {
                    Log.d("pospos",""+MainActivity.arraynull);
                    for (int i =0; i<MusicService.uriArrayList.size();i++) {
                        if (i > MusicService.uriArrayList.size()-MusicService.fileArrayList.size() - 1) {
                            Log.d("pospos", "bbbbbb"+MusicService.fileArrayList.size()+","+MusicService.uriArrayList.size());
                            Uri uri = MusicService.uriArrayList.get(i);
                            DownInfo downInfo = new DownInfo();
                            MediaMetadataRetriever metaRetriver = new MediaMetadataRetriever();
                            metaRetriver.setDataSource(getApplicationContext(), uri);
                            downInfo.setCover(metaRetriver.getEmbeddedPicture());
                            downInfo.setAlbum(metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
                            downInfo.setArtist(metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
                            String a = metaRetriver.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
                            if (a!=null){
                                StringBuffer sb = new StringBuffer(a);
                                sb.insert(4, ".");
                                sb.insert(7, ".");
                                downInfo.setDate(sb.toString());
                            } else {
                                downInfo.setDate("0000.00.00");
                            }
                            downInfoArrayList.add(downInfo);
                        }
                    }
            }
            viewPagerAdapter = new DownViewPagerAdapter(getApplicationContext(),downInfoArrayList);
            pager.setAdapter(viewPagerAdapter);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // 인텐트에서 액션을 추출
        String action = intent.getAction();
        // 인텐트에서 태그 정보 추출
        String tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG).toString();
        String strMsg = action + "\n\n" + tag;
        BusProvider.getInstance().post(new DownState(1));
        Glide.with(getApplicationContext()).load(R.drawable.nfccontact).into(signal);
        // 액션 정보와 태그 정보를 화면에 출력
        // 인텐트에서 NDEF 메시지 배열을 구한다
        Parcelable[] messages = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (messages == null) return;

        for (int i = 0; i < messages.length; i++)
            // NDEF 메시지를 화면에 출력
            showMsg((NdefMessage) messages[i]);
    }

    // NDEF 메시지를 화면에 출력
    public void showMsg(NdefMessage mMessage) {
        String strMsg = "";
        String strRec = "";
        // NDEF 메시지에서 NDEF 레코드 배열을 구한다
        NdefRecord[] recs = mMessage.getRecords();
        for (int i = 0; i < recs.length; i++) {
            // 개별 레코드 데이터를 구한다
            NdefRecord record = recs[i];
            byte[] payload = record.getPayload();
            // 레코드 데이터 종류가 텍스트 일때
            if (Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                // 버퍼 데이터를 인코딩 변환
                strRec = byteDecoding(payload);
            }
            // 레코드 데이터 종류가 URI 일때
            else if (Arrays.equals(record.getType(), NdefRecord.RTD_URI)) {
                strRec = new String(payload, 0, payload.length);
                strRec = "URI: " + strRec;
            }

        }
        Handler handler = new Handler();
        final String finalStrRec = strRec;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (!stateTag){
                    dataGet(finalStrRec);
                } else {
                    Toast.makeText(getApplicationContext(),"노래 다운로드 중입니다 끝나고 다시 시도해 주세요",Toast.LENGTH_SHORT).show();
                }
            }
        }, 2500);
    }

    // 버퍼 데이터를 디코딩해서 String 으로 변환
    public String byteDecoding(byte[] buf) {
        String strText = "";
        String textEncoding = ((buf[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
        int langCodeLen = buf[0] & 0077;

        try {
            strText = new String(buf, langCodeLen + 1,
                    buf.length - langCodeLen - 1, textEncoding);
        } catch (Exception e) {
            Log.d("tag1", e.toString());
        }
        return strText;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 앱이 실행될때 NFC 어댑터를 활성화 한다
        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mIntentFilters, mNFCTechLists);

        // NFC 태그 스캔으로 앱이 자동 실행되었을때
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction()))
            // 인텐트에 포함된 정보를 분석해서 화면에 표시
            onNewIntent(getIntent());
    }

    @Override
    public void onPause() {
        super.onPause();
        // 앱이 종료될때 NFC 어댑터를 비활성화 한다
        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }
    //파이어베이스의 리얼타임데이터베이스를 통해서 각각의 다운로드 컨텐츠의 url을 가져오고 그것을 다운로드 태스크로 이용하요 백그라운드에서 다운로드를 실시한다.
    public void dataGet(String audioKey) {
        MainActivity.DOWNBOOLEAN = true;
        BusProvider.getInstance().post(new DownState(2));
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiService.SOLLY_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        ApiService apiService = retrofit.create(ApiService.class);
        final Call<JsonObject> getAudio = apiService.getAudio(audioKey);
        getAudio.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject a = response.body();
                    MusicService.fileArrayList.clear();
                    MusicService.lrcfileArrayList.clear();
                    LinkedHashMap<String, LinkedHashMap<String, String>> retMap = new Gson().fromJson(
                            a.toString(), new TypeToken<LinkedHashMap<String, LinkedHashMap<String, String>>>() {
                            }.getType()
                    );
                    DONWSIZE = retMap.size();
                    Set key = retMap.keySet();
                    for (Iterator iterator = key.iterator(); iterator.hasNext(); ) {
                        String keyName = (String) iterator.next();
                        LinkedHashMap<String, String> valueName = retMap.get(keyName);
                        Log.d("json", keyName + " = " + valueName);
                        MusicService.fileArrayList.add(valueName.get("audio"));
                        MusicService.lrcfileArrayList.add(valueName.get("lrc"));
                        String fileName = Uri.parse(valueName.get("audio")).getLastPathSegment();
                        downloadlrcTask = new DownloadlrcTask(getBaseContext(), fileName);
                        downloadlrcTask.execute(valueName.get("lrc"));
                        downloadTask = new DownloadTask(getBaseContext());
                        downloadTask.execute(valueName.get("audio"));
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d("error", t.getMessage());
            }
        });
    }

    public class DownloadTask extends AsyncTask<String, Integer, String> {
        private Context context;
        String sdPath;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                String fileName = Uri.parse(sUrl[0]).getLastPathSegment();
                sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                sdPath += "/Solly/" + fileName + "/";
                File file = new File(sdPath);
                file.mkdirs();
                file = new File(sdPath, fileName);

                output = new FileOutputStream(file);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
                Uri uri = Uri.fromFile(file);
                boolean isExist = MusicService.uriArrayList.contains(uri);
                if (!isExist){
                    MusicService.uriArrayList.add(uri);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return e.toString();
            } finally {
                try {
                    if (output != null) {
                        output.flush();
                        output.close();
                    }
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            stateTag =true;
            BusProvider.getInstance().post(new DownProgress(0, 0));
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            if (progress[0] == 100) {
                NFCActivity.A++;
            }
            if (progress[0] > 1 && progress[0] < 100) {
                signal.setVisibility(View.GONE);
            }
            MainActivity.DOWNPROGRESS = progress[0];
            verticalProgressBar.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            BusProvider.getInstance().post(new DownProgress(NFCActivity.A, 100));
            Log.d("down3", String.valueOf(MusicService.uriArrayList.size()) + MusicService.lrclist.size());
            if (NFCActivity.A == MusicService.fileArrayList.size() && NFCActivity.A == MusicService.lrcfileArrayList.size()) {
                if (NFCActivity.A == NFCActivity.DONWSIZE) {
                    Log.d("down2", String.valueOf(MusicService.uriArrayList.size()) + MusicService.lrclist.size());
                    BusProvider.getInstance().post(new UriArray(MusicService.uriArrayList, MusicService.lrclist));
                    NFCActivity.A = 0;
                    context.startService(new Intent(context, MusicService.class));
                    MainActivity.DOWNBOOLEAN = false;
                    BusProvider.getInstance().post(new DownState(3));
                    stateTag = false;
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            BusProvider.getInstance().post(new DownState(4));
                        }
                    },3000);
                }
            }
            if (result != null) {
                Log.d("error", result);
            }
        }
//    public static void startFileMediaScan(Context mContext, String path){
//        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path))); }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (MainActivity.DOWNSTATE ==4){
            MainActivity.DOWNSTATE=0;
            finish();
        }
    }
}
