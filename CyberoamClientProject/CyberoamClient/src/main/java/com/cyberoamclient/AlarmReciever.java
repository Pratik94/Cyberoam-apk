package com.cyberoamclient;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Created by shantanu on 12/2/14.
 */
public class AlarmReciever extends BroadcastReceiver {
    String Username, Password, Mode;
    Boolean AutoLogin;
    Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        Log.e("Alaem reciever", "true");
        getResult();
        ConnectivityManager connManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            if (AutoLogin) {
                new ClickAction().execute();
                // ScheduleAlarm(this.context);
            }
        }

    }

    private void ScheduleAlarm(Context context) {
        Long time = new GregorianCalendar().getTimeInMillis() + 60 * 60 * 1000;
        Intent intentAlarm = new Intent(context, AlarmReciever.class);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, PendingIntent.getBroadcast(context, 1, intentAlarm, 0));
    }

    public void getResult() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Username = sharedPrefs.getString("prefUsername", "NULL");
        Password = sharedPrefs.getString("prefPassword", "NULL");
        AutoLogin = sharedPrefs.getBoolean("prefAutoLogin", false);
    }

    public DefaultHttpClient getNewHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            SingleClientConnManager ccm = new SingleClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }

    public class ClickAction extends AsyncTask<Void, Void, String> {
        String output = "";
        ItemList itemList;


        @Override
        protected String doInBackground(Void... voids) {
            System.out.println(">>>>0");

            DefaultHttpClient httpclient = getNewHttpClient();
            HttpPost httppost = new HttpPost("https://10.100.56.55:8090/httpclient.html");
            StringBuilder builder = new StringBuilder();
            try {
                System.out.println(">>>>1");
                Long time = (new Date()).getTime();
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(3);
                nameValuePairs.add(new BasicNameValuePair("username", Username));
                nameValuePairs.add(new BasicNameValuePair("password", Password));
                nameValuePairs.add(new BasicNameValuePair("mode", "191"));
                nameValuePairs.add(new BasicNameValuePair("a", time.toString()));
                nameValuePairs.add(new BasicNameValuePair("producttype", "0"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                System.out.println(">>>>2");

                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }
                } else {
                    Log.e("==>", "Failed to download file");
                }

            } catch (ClientProtocolException e) {
                System.out.println(">>>>e1");

                e.printStackTrace();
            } catch (IOException e) {
                System.out.println(">>>>e2");

                e.printStackTrace();
            }
            try {
                output = builder.toString();
            } catch (Exception e) {
                System.out.println(">>>>e3");

                e.printStackTrace();
            }

            return output;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            try {
                SAXParserFactory spf = SAXParserFactory.newInstance();
                SAXParser sp = spf.newSAXParser();
                XMLReader xr = sp.getXMLReader();

                /** Create handler to handle XML Tags ( extends DefaultHandler ) */
                MyXMLHandler myXMLHandler = new MyXMLHandler();
                xr.setContentHandler(myXMLHandler);

                ByteArrayInputStream is = new ByteArrayInputStream(s.getBytes());
                xr.parse(new InputSource(is));
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                itemList = MyXMLHandler.itemList;

                Intent resultIntent = new Intent(context, StartActivity.class);

                ArrayList<String> listManu = itemList.getMessage();

                PendingIntent pIntent = PendingIntent.getActivity(context, 0, resultIntent, 0);
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(context)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setContentTitle("Cyberoam Client")
                                .setContentText(listManu.get(0).substring(9))
                                .setContentIntent(pIntent)
                                .setAutoCancel(true)
                                .setDefaults(-1);

                NotificationManager mNotificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.


                mNotificationManager.notify(1, mBuilder.build());

            } catch (Exception e) {
                e.printStackTrace();
            }

        }


    }
}
