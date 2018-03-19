package com.artbating.solly;

/**
 * Created by kim on 2017. 3. 27..
 */

public class DownProgress {
    int complete;
    int progress;

    public DownProgress(int complete, int progress) {
        this.complete = complete;
        this.progress = progress;
    }

    public int getComplete() {
        return complete;
    }

    public int getProgress() {
        return progress;
    }
}
