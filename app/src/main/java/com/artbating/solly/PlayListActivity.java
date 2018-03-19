package com.artbating.solly;

import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;

/**
 * Created by kim on 2017. 3. 27..
 */

public class PlayListActivity extends AppCompatActivity {
    RelativeLayout cancel;
    ArrayList<SongData> songDataArrayList = new ArrayList<>();
    ListView listView;
    LetterSpacingTextView title;
    RelativeLayout empty;
    PlayListAdapter adapter;

    Typeface typeface;
//플레이리스트 클릭 또는 외부에서 플레이리스트 변경이벤트를 동기화하여 세팅한다.
    @Subscribe
    public void setPosition(PlayListSelect position) {
       try {
           final int pos = position.getPosition();
           if (pos ==0){
               adapter.setSelectedIndex(0,true);
           } else {
               adapter.setSelectedIndex(pos,true);
           }
           adapter.notifyDataSetChanged();
       } catch (Exception e){
           e.printStackTrace();
       }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist);
        BusProvider.getInstance().register(this);
        empty = (RelativeLayout) findViewById(R.id.empty);
        title = (LetterSpacingTextView) findViewById(R.id.title);
        title.setLetterSpacing(3f);
        title.setText("재생목록");
        typeface = Typeface.createFromAsset(getApplicationContext().getAssets(), "나눔고딕Bold.ttf");
        title.setTypeface(typeface);
        cancel = (RelativeLayout) findViewById(R.id.cancel);
        listView = (ListView) findViewById(R.id.playlist);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        if (MusicService.uriArrayList.size() != 0) {
            empty.setVisibility(View.GONE);
            for (int i = 0; i < MusicService.uriArrayList.size(); i++) {
                SongData songData = new SongData();
                MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                metadataRetriever.setDataSource(getApplicationContext(), MusicService.uriArrayList.get(i));
                songData.setArtist(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
                songData.setCover(metadataRetriever.getEmbeddedPicture());
                songData.setTitle(metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
                String time = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (time != null) {
                    songData.setDuration(milliSecondsToTimer(time));
                } else {
                    songData.setDuration(milliSecondsToTimer(String.valueOf(MusicService.duration)));
                }
                songDataArrayList.add(songData);
            }
            adapter = new PlayListAdapter(songDataArrayList, getApplicationContext());
            listView.setAdapter(adapter);
            adapter.setSelectedIndex(MusicService.playing,MusicService.isplay);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    BusProvider.getInstance().post(new PlayListSelect(position));

                }
            });
        } else {
            listView.setVisibility(View.GONE);
            empty.setVisibility(View.VISIBLE);
        }

    }

    public String milliSecondsToTimer(String duration) {
        long milliseconds = Long.parseLong(duration);
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
}
