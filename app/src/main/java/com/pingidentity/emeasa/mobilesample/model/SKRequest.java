package com.pingidentity.emeasa.mobilesample.model;

public abstract class SKRequest {


    protected String id;
    protected String eventName;

    public SKRequest(SKResponse response) {
        id = response.getResponseId();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public abstract String toJSON () ;
}
