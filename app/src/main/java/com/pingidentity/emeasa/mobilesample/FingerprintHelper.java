package com.pingidentity.emeasa.mobilesample;

import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;


/**
     * Small helper class to manage the UI on the fingerprint dialogue
     * Created by Ping Identity on 11/5/18.
     */

    @RequiresApi(api = Build.VERSION_CODES.M)
    public class FingerprintHelper extends FingerprintManager.AuthenticationCallback {
        private static final long ERROR_TIMEOUT_MILLIS = 1600;
        private static final long SUCCESS_DELAY_MILLIS = 1300;

        private final FingerprintManager mFingerprintManager;
        private final ImageView mIcon;
        private final TextView mErrorTextView;
        private final Callback mCallback;
        private CancellationSignal mCancellationSignal;

        private boolean mSelfCancelled;

        public interface Callback {

            void onAuthenticated();

            void onError();
        }
        FingerprintHelper(FingerprintManager fingerprintManager,
                          ImageView icon, TextView errorTextView, Callback callback) {
            mFingerprintManager = fingerprintManager;
            mIcon = icon;
            mErrorTextView = errorTextView;
            mCallback = callback;
        }

        public boolean isFingerprintAuthAvailable() {
            // The line below prevents the false positive inspection from Android Studio
            // noinspection ResourceType
            return mFingerprintManager.isHardwareDetected()
                    && mFingerprintManager.hasEnrolledFingerprints();
        }

        public void startListening(FingerprintManager.CryptoObject cryptoObject) {
            if (!isFingerprintAuthAvailable()) {
                return;
            }
            mCancellationSignal = new CancellationSignal();
            mSelfCancelled = false;
            // The line below prevents the false positive inspection from Android Studio
            // noinspection ResourceType
            mFingerprintManager
                    .authenticate(cryptoObject, mCancellationSignal, 0 /* flags */, this, null);
            mIcon.setImageResource(R.mipmap.ic_fp_40px);
        }

        public void stopListening() {
            if (mCancellationSignal != null) {
                mSelfCancelled = true;
                mCancellationSignal.cancel();
                mCancellationSignal = null;
            }
        }

        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            if (!mSelfCancelled) {
                showError(errString);
                mIcon.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onError();
                    }
                }, ERROR_TIMEOUT_MILLIS);
            }
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            showError(helpString);
        }

        @Override
        public void onAuthenticationFailed() {
            showError(mIcon.getResources().getString(
                    R.string.fingerprint_not_recognized));
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            mErrorTextView.removeCallbacks(mResetErrorTextRunnable);
            mIcon.setImageResource(R.drawable.fingerprint_success);
            mErrorTextView.setTextColor(
                    mErrorTextView.getResources().getColor(R.color.fingerprint_success_color, null));
            mErrorTextView.setText(
                    mErrorTextView.getResources().getString(R.string.fingerprint_success));
            mIcon.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mCallback.onAuthenticated();
                }
            }, SUCCESS_DELAY_MILLIS);
        }

        private void showError(CharSequence error) {
            mIcon.setImageResource(R.drawable.fingerprint_error);
            mErrorTextView.setText(error);
            mErrorTextView.setTextColor(
                    mErrorTextView.getResources().getColor(R.color.fingerprint_warning_color, null));
            mErrorTextView.removeCallbacks(mResetErrorTextRunnable);
            mErrorTextView.postDelayed(mResetErrorTextRunnable, ERROR_TIMEOUT_MILLIS);
        }

        private Runnable mResetErrorTextRunnable = new Runnable() {
            @Override
            public void run() {
                mErrorTextView.setTextColor(
                        mErrorTextView.getResources().getColor(R.color.fingerprint_hint_color, null));
                mErrorTextView.setText(
                        mErrorTextView.getResources().getString(R.string.fingerprint_hint));
                mIcon.setImageResource(R.mipmap.ic_fp_40px);
            }
        };
}
