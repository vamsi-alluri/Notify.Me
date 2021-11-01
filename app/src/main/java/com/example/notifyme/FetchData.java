package com.example.notifyme;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.monkeylearn.MonkeyLearn;
import com.monkeylearn.MonkeyLearnException;
import com.monkeylearn.MonkeyLearnResponse;

import org.json.JSONObject;
import org.json.simple.JSONArray;

import java.util.Arrays;

import javax.security.auth.callback.Callback;

public class FetchData extends AsyncTask<Void, Void, Void>  {
    String[] data = new String[1];
    MonkeyLearnResponse res = null;
    String category = null;
    Runnable context;
    Callback cb;
    FetchData(String text) {
        data[0] = text;
    }
    @Override
    protected Void doInBackground(Void... voids) {
//        NotificationInfo.emergency = false;
        MonkeyLearn ml = new MonkeyLearn("3056b7af89a3303bc9d34fd8c32edd390c4dc7c0");
        String modelId = "cl_zANWFJd8";
        Intent intent;
        try {
            res = ml.classifiers.classify(modelId, data, true);
        } catch (MonkeyLearnException e) {
            e.printStackTrace();
        }

        System.out.println(data[0]+  res.arrayResult );
        String s = Arrays.toString(new JSONArray[]{res.arrayResult});
        String[] tokens = s.split(":");
        String msg = tokens[tokens.length - 1].substring(1,tokens[tokens.length - 1].length()-6);
        Log.i("result Nikhitha:   ", Arrays.toString(new JSONArray[]{res.arrayResult}));
        Log.e("fetch emergency:    " + data[0], msg);
        NotificationInfo.setemergency(msg);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {

        super.onPostExecute(aVoid);

    }

}
