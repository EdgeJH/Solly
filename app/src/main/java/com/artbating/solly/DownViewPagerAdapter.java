package com.artbating.solly;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

/**
 * Created by kim on 2017. 4. 10..
 */

public class DownViewPagerAdapter extends PagerAdapter {
    Context context;
    LayoutInflater inflater;
    ArrayList<DownInfo> downInfoArrayList = new ArrayList<>();
    DownInfo downInfo;
    SquareImageView albumCover;
    TextView album,artist,date;
    SparseArray< View > views = new SparseArray< View >();
    public DownViewPagerAdapter(Context context, ArrayList<DownInfo> downInfoArrayList) {
        this.context = context;
        this.downInfoArrayList = downInfoArrayList;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return downInfoArrayList.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        View view=null;
        view= inflater.inflate(R.layout.downpageritem, null);
        downInfo = downInfoArrayList.get(position);
        albumCover = (SquareImageView) view.findViewById(R.id.coverimage);
        album = (TextView) view.findViewById(R.id.albumname);
        artist = (TextView) view.findViewById(R.id.artist);
        date = (TextView) view.findViewById(R.id.date);
        if (downInfo.getAlbum()!=null){
            album.setText(downInfo.getAlbum());
        } else {
            album.setVisibility(View.GONE);
        }
        if (downInfo.getArtist()!=null){
            artist.setText(downInfo.getArtist());
        } else {
            artist.setVisibility(View.GONE);
        }
        if (downInfo.getDate()!=null){
            date.setText(downInfo.getDate());
        } else {
            date.setVisibility(View.GONE);
        }
        if (downInfo.getCover()!=null){
            Glide.with(context).load(downInfo.getCover())
                    .thumbnail(0.6f)
                    .into(albumCover);
        } else {
            Glide.with(context).load(R.drawable.nocover)
                    .thumbnail(0.6f)
                    .into(albumCover);
        }



        //ViewPager에 만들어 낸 View 추가

        container.addView(view);

        views.put(position, view);

        //Image가 세팅된 View를 리턴

        return view;

    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = (View)object;
        container.removeView(view);
        views.remove(position);
        view = null;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view==object;
    }

}
