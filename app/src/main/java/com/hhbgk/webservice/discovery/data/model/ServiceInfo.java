package com.hhbgk.webservice.discovery.data.model;

/**
 * Created by bob on 16-6-24.
 */
public class ServiceInfo implements Camera {
    protected String namespace;
    protected String uri;//XAddr

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }
}
