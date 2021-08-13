package com.danikula.videocache;

import android.util.Log;

import com.danikula.videocache.log.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author zhanghongjie
 * @date 2021/8/10
 * @description
 */
public class OkHttpClientInstance {

    private static final LoggerFactory.Logger LOG = LoggerFactory.getLogger("OkHttpClientInstance");

    private OkHttpClientInstance() {}

    private static final class Holder {
        private static OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(new RetryInterceptor(3))
                .build();
    }

    private static class RetryInterceptor implements Interceptor {
        private int maxRetry;
        private int retryNum = 0;

        RetryInterceptor(int maxRetry) {
            this.maxRetry = maxRetry;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            Response response;
            for(response = chain.proceed(request); !response.isSuccessful()
                    && this.retryNum < this.maxRetry;
                response = chain.proceed(request)) {
                ++this.retryNum;
                LOG.debug("begin retry, num: " + retryNum);
            }

            return response;
        }
    }

    public static OkHttpClient getInstance() {
        return Holder.client;
    }
}
