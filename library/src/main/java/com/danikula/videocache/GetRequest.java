package com.danikula.videocache;

import android.text.TextUtils;

import com.danikula.videocache.log.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.danikula.videocache.Preconditions.checkNotNull;

/**
 * Model for Http GET request.
 * 保存对应的URL，请求的起始位置rangeOffset和是否是分段下载partial
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
class GetRequest {

    private static final Pattern RANGE_HEADER_PATTERN = Pattern.compile("[R,r]ange:[ ]?bytes=(\\d*)-");
    private static final Pattern URL_PATTERN = Pattern.compile("GET /(.*) HTTP");
    private static final LoggerFactory.Logger LOG = LoggerFactory.getLogger("GetRequest");

    public final String uri;
    public final long rangeOffset;
    public final boolean partial;

    public GetRequest(String request) {
        checkNotNull(request);
        long offset = findRangeOffset(request);
        this.rangeOffset = Math.max(0, offset);
        this.partial = offset >= 0;
        this.uri = findUri(request);
    }



    public static GetRequest read(InputStream inputStream) throws IOException {
        //从inputStream中读出来的内容如下：
        //GET /http%3A%2F%2Fvfx.mtime.cn%2FVideo%2F2019%2F02%2F04%2Fmp4%2F190204084208765161.mp4 HTTP/1.1
        //User-Agent: stagefright/1.2 (Linux;Android 8.0.0)
        //Host: 127.0.0.1:41271
        //Connection: Keep-Alive
        //Accept-Encoding: gzip
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder stringRequest = new StringBuilder();
        String line;
        while (!TextUtils.isEmpty(line = reader.readLine())) { // until new line (headers ending)
            stringRequest.append(line).append('\n');
        }
        return new GetRequest(stringRequest.toString());
    }

    private long findRangeOffset(String request) {
        Matcher matcher = RANGE_HEADER_PATTERN.matcher(request);
        if (matcher.find()) {
            String rangeValue = matcher.group(1);
            return Long.parseLong(rangeValue);
        }
        return -1;
    }

    private String findUri(String request) {
        Matcher matcher = URL_PATTERN.matcher(request);
        if (matcher.find()) {
            //group是用括号划分的正则表达式，可以根据组的编号来引用这个组。
            //组号为0表示整个表达式，组号为1表示被第一对括号括起的组，依次类推，例如A(B(C))D，group(0)是ABCD，group(1)是BC，group(2)是C。
            //所以这里group(1)指的就是原链接
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Invalid request `" + request + "`: url not found!");
    }

    @Override
    public String toString() {
        return "GetRequest{" +
                "rangeOffset=" + rangeOffset +
                ", partial=" + partial +
                ", uri='" + uri + '\'' +
                '}';
    }
}
