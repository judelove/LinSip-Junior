package linsipjunior.rootio.org.linsipjunior;

import android.app.Activity;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
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
    private boolean isRunning;
    private String stun;

    @Override
    public IBinder onBind(Intent arg0) {
        BindingAgent bindingAgent = new BindingAgent(this);
        return bindingAgent;
    }

    @Override
    public void onCreate() {
        this.coreListener = new SipListener();
        this.prefs = this.getSharedPreferences("org.rootio.linsipjunior", Context.MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.startForeground(1, new NotificationCompat.Builder(this).setContentTitle("LinSipJunior").setContentText("LinSipJunior Running").setAutoCancel(false).build());
        return Service.START_STICKY;

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

    private NatPolicy createNatPolicy()
    {
        NatPolicy natPolicy = linphoneCore.createNatPolicy();
        natPolicy.enableStun(true);
        natPolicy.setStunServer(this.stun);
        return natPolicy;
    }


    private void prepareProxy() {
        this.proxyConfig = linphoneCore.createProxyConfig();
        this.proxyConfig.setNatPolicy(this.createNatPolicy()); //use STUN. There is every chance you are on a NATted network

        //The address of the peer
        Address addr = Factory.instance().createAddress(String.format("sip:%s@%s", this.username, this.domain));
        this.authInfo = Factory.instance().createAuthInfo(addr.getUsername(), null, this.password, null, null, null);
        this.linphoneCore.addAuthInfo(authInfo);
        this.proxyConfig.setIdentityAddress(addr);
        this.proxyConfig.setServerAddr(addr.getDomain());

        //Registration deets
        this.proxyConfig.setExpires(2000);
        this.proxyConfig.enableRegister(true);

        //add the proxy config, default if only one
        this.linphoneCore.addProxyConfig(this.proxyConfig);
        this.linphoneCore.setDefaultProxyConfig(this.proxyConfig);

    }

    private void prepareSipProfile() {
        String conf = "#\n" +
                "#This file shall not contain path referencing package name, in order to be portable when app is renamed.\n" +
                "#Paths to resources must be set from LinphoneManager, after creating LinphoneCore.\n" +
                "[net]\n" +
                "mtu=1300\n" +
                "#Because dynamic bitrate adaption can increase bitrate, we must allow \"no limit\"\n" +
                "download_bw=0\n" +
                "upload_bw=0\n" +
                "force_ice_disablement=0\n" +
                "\n" +
                "[sip]\n" +
                "guess_hostname=1\n" +
                "register_only_when_network_is_up=1\n" +
                "auto_net_state_mon=0\n" +
                "auto_answer_replacing_calls=1\n" +
                "ping_with_options=0\n" +
                "rls_uri=\n" +
                "use_cpim=1\n" +
                "linphone_specs=groupchat\n" +
                "\n" +
                "[rtp]\n" +
                "audio_rtp_port=7076\n" +
                "video_rtp_port=9078\n" +
                "audio_jitt_comp=60\n" +
                "video_jitt_comp=60\n" +
                "nortp_timeout=30\n" +
                "disable_upnp=1\n" +
                "\n" +
                "[sound]\n" +
                "playback_dev_id=\n" +
                "ringer_dev_id=\n" +
                "capture_dev_id=\n" +
                "dtmf_player_amp=0.1\n" +
                "\n" +
                "#remove this property for any application that is not Linphone public version itself\n" +
                "ec_calibrator_cool_tones=1\n" +
                "\n" +
                "[misc]\n" +
                "max_calls=10\n" +
                "history_max_size=100\n" +
                "enable_basic_to_client_group_chat_room_migration=0\n" +
                "enable_simple_group_chat_message_state=0\n" +
                "aggregate_imdn=1\n" +
                "version_check_url_root=https://www.linphone.org/releases\n" +
                "\n" +
                "[app]\n" +
                "activation_code_length=4\n" +
                "prefer_basic_chat_room=1\n" +
                "\n" +
                "[in-app-purchase]\n" +
                "server_url=https://subscribe.linphone.org:444/inapp.php\n" +
                "purchasable_items_ids=test_account_subscription\n" +
                "\n" +
                "[assistant]\n" +
                "domain=sip.linphone.org\n" +
                "password_max_length=-1\n" +
                "password_min_length=1\n" +
                "username_length=-1\n" +
                "username_max_length=64\n" +
                "username_min_length=1\n" +
                "username_regex=^[a-z0-9_.\\-]*$\n" +
                "xmlrpc_url=https://subscribe.linphone.org:444/wizard.php";

        this.profile = Factory.instance().createConfigFromString(""); //conf); Config string above was tripping the client, would not connect..

    }

    void register() {
        try {
            this.loadConfig();
            if(this.username.isEmpty() || this.password.isEmpty() || this.domain.isEmpty())
            {
                this.showToast("Can't register! Username, password or domain is missing!");
                this.parent.updateRegistrationState(RegistrationState.None, null);
                return;
            }
            this.prepareSipProfile();
            this.linphoneCore = Factory.instance().createCoreWithConfig(this.profile, this);
            this.linphoneCore.addListener(this.coreListener);
            this.prepareProxy();
            this.isRunning = true;

            //listen for SIP events on new thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    linphoneCore.start();
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
            //Ideally if this happens, then reg fails, the status should still be DEREGISTERED
            ContentValues values = new ContentValues();
            values.put("errorCode", 0);
            values.put("errorMessage", "SIP Error occurred. Please check config and network availability");
            //this.notifyRegistrationEvent(this.registrationState, values);
        }
    }

    public void deregister() {
        try {
            if(linphoneCore.inCall()) {
                try { //state might change between check and termination call..
                    this.linphoneCore.getCurrentCall().terminate();
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                }
            }
            this.linphoneCore.getDefaultProxyConfig().edit();
            this.linphoneCore.getDefaultProxyConfig().enableRegister(false);
            this.linphoneCore.getDefaultProxyConfig().done();
            //this.isRunning = false;
            this.linphoneCore.clearProxyConfig(); //only thing similar to deregistration

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
