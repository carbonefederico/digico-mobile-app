package com.pingidentity.emeasa.mobilesample;

import android.app.DialogFragment;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.M)
public class FingerprintAuthenticationDialogFragment extends DialogFragment
        implements FingerprintHelper.Callback  {

    private FingerprintHelper mFingerprintHelper;
    private FingerprintManager.CryptoObject mCryptoObject;
    private BaseActivity mActivity;

    private String theTitle, theDescription;
    private int theMode;


    public void setArguments(Bundle args) {
        theTitle = args.getString("title");
        theDescription = args.getString("description");

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (BaseActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (this.theTitle == null)
            getDialog().setTitle(getString(R.string.auth_please_authenticate));
        else
            getDialog().setTitle(this.theTitle);
        View v = inflater.inflate(R.layout.fingerprint_dialog_container, container, false);
        Button mCancelButton = (Button) v.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        TextView t = (TextView) v.findViewById(R.id.fingerprint_description);
        if (this.theDescription != null) {
            t.setText(this.theDescription);
        }
        mFingerprintHelper = new FingerprintHelper(
                mActivity.getSystemService(FingerprintManager.class),
                (ImageView) v.findViewById(R.id.fingerprint_icon),
                (TextView) v.findViewById(R.id.fingerprint_status), this);
        // If fingerprint authentication is not available
        if (!mFingerprintHelper.isFingerprintAuthAvailable()) {

        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mFingerprintHelper.startListening(mCryptoObject);
    }

    @Override
    public void onPause() {
        super.onPause();
        mFingerprintHelper.stopListening();
    }

    @Override
    public void onAuthenticated() {
        mActivity.onFingerPrintAuthenticatedCallback();
    }

    @Override
    public void onError() {

    }
}
