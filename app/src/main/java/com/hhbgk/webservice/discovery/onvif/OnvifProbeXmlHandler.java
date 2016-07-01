package com.hhbgk.webservice.discovery.onvif;

import com.hhbgk.webservice.discovery.data.model.CameraInfo;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OnvifProbeXmlHandler extends DefaultHandler {
    private String tag = getClass().getSimpleName();
    private String mLocalName = "";
    private CameraInfo mCameraInfo;

    private OnCompletionListener mOnCompletionListener = null;
    public interface OnCompletionListener {
        void onCompletion(Object object);
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    @Override
    public void startDocument() throws SAXException {
        //Log.i(tag, "startDocument=");
        mCameraInfo = new CameraInfo();
    }

    @Override
    public void endDocument() throws SAXException {
        //Log.i(tag, "CameraInfo=" + mCameraInfo.toString());
        if (mOnCompletionListener != null) {
            mOnCompletionListener.onCompletion(mCameraInfo);
            mOnCompletionListener = null;
        }
        //Log.i(tag, "endDocument=");
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        //Log.w(tag, "startElement localName=" + localName + ", qName=" + qName);
        mLocalName = localName;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        //Log.w(tag, "endElement localName=" + localName + ", qName=" + qName);
        mLocalName = "";
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        switch (mLocalName) {
            case "XAddrs":
                char[] xaddrs = new char[length];
                System.arraycopy(ch, start, xaddrs, 0, length);
                //Log.e(tag, "characters=" + String.valueOf(xaddrs));
                mCameraInfo.setUri(String.valueOf(xaddrs));
                break;
            case "Scopes":
                char[] scopes = new char[length];
                System.arraycopy(ch, start, scopes, 0, length);
                //Log.e(tag, "characters=" + String.valueOf(scopes));
                String[] scopesArray = String.valueOf(scopes).split(" ");
                for (String s : scopesArray) {
                    if (s.startsWith("onvif://www.onvif.org/name/")) {
                        String name = s.substring("onvif://www.onvif.org/name/".length());
                        mCameraInfo.setName(name);
                    } else if (s.startsWith("onvif://www.onvif.org/location/")){
                        String location = s.substring("onvif://www.onvif.org/location/".length());
                        mCameraInfo.setLocation(location);
                    }
                }
                break;
        }
    }
}
