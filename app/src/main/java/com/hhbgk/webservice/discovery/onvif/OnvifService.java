package com.hhbgk.webservice.discovery.onvif;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.hhbgk.webservice.discovery.data.model.CameraInfo;
import com.hhbgk.webservice.discovery.data.model.PtzNode;
import com.hhbgk.webservice.discovery.data.model.PtzSpaceInfo;
import com.hhbgk.webservice.discovery.data.model.ServiceInfo;
import com.hhbgk.webservice.discovery.data.model.StreamInfo;
import com.hhbgk.webservice.discovery.util.Dbug;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class OnvifService {
    private final String tag = getClass().getSimpleName();

    private static OnvifService instance = null;
    private final static int WS_DISCOVERY_PORT = 3702;
    private final static String WS_DISCOVERY_ADDRESS_IPv4 = "239.255.255.250";
    private final static String WS_DISCOVERY_PROBE_MESSAGE =
            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" " +
            "xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" " +
            "xmlns:tns=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\">" +
            "<soap:Header><wsa:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</wsa:Action>" +
            "<wsa:MessageID>urn:uuid:c032cfdd-c3ca-49dc-820e-ee6696ad63e2</wsa:MessageID>" +
            "<wsa:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</wsa:To></soap:Header>" +
            "<soap:Body><tns:Probe/></soap:Body></soap:Envelope>";
    private static final Random random = new SecureRandom();

    private final Handler mHandler;

    private static final int MSG_PROBE_WEB_SERVICE = 100;
    private static final int MSG_GET_SERVICES = 101;
    private static final int MSG_GET_MEDIA_SERVICE = 102;
    private static final int MSG_GET_PTZ_SERVICE = 103;
    private static final int MSG_SET_PTZ_CONTINUOUS_MOVE_UP = 200;
    private static final int MSG_SET_PTZ_CONTINUOUS_MOVE_DOWN = 201;
    private static final int MSG_SET_PTZ_CONTINUOUS_MOVE_LEFT = 202;
    private static final int MSG_SET_PTZ_CONTINUOUS_MOVE_RIGHT = 203;
    private static final int MSG_PTZ_GOTO_HOME_POSITION = 300;
    private static final int MSG_STOP_PTZ_CONTINUOUS_MOVE = 301;

    public static OnvifService getInstance() {
        if (null == instance) {
            instance = new OnvifService();
        }
        return instance;
    }

    private OnvifService() {
        HandlerThread probeThread = new HandlerThread("HandlerThread_"+new Random().nextInt(Integer.MAX_VALUE));
        probeThread.start();
        Handler.Callback handlerCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Log.i(tag, "what==" + msg.what);
                switch (msg.what) {
                    case MSG_PROBE_WEB_SERVICE:
                        for (InetAddress inetAddress : getLoopAddress()) {
                            //Log.i(tag, "ip==" + inetAddress.getHostName());
                            if (inetAddress instanceof Inet4Address) {
                                broadcastProbeMessage(msg, inetAddress);
                            }
                        }
                        break;
                    case MSG_GET_SERVICES:
                    case MSG_GET_MEDIA_SERVICE:
                    case MSG_GET_PTZ_SERVICE:
                    case MSG_SET_PTZ_CONTINUOUS_MOVE_UP:
                    case MSG_SET_PTZ_CONTINUOUS_MOVE_DOWN:
                    case MSG_STOP_PTZ_CONTINUOUS_MOVE:
                    case MSG_PTZ_GOTO_HOME_POSITION:
                    case MSG_SET_PTZ_CONTINUOUS_MOVE_RIGHT:
                    case MSG_SET_PTZ_CONTINUOUS_MOVE_LEFT:
                        postRequest(msg);
                        break;
                }

                return false;
            }
        };
        mHandler = new Handler(probeThread.getLooper(), handlerCallback);
    }

    /**请求服务和操作
     * @param msg 具体请求的信息
     */
    private void postRequest(Message msg) {
        if (msg == null || msg.getData().isEmpty()) {
            throw new NullPointerException("message is null");
        }
        Bundle bundle = msg.getData();
        String deviceService = bundle.getString("service_url");
        String namespace = bundle.getString("name_space");
        String soapAction = bundle.getString("soap_action");
        String methodName =bundle.getString("method_name");

        Log.e(tag, "soapAction=" + soapAction + "\nnameSpace=" + namespace + "\nmethodName" + methodName);
        SoapObject soapObject = new SoapObject(namespace, methodName);
        packageRequest(soapObject, msg, namespace);

        final SoapSerializationEnvelope soapSerializationEnvelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
        soapSerializationEnvelope.bodyOut = soapObject;
        soapSerializationEnvelope.dotNet = false;
        soapSerializationEnvelope.implicitTypes = true;
        soapSerializationEnvelope.setAddAdornments(false);

        soapSerializationEnvelope.headerOut = new Element[1];
        soapSerializationEnvelope.headerOut[0] = createHeader(soapSerializationEnvelope, "admin", "888888");

        final HttpTransportSE httpSe = new HttpTransportSE(deviceService, 5000);
        try {
            httpSe.call(soapAction, soapSerializationEnvelope);
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }

        Object response = null;
        try {
            response = soapSerializationEnvelope.getResponse();
        } catch (SoapFault soapFault) {
            soapFault.printStackTrace();
        }

        if (response instanceof SoapFault) {
            SoapFault fault = (SoapFault) response;
            Exception ex = new Exception(fault.faultstring);
            String faultInformation = "Request fault:" + ex.getMessage() + ", fault code:" + fault.faultcode;
            if (msg.obj instanceof OnGetMediaServiceListener) {
                ((OnGetMediaServiceListener)msg.obj).onFailure(faultInformation);
            } else if (msg.obj instanceof OnGetServicesListener) {
                ((OnGetServicesListener)msg.obj).onFailure(faultInformation);
            } else if(msg.obj instanceof OnProbeListener) {
                ((OnProbeListener)msg.obj).onFailure(faultInformation);
            } else if (msg.obj instanceof OnGetPtzServiceListener) {
                ((OnGetPtzServiceListener)msg.obj).onFailure(faultInformation);
            }

        } else if (response instanceof SoapPrimitive) {
            SoapPrimitive soapPrimitive = (SoapPrimitive) response;
            Dbug.e(tag, "soapPrimitive="+ soapPrimitive.getName());
        }

        SoapObject result = (SoapObject) soapSerializationEnvelope.bodyIn;
        if (result != null && result.getPropertyCount() > 0) {
            Log.w(tag, "result getName: " + result.getName() + ", getPropertyCount=" + result.getPropertyCount() + ", result=\n" + result.toString());
            parseResponse(result, msg);
        } else {
            Log.e(tag, "soapSerializationEnvelope.bodyIn is empty.");
        }
    }

    /**打包请求的操作信息
     * @param soapObject 打包对象
     * @param msg 信息
     * @param namespace 命名空间
     */
    private void packageRequest(SoapObject soapObject, Message msg, String namespace) {
        SoapObject tempSoapObject, attributeSoapObject;
        String ttNamespace = "http://www.onvif.org/ver10/schema";
        switch (msg.what) {
            case MSG_GET_SERVICES:
                soapObject.addProperty(namespace, "IncludeCapability", "false");
                break;
            case MSG_GET_MEDIA_SERVICE:
                tempSoapObject = new SoapObject(namespace, "StreamSetup");

                SoapObject transportSoapObject = new SoapObject();
                transportSoapObject.addProperty(ttNamespace, "Protocol", "UDP");

                tempSoapObject.addProperty(ttNamespace, "Transport", transportSoapObject);
                tempSoapObject.addProperty(ttNamespace, "Stream", "RTP-Unicast");

                soapObject.addSoapObject(tempSoapObject);
                soapObject.addProperty(namespace, "ProfileToken", "PROFILE_000");
                break;

            case MSG_SET_PTZ_CONTINUOUS_MOVE_UP:
                soapObject.addProperty(namespace, "ProfileToken", "PROFILE_000");

                attributeSoapObject = new SoapObject();
                attributeSoapObject.addAttribute("space", "http://www.onvif.org/ver10/tptz/PanTiltSpaces/VelocityGenericSpace");
                attributeSoapObject.addAttribute("y", "0.5");
                attributeSoapObject.addAttribute("x", "0");

                tempSoapObject = new SoapObject(namespace, "Velocity");
                tempSoapObject.addProperty(ttNamespace, "PanTilt", attributeSoapObject);
                soapObject.addSoapObject(tempSoapObject);
                break;

            case MSG_SET_PTZ_CONTINUOUS_MOVE_DOWN:
                soapObject.addProperty(namespace, "ProfileToken", "PROFILE_000");

                attributeSoapObject = new SoapObject();
                attributeSoapObject.addAttribute("space", "http://www.onvif.org/ver10/tptz/PanTiltSpaces/VelocityGenericSpace");
                attributeSoapObject.addAttribute("y", "-0.5");
                attributeSoapObject.addAttribute("x", "0");

                tempSoapObject = new SoapObject(namespace, "Velocity");
                tempSoapObject.addProperty(ttNamespace, "PanTilt", attributeSoapObject);
                soapObject.addSoapObject(tempSoapObject);
                break;

            case MSG_SET_PTZ_CONTINUOUS_MOVE_LEFT:
                soapObject.addProperty("ProfileToken", "PROFILE_000");

                attributeSoapObject = new SoapObject();
                attributeSoapObject.addAttribute("space", "http://www.onvif.org/ver10/tptz/PanTiltSpaces/VelocityGenericSpace");
                attributeSoapObject.addAttribute("x", "-0.5");
                attributeSoapObject.addAttribute("y", "0");

                tempSoapObject = new SoapObject(namespace, "Velocity");
                tempSoapObject.addProperty(ttNamespace, "PanTilt", attributeSoapObject);
                soapObject.addSoapObject(tempSoapObject);
                break;

            case MSG_SET_PTZ_CONTINUOUS_MOVE_RIGHT:
                soapObject.addProperty("ProfileToken", "PROFILE_000");

                attributeSoapObject = new SoapObject();
                attributeSoapObject.addAttribute("space", "http://www.onvif.org/ver10/tptz/PanTiltSpaces/VelocityGenericSpace");
                attributeSoapObject.addAttribute("x", "0.5");
                attributeSoapObject.addAttribute("y", "0");

                tempSoapObject = new SoapObject(namespace, "Velocity");
                tempSoapObject.addProperty(ttNamespace, "PanTilt", attributeSoapObject);
                soapObject.addSoapObject(tempSoapObject);
                break;

            case MSG_STOP_PTZ_CONTINUOUS_MOVE:
                soapObject.addProperty(namespace, "ProfileToken", "PROFILE_000");
                soapObject.addProperty(namespace, "PanTilt", "true");
                soapObject.addProperty(namespace, "Zoom", "false");
                break;

            case MSG_PTZ_GOTO_HOME_POSITION:
                soapObject.addProperty(namespace, "ProfileToken", "PROFILE_000");

                attributeSoapObject = new SoapObject();
                attributeSoapObject.addAttribute("space", "http://www.onvif.org/ver10/tptz/PanTiltSpaces/VelocityGenericSpace");
                attributeSoapObject.addAttribute("y", "0");
                attributeSoapObject.addAttribute("x", "0");

                tempSoapObject = new SoapObject(namespace, "Speed");
                tempSoapObject.addProperty(ttNamespace, "PanTilt", attributeSoapObject);
                soapObject.addSoapObject(tempSoapObject);
                break;
        }
    }

    private void parseResponse(SoapObject result, Message msg) {
        switch (msg.what) {
            case MSG_GET_SERVICES:
                List<ServiceInfo> serviceInfoList = new ArrayList<>();
                for (int i = 0; i < result.getPropertyCount(); i++) {
                    SoapObject sub = (SoapObject) result.getProperty(i);
                    ServiceInfo serviceInfo = new ServiceInfo();
                    serviceInfo.setNamespace(sub.getPropertySafely("Namespace").toString());
                    serviceInfo.setUri(sub.getPropertySafely("XAddr").toString());
                    serviceInfoList.add(serviceInfo);
                }
                OnGetServicesListener onResponseListener = (OnGetServicesListener) msg.obj;
                if (onResponseListener != null) {
                    onResponseListener.onSuccess(serviceInfoList);
                }
                break;
            case MSG_GET_MEDIA_SERVICE:
                List<StreamInfo> mediaServiceList = new ArrayList<>();
                for (int i = 0; i < result.getPropertyCount(); i++) {
                    SoapObject sub = (SoapObject) result.getProperty(i);
                    StreamInfo streamInfo = new StreamInfo();
                    streamInfo.setNamespace(sub.getPropertySafely("Namespace").toString());
                    streamInfo.setUri(sub.getPropertySafely("XAddr").toString());
                    streamInfo.setStreamUri(sub.getPropertySafely("Uri").toString());
                    mediaServiceList.add(streamInfo);
                }
                OnGetMediaServiceListener onGetMediaServiceListener = (OnGetMediaServiceListener) msg.obj;
                if (onGetMediaServiceListener != null) {
                    onGetMediaServiceListener.onSuccess(mediaServiceList);
                }
                break;
            case MSG_GET_PTZ_SERVICE:
                //Dbug.e(tag, "result count="+ result.getPropertyCount());
                List<PtzNode> ptzServiceList = new ArrayList<>();
                for (int i = 0; i < result.getPropertyCount(); i++) {
                    SoapObject ptzNodeObject = (SoapObject) result.getProperty(i);
                    //Dbug.w(tag, "HomeSupported="+ ptzNodeObject.getPropertySafely("HomeSupported")
                    //      +", name=" + ptzNodeObject.getPropertySafely("MaximumNumberOfPresets") + ", HomeSupported=" + ptzNodeObject.getPrimitivePropertySafely("HomeSupported"));

                    PtzNode ptzNode = new PtzNode();

                    ptzNode.setHomeSupported(Boolean.parseBoolean(ptzNodeObject.getPrimitivePropertySafely("HomeSupported").toString()));
                    ptzNode.setMaximumNumberOfPresets(Integer.parseInt(ptzNodeObject.getPropertySafely("MaximumNumberOfPresets").toString()));
                    SoapObject supportedPTZSpaces = (SoapObject) ptzNodeObject.getPropertySafely("SupportedPTZSpaces");
                    for (int j = 0; j < supportedPTZSpaces.getPropertyCount(); j ++){
                        PropertyInfo propertyInfo = supportedPTZSpaces.getPropertyInfo(j);
                        //Dbug.e(tag, "getName==\n" + propertyInfo.getName() + ">>>>>>>"+propertyInfo.getNamespace()+", >>>>>>"+propertyInfo.getElementType()
                        //+",>>>>>>>>>" + propertyInfo.getType()+",>>>>>>>" + propertyInfo.getFlags() + ",>>>>>>>>>>" +propertyInfo.getValue());
                        SoapObject space = (SoapObject) supportedPTZSpaces.getPropertySafely(propertyInfo.getName());
                        PtzSpaceInfo ptzSpaceInfo = new PtzSpaceInfo();
                        //Dbug.w(tag, "URI=" + space.getPropertySafely("URI")+ ", nameSpace=" + space.getNamespace() +", nameSpace="+ propertyInfo.getNamespace());
                        ptzSpaceInfo.setUri((String) space.getPrimitivePropertySafely("URI"));
                        if (space.hasProperty("XRange")){
                            SoapObject xRange = (SoapObject) space.getPropertySafely("XRange");
                            //Dbug.w(tag, "Min=" + xRange.getPropertySafely("Min") + ",Max="+ xRange.getPropertySafely("Max"));
                            ptzSpaceInfo.setMinXRange(Float.parseFloat(xRange.getPrimitivePropertySafely("Min").toString()));
                            ptzSpaceInfo.setMaxXRange(Float.parseFloat(xRange.getPrimitivePropertySafely("Max").toString()));
                        }
                        if (space.hasProperty("YRange")){
                            SoapObject yRange = (SoapObject) space.getPropertySafely("YRange");
                            //Dbug.w(tag, "Min=" + yRange.getPropertySafely("Min") + ",Max="+ yRange.getPropertySafely("Max"));
                            ptzSpaceInfo.setMinYRange(Float.parseFloat(yRange.getPrimitivePropertySafely("Min").toString()));
                            ptzSpaceInfo.setMaxYRange(Float.parseFloat(yRange.getPrimitivePropertySafely("Max").toString()));
                        }
                        ptzNode.setPtzSpaceInfo(ptzSpaceInfo);
                        ptzServiceList.add(ptzNode);
                    }
                }
                Dbug.w(tag, "ptzServiceList size=" + ptzServiceList.size());
                OnGetPtzServiceListener listener = (OnGetPtzServiceListener) msg.obj;
                if (listener != null) {
                    listener.onSuccess(ptzServiceList);
                }
                break;
        }
    }

    private Element createHeader(SoapSerializationEnvelope envelope, String userName, String password) {
        String securityNamespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
        Element header = new Element().createElement(securityNamespace, "Security");
        header.setPrefix("wsse", securityNamespace);
        header.setAttribute(envelope.env, "mustUnderstand", "true");

        String created = getCreated();
        String nonce = getNonce();

        Element usernameToken = new Element().createElement(securityNamespace, "UsernameToken");

        Element usernameElement = new Element().createElement(securityNamespace, "Username");
        usernameElement.addChild(Node.TEXT, userName);
        usernameToken.addChild(Node.ELEMENT, usernameElement);

        Element passwordElement = new Element().createElement(securityNamespace, "Password");
        String type = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest";
        //"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText"
        passwordElement.setAttribute(null, "Type", type);
        passwordElement.addChild(Node.TEXT, getPasswordEncode(nonce, password, created));
        usernameToken.addChild(Node.ELEMENT, passwordElement);

        Element nonceElement = new Element().createElement(securityNamespace, "Nonce");
        //"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary"
        nonceElement.addChild(Node.TEXT, nonce);
        usernameToken.addChild(Node.ELEMENT, nonceElement);

        String wsuNamespace = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
        Element createdElement = new Element().createElement(wsuNamespace, "Created");
        createdElement.setPrefix("wsu", wsuNamespace);
        createdElement.addChild(Node.TEXT, created);
        usernameToken.addChild(Node.ELEMENT, createdElement);

        header.addChild(Node.ELEMENT, usernameToken);
        return header;
    }

    public interface OnProbeListener {
        void onSuccess(CameraInfo cameraInfo);
        void onFailure(String message);
    }

    public interface OnGetServicesListener {
        void onSuccess(List<ServiceInfo> serviceInfoList);
        void onFailure(String message);
    }

    public interface OnGetMediaServiceListener {
        void onSuccess(List<StreamInfo> streamInfoList);
        void onFailure(String message);
    }

    public interface OnGetPtzServiceListener {
        void onSuccess(List<PtzNode> ptzNodeInfoList);
        void onFailure(String message);
    }

    public void probeDeviceService(OnProbeListener listener) {
        //Log.e(tag, "mOnProbeListener=" + mOnProbeListener);
        mHandler.removeMessages(MSG_PROBE_WEB_SERVICE);
        Message message = Message.obtain();
        message.what = MSG_PROBE_WEB_SERVICE;
        message.obj = listener;
        mHandler.sendMessageDelayed(message, 200);
    }

    public void requestServices(String deviceServiceURL, OnGetServicesListener listener) {
        mHandler.removeMessages(MSG_GET_SERVICES);
        Message message = Message.obtain();
        message.what = MSG_GET_SERVICES;
        message.obj = listener;
        Bundle bundle = new Bundle();
        bundle.putString("service_url", deviceServiceURL);
        bundle.putString("name_space", "http://www.onvif.org/ver10/device/wsdl");
        bundle.putString("soap_action", "http://www.onvif.org/ver10/device/wsdl/GetServices");
        bundle.putString("method_name", "GetServices");
        message.setData(bundle);
        mHandler.sendMessageDelayed(message, 200);
    }

    public void requestMediaService(String serviceURL, OnGetMediaServiceListener listener) {
        mHandler.removeMessages(MSG_GET_MEDIA_SERVICE);
        Message message = Message.obtain();
        message.what = MSG_GET_MEDIA_SERVICE;
        message.obj = listener;
        Bundle bundle = new Bundle();
        bundle.putString("service_url", serviceURL);
        bundle.putString("name_space", "http://www.onvif.org/ver10/media/wsdl");
        bundle.putString("soap_action", "http://www.onvif.org/ver10/media/wsdl/GetStreamUri");
        bundle.putString("method_name", "GetStreamUri");
        message.setData(bundle);
        mHandler.sendMessageDelayed(message, 200);
    }

    public void requestPtzService(String serviceURL, OnGetPtzServiceListener listener) {
        mHandler.removeMessages(MSG_GET_PTZ_SERVICE);
        Message message = Message.obtain();
        message.what = MSG_GET_PTZ_SERVICE;
        message.obj = listener;
        Bundle bundle = new Bundle();
        bundle.putString("service_url", serviceURL);
        bundle.putString("name_space", "http://www.onvif.org/ver10/ptz/wsdl");
        bundle.putString("soap_action", "http://www.onvif.org/ver10/ptz/wsdl/GetNodes");
        bundle.putString("method_name", "GetNodes");
        message.setData(bundle);
        mHandler.sendMessageDelayed(message, 200);
    }

    public void requestPtzContinuousMoveUp(String serviceURL, OnGetPtzServiceListener listener) {
        mHandler.removeMessages(MSG_SET_PTZ_CONTINUOUS_MOVE_UP);
        Message message = Message.obtain();
        message.what = MSG_SET_PTZ_CONTINUOUS_MOVE_UP;
        message.obj = listener;
        Bundle bundle = new Bundle();
        bundle.putString("service_url", serviceURL);
        bundle.putString("name_space", "http://www.onvif.org/ver20/ptz/wsdl");
        bundle.putString("soap_action", "http://www.onvif.org/ver20/ptz/wsdl/ContinuousMove");
        bundle.putString("method_name", "ContinuousMove");
        message.setData(bundle);
        mHandler.sendMessageDelayed(message, 200);
    }

    public void requestPtzContinuousMoveDown(String serviceURL, OnGetPtzServiceListener listener) {
        mHandler.removeMessages(MSG_SET_PTZ_CONTINUOUS_MOVE_DOWN);
        Message message = Message.obtain();
        message.what = MSG_SET_PTZ_CONTINUOUS_MOVE_DOWN;
        message.obj = listener;
        Bundle bundle = new Bundle();
        bundle.putString("service_url", serviceURL);
        bundle.putString("name_space", "http://www.onvif.org/ver20/ptz/wsdl");
        bundle.putString("soap_action", "http://www.onvif.org/ver20/ptz/wsdl/ContinuousMove");
        bundle.putString("method_name", "ContinuousMove");
        message.setData(bundle);
        mHandler.sendMessageDelayed(message, 200);
    }

    public void requestPtzContinuousMoveLeft(String serviceURL, OnGetPtzServiceListener listener) {
        mHandler.removeMessages(MSG_SET_PTZ_CONTINUOUS_MOVE_LEFT);
        Message message = Message.obtain();
        message.what = MSG_SET_PTZ_CONTINUOUS_MOVE_LEFT;
        message.obj = listener;
        Bundle bundle = new Bundle();
        bundle.putString("service_url", serviceURL);
        bundle.putString("name_space", "http://www.onvif.org/ver20/ptz/wsdl");
        bundle.putString("soap_action", "http://www.onvif.org/ver20/ptz/wsdl/ContinuousMove");
        bundle.putString("method_name", "ContinuousMove");
        message.setData(bundle);
        mHandler.sendMessageDelayed(message, 200);
    }

    public void requestPtzContinuousMoveRight(String serviceURL, OnGetPtzServiceListener listener) {
        mHandler.removeMessages(MSG_SET_PTZ_CONTINUOUS_MOVE_RIGHT);
        Message message = Message.obtain();
        message.what = MSG_SET_PTZ_CONTINUOUS_MOVE_RIGHT;
        message.obj = listener;
        Bundle bundle = new Bundle();
        bundle.putString("service_url", serviceURL);
        bundle.putString("name_space", "http://www.onvif.org/ver20/ptz/wsdl");
        bundle.putString("soap_action", "http://www.onvif.org/ver20/ptz/wsdl/ContinuousMove");
        bundle.putString("method_name", "ContinuousMove");
        message.setData(bundle);
        mHandler.sendMessageDelayed(message, 200);
    }

    public void requestStopPtzContinuousMove(String serviceURL, OnGetPtzServiceListener listener) {
        mHandler.removeMessages(MSG_STOP_PTZ_CONTINUOUS_MOVE);
        Message message = Message.obtain();
        message.what = MSG_STOP_PTZ_CONTINUOUS_MOVE;
        message.obj = listener;
        Bundle bundle = new Bundle();
        bundle.putString("service_url", serviceURL);
        bundle.putString("name_space", "http://www.onvif.org/ver20/ptz/wsdl");
        bundle.putString("soap_action", "http://www.onvif.org/ver20/ptz/wsdl/Stop");
        bundle.putString("method_name", "Stop");
        message.setData(bundle);
        mHandler.sendMessageDelayed(message, 200);
    }

    public void requestPtzGotoHomePosition(String serviceURL, OnGetPtzServiceListener listener) {
        mHandler.removeMessages(MSG_PTZ_GOTO_HOME_POSITION);
        Message message = Message.obtain();
        message.what = MSG_PTZ_GOTO_HOME_POSITION;
        message.obj = listener;
        Bundle bundle = new Bundle();
        bundle.putString("service_url", serviceURL);
        bundle.putString("name_space", "http://www.onvif.org/ver20/ptz/wsdl");
        bundle.putString("soap_action", "http://www.onvif.org/ver20/ptz/wsdl/GotoHomePosition");
        bundle.putString("method_name", "GotoHomePosition");
        message.setData(bundle);
        mHandler.sendMessageDelayed(message, 200);
    }

    private Collection<InetAddress> getLoopAddress() {
        final Collection<InetAddress> addressList = new ArrayList<>();
        try {
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface anInterface = interfaces.nextElement();
                    if (!anInterface.isLoopback()) {
                        final List<InterfaceAddress> interfaceAddresses = anInterface.getInterfaceAddresses();
                        for (final InterfaceAddress address : interfaceAddresses) {
                            if (address.getAddress() instanceof Inet4Address) {
                                Log.e(tag, "address.getAddress() =" + address.getAddress().getHostName());
                            }
                            addressList.add(address.getAddress());
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return addressList;
    }

    private DatagramSocket udpSocket = null;
    private void broadcastProbeMessage(final Message msg, InetAddress inetAddress) {
        final String uuid = UUID.randomUUID().toString();
        final String probe = WS_DISCOVERY_PROBE_MESSAGE.replaceAll(
                "<wsa:MessageID>urn:uuid:.*</wsa:MessageID>",
                "<wsa:MessageID>urn:uuid:" + uuid + "</wsa:MessageID>");
        final int port = random.nextInt(20000) + 40000;
        Dbug.i(tag, "UUID=" + uuid + ", port=" + port);

        if (udpSocket != null) {
            udpSocket.close();
        }
        try {
            udpSocket = new DatagramSocket(port, inetAddress);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        if (udpSocket == null) {
            Log.e(tag, "udpSocket is null");
            return;
        }

        byte[] buffer = new byte[4096];
        final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            udpSocket.setSoTimeout(3000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (inetAddress instanceof Inet4Address) {
            //Log.w(tag, "probe===\n" + new String(probe.getBytes()));
            try {
                udpSocket.send(new DatagramPacket(probe.getBytes(), probe.length(), InetAddress.getByName(WS_DISCOVERY_ADDRESS_IPv4), WS_DISCOVERY_PORT));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        long start = System.currentTimeMillis();
        long currentTime = 0;
        do {
            try {
                Log.i(tag, "receive wait.....current time=" + currentTime);
                udpSocket.receive(packet);
            } catch (IOException e) {
                Subscription subscription = Observable.create(new Observable.OnSubscribe<Void>() {
                    @Override
                    public void call(Subscriber<? super Void> subscriber) {
                        subscriber.onNext(null);
                    }
                }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        OnProbeListener listener = (OnProbeListener) msg.obj;
                        if (listener != null) {
                            listener.onFailure("Socket timeout exception.");
                        }
                    }
                });
                if (!subscription.isUnsubscribed()) {
                    subscription.unsubscribe();
                }
                e.printStackTrace();
            }
            //final Collection<String> collection = parseSoapResponseForUrls(Arrays.copyOf(packet.getData(), packet.getLength()));
            //byte[] data = Arrays.copyOf(packet.getData(), packet.getLength());
            Log.i(tag, "packet.getData size===" + packet.getData().length + ", packet =" + packet.getLength());
            if (packet.getLength() > 0) {
                String receiveData = new String(packet.getData()).trim();
                if (TextUtils.isEmpty(receiveData)) {
                    try {
                        udpSocket.send(new DatagramPacket(probe.getBytes(), probe.length(), InetAddress.getByName(WS_DISCOVERY_ADDRESS_IPv4), WS_DISCOVERY_PORT));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    //Log.e(tag, "Receive data===\n" + receiveData);
                    //mServiceUrl = parseDeviceUrlFromXML(new ByteArrayInputStream(receiveData.getBytes()));
                    parseProbeMatch(msg, receiveData);
                    break;
                }
            }
            currentTime = (System.currentTimeMillis() - start);
        } while (currentTime < 1000);
        Log.i(tag, "over...........currentTime=" + currentTime);
        udpSocket.close();
    }

    private void parseProbeMatch(final Message msg, String xml) {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = null;
        try {
            saxParser = saxParserFactory.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }

        if (saxParser != null) {
            OnvifProbeXmlHandler onvifProbeXmlHandler = new OnvifProbeXmlHandler();
            onvifProbeXmlHandler.setOnCompletionListener(new OnvifProbeXmlHandler.OnCompletionListener() {
                @Override
                public void onCompletion(final Object object) {
                    //Log.i(tag, "IP camera=" + object.toString());
                    final OnProbeListener listener = (OnProbeListener) msg.obj;
                    if (listener != null) {
                        Observable.create(new Observable.OnSubscribe<Void>() {
                            @Override
                            public void call(Subscriber<? super Void> subscriber) {
                                subscriber.onNext(null);
                            }
                        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Void>() {
                            @Override
                            public void call(Void aVoid) {
                                listener.onSuccess((CameraInfo) object);
                            }
                        });
                    } else {
                        Log.e(tag, "Probe listener is null");
                    }
                }
            });
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(xml.getBytes());
            try {
                saxParser.parse(byteArrayInputStream, onvifProbeXmlHandler);
            } catch (SAXException | IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    byteArrayInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else {
            Log.e(tag, "saxParser is null");
        }
    }

    private String getPasswordEncode(String nonce, String password, String createdDate) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] b1 = Base64.decode(nonce.getBytes(), Base64.DEFAULT);
            byte[] b2 = createdDate.getBytes(); // "2013-09-17T09:13:35Z";
            byte[] b3 = password.getBytes();
            byte[] b4;// = new byte[b1.length + b2.length + b3.length];
            md.update(b1, 0, b1.length);
            md.update(b2, 0, b2.length);
            md.update(b3, 0, b3.length);
            b4 = md.digest();
            String result = new String(Base64.encode(b4, Base64.DEFAULT));
            return result.replace("\n", "");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String getNonce() {
        String base = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 24; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    private String getCreated() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CHINA);
        return df.format(new Date());
    }
}
