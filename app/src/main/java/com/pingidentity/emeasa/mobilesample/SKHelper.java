package com.pingidentity.emeasa.mobilesample;


import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.pingidentity.emeasa.mobilesample.model.SKContinueRequest;
import com.pingidentity.emeasa.mobilesample.model.SKRequest;
import com.pingidentity.emeasa.mobilesample.model.SKResponse;
import com.pingidentity.pingidsdkv2.PairingObject;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;
import com.pingidentity.pingidsdkv2.types.PairingInfo;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class SKHelper {

    private static final String TAG = "SKHelper";
    private String flowInitiationToken;
    private MainActivity callingActivity;
    private String nextURL;
    private SKResponse skResponse;
    private SKRequest nextRequest;

    public void postRequest(String flowID, JSONObject flowInput, MainActivity mainActivity) {
        Log.i(TAG, "postRequest " + flowID);
        callingActivity = mainActivity;

        String requestURL = String.format("%scompany/%s/sdkToken", Consts.SK_API_URL_BASE, Consts.COMPANY_ID);
        Log.i(TAG, "requestURL " + requestURL);
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("X-SK-API-KEY", Consts.API_KEY);
        client.get(requestURL, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    flowInitiationToken = response.getString("access_token");
                    Log.i(TAG, "flowInitiationToken " + flowInitiationToken);
                    postRequestWithToken(flowID, flowInput, mainActivity);
                } catch (Exception e) {
                    Log.e(TAG, "Error", e);
                    callingActivity.showErrorMessage("An error has occurred. Try again later");
                }
            }
        });
    }

    public void postRequestWithToken(String flowID, JSONObject flowInput, MainActivity mainActivity) {
        Log.i(TAG, "postRequestWithToken " + flowID);
        String requestURL;
        StringEntity entity;
        if (nextURL == null || nextURL.isEmpty()) {
            // start flow
            Log.i(TAG, "start flow " + flowID);
            requestURL = String.format("%sauth/%s/flows/%s/start", Consts.SK_API_URL_BASE, Consts.COMPANY_ID, flowID);
            entity = new StringEntity(flowInput.toString(), "UTF-8");
        } else {
            // continue flow
            Log.i(TAG, "continue flow " + flowID);
            requestURL = nextURL;
            entity = new StringEntity(nextRequest.toJSON(), "UTF-8");
        }

        AsyncHttpClient client = getAsyncHttpClient();
        Log.i(TAG, "request url " + requestURL);

        client.post(callingActivity, requestURL, entity, "application/json", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.i(TAG, "postRequestWithToken - onSuccess " + response.toString());
                    skResponse = new SKResponse(response);
                    if (skResponse.isSuccess()) {
                        Log.i(TAG, "createSession");
                        callingActivity.setUser(skResponse);
                        doCleanUp();
                    } else if (skResponse.getCapabilityName().equalsIgnoreCase("customHtmlMessage")) {
                        Log.i(TAG, "customHtmlMessage");
                        nextURL = String.format("%sauth/%s/connections/%s/capabilities/%s", Consts.SK_API_URL_BASE, Consts.COMPANY_ID, skResponse.getConnectionId(), skResponse.getCapabilityName());
                        String msgTitle = skResponse.getScreen().getJSONObject("properties").getJSONObject("messageTitle").getString("value");
                        if ("pairing".equalsIgnoreCase(msgTitle)) {
                            pairDevice(flowID, flowInput, mainActivity);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage(), e);
                    callingActivity.showErrorMessage("An error has occurred. Try again later");
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject response) {
                Log.i(TAG, "startFlowWithToken - onFailure " + response.toString());
                Log.e(TAG, t.getMessage(), t);
                doCleanUp();
                callingActivity.showErrorMessage("An error has occurred. Try again later");
            }

        });
    }

    private void pairDevice(String flowID, JSONObject flowInput, MainActivity mainActivity) throws JSONException {
        Log.i(TAG, "pairing");
        nextRequest = new SKContinueRequest(skResponse);
        String pairingToken = skResponse.getScreen().getJSONObject("properties").getJSONObject("message").getString("value");
        Log.i(TAG, "pairing token " + pairingToken);
        PingOne.processIdToken(pairingToken, new PingOne.PingOnePairingObjectCallback() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable PairingObject pairingObject, @Nullable @org.jetbrains.annotations.Nullable PingOneSDKError error) {
                pairingObject.approve(callingActivity, new PingOne.PingOneSDKPairingCallback() {
                    @Override
                    public void onComplete(PairingInfo pairingInfo, @Nullable @org.jetbrains.annotations.Nullable PingOneSDKError error) {
                        Log.i(TAG, "pairingObject.approve approve. Pairing info " + pairingInfo + " error " + error);
                    }

                    @Override
                    public void onComplete(@Nullable @org.jetbrains.annotations.Nullable PingOneSDKError error) {
                        Log.i(TAG, "pairingObject.approve approve error " + error);
                    }
                });
            }
        });
        postRequestWithToken(flowID, new JSONObject(), mainActivity);
    }

    @NonNull
    private AsyncHttpClient getAsyncHttpClient() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("Authorization", String.format("Bearer %s", this.flowInitiationToken));
        if (skResponse != null) {
            client.addHeader("interactionId", skResponse.getInteractionId());
            client.addHeader("interactionToken", skResponse.getInteractionToken());
        }
        return client;
    }


    private void doCleanUp() {
        this.nextRequest = null;
        this.skResponse = null;
        this.nextURL = null;
    }

}
