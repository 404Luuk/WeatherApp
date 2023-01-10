package com.example.weatherapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class EarthquakeInfo extends AppCompatActivity {

    String dataUrl;

    ListView lvRss;

    ArrayList<String> Titles;
    ArrayList<String> Desc;
    ArrayList<String> Links;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_earthquake_info);

        lvRss = findViewById(R.id.ListViewRss);

        Titles = new ArrayList<>();
        Desc = new ArrayList<>();
        Links = new ArrayList<>();

        lvRss.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Uri uri = Uri.parse(Links.get(i));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        new BackgroundProcess().execute();

    }

    public InputStream getInputStream(URL url)
    {
        try {
            return url.openConnection().getInputStream();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }


    public class BackgroundProcess extends AsyncTask<Integer, Void, Exception>
    {
        ProgressDialog progressDialog = new ProgressDialog(EarthquakeInfo.this);

        Exception exception = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog.setMessage("LOADING RSS FEED..");
            progressDialog.show();
        }

        @Override
        protected Exception doInBackground(Integer... params) {

            try {
                URL url = new URL("https://cdn.knmi.nl/knmi/map/page/seismologie/GQuake_KNMI_RSS.xml");

                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(false);

                XmlPullParser xpp = factory.newPullParser();

                xpp.setInput(getInputStream(url), "UTF_8"); // Shit goes wrong here

                boolean inItem = false;

                int eventType = xpp.getEventType();

                while (eventType != XmlPullParser.END_DOCUMENT)
                {

                    Log.e("error", "test3");


                    if(eventType == XmlPullParser.START_TAG)
                    {
                        if(xpp.getName().equalsIgnoreCase("item"))
                        {
                            inItem = true;
                        }
                        else if(xpp.getName().equalsIgnoreCase("title"))
                        {
                             if(inItem)
                             {
                                Titles.add(xpp.nextText());
                             }
                        }
                        else if (xpp.getName().equalsIgnoreCase("description"))
                        {
                            if (inItem)
                            {
                                Desc.add(xpp.nextText());
                            }
                        }
                        else if(xpp.getName().equalsIgnoreCase("link"))
                        {
                            if (inItem)
                            {
                                Links.add(xpp.nextText());
                            }
                        }
                    }
                    else if (eventType == XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("item"))
                    {
                        inItem = false;
                    }

                    eventType = xpp.next();
                }


            }
            catch (MalformedURLException e) { exception = e; }
            catch (XmlPullParserException e) { exception = e; }
            catch (IOException e) { exception = e; }

            return exception;
        }

        @Override
        protected void onPostExecute(Exception e) {
            super.onPostExecute(e);

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(EarthquakeInfo.this, android.R.layout.simple_list_item_1, Titles);

            lvRss.setAdapter(adapter);

            progressDialog.dismiss();
        }
    }


    public void StartMain(View v) {
        Intent i = new Intent(this,MainActivity.class);
        startActivity(i);
    }


}