package com.hhbgk.webservice.discovery.data.model;

/**
 * Author: bob
 * Date: 16-6-28 15:41
 * Version: V1
 * Description:
 */
public class PtzNode {
    private PtzSpaceInfo ptzSpaceInfo;
    private Integer maximumNumberOfPresets;
    private Boolean isHomeSupported;

    public PtzSpaceInfo getPtzSpaceInfo() {
        return ptzSpaceInfo;
    }

    public void setPtzSpaceInfo(PtzSpaceInfo ptzSpaceInfo) {
        this.ptzSpaceInfo = ptzSpaceInfo;
    }

    public int getMaximumNumberOfPresets() {
        return maximumNumberOfPresets;
    }

    public void setMaximumNumberOfPresets(int maximumNumberOfPresets) {
        this.maximumNumberOfPresets = maximumNumberOfPresets;
    }

    public boolean isHomeSupported() {
        return isHomeSupported;
    }

    public void setHomeSupported(boolean homeSupported) {
        isHomeSupported = homeSupported;
    }
}
