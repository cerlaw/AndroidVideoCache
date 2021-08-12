package com.danikula.videocache;

import android.text.TextUtils;

import com.danikula.videocache.file.FileCache;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Locale;

import static com.danikula.videocache.ProxyCacheUtils.DEFAULT_BUFFER_SIZE;

/**
 * {@link ProxyCache} that read http url and writes data to {@link Socket}
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
class HttpProxyCache extends ProxyCache {

    private static final float NO_CACHE_BARRIER = .2f;

    private final OkHttpUrlSource source;
    private final FileCache cache;
    private CacheListener listener;

    public HttpProxyCache(OkHttpUrlSource source, FileCache cache) {
        super(source, cache);
        this.cache = cache;
        this.source = source;
    }

    public void registerCacheListener(CacheListener cacheListener) {
        this.listener = cacheListener;
    }

    public void processRequest(GetRequest request, Socket socket) throws IOException, ProxyCacheException {
        //向播放器返回Head信息（会以流的方式，先缓存到本地再给到播放器）
        OutputStream out = new BufferedOutputStream(socket.getOutputStream());
        String responseHeaders = newResponseHeaders(request);
        out.write(responseHeaders.getBytes("UTF-8"));

        long offset = request.rangeOffset;
        //判断是否需要缓存，TODO 这里的可以进行优化，否则一旦seek后就可能不会在缓存了
        //要处理seek后继续缓存就要考虑文件空洞的以及merge的事情
        if (isUseCache(request)) {
            responseWithCache(out, offset);
        } else {
            responseWithoutCache(out, offset);
        }
    }

    /**
     * 判断是否使用缓存
     * request.rangeOffset <= cacheAvailable + sourceLength * NO_CACHE_BARRIER
     * 当seek超过视频总长的20%就会跳过缓存，如果seek在20%总长以内，则会把seek部分的全部下载完全后再把对应的部分交给播放器
     * @param request
     * @return
     * @throws ProxyCacheException
     */
    private boolean isUseCache(GetRequest request) throws ProxyCacheException {
        //原始长度
        long sourceLength = source.length();
        boolean sourceLengthKnown = sourceLength > 0;
        //已经缓存的长度
        long cacheAvailable = cache.available();
        // do not use cache for partial requests which too far from available cache. It seems user seek video.
        return !sourceLengthKnown || !request.partial || request.rangeOffset <= cacheAvailable + sourceLength * NO_CACHE_BARRIER;
    }

    private String newResponseHeaders(GetRequest request) throws IOException, ProxyCacheException {
        String mime = source.getMime();
        boolean mimeKnown = !TextUtils.isEmpty(mime);
        long length = cache.isCompleted() ? cache.available() : source.length();
        boolean lengthKnown = length >= 0;
        long contentLength = request.partial ? length - request.rangeOffset : length;
        boolean addRange = lengthKnown && request.partial;
        return new StringBuilder()
                .append(request.partial ? "HTTP/1.1 206 PARTIAL CONTENT\n" : "HTTP/1.1 200 OK\n")
                .append("Accept-Ranges: bytes\n")
                .append(lengthKnown ? format("Content-Length: %d\n", contentLength) : "")
                .append(addRange ? format("Content-Range: bytes %d-%d/%d\n", request.rangeOffset, length - 1, length) : "")
                .append(mimeKnown ? format("Content-Type: %s\n", mime) : "")
                .append("\n") // headers end
                .toString();
    }

    private void responseWithCache(OutputStream out, long offset) throws ProxyCacheException, IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int readBytes;
        while ((readBytes = read(buffer, offset, buffer.length)) != -1) {
            out.write(buffer, 0, readBytes);
            offset += readBytes;
        }
        out.flush();
    }

    private void responseWithoutCache(OutputStream out, long offset) throws ProxyCacheException, IOException {
        OkHttpUrlSource newSourceNoCache = new OkHttpUrlSource(this.source);
        try {
            newSourceNoCache.open((int) offset);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int readBytes;
            //这里的read方法，每次读取8192个字节，直到读完为止
            while ((readBytes = newSourceNoCache.read(buffer)) != -1) {
                out.write(buffer, 0, readBytes);
                offset += readBytes;
            }
            out.flush();
        } finally {
            newSourceNoCache.close();
        }
    }

    private String format(String pattern, Object... args) {
        return String.format(Locale.US, pattern, args);
    }

    @Override
    protected void onCachePercentsAvailableChanged(int percents) {
        if (listener != null) {
            listener.onCacheAvailable(cache.file, source.getUrl(), percents);
        }
    }
}
