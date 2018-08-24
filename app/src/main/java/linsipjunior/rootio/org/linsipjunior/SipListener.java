package linsipjunior.rootio.org.linsipjunior;

import android.app.Activity;
import android.content.ContentValues;
import android.widget.Toast;

import org.linphone.core.AuthInfo;
import org.linphone.core.AuthMethod;
import org.linphone.core.Call;
import org.linphone.core.CallLog;
import org.linphone.core.CallStats;
import org.linphone.core.ChatMessage;
import org.linphone.core.ChatRoom;
import org.linphone.core.ConfiguringState;
import org.linphone.core.Content;
import org.linphone.core.Core;
import org.linphone.core.CoreListener;
import org.linphone.core.EcCalibratorStatus;
import org.linphone.core.Event;
import org.linphone.core.Friend;
import org.linphone.core.FriendList;
import org.linphone.core.GlobalState;
import org.linphone.core.InfoMessage;
import org.linphone.core.PresenceModel;
import org.linphone.core.ProxyConfig;
import org.linphone.core.PublishState;
import org.linphone.core.RegistrationState;
import org.linphone.core.SubscriptionState;
import org.linphone.core.VersionUpdateCheckResult;

public class SipListener implements CoreListener {
    private SipEventsNotifiable notifiable;
    private RegistrationState lastRegistrationState;
    private ProxyConfig proxyConfig;
    private Call.State lastCallState;
    private Call lastCall;


    public void setNotifiable(SipEventsNotifiable notifiable)
    {
        this.notifiable = notifiable;
        this.notifyRegistrationStatusChange(this.proxyConfig, this.lastRegistrationState);
        this.notifyCallStatusChange(this.lastCall, this.lastCallState);
    }

    private void showToast(final String message) {
        ((Activity) this.notifiable).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText((Activity) SipListener.this.notifiable, message, Toast.LENGTH_LONG).show();
            }
        });
    }


    @Override
    public void onGlobalStateChanged(Core core, GlobalState globalState, String s) {
        //this.showToast("Global state is "+globalState.toString());
    }

    @Override
    public void onRegistrationStateChanged(Core core, ProxyConfig proxyConfig, final RegistrationState registrationState, String s) {
        this.proxyConfig = proxyConfig;
        this.lastRegistrationState = registrationState;
        notifyRegistrationStatusChange(proxyConfig, registrationState);
    }

    private void notifyRegistrationStatusChange(ProxyConfig proxyConfig, final RegistrationState registrationState) {
        final ContentValues values = new ContentValues();
        if(proxyConfig != null)
        {
            values.put("username", proxyConfig.getContact().getUsername());
            values.put("domain", proxyConfig.getContact().getDomain());
        }
        ((Activity)this.notifiable).runOnUiThread(new Runnable()
        {

            @Override
            public void run() {
                SipListener.this.notifiable.updateRegistrationState(registrationState, values);
            }

        });
    }

    @Override
    public void onCallStateChanged(Core core, final Call call, final Call.State state, String s) {
        this.lastCall = call;
        this.lastCallState = state;
        notifyCallStatusChange(call, state);


    }

    private void notifyCallStatusChange(final Call call, final Call.State state) {
        final ContentValues values = new ContentValues();
        if(call !=  null)
        {
            values.put("otherParty", call.getRemoteContact());
        }
        ((Activity)this.notifiable).runOnUiThread(new Runnable()
        {
            @Override
            public void run() {
                SipListener.this.notifiable.updateCallState(state, call, values);
                }
        });
    }

    @Override
    public void onNotifyPresenceReceived(Core core, Friend friend) {

    }

    @Override
    public void onNotifyPresenceReceivedForUriOrTel(Core core, Friend friend, String s, PresenceModel presenceModel) {

    }

    @Override
    public void onNewSubscriptionRequested(Core core, Friend friend, String s) {

    }

    @Override
    public void onAuthenticationRequested(Core core, AuthInfo authInfo, AuthMethod authMethod) {

    }

    @Override
    public void onCallLogUpdated(Core core, CallLog callLog) {

    }

    @Override
    public void onMessageReceived(Core core, ChatRoom chatRoom, ChatMessage chatMessage) {

    }

    @Override
    public void onMessageReceivedUnableDecrypt(Core core, ChatRoom chatRoom, ChatMessage chatMessage) {

    }

    @Override
    public void onIsComposingReceived(Core core, ChatRoom chatRoom) {

    }

    @Override
    public void onDtmfReceived(Core core, Call call, int i) {

    }

    @Override
    public void onReferReceived(Core core, String s) {

    }

    @Override
    public void onCallEncryptionChanged(Core core, Call call, boolean b, String s) {

    }

    @Override
    public void onTransferStateChanged(Core core, Call call, Call.State state) {

    }

    @Override
    public void onBuddyInfoUpdated(Core core, Friend friend) {

    }

    @Override
    public void onCallStatsUpdated(Core core, Call call, CallStats callStats) {

    }

    @Override
    public void onInfoReceived(Core core, Call call, InfoMessage infoMessage) {

    }

    @Override
    public void onSubscriptionStateChanged(Core core, Event event, SubscriptionState subscriptionState) {

    }

    @Override
    public void onNotifyReceived(Core core, Event event, String s, Content content) {

    }

    @Override
    public void onSubscribeReceived(Core core, Event event, String s, Content content) {

    }

    @Override
    public void onPublishStateChanged(Core core, Event event, PublishState publishState) {

    }

    @Override
    public void onConfiguringStatus(Core core, ConfiguringState configuringState, String s) {

    }

    @Override
    public void onNetworkReachable(Core core, boolean b) {

    }

    @Override
    public void onLogCollectionUploadStateChanged(Core core, Core.LogCollectionUploadState logCollectionUploadState, String s) {

    }

    @Override
    public void onLogCollectionUploadProgressIndication(Core core, int i, int i1) {

    }

    @Override
    public void onFriendListCreated(Core core, FriendList friendList) {

    }

    @Override
    public void onFriendListRemoved(Core core, FriendList friendList) {

    }

    @Override
    public void onCallCreated(Core core, Call call) {

    }

    @Override
    public void onVersionUpdateCheckResultReceived(Core core, VersionUpdateCheckResult versionUpdateCheckResult, String s, String s1) {

    }

    @Override
    public void onChatRoomStateChanged(Core core, ChatRoom chatRoom, ChatRoom.State state) {

    }

    @Override
    public void onQrcodeFound(Core core, String s) {

    }

    @Override
    public void onEcCalibrationResult(Core core, EcCalibratorStatus ecCalibratorStatus, int i) {

    }

    @Override
    public void onEcCalibrationAudioInit(Core core) {

    }

    @Override
    public void onEcCalibrationAudioUninit(Core core) {

    }
}
