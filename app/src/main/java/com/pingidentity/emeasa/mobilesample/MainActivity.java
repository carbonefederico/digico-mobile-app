package com.pingidentity.emeasa.mobilesample;

import android.app.Activity;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pingidentity.emeasa.mobilesample.databinding.ActivityMainBinding;
import com.pingidentity.emeasa.mobilesample.model.SKResponse;
import com.pingidentity.pingidsdkv2.PingOne;

import org.json.JSONObject;

public class MainActivity extends BaseActivity {

    private static String TAG = "MainActivity";

    private ActivityMainBinding binding;
    private int biomode = Consts.BIOMETRIC_PAIRING;
    private String challenge = null;
    private SKHelper helper;
    private static final String FINGERPRINT_AUTH_DIALOG_TAG = "fingerprint_authentication_fragment";
    FingerprintAuthenticationDialogFragment fragment;
    private static int MODE_INITIAL = 0;
    private static int MODE_START = 1;
    private static int MODE_LOGGED_IN = 2;

    private EditText usernameInput;
    private EditText passwordInput;
    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setUIFields ();
        challenge = getIntentChallenge();
        helper = new SKHelper();

        MobileApplication app = (MobileApplication) getApplication();
        if (app.isPaired(this)) {
            Log.i(TAG, "App paired");
            doFingerprintLogin();
        } else {
            Log.i(TAG, "App not paired");
            updateView(MODE_INITIAL);
        }

        Button startButton = (Button) this.findViewById(R.id.buttonStart);
        startButton.setOnClickListener(view -> {
            Log.i(TAG, "Start - onClick");
            updateView(MODE_START);
        });


        nextButton.setOnClickListener(view -> {
            Log.i(TAG, "Next - onClick");
            showProgressBar();
            try {
                hideKeyboard(MainActivity.this);
                JSONObject flowInput = new JSONObject();
                String mobilePayload = PingOne.generateMobilePayload(MainActivity.this);
                Log.i(TAG, "Mobile Payload genearated " + mobilePayload);
                flowInput.put("mobilePayload", mobilePayload);
                flowInput.put("username", usernameInput.getText().toString().trim());
                flowInput.put("password", passwordInput.getText().toString().trim ());
                helper.postRequest(Consts.MOBILE_PAIRING_FLOW,flowInput,MainActivity.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Button authenticateButton = this.findViewById(R.id.qrCodeAuthButton);
        authenticateButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this,ScanQRActivity.class)));
    }

    private void setUIFields () {
        nextButton = (Button) this.findViewById(R.id.buttonNext);
        usernameInput = (EditText) findViewById(R.id.username);
        passwordInput = ((EditText) findViewById(R.id.password));
    }

    private String getIntentChallenge() {
        Uri appLinkData = getIntent().getData();
        Log.i (TAG, "onCreate with appLinkData " + appLinkData);
        if (appLinkData != null) {
            challenge = appLinkData.getQueryParameter("challenge");
            Log.i (TAG, "challenge " + challenge);
            return challenge;
        }
        return null;
    }

    private void doFingerprintLogin() {
        triggerBiometricAuth("Fingerprint Login", "Please touch the sensor to login", Consts.BIOMETRIC_AUTHENTICATION);
    }

    private void updateView(int mode) {
        Log.i (TAG, "updateView");
        hideProgressBar();
        findViewById(R.id.start_layout).setVisibility(View.GONE);
        findViewById(R.id.login_layout).setVisibility(View.GONE);
        findViewById(R.id.loggedin_layout).setVisibility(View.GONE);
        if (mode == MODE_INITIAL) {
            Log.i (TAG, "Updating view with MODE_INITIAL");
            findViewById(R.id.start_layout).setVisibility(View.VISIBLE);
        } else if (mode == MODE_START) {
            Log.i (TAG, "Updating view with MODE_START");
            findViewById(R.id.login_layout).setVisibility(View.VISIBLE);
        } else if (mode == MODE_LOGGED_IN) {
            Log.i (TAG, "Updating view with MODE_LOGGED_IN");
            MobileApplication app = (MobileApplication) getApplication();
            ((TextView) findViewById(R.id.lblUser)).setText(app.getPreference(this,MobileApplication.NAME));
            findViewById(R.id.loggedin_layout).setVisibility(View.VISIBLE);
        }
    }

    public void showProgressBar () {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
    }

    public void hideProgressBar () {
        findViewById(R.id.progressBar).setVisibility(View.GONE);
    }

    public void showErrorMessage (String errorMessage) {
        hideProgressBar();
        Toast.makeText(this, errorMessage,
                Toast.LENGTH_LONG).show();
    }

    public void setUser(SKResponse response) {
        Log.i(TAG, "setUser");
        String name = (String) response.getAdditionalProperties().get("given_name");
        String username = (String) response.getAdditionalProperties().get("username");
        Log.i(TAG, "Name " + name);
        MobileApplication app = (MobileApplication) getApplication();
        app.setPreference(this, MobileApplication.NAME, name);
        app.setPreference(this, MobileApplication.USERNAME, username);
        ((TextView) findViewById(R.id.lblUser)).setText(name);

        if (!app.isPaired(this)) {
            triggerBiometricAuth("Fingerprint Login", "Please touch the sensor to set up fingerprint auto-login", Consts.BIOMETRIC_PAIRING);
        } else {
            updateView(MODE_LOGGED_IN);
        }
    }

    public void onFingerPrintAuthenticatedCallback() {
        Log.i(TAG, "onFingerPrintAuthenticatedCallback");
        fragment.dismissAllowingStateLoss();
        if (biomode == Consts.BIOMETRIC_PAIRING) {
            Log.i(TAG, "BIOMETRIC_PAIRING");
            MobileApplication app = (MobileApplication) getApplication();
            app.setPaired(this, true);
            updateView(MODE_LOGGED_IN);
        } else if (biomode == Consts.BIOMETRIC_AUTHENTICATION) {
            Log.i(TAG, "BIOMETRIC_AUTHENTICATION");
            showProgressBar();
            try {
                MobileApplication app = (MobileApplication) getApplication();
                JSONObject flowInput = new JSONObject();
                flowInput.put("mobilePayload", PingOne.generateMobilePayload(MainActivity.this));
                flowInput.put("username", app.getPreference(this, MobileApplication.USERNAME));
                if (challenge != null) {
                    Log.i(TAG, "Adding challenge " + challenge);
                    flowInput.put("challenge", challenge);
                }
                helper.postRequest( Consts.MOBILE_AUTH_FLOW, flowInput, MainActivity.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void triggerBiometricAuth(String title, String description, int mode) {
        Log.i(TAG, "triggerBiometricAuth");
        biomode = mode;
        FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);
        Log.i(TAG, "isHardwareDetected " + fingerprintManager.isHardwareDetected());
        Log.i(TAG, "hasEnrolledFingerprints " + fingerprintManager.hasEnrolledFingerprints());
        if (fingerprintManager != null && fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints()) {
            Log.i(TAG, "Show fingerprint");
            fragment = new FingerprintAuthenticationDialogFragment();
            Bundle b = new Bundle();
            b.putString("title", title);
            b.putString("description", description);

            fragment.setArguments(b);
            fragment.show(getFragmentManager(), FINGERPRINT_AUTH_DIALOG_TAG);
        }
        Log.i(TAG, "Leaving triggerBiometricAuth");
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void doLogout(View view) {
        Log.i(TAG, "doLogout");
        MobileApplication app = (MobileApplication) getApplication();
        app.setPreference(this, MobileApplication.NAME, null);
        app.setPreference(this, MobileApplication.ACCESS_TOKEN, null);
        app.setPreference(this, MobileApplication.ID_TOKEN, null);
        app.setPaired(this, false);
        ((TextView) findViewById(R.id.lblUser)).setText("");
        updateView(MODE_INITIAL);
    }
}