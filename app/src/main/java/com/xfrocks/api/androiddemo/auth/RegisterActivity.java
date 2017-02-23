package com.xfrocks.api.androiddemo.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.annotations.SerializedName;
import com.xfrocks.api.androiddemo.App;
import com.xfrocks.api.androiddemo.R;
import com.xfrocks.api.androiddemo.common.ApiConstants;
import com.xfrocks.api.androiddemo.common.ApiBaseResponse;
import com.xfrocks.api.androiddemo.common.DatePickerDialogFragment;
import com.xfrocks.api.androiddemo.common.Api;
import com.xfrocks.api.androiddemo.common.model.ApiAccessToken;
import com.xfrocks.api.androiddemo.common.model.ApiUser;

import java.util.Locale;

public class RegisterActivity extends AppCompatActivity
        implements TfaDialogFragment.TfaDialogListener,
        DatePickerDialogFragment.Listener {

    public static final String EXTRA_USER = "user";
    public static final String RESULT_EXTRA_ACCESS_TOKEN = "access_token";
    private static final int DATE_PICKER_RC_DOB = 1;

    private EditText mEmailView;
    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mDobView;
    private Button mRegister;
    private ProgressDialog mProgressDialog;

    private Integer mDobYear;
    private Integer mDobMonth;
    private Integer mDobDay;
    private ApiUser mTargetUser;
    private ApiUser[] mAssocUsers;
    private String mExtraData;
    private long mExtraTimestamp;
    private Api.PostRequest mRegisterRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mUsernameView = (EditText) findViewById(R.id.username);
        mEmailView = (EditText) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mDobView = (EditText) findViewById(R.id.dob);

        mDobView.setSelectAllOnFocus(true);
        mDobView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    showDatePicker();
                }
            }
        });
        mDobView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker();
            }
        });

        mRegister = (Button) findViewById(R.id.register);
        mRegister.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTargetUser == null) {
                    attemptRegister();
                } else {
                    attemptAssociate(null, null);
                }
            }
        });

        Intent registerIntent = getIntent();
        if (registerIntent != null && registerIntent.hasExtra(EXTRA_USER)) {
            ApiUser u = (ApiUser) registerIntent.getSerializableExtra(EXTRA_USER);

            final String username = u.getUsername();
            if (!TextUtils.isEmpty(username)) {
                mUsernameView.setText(username);
            }

            final String email = u.getEmail();
            if (!TextUtils.isEmpty(email)) {
                mEmailView.setText(email);
            }

            final Integer dobYear = u.getDobYear();
            final Integer dobMonth = u.getDobMonth();
            final Integer dobDay = u.getDobDay();
            if (dobYear != null
                    && dobMonth != null
                    && dobDay != null) {
                setDate(dobYear, dobMonth, dobDay);
            }

            mAssocUsers = u.getAssocs();
            mExtraData = u.getExtraData();
            mExtraTimestamp = u.getExtraTimestamp();

            if (mAssocUsers.length > 0) {
                TabLayout mTabLayout = (TabLayout) findViewById(R.id.tab);
                mTabLayout.setVisibility(View.VISIBLE);
                mTabLayout.addTab(mTabLayout.newTab().setText(R.string.action_register));
                for (ApiUser assocUser : mAssocUsers) {
                    mTabLayout.addTab(mTabLayout.newTab()
                            .setText(assocUser.getUsername() != null
                                    ? assocUser.getUsername()
                                    : assocUser.getEmail())
                            .setTag(assocUser.getId()));
                }
                mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        if (tab.getTag() instanceof Integer) {
                            setRegisterOrAssociate((Integer) tab.getTag());
                        } else {
                            setRegisterOrAssociate(0);
                        }
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {

                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {

                    }
                });
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mRegisterRequest != null) {
            mRegisterRequest.cancel();
        }

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    public void onTfaTrigger(String providerId) {
        attemptAssociate(providerId, null);
    }

    @Override
    public void onTfaFinishDialog(String providerId, String code) {
        attemptAssociate(providerId, code);
    }

    /**
     * Attempts to register the account with information from the form fields.
     * If there are input errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual register attempt is made.
     */
    private void attemptRegister() {
        if (mRegisterRequest != null) {
            return;
        }
        if (mTargetUser != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        String username = mUsernameView.getText().toString().trim();
        String email = mEmailView.getText().toString().trim();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }

        if (TextUtils.isEmpty(password)) {
            if (TextUtils.isEmpty(mExtraData)) {
                // only requires password if no extra data exists
                mPasswordView.setError(getString(R.string.error_field_required));
                focusView = mPasswordView;
                cancel = true;
            }
        } else if (isPasswordInvalid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (isEmailInvalid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            new RegisterRequest(
                    username, email, password,
                    mDobYear != null ? mDobYear : 0,
                    mDobMonth != null ? mDobMonth : 0,
                    mDobDay != null ? mDobDay : 0
            ).start();
        }
    }

    private void attemptAssociate(String tfaProviderId, String tfaProviderCode) {
        if (mRegisterRequest != null) {
            return;
        }
        if (mTargetUser == null) {
            return;
        }

        // Reset errors.
        mPasswordView.setError(null);

        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (isPasswordInvalid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            new AssociateRequest(mTargetUser.getId(), password, tfaProviderId, tfaProviderCode).start();
        }
    }

    private boolean isEmailInvalid(String email) {
        return !email.contains("@");
    }

    private boolean isPasswordInvalid(String password) {
        return password.length() <= 4;
    }

    private void setRegisterOrAssociate(int userId) {
        mTargetUser = null;
        for (ApiUser assocUser : mAssocUsers) {
            if (assocUser.getId() == userId) {
                mTargetUser = assocUser;
            }
        }

        if (mTargetUser == null) {
            mUsernameView.setVisibility(View.VISIBLE);
            mEmailView.setVisibility(View.VISIBLE);
            mPasswordView.setText("");
            mDobView.setVisibility(View.VISIBLE);
            mRegister.setText(R.string.action_register);
        } else {
            mUsernameView.setVisibility(View.GONE);
            mEmailView.setVisibility(View.GONE);
            mPasswordView.setText("");
            mDobView.setVisibility(View.GONE);
            mRegister.setText(R.string.action_associate);
        }
    }

    private void setViewsEnabled(boolean enabled) {
        if (!enabled) {
            // disabling views, let's show the progress dialog (if not yet showing)
            if (mProgressDialog == null) {
                mProgressDialog = new ProgressDialog(RegisterActivity.this);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
            }
        } else {
            // enabling views, hide the progress dialog if any
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        }
    }

    private void showDatePicker() {
        DatePickerDialogFragment dpdf = DatePickerDialogFragment.newInstance(DATE_PICKER_RC_DOB, mDobYear, mDobMonth, mDobDay);
        dpdf.show(getSupportFragmentManager(), "DatePickerDialog");
    }

    private void setDate(int year, int month, int day) {
        mDobYear = year;
        mDobMonth = month;
        mDobDay = day;

        mDobView.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month, day));
    }

    @Override
    public void onDatePickerDialogFragmentNewDate(int year, int monthFromOne, int day) {
        setDate(year, monthFromOne, day);
        mDobView.selectAll();
    }

    class RegisterRequest extends Api.PostRequest {
        RegisterRequest(String username,
                        String email,
                        String password,
                        int dobYear, int dobMonth, int dobDay) {
            super(ApiConstants.URL_USERS, new Api.Params(ApiConstants.URL_USERS_PARAM_USERNAME, username)
                    .and(ApiConstants.URL_USERS_PARAM_EMAIL, email)
                    .and(ApiConstants.URL_USERS_PARAM_PASSWORD, password)
                    .and(ApiConstants.URL_USERS_PARAM_DOB_YEAR, dobYear)
                    .and(ApiConstants.URL_USERS_PARAM_DOB_MONTH, dobMonth)
                    .and(ApiConstants.URL_USERS_PARAM_DOB_DAY, dobDay)
                    .and(ApiConstants.URL_USERS_PARAM_EXTRA_DATA, mExtraData)
                    .and(ApiConstants.URL_USERS_PARAM_EXTRA_TIMESTAMP, mExtraTimestamp)
                    .andClientCredentials());
        }

        @Override
        protected void onStart() {
            mRegisterRequest = this;
            setViewsEnabled(false);
        }

        @Override
        protected void onSuccess(String response) {
            RegisterResponse data = App.getGsonInstance().fromJson(response, RegisterResponse.class);
            if (data.at != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(RESULT_EXTRA_ACCESS_TOKEN, data.at);
                setResult(RESULT_OK, resultIntent);
                finish();
                return;
            }

            String error = data.getError();
            if (TextUtils.isEmpty(error)) {
                return;
            }

            Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onComplete() {
            mRegisterRequest = null;
            setViewsEnabled(true);
        }
    }

    class AssociateRequest extends Api.PostRequest {
        AssociateRequest(int userId, String password, String tfaProviderId, String tfaProviderCode) {
            super(ApiConstants.URL_OAUTH_TOKEN_ASSOCIATE, new Api.Params(ApiConstants.URL_USERS_PARAM_USER_ID, userId)
                    .and(ApiConstants.URL_USERS_PARAM_PASSWORD, password)
                    .and(ApiConstants.URL_USERS_PARAM_EXTRA_DATA, mExtraData)
                    .and(ApiConstants.URL_USERS_PARAM_EXTRA_TIMESTAMP, mExtraTimestamp)
                    .andIf(tfaProviderId != null,
                            ApiConstants.URL_OAUTH_TOKEN_PARAM_TFA_PROVIDER_ID, tfaProviderId)
                    .andIf(tfaProviderId != null && tfaProviderCode == null,
                            ApiConstants.URL_OAUTH_TOKEN_PARAM_TFA_TRIGGER, 1)
                    .andIf(tfaProviderId != null && tfaProviderCode != null,
                            ApiConstants.URL_OAUTH_TOKEN_PARAM_TFA_PROVIDER_CODE, tfaProviderCode)
                    .andClientCredentials());
        }

        @Override
        protected void onStart() {
            mRegisterRequest = this;
            setViewsEnabled(false);
        }

        @Override
        protected void onSuccess(String response) {
            ApiAccessToken at = App.getGsonInstance().fromJson(response, ApiAccessToken.class);
            if (!TextUtils.isEmpty(at.getToken())) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(RESULT_EXTRA_ACCESS_TOKEN, at);
                setResult(RESULT_OK, resultIntent);
                finish();
                return;
            }

            if (mResponseHeaders != null
                    && mResponseHeaders.containsKey(ApiConstants.URL_OAUTH_TOKEN_RESPONSE_HEADER_TFA_PROVIDERS)) {
                String headerValue = mResponseHeaders.get(ApiConstants.URL_OAUTH_TOKEN_RESPONSE_HEADER_TFA_PROVIDERS);
                String[] providerIds = headerValue.split(",");

                FragmentManager fm = getSupportFragmentManager();
                TfaDialogFragment tfaDialog = TfaDialogFragment.newInstance(providerIds);
                tfaDialog.show(fm, tfaDialog.getClass().getSimpleName());

                return;
            }

            ApiBaseResponse data = App.getGsonInstance().fromJson(response, ApiBaseResponse.class);
            String error = data.getError();
            if (TextUtils.isEmpty(error)) {
                return;
            }

            Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onComplete() {
            mRegisterRequest = null;
            setViewsEnabled(true);
        }
    }

    static class RegisterResponse extends ApiBaseResponse {
        @SerializedName("token")
        ApiAccessToken at;
    }
}

