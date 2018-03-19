package com.artbating.solly;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

/**
 * Created by kim on 2017. 3. 28..
 */

public class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.ViewHolder> {
    ArrayList<SongData> songDataArrayList = new ArrayList<>();
    Context context;
    SongData songData;

    public MarketAdapter(Context context, ArrayList<SongData> songDataArrayList) {
        this.songDataArrayList = songDataArrayList;
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.listitem,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        songData = songDataArrayList.get(position);
        if (songData.getCover()!=null){
            Glide.with(context).load(songData.getCover()).into(holder.cover);
        } else {
            Glide.with(context).load(R.drawable.nocover).into(holder.cover);
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

    }

    @Override
    public int getItemCount() {
        return songDataArrayList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        SquareImageView cover;
        TextView artist;
        TextView title;

        public ViewHolder(View itemView) {
            super(itemView);
            cover = (SquareImageView) itemView.findViewById(R.id.product);
            artist = (TextView) itemView.findViewById(R.id.artist);
            title = (TextView) itemView.findViewById(R.id.title);
        }
    }
}
