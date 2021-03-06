package linsipjunior.rootio.org.linsipjunior;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.linphone.core.Call;
import org.linphone.core.RegistrationState;
import org.linphone.core.Utils;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements SipEventsNotifiable {
    private ComponentName registrationService;
    private SIPHandler handler;
    private Call call;
    private String[] requirePermissions = new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.RECORD_AUDIO,  Manifest.permission.CAMERA, Manifest.permission.PROCESS_OUTGOING_CALLS,  Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,  Manifest.permission.READ_EXTERNAL_STORAGE};


    public void toggleRegistration(View v) {
        if (((Switch) v).isChecked()) {
            this.handler.register();
        } else {
            //this.stopService(new Intent(this, SIPHandler.class));
            this.handler.deregister();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        for (String permission : requirePermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                askAllPermissions();
            } else {

                this.registrationService = this.startService(new Intent(this, SIPHandler.class));
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        this.bindService(new Intent(this, SIPHandler.class), new ServiceConnectionAgent(), Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {

        super.onResume();
        this.bindService(new Intent(this, SIPHandler.class), new ServiceConnectionAgent(), Context.BIND_AUTO_CREATE);
    }

    private void askAllPermissions() {
        ActivityCompat.requestPermissions(this, requirePermissions, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean allGranted = true;
        for (int permissionIndex : grantResults) {
            if (permissionIndex != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
            }
        }
        if (!allGranted) {
            warnOnScreen(this, "please go to settings and grant this app the required permissions.", new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }
            });
        }
        else
        {
            this.bindService(new Intent(this, SIPHandler.class), new ServiceConnectionAgent(), Context.BIND_AUTO_CREATE);
        }
    }

    public void warnOnScreen(Activity triggerActivity, String message, DialogInterface.OnDismissListener listener) {
        new AlertDialog.Builder(triggerActivity).setIcon(R.drawable.attention).setOnDismissListener(listener).setTitle("Warning").setMessage(message).setNeutralButton("Close", null).show();
    }

    public void call(View v) {
        TextView phoneNumberTv = (TextView) this.findViewById(R.id.phoneNumber_tv);
        String phoneNumber = phoneNumberTv.getText().toString();
        this.handler.call(phoneNumber);

    }

    public void hangup(View v) {
        this.handler.hangup();
    }

    private void answer(View v) {
        this.handler.answer(this.call);
    }

    private void startChronometry() {
        Chronometer cr = ((Chronometer) findViewById(R.id.chronometer2));
        cr.setVisibility(View.VISIBLE);
        final long base = Calendar.getInstance().getTimeInMillis();
        cr.setBase(base);
        cr.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {

                long time = Calendar.getInstance().getTimeInMillis() - base;
                int h = (int) (time / 3600000);
                int m = (int) (time - h * 3600000) / 60000;
                int s = (int) (time - h * 3600000 - m * 60000) / 1000;
                chronometer.setText(String.format("%02d:%02d:%02d", h, m, s));
            }
        });
        cr.start();
    }

    private void stopChronometry() {
        Chronometer cr = ((Chronometer) findViewById(R.id.chronometer2));
        cr.setVisibility(View.INVISIBLE);
        cr.stop();
    }

    @Override
    public void updateCallState(Call.State callState, Call call, ContentValues values) {
        if (call != null && callState != null) //this notif could come in before the listener connects to SIP server
        {
            this.call = call;
            Button btn = ((Button) findViewById(R.id.call_btn));
            switch (callState) {
                case End:
                case Error:
                    this.stopChronometry();
                    btn.setText("Call");
                    btn.setBackgroundColor(Color.parseColor("#ff669900"));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MainActivity.this.call(v);
                        }
                    });
                    if (values != null && values.containsKey("otherParty")) //not being sent au moment
                    {
                        Toast.makeText(this, "Call with " + values.getAsString("otherParty") + " terminated", Toast.LENGTH_LONG).show();
                    }
                    break;
                case Connected:
                case StreamsRunning: //in case you reconnect to the main activity during call.
                    this.startChronometry();
                    btn.setText("End");
                    btn.setBackgroundColor(Color.parseColor("#ff990000"));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MainActivity.this.hangup(v);
                        }
                    });
                    if (values != null && values.containsKey("otherParty")) //ideally check for direction and report if outgoing or incoming
                    {
                        Toast.makeText(this, "In call with " + values.getAsString("otherParty"), Toast.LENGTH_LONG).show();
                    }
                    break;
                case IncomingReceived:
                    btn.setText("Answer Call");
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MainActivity.this.answer(v);
                        }
                    });
                    if (values != null && values.containsKey("otherParty")) {
                        Toast.makeText(this, "Incoming call from " + values.getAsString("otherParty"), Toast.LENGTH_LONG).show();
                    }
                    break;
                case OutgoingInit:
                    btn.setText("Cancel");
                    btn.setBackgroundColor(Color.parseColor("#ff996600"));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MainActivity.this.hangup(v);
                        }
                    });
                    if (values != null && values.containsKey("otherParty")) {
                        Toast.makeText(this, "Dialling out... " , Toast.LENGTH_LONG).show();
                    }
                    break;

                default: //handles 13 other states!
                    break;
            }
        }

    }


    @Override
    public void updateRegistrationState(RegistrationState registrationState, ContentValues values) {
        if (registrationState != null) { //could be sent before listener has any notifs, e.g when this service connects to service before registration
            Switch sw = ((Switch) findViewById(R.id.register_sw));
            TextView acc = ((TextView) findViewById(R.id.account_tv));
            switch (registrationState) {
                case Progress:
                    sw.setEnabled(false);
                    sw.setText("Registering...");
                    break;
                case Ok:
                    sw.setText("Registered");
                    sw.setEnabled(true);
                    sw.setChecked(true);


                    acc.setVisibility(View.VISIBLE);
                    if (values != null && values.containsKey("username")) //Requested notifications will not come with values
                    {
                        acc.setText(values.getAsString("username") + "@" + values.getAsString("domain"));
                        Toast.makeText(this, "Registered " + values.getAsString("username") + "@" + values.getAsString("domain"), Toast.LENGTH_LONG).show();
                    }
                    break;
                case None:
                case Cleared:
                case Failed:
                    sw.setText("Not Registered");
                    sw.setEnabled(true);
                    sw.setChecked(false);
                    acc.setVisibility(View.INVISIBLE);
                    if (values != null && values.containsKey("localProfileUri")) {
                        Toast.makeText(this, "Unregistered " + values.getAsString("localProfileUri"), Toast.LENGTH_LONG).show();
                    }
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.navigation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.config:
                Intent intent = new Intent(this, Config.class);
                this.startActivity(intent);
                break;
            case R.id.exit:
                this.stopService(new Intent(this, SIPHandler.class));
                this.finish();
                break;
        }
        return true;
    }

    public class ServiceConnectionAgent implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SIPHandler.BindingAgent bindingAgent = (SIPHandler.BindingAgent) service;
            MainActivity.this.handler = (SIPHandler) bindingAgent.getService();
            MainActivity.this.handler.setNotifiable(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }


    }

}
