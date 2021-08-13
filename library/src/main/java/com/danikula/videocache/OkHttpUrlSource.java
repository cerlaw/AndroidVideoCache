package com.danikula.videocache;

import android.text.TextUtils;
import android.util.Log;

import com.danikula.videocache.headers.EmptyHeadersInjector;
import com.danikula.videocache.headers.HeaderInjector;
import com.danikula.videocache.log.LoggerFactory;
import com.danikula.videocache.sourcestorage.SourceInfoStorage;
import com.danikula.videocache.sourcestorage.SourceInfoStorageFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.danikula.videocache.Preconditions.checkNotNull;
import static com.danikula.videocache.ProxyCacheUtils.DEFAULT_BUFFER_SIZE;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;

/**
 * @author zhanghongjie
 * @date 2021/8/10
 * @description
 */
public class OkHttpUrlSource implements Source {

    private static final LoggerFactory.Logger LOG = LoggerFactory.getLogger("OkHttpUrlSource");
    private final SourceInfoStorage sourceInfoStorage;
    private final HeaderInjector headerInjector;
    private final OkHttpClient client = OkHttpClientInstance.getInstance();
    private SourceInfo sourceInfo;
    private InputStream bufferedInputStream;
    private InputStream inputStream;

    public OkHttpUrlSource(String url) {
        this(url, SourceInfoStorageFactory.newEmptySourceInfoStorage());
    }

    public OkHttpUrlSource(String url, SourceInfoStorage sourceInfoStorage) {
        this(url, sourceInfoStorage, new EmptyHeadersInjector());
    }

    public OkHttpUrlSource(String url, SourceInfoStorage sourceInfoStorage, HeaderInjector headerInjector) {
        this.sourceInfoStorage = checkNotNull(sourceInfoStorage);
        this.headerInjector = checkNotNull(headerInjector);
        SourceInfo sourceInfo = sourceInfoStorage.get(url);
        this.sourceInfo = sourceInfo != null ? sourceInfo :
                new SourceInfo(url, Integer.MIN_VALUE, ProxyCacheUtils.getSupposablyMime(url));
    }

    public OkHttpUrlSource(OkHttpUrlSource source) {
        this.sourceInfo = source.sourceInfo;
        this.sourceInfoStorage = source.sourceInfoStorage;
        this.headerInjector = source.headerInjector;
    }


    @Override
    public void open(long offset) throws ProxyCacheException {
        LOG.debug( "open url: " + sourceInfo.url);
        Request request = new Request.Builder().url(sourceInfo.url).build();
        try {
            Response response = client.newCall(request).execute();
            inputStream = response.body().byteStream();
            bufferedInputStream = new BufferedInputStream(inputStream, DEFAULT_BUFFER_SIZE);
            long length = readSourceAvailableBytes(response, offset, response.code());
            String mime = null;
            if (response.body().contentType() != null) {
                mime = response.body().contentType().toString();
            }
            sourceInfo = new SourceInfo(sourceInfo.url, length, mime);
            sourceInfoStorage.put(sourceInfo.url, sourceInfo);
        } catch (IOException | NullPointerException e) {
            throw new ProxyCacheException("Error opening connection for " + sourceInfo.url + " with offset " + offset, e);
        }
    }

    @Override
    public synchronized long length() throws ProxyCacheException {
        if (sourceInfo.length == Integer.MIN_VALUE) {
            fetchContentInfo();
        }
        return sourceInfo.length;
    }

    @Override
    public int read(byte[] buffer) throws ProxyCacheException {
        if (bufferedInputStream == null) {
            throw new ProxyCacheException("Error reading data from " + sourceInfo.url + ": connection is absent!");
        }
        try {
            return bufferedInputStream.read(buffer, 0, buffer.length);
        } catch (IOException e) {
            throw new ProxyCacheException("Error reading data from " + sourceInfo.url, e);
        }
    }

    @Override
    public void close() throws ProxyCacheException {
        ProxyCacheUtils.close(inputStream);
        ProxyCacheUtils.close(bufferedInputStream);
    }

    private long readSourceAvailableBytes(Response response, long offset, int responseCode) throws IOException {
        long contentLength = response.body().contentLength();
        return responseCode == HTTP_OK ? contentLength
                : responseCode == HTTP_PARTIAL ? contentLength + offset : sourceInfo.length;
    }

    private void fetchContentInfo() throws ProxyCacheException {
        LOG.debug("Read content info from " + sourceInfo.url);
        Request request = new Request.Builder().url(sourceInfo.url).build();
        InputStream is = null;
        try {
            Response response = client.newCall(request).execute();
            is = response.body().byteStream();
            long length = response.body().contentLength();
            String mime = response.body().contentType().toString();
            sourceInfo = new SourceInfo(sourceInfo.url, length, mime);
            sourceInfoStorage.put(sourceInfo.url, sourceInfo);
            LOG.debug("Source info fetched: " + sourceInfo);
        } catch (IOException | NullPointerException e) {
            LOG.debug( "Error fetching info from " + sourceInfo.url + e.getMessage());
        } finally {
            ProxyCacheUtils.close(is);
        }
    }

    public synchronized String getMime() throws ProxyCacheException {
        if (TextUtils.isEmpty(sourceInfo.mime)) {
            fetchContentInfo();
        }
        return sourceInfo.mime;
    }

    public String getUrl() {
        return sourceInfo.url;
    }

    @Override
    public String toString() {
        return "HttpUrlSource{sourceInfo='" + sourceInfo + "}";
    }
}
