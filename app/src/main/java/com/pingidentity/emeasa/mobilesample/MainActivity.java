package com.pingidentity.emeasa.mobilesample;

import android.app.Activity;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.navigation.ui.AppBarConfiguration;

import com.pingidentity.emeasa.mobilesample.databinding.ActivityMainBinding;
import com.pingidentity.emeasa.mobilesample.model.SKResponse;
import com.pingidentity.pingidsdkv2.PingOne;

import org.json.JSONException;
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

    private EditText input1;
    private EditText input2;
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
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Start - onClick");
                try {
                    showProgressBar();
                    JSONObject flowInput = new JSONObject();
                    String mobilePayload = PingOne.generateMobilePayload(MainActivity.this);
                    Log.i(TAG, "Mobile Payload genearated " + mobilePayload);
                    flowInput.put("mobilePayload", mobilePayload);
                    helper.startFlow(MainActivity.this, flowInput, Consts.MOBILE_PAIRING_FLOW);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });


        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Next - onClick");
                showProgressBar();
                try {
                    hideKeyboard(MainActivity.this);
                    String[] formInputs = new String[5];
                    formInputs[0] = input1.getText().toString().trim();
                    formInputs[1] = input2.getText().toString().trim ();
                    Log.i(TAG, "formInput " + formInputs);
                    helper.continueFlow(MainActivity.this, formInputs);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Button authenticateButton = this.findViewById(R.id.authenticateButton);
        authenticateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,ScanQRActivity.class));
            }
        });
    }

    private void setUIFields () {
        nextButton = (Button) this.findViewById(R.id.buttonNext);
        input1 = (EditText) findViewById(R.id.input1);
        input2 = ((EditText) findViewById(R.id.input2));
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

    public void updateFlowUI(JSONObject screen) throws JSONException {
        Log.i(TAG, "UpdateFlowUI with screen " + screen.toString());
        JSONObject properties = screen.getJSONObject("properties");
        updateTitle(properties);
        input1.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.VISIBLE);
        input1.setText("");
        input1.requestFocus();
        boolean secret = properties.getJSONObject("formFieldsList").getJSONArray("value").getJSONObject(0).getBoolean("hashedVisibility");

        if (properties.getJSONObject("formFieldsList").getJSONArray("value").length() > 1) {

            input2.setHint(properties.getJSONObject("formFieldsList").getJSONArray("value").getJSONObject(1).getString("displayName"));
            input1.setHint(properties.getJSONObject("formFieldsList").getJSONArray("value").getJSONObject(0).getString("displayName"));
            input1.requestFocus();
            secret = properties.getJSONObject("formFieldsList").getJSONArray("value").getJSONObject(1).getBoolean("hashedVisibility");
            if (secret)
                input2.setTransformationMethod(PasswordTransformationMethod.getInstance());
            else
                input2.setTransformationMethod(null);
            input2.setVisibility(View.VISIBLE);
        } else {
            input2.setVisibility(View.GONE);
        }
        nextButton.setText(properties.getJSONObject("nextButtonText").getString("value"));
        updateView (MODE_START);
    }

    public void updateFlowUIWithMessage(JSONObject screen) throws JSONException {
        Log.i(TAG, "updateFlowUIWithMessage with screen " + screen.toString());
        JSONObject properties = screen.getJSONObject("properties");
        updateTitle(properties);
        input1.setVisibility(View.GONE);
        input2.setVisibility(View.GONE);
        input1.setText("");
        input2.setText("");
        nextButton.setVisibility(View.VISIBLE);
        nextButton.setText(properties.getJSONObject("button").getString("displayName"));
    }

    protected void updateTitle (JSONObject properties) throws JSONException {
        ((TextView) findViewById(R.id.lblTitle)).setText(properties.getJSONObject("title").getString("value"));
        ((TextView) findViewById(R.id.lblDescription)).setText(properties.getJSONObject("bodyHeaderText").getString("value"));
        findViewById(R.id.lblDescription).setVisibility(View.VISIBLE);
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
                helper.startFlow(MainActivity.this, flowInput, Consts.MOBILE_AUTH_FLOW);
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