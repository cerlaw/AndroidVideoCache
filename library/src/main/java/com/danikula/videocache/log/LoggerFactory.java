package com.danikula.videocache.log;

import android.util.Log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author zhanghongjie
 * @date 2021/8/13
 * @description
 */
public class LoggerFactory {

    private static final String TAG = "VideoCache - ";

    public static Logger getLogger(String tag) {
        return new Logger(tag);
    }

    public static class Logger {

        private String classTag;

        Logger(String tag) {
            classTag = TAG + tag;
        }

        public void debug(String msg) {
            Log.d(classTag, msg);
        }

        public void info(String msg) {
            Log.i(classTag, msg);
        }

        public void verbose(String msg) {
            Log.v(classTag, msg);
        }

        public void warn(String msg) {
            Log.w(classTag, msg);
        }

        public void warn(String msg, Throwable e) {
            this.warn(msg);
            Log.w(classTag, getStackTrace(e));
        }

        public void error(String msg) {
            Log.e(classTag, msg);
        }

        public void error(String msg, Throwable e) {
            this.error(msg);
            Log.e(classTag, getStackTrace(e));
        }

        private String getStackTrace(Throwable throwable) {

            String stackTrace = "";
            try (StringWriter sw = new StringWriter();
                 PrintWriter pw = new PrintWriter(sw)){
                throwable.printStackTrace(pw);
                stackTrace = sw.toString();
            } catch (IOException e) {
                Log.e(classTag, "get stack trace fail !");
            }

            return stackTrace;
        }
    }

}
