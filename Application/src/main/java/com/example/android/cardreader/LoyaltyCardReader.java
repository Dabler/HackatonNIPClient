/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.cardreader;

import android.app.DownloadManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import com.example.android.common.logger.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LoyaltyCardReader implements NfcAdapter.ReaderCallback {
    private static final String TAG = "LoyaltyCardReader";

    // Weak reference to prevent retain loop. mAccountCallback is responsible for exiting
    // foreground mode before it becomes invalid (e.g. during onPause() or onStop()).
    private WeakReference<AccountCallback> mAccountCallback;

    public interface AccountCallback {
        public void onAccountReceived(String account);
    }

    public LoyaltyCardReader(AccountCallback accountCallback) {
        mAccountCallback = new WeakReference<AccountCallback>(accountCallback);
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        byte[] tagId = tag.getId();
        StringBuilder builder = new StringBuilder();
        for (byte b : tagId)
        {
            builder.append(b);
        }
        String str = builder.toString();
        final Integer hash = str.hashCode();

        new Thread(){
            public void run(){
//                HttpClient myClient = new DefaultHttpClient();
//                HttpPost post = new HttpPost("http://10.100.0.155:8080/post/id");
//                try {
//                    List<NameValuePair> myArgs = new ArrayList<NameValuePair>();
//                    myArgs.add(new BasicNameValuePair("id", hash.toString()));
//                    post.setEntity(new UrlEncodedFormEntity(myArgs));
//                    HttpResponse myResponse = myClient.execute(post);
//                    BufferedReader br = new BufferedReader( new InputStreamReader(myResponse.getEntity().getContent()));
//                    String line = "";
//                    while ((line = br.readLine()) != null)
//                    {
//                        Log.d("mytag", line);
//                    }
//                }
//                catch (IOException e) {
//                    e.printStackTrace();
//                }

                String str = "http://10.100.0.155:8080/get/"+hash.toString();
                try {
                    downloadUrl(str);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "New tag discovered");

            }
        }.start();


    }

    private static String executeHttpRequest(String data) {
        String result = "";
        try {
            URL url = new URL("http://10.100.0.155:8080/get/");
            URLConnection connection = url.openConnection();

            // We need to make sure we specify that we want to provide input and
            // get output from this connection. We also want to disable caching,
            // so that we get the most up-to-date result. And, we need to
            // specify the correct content type for our data.
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // Send the POST data
            DataOutputStream dataOut = new DataOutputStream(connection.getOutputStream());
            dataOut.writeBytes(data);
            dataOut.flush();
            dataOut.close();

            // get the response from the server and store it in result
            DataInputStream dataIn = new DataInputStream(connection.getInputStream());
            String inputLine;
            while ((inputLine = dataIn.readLine()) != null) {
                result += inputLine;
            }
            dataIn.close();
        } catch (IOException e) {
        /*
         * In case of an error, we're going to return a null String. This
         * can be changed to a specific error message format if the client
         * wants to do some error handling. For our simple app, we're just
         * going to use the null to communicate a general error in
         * retrieving the data.
         */
            e.printStackTrace();
            result = null;
        }

        return result;
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
// the web page content as a InputStream, which it returns as
// a string.
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first 500 characters of the retrieved
        // web page content.
        int len = 500;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d("mytag", "The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = readIt(is, len);
            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }
}
