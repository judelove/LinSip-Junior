package linsipjunior.rootio.org.linsipjunior;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.linphone.core.Address;
import org.linphone.core.AuthInfo;
import org.linphone.core.Call;
import org.linphone.core.Config;
import org.linphone.core.Core;
import org.linphone.core.Factory;
import org.linphone.core.NatPolicy;
import org.linphone.core.ProxyConfig;
import org.linphone.core.RegistrationState;
import org.linphone.core.Transports;

/**
 * Created by Jude Mukundane on 06/03/2018.
 */

public class SIPHandler extends Service {

    private SipListener coreListener;
    private AuthInfo authInfo;
    private Core linphoneCore;
    private ProxyConfig proxyConfig;
    private Config profile;
    private String username, password, domain;
    private SipEventsNotifiable parent;
    private SharedPreferences prefs;
    private boolean isRunning, sipRunning;
    private String stun;

    @Override
    public IBinder onBind(Intent arg0) {
        BindingAgent bindingAgent = new BindingAgent(this);
        return bindingAgent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.coreListener = new SipListener();
        this.prefs = this.getSharedPreferences("org.rootio.linsipjunior", Context.MODE_PRIVATE);
        this.initializeStack();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.startForeground(1, Build.VERSION.SDK_INT < Build.VERSION_CODES.O? new NotificationCompat.Builder(this).setContentTitle("LinSipJunior").setContentText("LinSip Jr. is Running").setAutoCancel(false).build():getNotification());
        return Service.START_STICKY;

    }


    @TargetApi(Build.VERSION_CODES.O)
    private Notification getNotification()
    {
    String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
    String channelName = "My Background Service";
    NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
    chan.setLightColor(Color.BLUE);
    chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
    NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    assert manager != null;
    manager.createNotificationChannel(chan);

    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
    Notification notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.logo_sj)
            .setContentTitle("LinSip jr. is running")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build();
    return notification;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        this.deregister();
        this.stopForeground(true);
    }


    public void setNotifiable(SipEventsNotifiable parent) //This is how you talk to the main activity
    {
        this.parent = parent;
        this.coreListener.setNotifiable(this.parent);
    }


    private void loadConfig() {
        if (this.prefs != null) {
            this.domain = prefs.getString("org.rootio.linsipjunior.domain", "");
            this.username = prefs.getString("org.rootio.linsipjunior.username", "");
            this.password = prefs.getString("org.rootio.linsipjunior.password", "");
            this.stun = prefs.getString("org.rootio.linsipjunior.stun", "");
        }
    }

    private NatPolicy createNatPolicy() {
        NatPolicy natPolicy = linphoneCore.createNatPolicy();
        natPolicy.setStunServer(this.stun);

        //natPolicy.enableTurn(true);
        natPolicy.enableIce(true);
        natPolicy.enableStun(true);

        natPolicy.resolveStunServer();
        return natPolicy;
    }


    private void prepareProxy() {
        this.proxyConfig = linphoneCore.createProxyConfig();
        Transports trns = Factory.instance().createTransports();
        trns.setUdpPort(-1);
        trns.setTcpPort(-1);
        this.linphoneCore.setTransports(trns);

        //The address of the peer
        Address addr = Factory.instance().createAddress(String.format("sip:%s@%s", this.username, this.domain));
        Address proxy = Factory.instance().createAddress("sip:" + this.domain);
        this.authInfo = Factory.instance().createAuthInfo(addr.getUsername(), null, this.password, null, null, null);
        this.linphoneCore.addAuthInfo(authInfo);


        this.proxyConfig.setIdentityAddress(addr);
        this.proxyConfig.setServerAddr(proxy.getDomain());

        this.proxyConfig.setNatPolicy(this.createNatPolicy()); //use STUN. There is every chance you are on a NATted network

        //Registration deets
        this.proxyConfig.setExpires(2000);
        this.proxyConfig.enableRegister(false);

        this.linphoneCore.addProxyConfig(this.proxyConfig);
        this.linphoneCore.setDefaultProxyConfig(this.proxyConfig);
    }

    private void prepareSipProfile() {
        this.profile = Factory.instance().createConfigFromString(""); //Config string above was tripping the client, would not connect..

    }

    void register() {
        initializeStack();
        this.linphoneCore.removeListener(coreListener);
        new Thread(new Runnable() {
            @Override
            public void run() {
                //This is a strange flow, but for STUN/TURN to kick in, you need to register first, then unregister and register again!
                //The registration triggers a stun update but it can only be used on next registration. That's what it looks like for now
                //so, register for 1 sec, then re-register

                sipRegister();
                try {
                    linphoneCore.addListener(coreListener);
                    Thread.sleep(1000);//too little and linphoneCore may not process our events due to backlog/network delay, too high and we sleep too long..
                    deregister();
                    Thread.sleep(1000);//too little and linphoneCore may not process our events due to backlog/network delay, too high and we sleep too long..
                    sipRegister();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void sipRegister() {

        linphoneCore.getDefaultProxyConfig().edit();
        linphoneCore.getDefaultProxyConfig().enableRegister(true);
        linphoneCore.getDefaultProxyConfig().done();
    }

    void initializeStack() {
        try {
            this.loadConfig();

            if (this.username.isEmpty() || this.password.isEmpty() || this.domain.isEmpty()) {
                this.showToast("Can't register! Username, password or domain is missing!");
                this.parent.updateRegistrationState(RegistrationState.None, null);
                return;
            }

            this.prepareSipProfile();
            this.linphoneCore = Factory.instance().createCoreWithConfig(profile, this);
            this.prepareProxy();

            linphoneCore.start();
            isRunning = true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                   while (isRunning) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        SIPHandler.this.linphoneCore.iterate();
                    }
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public void deregister() {
        try {
            if (linphoneCore.inCall()) {
                try { //state might change between check and termination call..
                    this.linphoneCore.getCurrentCall().terminate();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            this.linphoneCore.getDefaultProxyConfig().edit();
            this.linphoneCore.getDefaultProxyConfig().enableRegister(false);
            this.linphoneCore.getDefaultProxyConfig().done();
            //this.isRunning = false;
            //this.linphoneCore.clearProxyConfig(); //only thing similar to deregistration

        } catch (Exception e) {
            e.printStackTrace();
            //this.notifyRegistrationEvent(this.registrationState, null); //potential conflict of handling to the receiver
        }
    }

    public void call(String phoneNumber) {
        try {
            Call call = linphoneCore.invite(phoneNumber);
        } catch (Exception e) {
            e.printStackTrace();
            this.showToast("A SIP error occurred. Check config details or Internet connection");
        }
    }

    public void hangup() {
        try {
            this.linphoneCore.getCurrentCall().terminate(); //overly simplistic
        } catch (Exception e) {
            e.printStackTrace();
            this.showToast("A SIP error occurred. Check config details or Internet connection");
        }
    }

    public void answer(Call call) {
        try {
            this.linphoneCore.acceptCall(call);
        } catch (Exception e) {
            e.printStackTrace();
            this.showToast("A SIP error occurred. Check config details or Internet connection");
        }
    }

    private void showToast(final String message) {
        ((Activity) SIPHandler.this.parent).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SIPHandler.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }


    class BindingAgent extends Binder {

        private Service service;

        BindingAgent(Service service) {
            this.service = service;
        }

        /**
         * Returns the service for which this class is providing a binding
         * connection
         *
         * @return Service object for the service bound to
         */
        Service getService() {
            return this.service;
        }
    }
}
