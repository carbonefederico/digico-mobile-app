package com.pingidentity.emeasa.mobilesample.model;

import android.util.Log;

import org.json.JSONObject;

public class SKContinueRequest extends SKRequest {

    private static final String TAG = "SKContinueRequest";

    public SKContinueRequest(SKResponse response) {
        super(response);
        eventName = "continue";
    }

    public String toJSON () {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("eventName", eventName);
            jsonObject.put("id", id);
        } catch (Exception e) {
            Log.e (TAG,"Error serializing ", e);
        }
        return jsonObject.toString();
    }
}
