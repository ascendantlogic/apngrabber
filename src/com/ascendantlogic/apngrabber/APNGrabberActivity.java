package com.ascendantlogic.apngrabber;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class APNGrabberActivity extends Activity {
    Button refreshAPNValues;
    Button saveAPNValues;
    
    LinearLayout apnValueList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apngrabber);
    
        refreshAPNValues = (Button)findViewById(R.id.refreshValues);
        refreshAPNValues.setOnClickListener(refreshValuesClickListener);
        
        saveAPNValues = (Button)findViewById(R.id.saveValues);
        saveAPNValues.setOnClickListener(saveValuesClickListener);
        
        apnValueList = (LinearLayout)findViewById(R.id.apnValueList);
    }

    private Button.OnClickListener refreshValuesClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            new RefreshApnValuesTask().execute();
        }
    };
    
    private Button.OnClickListener saveValuesClickListener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            new SaveApnValuesTask().execute();
        }
    };
    
    private JSONArray getApnValues() {
        JSONArray apnValues = null;
        JSONObject apnRecord = null;
        ContentResolver cr = this.getContentResolver();
        Cursor apnCursor = null;
        ArrayList<String> columnNames = null;
        
        apnCursor = cr.query(Uri.parse("content://telephony/carriers"), 
                             null, null, null, null);
        
        if (apnCursor != null && apnCursor.getCount() >= 1) {
            apnValues = new JSONArray();
            columnNames = new ArrayList<String>();
            
            // set to 1 to avoid the _id column
            for(int i=1; i < apnCursor.getColumnCount(); i++) {
                columnNames.add(apnCursor.getColumnName(i));
            }
            
            while (apnCursor.moveToNext()) {
                apnRecord = new JSONObject();
                
                try {
                    for (String columnName : columnNames) {
                        apnRecord.put(columnName, apnCursor.getString(apnCursor.getColumnIndex(columnName)));
                    }
                    
                    apnValues.put(apnRecord);
                } catch (Exception e) {
                    Log.e("APNGrabberActivity", getStackTrace(e));
                }
            }
            
            apnCursor.close();
        }
        
        return apnValues;
    }
    
    private class RefreshApnValuesTask extends AsyncTask<Void, Void, JSONArray> {
        @Override
        protected JSONArray doInBackground(Void... arg0) {
            return getApnValues();
        }
        
        @Override
        protected void onPostExecute(JSONArray result) {
            LinearLayout apnListItem = null;
            TextView name = null;
            TextView numeric = null;
            TextView mcc = null;
            TextView mnc = null;
            TextView apn = null;
            TextView user = null;
            TextView server = null;
            TextView password = null;
            TextView proxy = null;
            TextView port = null;
            TextView mmsProxy = null;
            TextView mmsPort = null;
            TextView mmsc = null;
            
            // Remove our existing views and construct a new list for display
            apnValueList.removeAllViews();
            
            if (result != null) {
                for (int i = 0; i < result.length(); i++) {
                    View v = LayoutInflater.from(APNGrabberActivity.this).inflate(R.layout.apn_listitem, null);
                    
                    apnListItem = (LinearLayout)v.findViewById(R.id.apnListItem);
                    apnListItem.setBackgroundColor((i % 2 == 0) ? android.R.color.white : android.R.color.background_light);
                    
                    name = (TextView)v.findViewById(R.id.name);
                    numeric = (TextView)v.findViewById(R.id.numeric);
                    mcc = (TextView)v.findViewById(R.id.mcc);
                    mnc = (TextView)v.findViewById(R.id.mnc);
                    apn = (TextView)v.findViewById(R.id.apn);
                    user = (TextView)v.findViewById(R.id.user);
                    server = (TextView)v.findViewById(R.id.server);
                    password = (TextView)v.findViewById(R.id.password);
                    proxy = (TextView)v.findViewById(R.id.proxy);
                    port = (TextView)v.findViewById(R.id.port);
                    mmsProxy = (TextView)v.findViewById(R.id.mmsProxy);
                    mmsPort = (TextView)v.findViewById(R.id.mmsPort);
                    mmsc = (TextView)v.findViewById(R.id.mmsc);
                    
                    try {
                        JSONObject apnValue = result.getJSONObject(i);
                        
                        Log.w("RefreshApnValuesTask", "Getting values for apn result: " + apnValue.toString());
                        
                        name.setText(getStringIfExists(apnValue, "name"));
                        numeric.setText(getStringIfExists(apnValue, "numeric"));
                        mcc.setText(getStringIfExists(apnValue, "mcc"));
                        mnc.setText(getStringIfExists(apnValue, "mnc"));
                        apn.setText(getStringIfExists(apnValue, "apn"));
                        user.setText(getStringIfExists(apnValue, "user"));
                        server.setText(getStringIfExists(apnValue, "server"));
                        password.setText(getStringIfExists(apnValue, "password"));
                        proxy.setText(getStringIfExists(apnValue, "proxy"));
                        port.setText(getStringIfExists(apnValue, "port"));
                        mmsProxy.setText(getStringIfExists(apnValue, "mmsproxy"));
                        mmsPort.setText(getStringIfExists(apnValue, "mmsport"));
                        mmsc.setText(getStringIfExists(apnValue, "mmsc"));
                        
                        
                        apnValueList.addView(v);
                    } catch (JSONException e) {
                        
                    }
                    
                }
            }
        }
    }
    
    // This could just be a regular Runnable 
    private class SaveApnValuesTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            // Serialize our JSON to a file on the SD card
            JSONArray result = getApnValues();
            String currentTime = String.valueOf(System.currentTimeMillis());
            String fileName = "apn-" + currentTime + ".gz";
            PrintWriter pw = null;
            GZIPOutputStream zipFile = null;
            File sdCardDir = Environment.getExternalStorageDirectory();
            
            if (sdCardDir != null) {
                try {
                    zipFile = new GZIPOutputStream(new FileOutputStream(sdCardDir.getAbsolutePath() + "/" + fileName));
                    pw = new PrintWriter(zipFile);
                    
                    pw.print(result.toString());
                    pw.flush();
                    pw.close();
                } catch (Exception e) {
                    Log.e("SaveApnValuesTask", getStackTrace(e));
                }
            }
            
            return null;
        }
    }
    
    public static String getStackTrace(Throwable t) {
        if (t != null) {
            StringWriter sWriter = new StringWriter();
            PrintWriter pWriter = new PrintWriter(sWriter);
            t.printStackTrace(pWriter);

            return sWriter.getBuffer().toString();
        }
        
        return "";
    }
    
    public String getStringIfExists(JSONObject object, String key) throws JSONException {
        if (object == null || key == null) return null;
        
        if (object.has(key)) {
            return object.getString(key);
        }
        
        return null;
    }
}
