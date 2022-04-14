package com.pingidentity.emeasa.mobilesample;

import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.pingidentity.emeasa.mobilesample.databinding.ActivityApprovalBinding;
import com.pingidentity.pingidsdkv2.NotificationObject;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;

public class ApprovalActivity extends BaseActivity{

    private static String TAG = "ApprovalActivity";
    private ActivityApprovalBinding binding;
    FingerprintAuthenticationDialogFragment fragment;
    private static final String FINGERPRINT_AUTH_DIALOG_TAG = "fingerprint_authentication_fragment";
    private NotificationObject pingOneNotificationObject = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i (TAG,"onCreate");
        binding = ActivityApprovalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if(getIntent().hasExtra("PingOneNotification")){
            Log.i (TAG,"PingOneNotification notification intent");
            NotificationObject pingOneNotificationObject = (NotificationObject) getIntent().getExtras().get("PingOneNotification");
            String title = "Authenticate?";
            String body = null;
            if(getIntent().hasExtra("title")){
                title = getIntent().getStringExtra("title");
            }
            if (getIntent().hasExtra("body")){
                body = getIntent().getStringExtra("body");
            }
            ((TextView) findViewById(R.id.lblTitle)).setText(title);
            ((TextView) findViewById(R.id.lblDescription)).setText(body);
            Button approve = (Button) findViewById(R.id.buttonApprove);
            Button reject = (Button) findViewById(R.id.buttonReject);
            reject.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Log.i (TAG,"reject - onClick");
                    pingOneNotificationObject.deny(ApprovalActivity.this, new PingOne.PingOneSDKCallback() {
                        @Override
                        public void onComplete(@Nullable @org.jetbrains.annotations.Nullable PingOneSDKError error) {
                            Log.i (TAG,"deny - onComplete");
                           finish();
                        }
                    });
                }
            });

            approve.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i (TAG,"approve - onClick");
                   ApprovalActivity.this.pingOneNotificationObject = pingOneNotificationObject;
                   triggerBiometricApproval("Confirm Approval" , "Please authenticate");
                }
            });
        }
    }

    protected void triggerBiometricApproval(String title, String description) {
        Log.i (TAG,"triggerBiometricApproval");
        FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);
        System.out.println(fingerprintManager.isHardwareDetected());
        System.out.println(fingerprintManager.hasEnrolledFingerprints());
        if(fingerprintManager!=null && fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints()){
            fragment = new FingerprintAuthenticationDialogFragment();
            Bundle b = new Bundle();
            b.putString("title", title);
            b.putString("description", description);

            fragment.setArguments(b);
            fragment.show(getFragmentManager(), FINGERPRINT_AUTH_DIALOG_TAG);
        }
    }

    public void onFingerPrintAuthenticatedCallback() {
        Log.i (TAG,"onFingerPrintAuthenticatedCallback");
        pingOneNotificationObject.approve(ApprovalActivity.this, new PingOne.PingOneSDKCallback() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable PingOneSDKError error) {
                Log.i (TAG,"approve - onComplete " + error);
                fragment.dismissAllowingStateLoss();
                finish();
            }
        });
    }
}