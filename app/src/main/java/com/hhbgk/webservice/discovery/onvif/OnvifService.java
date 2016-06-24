package com.hhbgk.webservice.discovery.onvif;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.hhbgk.webservice.discovery.data.model.CameraInfo;
import com.hhbgk.webservice.discovery.data.model.ServiceInfo;
import com.hhbgk.webservice.discovery.data.model.StreamInfo;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
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

    //private static OnvifService instance = null;
    private final static int WS_DISCOVERY_PORT = 3702;
    private final static String WS_DISCOVERY_ADDRESS_IPv4 = "239.255.255.250";
    private final static String WS_DISCOVERY_PROBE_MESSAGE = "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:tns=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\"><soap:Header><wsa:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</wsa:Action><wsa:MessageID>urn:uuid:c032cfdd-c3ca-49dc-820e-ee6696ad63e2</wsa:MessageID><wsa:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</wsa:To></soap:Header><soap:Body><tns:Probe/></soap:Body></soap:Envelope>";
    private static final Random random = new SecureRandom();

    private final Handler mHandler;

    private static final int MSG_WS_PROBE = 100;
    private static final int MSG_GET_SERVICES = 101;
    private static final int MSG_GET_MEDIA_SERVICE = 102;
    private static final int MSG_GET_PTZ_SERVICE = 103;

/*    public static OnvifService getInstance() {
        if (null == instance) {
            instance = new OnvifService();
        }
        return instance;
    }*/

    public OnvifService() {
        HandlerThread probeThread = new HandlerThread("HandlerThread_"+new Random().nextInt(Integer.MAX_VALUE));
        probeThread.start();
        Handler.Callback handlerCallback = new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Log.i(tag, "what==" + msg.what);
                Bundle bundle;
                switch (msg.what) {
                    case MSG_WS_PROBE:
                        Log.i(tag, "11111 thread id==" + Thread.currentThread().getId());
                        for (InetAddress inetAddress : getLoopAddress()) {
                            //Log.i(tag, "ip==" + inetAddress.getHostName());
                            if (inetAddress instanceof Inet4Address) {
                                broadcastProbeMessage(inetAddress);
                            }
                        }
                        break;
                    case MSG_GET_SERVICES:
                        Log.i(tag, "get service listener=" + msg.obj);
                        bundle = msg.getData();
                        String nameSpace = "http://www.onvif.org/ver10/device/wsdl";
                        String soapAction = "http://www.onvif.org/ver10/device/wsdl/GetServices";
                        String methodName = "GetServices";
                        postRequest(bundle.getString("service_url"), nameSpace, soapAction, methodName, msg);
                        break;
                    case MSG_GET_MEDIA_SERVICE:
                        Log.i(tag, "media listener=" + msg.obj);
                        bundle = msg.getData();
                        postRequest(bundle.getString("service_url"), bundle.getString("name_space"), bundle.getString("soap_action")
                                , bundle.getString("method_name"), msg);
                        break;
                    case MSG_GET_PTZ_SERVICE:
                        break;
                }

                return false;
            }
        };
        mHandler = new Handler(probeThread.getLooper(), handlerCallback);
    }

    private void postRequest(String deviceService, String nameSpace, String soapAction, String methodName, Message msg) {
        Log.e(tag, "soapAction=" + soapAction);
//        String created = getCreated();
//        String nonce = getNonce();
        SoapObject soapObject = new SoapObject(nameSpace, methodName);
//        soapObject.addProperty("Username", "admin");
//        soapObject.addProperty("Password", getPasswordEncode(nonce, "888888", created));
//        soapObject.addProperty("Nonce", nonce);
//        soapObject.addProperty("Created", created);
        switch (msg.what) {
            case MSG_GET_MEDIA_SERVICE:
                soapObject.addProperty("Stream", "RTP-Unicast");
                soapObject.addProperty("Protocol", "UDP");
                soapObject.addProperty("ProfileToken", "PROFILE_000");
                break;
        }
        final SoapSerializationEnvelope soapSerializationEnvelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
        soapSerializationEnvelope.bodyOut = soapObject;
        soapSerializationEnvelope.dotNet = true;
//        soapSerializationEnvelope.setOutputSoapObject(soapObject);

        final HttpTransportSE httpSe = new HttpTransportSE(deviceService, 5000);
        //httpSe.debug = true;
        try {
            httpSe.call(soapAction, soapSerializationEnvelope);
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }

        try {
            if (soapSerializationEnvelope.getResponse() != null) {
                SoapObject result = (SoapObject) soapSerializationEnvelope.bodyIn;
                Log.w(tag, "result getName: " + result.getName() + ", getPropertyCount=" + result.getPropertyCount());
                Log.i(tag, "result=\n" + result.toString());
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
                }


            } else {
                Log.e(tag, "Request and response failing");
                switch (msg.what) {
                    case MSG_GET_SERVICES:
                        OnGetServicesListener onResponseListener = (OnGetServicesListener) msg.obj;
                        if (onResponseListener != null) {
                            onResponseListener.onFailure("Request failure.");
                        }
                        break;
                    case MSG_GET_MEDIA_SERVICE:
                        OnGetMediaServiceListener onGetMediaServiceListener = (OnGetMediaServiceListener) msg.obj;
                        if (onGetMediaServiceListener != null) {
                            onGetMediaServiceListener.onFailure("Request failure.");
                        }
                        break;
                }
            }
        } catch (SoapFault soapFault) {
            soapFault.printStackTrace();
        }
    }

    private OnProbeListener mOnProbeListener = null;

    public interface OnProbeListener {
        void onSuccess(CameraInfo object);
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

    public void probeDeviceService(OnProbeListener listener) {
        Log.e(tag, "mOnProbeListener=" + mOnProbeListener);
        mOnProbeListener = listener;
        mHandler.removeMessages(MSG_WS_PROBE);
        mHandler.sendEmptyMessageDelayed(MSG_WS_PROBE, 200);
    }

    public void requestServices(String deviceServiceURL, OnGetServicesListener listener) {
        mHandler.removeMessages(MSG_GET_SERVICES);
        Message message = Message.obtain();
        message.what = MSG_GET_SERVICES;
        message.obj = listener;
        Bundle b = new Bundle();
        b.putString("service_url", deviceServiceURL);
        message.setData(b);
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

    /*public void requestPtzService(String serviceURL, OnGetServicesListener listener) {
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
*/
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

    private void broadcastProbeMessage(InetAddress inetAddress) {
        final String uuid = UUID.randomUUID().toString();
        final String probe = WS_DISCOVERY_PROBE_MESSAGE.replaceAll(
                "<wsa:MessageID>urn:uuid:.*</wsa:MessageID>",
                "<wsa:MessageID>urn:uuid:" + uuid
                        + "</wsa:MessageID>");
        final int port = random.nextInt(20000) + 40000;
        DatagramSocket udpSocket = null;
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
                        if (mOnProbeListener != null) {
                            mOnProbeListener.onFailure("Socket timeout exception.");
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
                    parseProbeMatch(receiveData);
                    break;
                }
            }
            currentTime = (System.currentTimeMillis() - start);
        } while (currentTime < 1000);
        Log.i(tag, "over...........currentTime=" + currentTime);
        udpSocket.close();
    }

    private void parseProbeMatch(String xml) {
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
                    if (mOnProbeListener != null) {
                        Observable.create(new Observable.OnSubscribe<Void>() {
                            @Override
                            public void call(Subscriber<? super Void> subscriber) {
                                subscriber.onNext(null);
                            }
                        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Void>() {
                            @Override
                            public void call(Void aVoid) {
                                mOnProbeListener.onSuccess((CameraInfo) object);
                                mOnProbeListener = null;
                                Log.i(tag, "on success .........");
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
            byte[] b4 = new byte[b1.length + b2.length + b3.length];
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
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 24; i++) {
            int number = random.nextInt(base.length());
            sb.append(base.charAt(number));
        }
        return sb.toString();
    }

    private String getCreated() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CHINA);
        return df.format(new Date());
//        String mAuthPwd = getPasswordEncode(mNonce, "admin", mCreated);
//        Log.w(tag, "authPwd=" + mAuthPwd);
    }
}