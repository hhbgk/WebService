package com.hhbgk.webservice.discovery.data.model;

public class StreamInfo implements Camera {
    private String streamUri;

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public void setNamespace(String namespace) {

    }

    @Override
    public String getUri() {
        return null;
    }

    @Override
    public void setUri(String uri) {

    }

    public String getStreamUri(){
        return streamUri;
    }

    public void setStreamUri(String streamUri) {
        this.streamUri = streamUri;
    }

}
