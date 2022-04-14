package com.pingidentity.emeasa.mobilesample;


import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.loopj.android.http.AsyncHttpClient;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.pingidentity.emeasa.mobilesample.model.SKContinueRequest;
import com.pingidentity.emeasa.mobilesample.model.SKRequest;
import com.pingidentity.emeasa.mobilesample.model.SKResponse;
import com.pingidentity.emeasa.mobilesample.model.SKSubmitFormInputRequest;
import com.pingidentity.pingidsdkv2.PairingObject;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;
import com.pingidentity.pingidsdkv2.types.PairingInfo;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class SKHelper {

    private String flowInitiationToken;
    private MainActivity callingActivity;
    private String nextURL;

    private SKResponse skResponse;
    private SKRequest nextRequest;

    private static String TAG = "SKHelper";

    public void startFlow(MainActivity activity, JSONObject input, String flowId) {
        Log.i(TAG, "startFlow " + flowId);
        callingActivity = activity;
        if (flowInitiationToken == null) {
            fetchFlowInitiationToken(flowId, input);
        } else {
            startFlowWithToken(flowId, input);
        }
    }

    public void startFlowWithToken(String flowID, JSONObject flowInput) {
        Log.i(TAG, "startFlowWithToken " + flowID);
        // Here we can be sure that the token is not null
        String requestURL = String.format("%sauth/%s/flows/%s/start", Consts.SK_API_URL_BASE, Consts.COMPANY_ID, flowID);
        Log.i(TAG, "requestURL " + requestURL);
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("Authorization", String.format("Bearer %s", this.flowInitiationToken));
        Log.i(TAG, "added token " + this.flowInitiationToken);
        StringEntity entity = new StringEntity(flowInput.toString(), "UTF-8");
        Log.i(TAG, "flow payload " + flowInput.toString());
        client.post(callingActivity, requestURL, entity, "application/json", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.i(TAG, "startFlowWithToken - onSuccess " + response.toString());
                    skResponse = new SKResponse(response);
                    if (skResponse.isSuccess()) {
                        Log.i(TAG, "createSession");
                        callingActivity.setUser(skResponse);
                        doCleanUp();
                    } else {
                        Log.i(TAG, "createSession false");
                        nextRequest = new SKSubmitFormInputRequest(skResponse);
                        nextURL = String.format("%sauth/%s/connections/%s/capabilities/%s", Consts.SK_API_URL_BASE, Consts.COMPANY_ID, skResponse.getConnectionId(), skResponse.getCapabilityName());
                        Log.i(TAG, "nextURL " + nextURL);
                        callingActivity.updateFlowUI(skResponse.getScreen());
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

    public void continueFlow(MainActivity mainActivity, String[] formInputs) throws JSONException {
        Log.i(TAG, "continueFlow");
        JSONObject flowInput = new JSONObject();
        for (int i = 0; i < formInputs.length; i++) {
            String inputValue = formInputs[i];
            if (inputValue != null && inputValue.length() > 0) {
                String paramName = skResponse.getScreen().getJSONObject("properties").getJSONObject("formFieldsList").getJSONArray("value").getJSONObject(i).getString("propertyName");
                flowInput.put(paramName, inputValue);
            }
        }
        Log.i(TAG, "flowInput " + flowInput.toString());
        if (flowInput.keys().hasNext()) {
            ((SKSubmitFormInputRequest) nextRequest).setParameters(flowInput);
        }
        AsyncHttpClient client = getAsyncHttpClient();
        StringEntity entity = new StringEntity(nextRequest.toJSON(), "UTF-8");
        Log.i(TAG, "continueFlow - nextPayload " + nextRequest.toJSON());
        client.post(callingActivity, nextURL, entity, "application/json", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.i(TAG, "continueFlow - onSuccess " + response.toString());
                try {
                    skResponse = new SKResponse(response);
                    nextURL = String.format("%sauth/%s/connections/%s/capabilities/%s", Consts.SK_API_URL_BASE, Consts.COMPANY_ID, skResponse.getConnectionId(), skResponse.getCapabilityName());
                    if (skResponse.getCapabilityName().equalsIgnoreCase("htmlFormInput")) {
                        Log.i(TAG, "htmlFormInput ");
                        nextRequest = new SKSubmitFormInputRequest(skResponse);
                        callingActivity.updateFlowUI(skResponse.getScreen());
                    } else if (skResponse.getCapabilityName().equalsIgnoreCase("customHtmlMessage")) {
                        Log.i(TAG, "customHtmlMessage");
                        String msgTitle = skResponse.getScreen().getJSONObject("properties").getJSONObject("messageTitle").getString("value");
                        if ("pairing".equalsIgnoreCase(msgTitle)) {
                            Log.i(TAG, "pairing");
                            nextRequest = new SKContinueRequest(skResponse);
                            String pairingToken = skResponse.getScreen().getJSONObject("properties").getJSONObject("message").getString("value");
                            PingOne.processIdToken(pairingToken, new PingOne.PingOnePairingObjectCallback() {
                                @Override
                                public void onComplete(@Nullable @org.jetbrains.annotations.Nullable PairingObject pairingObject, @Nullable @org.jetbrains.annotations.Nullable PingOneSDKError error) {
                                    pairingObject.approve(callingActivity, new PingOne.PingOneSDKPairingCallback() {
                                        @Override
                                        public void onComplete(PairingInfo pairingInfo, @Nullable @org.jetbrains.annotations.Nullable PingOneSDKError error) {
                                            Log.i(TAG, "pairingObject.approve approve. Pairing info " + pairingInfo + " error " + error );
                                        }

                                        @Override
                                        public void onComplete(@Nullable @org.jetbrains.annotations.Nullable PingOneSDKError error) {
                                            Log.i(TAG, "pairingObject.approve approve error " + error );
                                        }
                                    });
                                }
                            });
                            continueFlow(mainActivity, new String[5]);
                        } else {
                            //This is a standard message to display and move on
                            nextRequest = new SKContinueRequest(skResponse);
                            callingActivity.updateFlowUIWithMessage(skResponse.getScreen());
                        }

                    } else if (skResponse.isSuccess()) {
                        Log.i(TAG, "createSession");
                        callingActivity.setUser(skResponse);
                        doCleanUp();
                    }
                } catch (Exception e) {
                    Log.e(TAG, response.toString(), e);
                    doCleanUp();
                    callingActivity.showErrorMessage("An error has occurred. Try again later");

                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject response) {
                Log.e (TAG,"continueFlow - onFailure ");
                Log.e(TAG, response.toString());
                callingActivity.showErrorMessage("An error has occurred. Try again");
            }
        });
    }

    @NonNull
    private AsyncHttpClient getAsyncHttpClient() {
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("Authorization", String.format("Bearer %s", this.flowInitiationToken));
        client.addHeader("interactionId", skResponse.getInteractionId());
        client.addHeader("interactionToken", skResponse.getInteractionToken());
        return client;
    }

    private void fetchFlowInitiationToken(String flowID, JSONObject flowInput) {
        Log.i(TAG, "fetchFlowInitiationToken " + flowID);
        try {
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
                        startFlowWithToken(flowID, flowInput);
                    } catch (Exception e) {
                        Log.e(TAG, "Error", e);
                        callingActivity.showErrorMessage("An error has occurred. Try again later");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
            callingActivity.showErrorMessage("An error has occurred. Try again later");
        }
    }

    private void doCleanUp() {
        this.nextRequest = null;
        this.skResponse = null;
        this.nextURL = null;
    }

}
