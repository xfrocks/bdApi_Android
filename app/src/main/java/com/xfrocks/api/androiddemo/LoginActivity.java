package com.xfrocks.api.androiddemo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterAuthClient;
import com.xfrocks.api.androiddemo.discussion.ConversationActivity;
import com.xfrocks.api.androiddemo.gcm.RegistrationService;
import com.xfrocks.api.androiddemo.persist.AccessTokenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.fabric.sdk.android.Fabric;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener,
        TfaDialogFragment.TfaDialogListener {

    public static final String EXTRA_REDIRECT_TO = "redirect_to";
    private static final String STATE_FACEBOOK_SIGN_IN = "facebookSignIn";
    private static final String STATE_TWITTER_SIGN_IN = "twitterSignIn";
    private static final String STATE_GOOGLE_SIGN_IN = "googleSignIn";
    private static final int RC_REGISTER = 2;
    private static final int RC_GOOGLE_SIGN_IN = 4;

    private TokenRequest mTokenRequest;
    private BroadcastReceiver mGcmReceiver;

    private GoogleApiClient mGoogleApiClient;

    private CallbackManager mFacebookCallbackManager;

    private TwitterAuthConfig mTwitterAuthConfig;
    private TwitterAuthClient mTwitterAuthClient;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private CheckBox mRememberView;
    private Button mGcmUnregister;
    private Button mFacebookSignin;
    private Button mTwitterSignIn;
    private Button mGoogleSignIn;

    private boolean mViewsEnabled = true;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mEmailView = (EditText) findViewById(R.id.email);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin(null, null);
                    return true;
                }
                return false;
            }
        });

        mRememberView = (CheckBox) findViewById(R.id.remember);

        Button mSignIn = (Button) findViewById(R.id.sign_in);
        mSignIn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin(null, null);
            }
        });

        Button mAuthorize = (Button) findViewById(R.id.authorize);
        mAuthorize.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                authorize();
            }
        });
        if (TextUtils.isEmpty(BuildConfig.AUTHORIZE_REDIRECT_URI)) {
            mAuthorize.setVisibility(View.GONE);
        }

        mGcmUnregister = (Button) findViewById(R.id.gcm_unregister);
        mGcmUnregister.setVisibility(View.GONE);

        if (!TextUtils.isEmpty(getString(R.string.facebook_app_id))) {
            FacebookSdk.sdkInitialize(getApplicationContext());
            mFacebookCallbackManager = CallbackManager.Factory.create();
            mFacebookSignin = (Button) findViewById(R.id.facebook_sign_in);
            mFacebookSignin.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    LoginManager.getInstance().logInWithReadPermissions(
                            LoginActivity.this,
                            Arrays.asList("public_profile", "user_friends")
                    );
                }
            });
            LoginManager.getInstance().registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    attemptLoginFacebook(loginResult);
                }

                @Override
                public void onCancel() {
                    Toast.makeText(LoginActivity.this, R.string.error_facebook_cancelled, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError(FacebookException e) {
                    Toast.makeText(LoginActivity.this, R.string.error_facebook_failed, Toast.LENGTH_LONG).show();
                }
            });
        }

        if (!TextUtils.isEmpty(BuildConfig.TWITTER_CONSUMER_KEY)
                && !TextUtils.isEmpty(BuildConfig.TWITTER_CONSUMER_SECRET)) {
            mTwitterAuthConfig = new TwitterAuthConfig(
                    BuildConfig.TWITTER_CONSUMER_KEY,
                    BuildConfig.TWITTER_CONSUMER_SECRET
            );
            Fabric.with(this, new Twitter(mTwitterAuthConfig));
            mTwitterAuthClient = new TwitterAuthClient();
            mTwitterSignIn = (Button) findViewById(R.id.twitter_sign_in);
            mTwitterSignIn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    mTwitterAuthClient.authorize(LoginActivity.this, new Callback<TwitterSession>() {
                        @Override
                        public void success(Result<TwitterSession> result) {
                            attemptLoginTwitter(result.data);
                        }

                        @Override
                        public void failure(TwitterException e) {
                            Toast.makeText(LoginActivity.this, R.string.error_twitter_failed, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }

        if (!TextUtils.isEmpty(getString(R.string.gcm_defaultSenderId))) {
            mGoogleSignIn = (Button) findViewById(R.id.google_sign_in);
            mGoogleSignIn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    attemptLoginGoogle();
                }
            });
        }

        if (mFacebookSignin != null
                || mTwitterSignIn != null
                || mGoogleSignIn != null) {
            if (savedInstanceState != null
                    && (savedInstanceState.containsKey(STATE_FACEBOOK_SIGN_IN)
                    || savedInstanceState.containsKey(STATE_TWITTER_SIGN_IN)
                    || savedInstanceState.containsKey(STATE_GOOGLE_SIGN_IN))) {
                setSocialVisibilities(
                        savedInstanceState.getBoolean(STATE_FACEBOOK_SIGN_IN, false),
                        savedInstanceState.getBoolean(STATE_TWITTER_SIGN_IN, false),
                        savedInstanceState.getBoolean(STATE_GOOGLE_SIGN_IN, false)
                );
            } else {
                new ToolsLoginSocialRequest().start();
            }
        }

        Button mRegister = (Button) findViewById(R.id.register);
        mRegister.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                register(null);
            }
        });

        if (RegistrationService.canRun(LoginActivity.this)) {
            mGcmReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getBooleanExtra(RegistrationService.ACTION_REGISTRATION_UNREGISTERED, false)) {
                        mGcmUnregister.setVisibility(View.GONE);
                    } else {
                        mGcmUnregister.setVisibility(View.VISIBLE);
                    }
                }
            };

            mGcmUnregister.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent gcmIntent = new Intent(LoginActivity.this, RegistrationService.class);
                    gcmIntent.putExtra(RegistrationService.EXTRA_UNREGISTER, true);
                    startService(gcmIntent);
                }
            });

            if (AccessTokenHelper.load(this) == null) {
                // only register if no existing token found
                Intent gcmIntent = new Intent(LoginActivity.this, RegistrationService.class);
                startService(gcmIntent);
            }
        }

        Intent intent = getIntent();
        if (intent != null) {
            attemptLogin(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mFacebookCallbackManager != null) {
            mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
        if (mTwitterAuthClient != null) {
            mTwitterAuthClient.onActivityResult(requestCode, resultCode, data);
        }

        switch (requestCode) {
            case RC_REGISTER:
                if (resultCode == RESULT_OK
                        && data.hasExtra(RegisterActivity.RESULT_EXTRA_ACCESS_TOKEN)) {
                    Api.AccessToken at = (Api.AccessToken) data.getSerializableExtra(RegisterActivity.RESULT_EXTRA_ACCESS_TOKEN);
                    if (at != null) {
                        attemptLogin(at);
                    }
                }
                break;
            case RC_GOOGLE_SIGN_IN:
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                attemptLoginGoogle(result);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        final Api.AccessToken at = AccessTokenHelper.load(this);
        if (at != null) {
            mRememberView.setChecked(true);

            if (App.getFeatureConfirmSignInWithRemember()) {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.sign_in_with_remember)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                attemptLogin(at);
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mRememberView.setChecked(false);
                                AccessTokenHelper.save(LoginActivity.this, null);
                            }
                        })
                        .show();
            } else {
                attemptLogin(at);
            }
        }

        if (mGcmReceiver != null) {
            IntentFilter intentFilter = new IntentFilter(RegistrationService.ACTION_REGISTRATION);
            registerReceiver(mGcmReceiver, intentFilter);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null) {
            attemptLogin(intent);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(STATE_FACEBOOK_SIGN_IN, mFacebookSignin != null
                && mFacebookSignin.getVisibility() == View.VISIBLE);
        outState.putBoolean(STATE_TWITTER_SIGN_IN, mTwitterSignIn != null
                && mTwitterSignIn.getVisibility() == View.VISIBLE);
        outState.putBoolean(STATE_GOOGLE_SIGN_IN, mGoogleSignIn != null
                && mGoogleSignIn.getVisibility() == View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mGcmReceiver != null) {
            unregisterReceiver(mGcmReceiver);
        }

        if (mTokenRequest != null) {
            mTokenRequest.cancel();
        }

        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    private void authorize() {
        Intent loginIntent = getIntent();
        String redirectTo = null;
        if (loginIntent.hasExtra(EXTRA_REDIRECT_TO)) {
            redirectTo = loginIntent.getStringExtra(EXTRA_REDIRECT_TO);
        }

        String authorizeUri = Api.makeAuthorizeUri(redirectTo);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizeUri));
        startActivity(intent);
    }

    private void register(Api.User u) {
        Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
        if (u != null) {
            registerIntent.putExtra(RegisterActivity.EXTRA_USER, u);
        }

        startActivityForResult(registerIntent, RC_REGISTER);
    }

    private void attemptLoginFacebook(LoginResult result) {
        new TokenFacebookRequest(result.getAccessToken().getToken()).start();
    }

    private void attemptLoginTwitter(TwitterSession session) {
        String method = "GET";
        String uri = "https://api.twitter.com/1.1/account/verify_credentials.json";
        Map<String, String> postParams = new HashMap<>();
        Map<String, String> headers = session.getAuthToken().getAuthHeaders(mTwitterAuthConfig, method, uri, postParams);
        String auth = headers.get("Authorization");

        new TokenTwitterRequest(uri, auth).start();
    }

    private void attemptLoginGoogle() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    private void attemptLoginGoogle(GoogleSignInResult result) {
        if (!result.isSuccess()) {
            return;
        }

        GoogleSignInAccount account = result.getSignInAccount();
        if (account == null) {
            return;
        }

        String token = account.getIdToken();
        if (TextUtils.isEmpty(token)) {
            return;
        }

        new TokenGoogleRequest(token).start();
    }

    private void attemptLogin(String tfaProviderId, String tfaProviderCode) {
        if (mTokenRequest != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            new PasswordRequest(email, password, tfaProviderId, tfaProviderCode).start();
        }
    }

    private void attemptLogin(Intent intent) {
        if (TextUtils.isEmpty(BuildConfig.AUTHORIZE_REDIRECT_URI)) {
            return;
        }

        Uri data = intent.getData();
        if (data == null) {
            return;
        }

        String code = data.getQueryParameter("code");

        String redirectTo = data.getQueryParameter("redirect_to");
        if (!TextUtils.isEmpty(redirectTo)) {
            intent.putExtra(EXTRA_REDIRECT_TO, redirectTo);
        }

        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, R.string.error_no_authorization_code, Toast.LENGTH_LONG).show();
        } else {
            new AuthorizationCodeRequest(code, redirectTo).start();
        }

        setIntent(intent);
    }

    private void attemptLogin(Api.AccessToken at) {
        if (mTokenRequest != null) {
            return;
        }

        if (at.isValid()) {
            startNextActivityAndFinish(at);
            return;
        }

        final String refreshToken = at.getRefreshToken();
        if (TextUtils.isEmpty(refreshToken)) {
            Toast.makeText(this, R.string.error_no_refresh_token, Toast.LENGTH_LONG).show();
            return;
        }

        new RefreshTokenRequest(refreshToken).start();
    }

    private void startNextActivityAndFinish(Api.AccessToken at) {
        Intent nextIntent = null;
        Intent loginIntent = getIntent();
        String redirectTo = "";
        if (loginIntent != null && loginIntent.hasExtra(EXTRA_REDIRECT_TO)) {
            redirectTo = loginIntent.getStringExtra(EXTRA_REDIRECT_TO);
        }

        Class[] discussionActivities = new Class[]{
                ConversationActivity.class,
        };
        for (Class discussionActivity: discussionActivities) {
            String discussionActivityRedirectToPrefix = discussionActivity.getSimpleName() + "://";
            if (redirectTo.startsWith(discussionActivityRedirectToPrefix)) {
                nextIntent = new Intent(LoginActivity.this, ConversationActivity.class);
                nextIntent.putExtra(ConversationActivity.EXTRA_ACCESS_TOKEN, at);
                nextIntent.putExtra(ConversationActivity.EXTRA_LOGIN_REDIRECTED_TO, redirectTo);
            }
        }

        if (nextIntent == null) {
            nextIntent = new Intent(LoginActivity.this, MainActivity.class);
            nextIntent.putExtra(MainActivity.EXTRA_ACCESS_TOKEN, at);
            if (!redirectTo.isEmpty()) {
                nextIntent.putExtra(MainActivity.EXTRA_URL, redirectTo);
            }
        }

        startActivity(nextIntent);

        if (RegistrationService.canRun(LoginActivity.this)) {
            Intent gcmIntent = new Intent(LoginActivity.this, RegistrationService.class);
            gcmIntent.putExtra(RegistrationService.EXTRA_ACCESS_TOKEN, at);
            startService(gcmIntent);
        }

        finish();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, R.string.error_google_failed, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTfaTrigger(String providerId) {
        attemptLogin(providerId, null);
    }

    @Override
    public void onTfaFinishDialog(String providerId, String code) {
        attemptLogin(providerId, code);
    }

    private void setViewsEnabled(final boolean enabled) {
        mViewsEnabled = enabled;

        // we have to use handler and postDelayed because in this activity
        // some requests are chained together and dismissing then showing immediately
        // the progress dialog seems to be not optimal
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mViewsEnabled != enabled) {
                    // view-enable state has been changed
                    return;
                }

                if (!enabled) {
                    // disabling views, let's show the progress dialog (if not yet showing)
                    if (mProgressDialog == null) {
                        mProgressDialog = new ProgressDialog(LoginActivity.this);
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
        }, enabled ? 0 : 100);
    }

    private void setSocialVisibilities(boolean facebook, boolean twitter, boolean google) {
        if (mFacebookSignin != null) {
            mFacebookSignin.setVisibility(facebook ? View.VISIBLE : View.GONE);
        }
        if (mTwitterSignIn != null) {
            mTwitterSignIn.setVisibility(twitter ? View.VISIBLE : View.GONE);
        }
        if (mGoogleSignIn != null) {
            mGoogleSignIn.setVisibility(google ? View.VISIBLE : View.GONE);
        }
    }

    private abstract class TokenRequest extends Api.PostRequest {
        TokenRequest(Map<String, String> params) {
            super(Api.URL_OAUTH_TOKEN, params);
        }

        TokenRequest(String url, Map<String, String> params) {
            super(url, params);
        }

        @Override
        protected void onStart() {
            mTokenRequest = this;
            setViewsEnabled(false);
            AccessTokenHelper.save(LoginActivity.this, null);
        }

        @Override
        protected void onSuccess(JSONObject response) {
            Api.AccessToken at = Api.makeAccessToken(response);
            if (at == null) {
                if (response.has("user_data")) {
                    try {
                        Api.User u = Api.makeUser(response.getJSONObject("user_data"));
                        if (u != null) {
                            register(u);
                        }
                        return;
                    } catch (JSONException e) {
                        // ignore
                    }
                }

                if (mResponseHeaders != null
                        && mResponseHeaders.containsKey(Api.URL_OAUTH_TOKEN_RESPONSE_HEADER_TFA_PROVIDERS)) {
                    String headerValue = mResponseHeaders.get(Api.URL_OAUTH_TOKEN_RESPONSE_HEADER_TFA_PROVIDERS);
                    String[] providerIds = headerValue.split(",");

                    FragmentManager fm = getSupportFragmentManager();
                    TfaDialogFragment tfaDialog = TfaDialogFragment.newInstance(providerIds);
                    tfaDialog.show(fm, tfaDialog.getClass().getSimpleName());

                    return;
                }

                String errorMessage = getErrorMessage(response);
                if (errorMessage != null) {
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    return;
                }

                return;
            }

            if (mRememberView.isChecked()) {
                AccessTokenHelper.save(LoginActivity.this, at);
            }

            startNextActivityAndFinish(at);
        }

        @Override
        protected void onError(VolleyError error) {
            String message = getErrorMessage(error);

            if (message != null) {
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onComplete() {
            mTokenRequest = null;
            setViewsEnabled(true);
        }
    }

    private class PasswordRequest extends TokenRequest {
        PasswordRequest(String email, String password, String tfaProviderId, String tfaProviderCode) {
            super(
                    new Api.Params(
                            Api.URL_OAUTH_TOKEN_PARAM_GRANT_TYPE,
                            Api.URL_OAUTH_TOKEN_PARAM_GRANT_TYPE_PASSWORD)
                            .and(Api.URL_OAUTH_TOKEN_PARAM_USERNAME, email)
                            .and(Api.URL_OAUTH_TOKEN_PARAM_PASSWORD, password)
                            .andIf(tfaProviderId != null,
                                    Api.URL_OAUTH_TOKEN_PARAM_TFA_PROVIDER_ID, tfaProviderId)
                            .andIf(tfaProviderId != null && tfaProviderCode == null,
                                    Api.URL_OAUTH_TOKEN_PARAM_TFA_TRIGGER, 1)
                            .andIf(tfaProviderId != null && tfaProviderCode != null,
                                    Api.URL_OAUTH_TOKEN_PARAM_TFA_PROVIDER_CODE, tfaProviderCode)
                            .andClientCredentials()
            );
        }
    }

    private class AuthorizationCodeRequest extends TokenRequest {
        AuthorizationCodeRequest(String code, String redirectTo) {
            super(
                    new Api.Params(
                            Api.URL_OAUTH_TOKEN_PARAM_GRANT_TYPE,
                            Api.URL_OAUTH_TOKEN_PARAM_GRANT_TYPE_AUTHORIZATION_CODE)
                            .and(Api.URL_OAUTH_TOKEN_PARAM_CODE, code)
                            .and(Api.URL_OAUTH_TOKEN_PARAM_REDIRECT_URI, Api.makeAuthorizeRedirectUri(redirectTo))
                            .andClientCredentials()
            );
        }

        @Override
        protected void onStart() {
            super.onStart();

            // auto remember with authorization code flow
            mRememberView.setChecked(true);
        }
    }

    private class RefreshTokenRequest extends TokenRequest {
        RefreshTokenRequest(String refreshToken) {
            super(
                    new Api.Params(
                            Api.URL_OAUTH_TOKEN_PARAM_GRANT_TYPE,
                            Api.URL_OAUTH_TOKEN_PARAM_GRANT_TYPE_REFRESH_TOKEN)
                            .and(Api.URL_OAUTH_TOKEN_PARAM_REFRESH_TOKEN, refreshToken)
                            .andClientCredentials()
            );
        }
    }

    private class ToolsLoginSocialRequest extends Api.PostRequest {
        ToolsLoginSocialRequest() {
            super(Api.URL_TOOLS_LOGIN_SOCIAL, new Api.Params(Api.makeOneTimeToken(0, null)));
        }

        @Override
        protected void onSuccess(JSONObject response) {
            try {
                if (response.has("social")) {
                    JSONArray networks = response.getJSONArray("social");
                    boolean facebook = false;
                    boolean twitter = false;
                    boolean google = false;

                    for (int i = 0; i < networks.length(); i++) {
                        String network = networks.getString(i);
                        switch (network) {
                            case "facebook":
                                facebook = true;
                                break;
                            case "twitter":
                                twitter = true;
                                break;
                            case "google":
                                google = true;
                                break;
                        }
                    }

                    setSocialVisibilities(facebook, twitter, google);
                }
            } catch (JSONException e) {
                // ignore
            }
        }
    }

    private class TokenFacebookRequest extends TokenRequest {
        TokenFacebookRequest(String accessToken) {
            super(
                    Api.URL_OAUTH_TOKEN_FACEBOOK,
                    new Api.Params(Api.URL_OAUTH_TOKEN_FACEBOOK_PARAM_TOKEN, accessToken)
                            .andClientCredentials()
            );
        }
    }

    private class TokenTwitterRequest extends TokenRequest {
        TokenTwitterRequest(String uri, String auth) {
            super(
                    Api.URL_OAUTH_TOKEN_TWITTER,
                    new Api.Params(Api.URL_OAUTH_TOKEN_TWITTER_PARAM_URI, uri)
                            .and(Api.URL_OAUTH_TOKEN_TWITTER_PARAM_AUTH, auth)
                            .andClientCredentials()
            );
        }
    }

    private class TokenGoogleRequest extends TokenRequest {
        TokenGoogleRequest(String idToken) {
            super(
                    Api.URL_OAUTH_TOKEN_GOOGLE,
                    new Api.Params(Api.URL_OAUTH_TOKEN_GOOGLE_PARAM_TOKEN, idToken)
                            .andClientCredentials()
            );
        }
    }
}

