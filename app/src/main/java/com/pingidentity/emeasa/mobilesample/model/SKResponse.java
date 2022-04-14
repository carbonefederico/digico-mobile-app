package com.pingidentity.emeasa.mobilesample.model;

import android.util.Log;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class SKResponse {

    private final static String TAG = "SKReponse";
        private String responseId;
        private String interactionToken;
        private JSONObject screen;
        private String interactionId;
        private String companyId;
        private String connectionId;
        private String connectorId;
        private String id;
        private String capabilityName;
        private String accessToken;
        private String tokenType;
        private Integer expiresIn;
        private String idToken;
        private Boolean success;
        private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public SKResponse (JSONObject response) {
        responseId = response.optString("id");
        interactionId = response.optString("interactionId");
        interactionToken = response.optString("interactionToken");
        screen = response.optJSONObject("screen");
        connectionId = response.optString("connectionId");
        capabilityName = response.optString("capabilityName");
        JSONObject jsonAdditionalProperties = response.optJSONObject("additionalProperties");
        if (isSuccess()) {
            Log.i(TAG, "isSuccess");
            additionalProperties.put("username", jsonAdditionalProperties.optString("username"));
            additionalProperties.put("given_name", jsonAdditionalProperties.optString("given_name"));
        }
    }

    public boolean isSuccess() {
        if (capabilityName.equalsIgnoreCase("createSuccessResponse")) {
            return true;
        }
        return false;
    }


    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public String getInteractionToken() {
        return interactionToken;
    }

    public void setInteractionToken(String interactionToken) {
        this.interactionToken = interactionToken;
    }

    public JSONObject getScreen() {
        return screen;
    }

    public void setScreen(JSONObject screen) {
        this.screen = screen;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public String getInteractionId() {
            return interactionId;
        }

        public void setInteractionId(String interactionId) {
            this.interactionId = interactionId;
        }

        public String getCompanyId() {
            return companyId;
        }

        public void setCompanyId(String companyId) {
            this.companyId = companyId;
        }

        public String getConnectionId() {
            return connectionId;
        }

        public void setConnectionId(String connectionId) {
            this.connectionId = connectionId;
        }

        public String getConnectorId() {
            return connectorId;
        }

        public void setConnectorId(String connectorId) {
            this.connectorId = connectorId;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCapabilityName() {
            return capabilityName;
        }

        public void setCapabilityName(String capabilityName) {
            this.capabilityName = capabilityName;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public Integer getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Integer expiresIn) {
            this.expiresIn = expiresIn;
        }

        public String getIdToken() {
            return idToken;
        }

        public void setIdToken(String idToken) {
            this.idToken = idToken;
        }

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }

        public Map<String, Object> getAdditionalProperties() {
            return this.additionalProperties;
        }

        public void setAdditionalProperty(String name, Object value) {
            this.additionalProperties.put(name, value);
        }



}
