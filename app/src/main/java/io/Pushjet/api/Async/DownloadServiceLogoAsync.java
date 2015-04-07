package io.Pushjet.api.Async;


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import io.Pushjet.api.MiscUtil;
import io.Pushjet.api.PushjetApi.PushjetService;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DownloadServiceLogoAsync extends AsyncTask<PushjetService, Void, Map<String, byte[]>> {
    private Context context;

    public DownloadServiceLogoAsync(Context context) {
        this.context = context;
    }

    @Override
    protected Map<String, byte[]> doInBackground(PushjetService... services) {
        Map<String, byte[]> bitmaps = new HashMap<String, byte[]>();
        for (PushjetService service : services) {
            if (!service.hasIcon() || service.iconCached(this.context))
                continue;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                HttpGet httpget = new HttpGet(service.getIcon());
                HttpResponse response = (new DefaultHttpClient()).execute(httpget);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() != HttpStatus.SC_OK)
                    continue;
                response.getEntity().writeTo(out);
                byte[] data = out.toByteArray();
                bitmaps.put(service.getIcon(), data);
            } catch (Exception ignore) {
                ignore.printStackTrace();
            } finally {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
        }
        context.sendBroadcast(new Intent("PushjetIconDownloaded"));
        return bitmaps;
    }

    @Override
    protected void onPostExecute(Map<String, byte[]> bitmaps) {
        for (String key : bitmaps.keySet()) {
            byte[] data = bitmaps.get(key);
            FileOutputStream out = null;
            try {
                out = context.openFileOutput(MiscUtil.iconFilename(key), Context.MODE_PRIVATE);
                out.write(data);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) out.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
