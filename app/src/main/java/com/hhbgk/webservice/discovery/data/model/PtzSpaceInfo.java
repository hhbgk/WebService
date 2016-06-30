package com.hhbgk.webservice.discovery.data.model;

/**
 * Author: bob
 * Date: 16-6-28 15:34
 * Version: V1
 * Description:
 */
public class PtzSpaceInfo implements Camera {
    private String uri;
    private Float minXRange;
    private Float maxXRange;
    private Float minYRange;
    private Float maxYRange;

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public void setNamespace(String namespace) {

    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }

    public Float getMinXRange() {
        return minXRange;
    }

    public void setMinXRange(Float minXRange) {
        this.minXRange = minXRange;
    }

    public Float getMaxXRange() {
        return maxXRange;
    }

    public void setMaxXRange(Float maxXRange) {
        this.maxXRange = maxXRange;
    }

    public Float getMinYRange() {
        return minYRange;
    }

    public void setMinYRange(Float minYRange) {
        this.minYRange = minYRange;
    }

    public Float getMaxYRange() {
        return maxYRange;
    }

    public void setMaxYRange(Float maxYRange) {
        this.maxYRange = maxYRange;
    }
}
