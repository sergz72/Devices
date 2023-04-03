package org.sz.ovencontrol;

import android.os.AsyncTask;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Implementation of AsyncTask designed to fetch data from the network.
 */
class RestTask extends AsyncTask<RestTask.Parameters, Integer, RestTask.Result> {
    public interface Callback {
        /**
         * Indicates that the download operation has finished. This method is called even if the
         * download hasn't completed successfully.
         */
        void finishDownloading(Result result);
    }

    static class Parameters {
        public final String mUrl;
        public final String mMethod;
        public final String mBody;
        public Parameters(String url, String method, String body) {
            mUrl = url;
            mMethod = method;
            mBody = body;
        }
    }
    static class Result {
        public final String mResultValue;
        public final Exception mException;
        public Result(String resultValue) {
            mResultValue = resultValue;
            mException = null;
        }
        public Result(Exception exception) {
            mResultValue = null;
            mException = exception;
        }
    }

    private final Callback mCallback;
    private final OkHttpClient mClient;

    RestTask(Callback callback) {
        mCallback = callback;
        mClient = new OkHttpClient();
    }

    /**
     * Defines work to perform on the background thread.
     */
    @Override
    protected Result doInBackground(Parameters... parameters) {
        Result result = null;
        if (!isCancelled() && parameters != null && parameters.length > 0) {
            Parameters parameter = parameters[0];
            try {

                RequestBody body = parameter.mBody == null ? null : RequestBody.create(MediaType.get("application/json"), parameter.mBody);
                Request request = new Request.Builder()
                        .method(parameter.mMethod, body)
                        .url(parameter.mUrl)
                        .build();
                Response response = mClient.newCall(request).execute();
                result = new Result(response.body().string());
            } catch(IOException e) {
                result = new Result(e);
            }
        }
        return result;
    }

    /**
     * Updates the DownloadCallback with the result.
     */
    @Override
    protected void onPostExecute(Result result) {
        if (result != null && mCallback != null) {
            mCallback.finishDownloading(result);
        }
    }

    /**
     * Override to add special behavior for cancelled AsyncTask.
     */
    @Override
    protected void onCancelled(Result result) {
    }
}