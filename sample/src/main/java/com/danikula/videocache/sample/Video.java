package com.danikula.videocache.sample;

public enum Video {

    ORANGE_1(Config.ROOT + "2019/02/04/mp4/190204084208765161.mp4"),
    ORANGE_2(Config.ROOT + "2019/03/21/mp4/190321153853126488.mp4"),
    ORANGE_3(Config.ROOT + "2019/03/19/mp4/190319222227698228.mp4"),
    ORANGE_4(Config.ROOT + "2019/03/19/mp4/190319212559089721.mp4"),
    ORANGE_5(Config.ROOT + "2019/03/18/mp4/190318231014076505.mp4");

    public final String url;

    Video(String url) {
        this.url = url;
    }

    private class Config {
        private static final String ROOT = "http://vfx.mtime.cn/Video/";
    }
}
