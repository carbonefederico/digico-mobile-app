package com.pingidentity.emeasa.mobilesample.model;

import android.util.Log;

import org.json.JSONObject;

public class SKSubmitFormInputRequest extends SKRequest{

    private static final String TAG = "SKSubmitFormInputRequest";
    private JSONObject nextEvent;
    private String connectionId;
    private String capabilityName;
    private int userViewIndex = 0;
    private JSONObject parameters;

    public SKSubmitFormInputRequest (SKResponse response) {
        super(response);
        nextEvent = response.getScreen().optJSONObject("properties").optJSONObject("nextEvent");
        connectionId = response.getConnectionId();
        capabilityName = response.getCapabilityName();
        eventName = "submitFormInput";
    }


    public String toJSON () {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("nextEvent", nextEvent);
            jsonObject.put("eventName", eventName);
            jsonObject.put("connectionId", connectionId);
            jsonObject.put("capabilityName", capabilityName);
            jsonObject.put("userViewIndex", userViewIndex);
            jsonObject.put("id", id);
            jsonObject.put("parameters",parameters);
        } catch (Exception e) {
            Log.e (TAG,"Error serializing ", e);
        }
        return jsonObject.toString();
    }

    public JSONObject getNextEvent() {
        return nextEvent;
    }

    public void setNextEvent(JSONObject nextEvent) {
        this.nextEvent = nextEvent;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getCapabilityName() {
        return capabilityName;
    }

    public void setCapabilityName(String capabilityName) {
        this.capabilityName = capabilityName;
    }

    public int getUserViewIndex() {
        return userViewIndex;
    }

    public void setUserViewIndex(int userViewIndex) {
        this.userViewIndex = userViewIndex;
    }

    public JSONObject getParameters() {
        return parameters;
    }

    public void setParameters(JSONObject parameters) {
        this.parameters = parameters;
    }
}
