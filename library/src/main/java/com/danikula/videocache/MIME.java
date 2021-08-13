package com.danikula.videocache;

/**
 * @author zhanghongjie
 * @date 2021/8/13
 * @description
 */
public enum MIME {

    /**
     * video/mp4
     */
    MPEG_4("video/mp4");

    private String contentType;

    MIME(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}
