package com.artbating.solly;

import android.content.Context;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;

import jp.wasabeef.glide.transformations.BlurTransformation;

/**
 * Created by kim on 2017. 4. 10..
 */

public class ViewPagerAdapter extends PagerAdapter {
    Context context;
    LayoutInflater inflater;
    ArrayList<byte[]> bytes = new ArrayList<>();

    SquareImageView albumCover;
    SparseArray< View > views = new SparseArray< View >();
    public ViewPagerAdapter(Context context, ArrayList<byte[]> bytes) {
        this.context = context;
        this.bytes = bytes;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return bytes.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        View view=null;


        view= inflater.inflate(R.layout.pageritem, null);
        albumCover = (SquareImageView) view.findViewById(R.id.coverimage);
        byte [] image = bytes.get(position);
        if (image!=null){
            Glide.with(context).load(image)
                    .thumbnail(0.6f)
                    .into(albumCover);
        } else {
            Glide.with(context).load(R.drawable.nocover)
                    .into(albumCover);
        }




        //ViewPager에 만들어 낸 View 추가

        container.addView(view);

        views.put(position, view);

        //Image가 세팅된 View를 리턴

        return view;

    }


    @Override
    public void notifyDataSetChanged() {
        int key = 0;
        for(int i = 0; i < views.size(); i++) {
            key = views.keyAt(i);
            View view = views.get(key);
        }
        super.notifyDataSetChanged();
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
