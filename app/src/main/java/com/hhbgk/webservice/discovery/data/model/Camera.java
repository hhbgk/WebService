package com.hhbgk.webservice.discovery.data.model;

public interface Camera {
//    protected String namespace;
//    protected String uri;//XAddr

    public abstract String getNamespace();
    public abstract void setNamespace(String namespace);

    public abstract String getUri();
    public abstract void setUri(String uri);
}
