package com.silion.asynctaskdemo;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by silion on 2016/7/7.
 */
public class MainActivity extends Activity {
    private Button btExecute;
    private Button btCancel;
    private ProgressBar pb;
    private TextView tvContent;

    private DownloadTask mTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btExecute = (Button) findViewById(R.id.execute);
        btExecute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTask = new DownloadTask();
                mTask.execute("http://www.baidu.com");

                btExecute.setEnabled(false);
                btCancel.setEnabled(true);
            }
        });
        btCancel = (Button) findViewById(R.id.cancel);
        btCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTask.getStatus() == AsyncTask.Status.RUNNING) {
                    mTask.cancel(true);
                }
            }
        });
        pb = (ProgressBar) findViewById(R.id.progress_bar);
        tvContent = (TextView) findViewById(R.id.text_view);
    }

    public class DownloadTask extends AsyncTask<String, Integer, String> {

        private OkHttpClient mClient;

        @Override
        protected void onPreExecute() {
            Log.i("silion", "onPreExecute called");
            tvContent.setText("loading...");
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            Log.i("silion", "doInBackground called");
            mClient = new OkHttpClient();
            InputStream is = null;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                ResponseBody body = run(params[0]);
                long total = body.contentLength();
                is = body.byteStream();
                byte[] buf = new byte[1024];
                int count = 0;
                int length;
                while ((length = is.read(buf)) != -1) {
                    baos.write(buf, 0, length);
                    count += length;
                    //调用publishProgress公布进度,最后onProgressUpdate方法将被执行
                    publishProgress((int) ((count / (float) total) * 100));
                    //为了演示进度,休眠500毫秒
                    Thread.sleep(100);
                }
                return new String(baos.toByteArray(), "utf-8");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (baos != null) {
                    try {
                        baos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progresses) {
            Log.i("silion", "onProgressUpdate called");
            pb.setProgress(progresses[0]);
            tvContent.setText("longding..." + progresses[0] + "%");
            super.onProgressUpdate(progresses);
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("silion", "onPostExecute called");
            tvContent.setText(result);

            btExecute.setEnabled(true);
            btCancel.setEnabled(false);
            super.onPostExecute(result);
        }

        @Override
        protected void onCancelled() {
            Log.i("silion", "onCancelled called");
            tvContent.setText("cancelled");
            pb.setProgress(0);

            btExecute.setEnabled(true);
            btCancel.setEnabled(false);
            super.onCancelled();
        }

        ResponseBody run(String url) throws IOException {
            /**
             * 把HttpClient替换成OKHttp之后，有时会获取不到content-length
             * 经常抓包分析，发现服务器会随机的对下发的资源做GZip操作，而此时就没有相应的content-length
             * 在Header中加入”Accept-Encoding”, “identity”，这样强迫服务器不走压缩。问题就得到了解决
             */
            Request request = new Request.Builder().url(url).header("Accept-Encoding", "identity").build();
            Response response = mClient.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                throw new IOException("Unexpected code" + response);
            }
        }
    }
}
