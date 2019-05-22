package com.delex.delexexpert;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.delex.delexexpert.databinding.ActivityMainBinding;
import com.delex.delexexpert.util.EditImageUtil;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity3 extends AppCompatActivity {

    public final String TAG = MainActivity3.class.getSimpleName();
    public final int POST = 1;
    public final int DELETE = 2;
    public final int PUT = 3;
    private ActivityMainBinding binding;
    private final long FINISH_INTERVAL_TIME = 2000;
    private long backPressedTime = 0;
    public static boolean sVisibleActivity;  //화면 보이면 노티 눌렀을때 다시 액티비티 켜지지 않게 설정하는 변수

    private String mUrl;  //현재 페이지 url

    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;

    private EditImageUtil editImageUtil;
//    private RetrofitLib mRetrofitLib;

    private SharedPreferences mPref;

    private static final int IMAGE_MAX_DIMENSION = 1280;

    private boolean firstPageLoadingCompleted = false;
    private ConnectivityManager mManager;
    //    private WaveLoadingView mWaveLoadingView;
    private NetworkInfo mMobile;
    private NetworkInfo mWifi;
    private Handler mHandler;

    private boolean canEnd = false;
    private int count = 0;
    public boolean splashLoadingComplite = false;
    private boolean loadingConfirm = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        mPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        editImageUtil = new EditImageUtil();

        networkCheck();

    }

    public void setWebview() {
        WebSettings webSettings = binding.webView.getSettings();

        webSettings.setSaveFormData(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setGeolocationEnabled(true);

        if (Build.VERSION.SDK_INT >= 16) {
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        binding.webView.getSettings().setJavaScriptEnabled(true);
        // JavaScript의 window.open 허용
        binding.webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        binding.webView.addJavascriptInterface(new MyJavascriptInterface(), "Android");
        binding.webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                super.onGeolocationPermissionsShowPrompt(origin, callback);
                callback.invoke(origin, true, false);
            }

            //For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUM = uploadMsg;
            }

            // For Android 3.0+, above method not supported in some android 3+ versions, in such case we use this
            public void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                mUM = uploadMsg;
            }

            //For Android 4.1+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUM = uploadMsg;

            }

            //For Android 5.0+
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                Log.d(TAG, "onShowFileChooser: ");

                if (mUMA != null) {
                    mUMA.onReceiveValue(null);
                }
                mUMA = filePathCallback;

                mCM = "file:";
                return true;
            }
        });


        binding.webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                binding.progressBar.setVisibility(View.VISIBLE);
                mUrl = url;

            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                mUrl = url;

                binding.progressBar.setVisibility(View.INVISIBLE);

                if (!firstPageLoadingCompleted) {  //처음 로딩할때 페이지 로딩 완료를 알려주는 변수
                    //로딩 끝
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!splashLoadingComplite) {
                                try {
                                    Thread.sleep(2000);
                                    if (loadingConfirm) {
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                binding.splashLayout.setVisibility(View.GONE);
                                            }
                                        });
                                        canEnd = true;
                                        splashLoadingComplite = true;
                                    }

                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).start();

                    firstPageLoadingCompleted = true;
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "shouldOverrideUrlLoading: " + url);
                mUrl = url;
                view.loadUrl(url);
                return true;
            }
        });

        webSettings.setJavaScriptEnabled(true);
    }

//    public void sendTokenToServer(final int howTo, final String userId) {
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
//
//        SharedPreferences.Editor editor = mPref.edit();
//
//        editor.putString("token", refreshedToken);
//
//        editor.commit();
//
//
//        String token = mPref.getString("token", null);
//
//        Log.d(TAG, "sendTokenToServer: " + token);
//        Call<TokenModel> tokenRequestCall = null;
//
//        switch (howTo) {
//            case POST:
//                tokenRequestCall = mRetrofitLib.getRetrofit(this).sendTokenPost(token, userId);
//                break;
//
//            case DELETE:
//                tokenRequestCall = mRetrofitLib.getRetrofit(this).sendTokenDelete(token, userId);
//                break;
//
//            case PUT:
//                tokenRequestCall = mRetrofitLib.getRetrofit(this).sendTokenPut(token, userId);
//                break;
//        }
//
//        tokenRequestCall.enqueue(new Callback<TokenModel>() {
//            @Override
//            public void onResponse(Call<TokenModel> call, Response<TokenModel> response) {
//                if (response.isSuccessful()) {
//
//                    Log.d(TAG, "json 값: " + response.body().getKey());
//
//                    if (response.body().getKey() == null) {  //access 토큰값이 제대로 된 값이 아닐때
//                        Log.d(TAG, "onResponse: ");
//                        switch (howTo) {
//                            case POST:
//                                sendTokenToServer(POST, userId);
//                                break;
//                            case DELETE:
//                                sendTokenToServer(DELETE, userId);
//                                break;
//                            case PUT:
//                                sendTokenToServer(PUT, userId);
//                                break;
//                        }
//                    }
//                }
//            }
//
//            @Override
//            public void onFailure(Call<TokenModel> call, Throwable t) {
//                Log.d(TAG, "실패" + t.toString());
//
//            }
//        });
//    }


    public String convertToString(InputStream inputStream) {
        StringBuffer string = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                string.append(line + "\n");
            }
        } catch (IOException e) {
        }
        return string.toString();
    }


    @Override
    protected void onResume() {
        super.onResume();

        sVisibleActivity = true;

//        SharedPreferences.Editor editor = mPref.edit();
//        editor.putInt(MyFirebaseMessagingService.PUSH_COUNT, 0);
//        editor.commit();
//
//        int pushCount = mPref.getInt(MyFirebaseMessagingService.PUSH_COUNT, 0);
//        Log.d("dd", "onResume: " + pushCount);
//
//        PushConnectService.setBadge(this, pushCount);
//        Log.d(TAG, "onResume: ");

    }

    @Override
    protected void onPause() {
        super.onPause();
        sVisibleActivity = false;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {

        if (canEnd) {

            long tempTime = System.currentTimeMillis();
            long intervalTime = tempTime - backPressedTime;

            if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
                super.onBackPressed();
                Log.d(TAG, "onBackPressed: ");
            } else {
                backPressedTime = tempTime;
                Toast.makeText(getApplicationContext(), "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onBackPressed: ");
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        String url = binding.webView.getUrl();
        String loginPageUrl = getString(R.string.login_url);
        String mainPageUrl = getString(R.string.main_url);
        Log.d(TAG, "onKeyDown: login"+loginPageUrl);

        if ((keyCode == KeyEvent.KEYCODE_BACK) && (url.equals(loginPageUrl) || url.equals(mainPageUrl))) {

            Log.d(TAG, "onKeyDown: main_url"+url);

        } else if ((keyCode == KeyEvent.KEYCODE_BACK) && binding.webView.canGoBack()) {  //메인인데 뒤로 갈 수 있으면
            binding.webView.goBack();
            Log.d(TAG, "onKeyDown dddd: " + mUrl);

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        editImageUtil.fileDelete(this);
        sVisibleActivity = false;
        count = 111;
        loadingConfirm = true;
        splashLoadingComplite = true;
    }

    public class MyJavascriptInterface {

        @JavascriptInterface
        public void kakaoNavi(final String url) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        }

//        @JavascriptInterface
//        public void postDeviceToken(String userId) {
//            Log.d(TAG, "postDeviceToken: ");
//            sendTokenToServer(POST, userId);
//        }
//
//        @JavascriptInterface
//        public void deleteDeviceToken(String userId) {
//            Log.d(TAG, "deleteDeviceToken: ");
//            sendTokenToServer(DELETE, userId);
//        }
//
//        @JavascriptInterface
//        public void putDeviceToken(String userId) {
//            Log.d(TAG, "putDeviceToken: ");
//            sendTokenToServer(PUT, userId);
//        }
    }

    public static void cookieMaker(String url) {
        //롤리팝 이하 버전 cookiesyncmanager로 사용

        String COOKIES_HEADER = "Set-Cookie";
        try {

            URL url1 = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) url1.openConnection();

            con.connect();

            Map<String, List<String>> headerFields = con.getHeaderFields();
            List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);

            if (cookiesHeader != null) {
                for (String cookie : cookiesHeader) {
                    String cookieName = HttpCookie.parse(cookie).get(0).getName();
                    String cookieValue = HttpCookie.parse(cookie).get(0).getValue();

                    String cookieString = cookieName + "=" + cookieValue;
                    Log.d("d", "cookieMaker: " + cookieString);

//                    CookieManager.getInstance().setCookie("https://example.co.kr", cookieString);

                }
            }

        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    private void noNetwork() {
        AlertDialog.Builder alert_confirm = new AlertDialog.Builder(MainActivity3.this);
        alert_confirm.setMessage("인터넷 연결 확인 후 다시 시도해주세요.").setCancelable(false).setPositiveButton("재접속",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        networkCheck();
                    }
                }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        AlertDialog alert = alert_confirm.create();
        alert.show();
    }

    public void networkCheck() {
        mMobile = mManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        mWifi = mManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWifi.isConnected() || mMobile.isConnected()) {  //인터넷 연결 됐을때
            permissionCheck();

        } else {
            //인터넷 연결 안됐을때
            noNetwork();
        }
    }

    /**
     * 퍼미션 체크
     */
    public void permissionCheck() {

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //마시멜로우 이상인지 체크
//
//            int[] permissionChecks = new int[5];
//
//            permissionChecks[0] = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
//            permissionChecks[1] = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//            permissionChecks[2] = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
//            permissionChecks[3] = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
//            permissionChecks[4] = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
//
//
//            if (permissionChecks[0] == PackageManager.PERMISSION_DENIED || permissionChecks[1] == PackageManager.PERMISSION_DENIED || permissionChecks[2] == PackageManager.PERMISSION_DENIED
//                    || permissionChecks[3] == PackageManager.PERMISSION_DENIED || permissionChecks[4] == PackageManager.PERMISSION_DENIED) {  //하나라도 허락안된거 있으면
//                setPermissionCheck();
//            } else {
//                splashThread();
//            }
//        } else {
//            마시멜로우 미만
//            퍼미션 체크 x
        splashThread();
//        }
    }

    public void setPermissionCheck() {
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {  // 퍼미션 체크 모두 승인 받으면
                splashThread();
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
                Toast.makeText(MainActivity3.this, "해당 권한을 거부하면 이 서비스를 이용할 수 없습니다.", Toast.LENGTH_LONG).show();
                finish();
            }
        };

        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
//                .setRationaleMessage("구글 로그인을 하기 위해서는 주소록 접근 권한이 필요해요")
                .setDeniedMessage("해당 권한을 거부하면 이 서비스를 이용할 수 없습니다.\n- 권한 승인 변경 방법\n[설정] > [애플리케이션] > [담너머] \n> [권한] > 모두 허용")
                .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA
                        , Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                .check();
    }

    public void splashThread() {
        Log.d(TAG, "splashThread: ");

        setWebview();

        binding.webView.loadUrl(getString(R.string.main_url));
        mHandler = new Handler();

        final Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {

//                try {
//                    Thread.sleep(1000);
                    loadingConfirm = true;
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

            }
        });
        thread.start();
    }
}
