package com.hhbgk.webservice.discovery.data.model;

public class CameraInfo implements Camera {
    protected String namespace;
    protected String uri;//XAddr
    /**
     * identifier
     */
    protected String name;
    protected String location;
    protected String hardwareVersion;
    protected String softwareVersion;
    protected String mac;
    protected String snapshotUri;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation(){
        return location;
    }

    public void setHardwareVersion(String version) {
        hardwareVersion = version;
    }

    public String getHardwareVersion() {
        return hardwareVersion;
    }
    public void setSoftwareVersion (String version) {
        softwareVersion = version;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }
    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getSnapshotUri() {
        return snapshotUri;
    }

    public void setSnapshotUri(String snapshotUri) {
        this.snapshotUri = snapshotUri;
    }

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

    @Override
    public String toString() {
        return  "\n<Camera Name:"              + name
                + ">\n<Hardware Version:"      + hardwareVersion
                + ">\n<Software Version:"      + softwareVersion
                + ">\n<Mac address:"           + mac
                + ">\n<location:"              + location
                + ">\n<URI:"                   + uri
                + ">\n<Snapshot JPEG Uri:"     + snapshotUri
                + ">";
    }

}
