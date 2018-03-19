package com.artbating.solly;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.ArrayList;

import es.claucookie.miniequalizerlibrary.EqualizerView;

/**
 * Created by kim on 2017. 3. 27..
 */

public class PlayListAdapter extends BaseAdapter {
    ArrayList<SongData> songDataArrayList = new ArrayList<>();
    Context context;
    LayoutInflater layoutInflater;
    SongData songData;
    int selectedIndex = -1;
    boolean setting = false;
    public PlayListAdapter(ArrayList<SongData> songDataArrayList, Context context) {
        this.songDataArrayList = songDataArrayList;
        this.context = context;
        layoutInflater =(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return songDataArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return songDataArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    public void setSelectedIndex(int index,boolean setting){
        Log.d("paaaa",""+index);
        this.selectedIndex = index;
        this.setting = setting;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final Holder holder;
        songData = songDataArrayList.get(position);
        if (convertView == null){
            holder = new Holder();
            convertView= layoutInflater.inflate(R.layout.songlistitem,null,false);
            holder.cover = (ImageView) convertView.findViewById(R.id.albumcover);
            holder.artist = (TextView) convertView.findViewById(R.id.artist);
            holder.title = (TextView) convertView.findViewById(R.id.songname);
            holder.item = (RelativeLayout) convertView.findViewById(R.id.item);
            holder.equalizerView = (EqualizerView) convertView.findViewById(R.id.equalizer_view);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

            byte[] a = songData.getCover();
        if (a ==null){
            Glide.with(context).load(R.drawable.nocover)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .dontAnimate()
                    .into(new SimpleTarget<Bitmap>() {

                        @Override
                        public void onResourceReady(Bitmap arg0, GlideAnimation<? super Bitmap> arg1) {
                            // TODO Auto-generated method stub
                            holder.cover.setImageBitmap(arg0);
                        }
                    });
        } else {
            Glide.with(context).load(a)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .dontAnimate()
                    .into(new SimpleTarget<Bitmap>() {

                        @Override
                        public void onResourceReady(Bitmap arg0, GlideAnimation<? super Bitmap> arg1) {
                            // TODO Auto-generated method stub
                            holder.cover.setImageBitmap(arg0);
                        }
                    });
        }
        if (songData.getTitle()!=null){
            holder.title.setText(songData.getTitle());
        } else {
            holder.title.setText("알 수 없는 음악");
        }
        if (songData.getArtist()!=null){
            holder.artist.setText(songData.getArtist());
        } else {
            holder.artist.setText("알 수 없는 음악가");
        }


        if (MusicService.isplay){
            if (position == selectedIndex){
                Log.d("pposition", String.valueOf(position));
                holder.title.setSelected(true);
                holder.title.setTextColor(Color.parseColor("#4fc3f7"));
                holder.artist.setTextColor(Color.parseColor("#4fc3f7"));
                holder.artist.setSelected(true);
                holder.equalizerView.setVisibility(View.VISIBLE);
                holder.equalizerView.animateBars();

            }else {
                Log.d("pposition1", String.valueOf(position));
                holder.title.setSelected(false);
                holder.title.setTextColor(Color.parseColor("#333333"));
                holder.artist.setTextColor(Color.parseColor("#333333"));
                holder.artist.setSelected(false);
                holder.equalizerView.setVisibility(View.GONE);
                holder.equalizerView.stopBars();

            }
        } else {
            if (setting){
                if (position == selectedIndex){
                    Log.d("pposition", String.valueOf(position));
                    holder.title.setSelected(true);
                    holder.title.setTextColor(Color.parseColor("#4fc3f7"));
                    holder.artist.setTextColor(Color.parseColor("#4fc3f7"));
                    holder.artist.setSelected(true);
                    holder.equalizerView.setVisibility(View.VISIBLE);
                    holder.equalizerView.animateBars();

                }else {
                    Log.d("pposition1", String.valueOf(position));
                    holder.title.setSelected(false);
                    holder.title.setTextColor(Color.parseColor("#333333"));
                    holder.artist.setTextColor(Color.parseColor("#333333"));
                    holder.artist.setSelected(false);
                    holder.equalizerView.setVisibility(View.GONE);
                    holder.equalizerView.stopBars();

                }
            } else {
                holder.title.setSelected(false);
                holder.artist.setSelected(false);
                holder.artist.setTextColor(Color.parseColor("#333333"));
                holder.title.setTextColor(Color.parseColor("#333333"));
                holder.equalizerView.setVisibility(View.GONE);
                holder.equalizerView.stopBars();
            }

        }
        return convertView;
    }
    class Holder {
        ImageView cover;
        TextView artist,title;
        RelativeLayout item;
        EqualizerView equalizerView;
    }
}
