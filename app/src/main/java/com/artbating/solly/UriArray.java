package com.artbating.solly;

import android.net.Uri;

import java.util.ArrayList;

/**
 * Created by kim on 2017. 3. 21..
 */

public class UriArray {
    private ArrayList<Uri> uriArrayList = new ArrayList<>();
    private ArrayList<Uri> lrcuriArrayList = new ArrayList<>();

    public UriArray(ArrayList<Uri> uriArrayList, ArrayList<Uri> lrcuriArrayList) {
        this.uriArrayList = uriArrayList;
        this.lrcuriArrayList = lrcuriArrayList;
    }

    public ArrayList<Uri> getUriArrayList() {
        return uriArrayList;
    }

    public void setUriArrayList(ArrayList<Uri> uriArrayList) {
        this.uriArrayList = uriArrayList;
    }

    public ArrayList<Uri> getLrcuriArrayList() {
        return lrcuriArrayList;
    }

    public void setLrcuriArrayList(ArrayList<Uri> lrcuriArrayList) {
        this.lrcuriArrayList = lrcuriArrayList;
    }
}
