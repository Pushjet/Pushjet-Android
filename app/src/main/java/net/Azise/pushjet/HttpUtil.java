package net.Azise.pushjet;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;


public class HttpUtil {
    public static String Post(String endpoint, Map<String, String> params) throws IOException {

        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Map.Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
                    .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
        byte[] bytes = body.getBytes();
        HttpURLConnection conn = null;
        BufferedReader in = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("user-agent", "Pushjet-android");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            // post the request
            OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();
            // handle the response
            int status = conn.getResponseCode();
            if (status != 200) {
                throw new IOException("Post failed with error code " + status);
            }
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String buf;
            String resp = "";
            while ((buf = in.readLine()) != null)
                resp += buf;
            return resp;
        } finally {
            if (conn != null)
                conn.disconnect();
            if (in != null)
                in.close();
        }
    }

    public static String Get(String endpoint, Map<String, String> data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            HttpGet httpget = new HttpGet(endpoint + "?" + urlencode(data));
            httpget.setHeader("user-agent", "Pushjet-android");
            HttpResponse response = (new DefaultHttpClient()).execute(httpget);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Get failed with error code " + statusLine.getStatusCode());
            }
            response.getEntity().writeTo(out);
            return out.toString();
        } finally {
            out.close();
        }
    }

    public static String Delete(String endpoint, Map<String, String> data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            HttpDelete httpDelete = new HttpDelete(endpoint + "?" + urlencode(data));
            httpDelete.setHeader("user-agent", "Pushjet-android");
            HttpResponse response = (new DefaultHttpClient()).execute(httpDelete);
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Get failed with error code " + statusLine.getStatusCode());
            }
            response.getEntity().writeTo(out);
            return out.toString();
        } finally {
            out.close();
        }
    }

    public static String urlencode(Map<String, String> data) {
        ArrayList<String> encoded = new ArrayList<String>();
        for (String key : data.keySet()) {
            try {
                encoded.add(URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(data.get(key)));
            } catch (UnsupportedEncodingException ignored) {
            }
        }
        String retval = "";
        for (int i = 0; i < encoded.size(); i++) {
            retval += encoded.get(i);
            if (i != encoded.size() - 1)
                retval += "&";
        }
        return retval;
    }
}
