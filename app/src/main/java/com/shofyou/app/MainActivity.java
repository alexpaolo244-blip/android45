package com.shofyou.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private String cameraPath;

    private static final String HOME_URL = "https://shofyou.com";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        hideSystemUI();

        webView = findViewById(R.id.webview);

        WebSettings ws = webView.getSettings();

        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new MyWebViewClient());

        webView.setWebChromeClient(new MyChrome());

        if(savedInstanceState != null)
            webView.restoreState(savedInstanceState);
        else
            webView.loadUrl(HOME_URL);

        handleBack();
    }

    private void hideSystemUI() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {

            getWindow().setDecorFitsSystemWindows(false);

            WindowInsetsController controller =
                    getWindow().getInsetsController();

            if(controller != null) {

                controller.hide(WindowInsets.Type.statusBars()
                        | WindowInsets.Type.navigationBars());

                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }

        }
    }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view,
                                                WebResourceRequest request) {

            String url = request.getUrl().toString();

            if(url.contains("shofyou.com"))
                return false;

            Intent intent =
                    new Intent(MainActivity.this,
                            PopupActivity.class);

            intent.putExtra("url", url);

            startActivity(intent);

            return true;
        }
    }

    private class MyChrome extends WebChromeClient {

        @Override
        public boolean onShowFileChooser(WebView webView,
                                         ValueCallback<Uri[]> callback,
                                         FileChooserParams params) {

            fileCallback = callback;

            Intent cameraIntent =
                    new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            File photoFile = null;

            try {

                photoFile = File.createTempFile(
                        "camera",
                        ".jpg",
                        getExternalFilesDir(null));

                cameraPath = photoFile.getAbsolutePath();

                Uri uri = FileProvider.getUriForFile(
                        MainActivity.this,
                        getPackageName() + ".provider",
                        photoFile);

                cameraIntent.putExtra(
                        MediaStore.EXTRA_OUTPUT, uri);

            } catch (IOException e) {}

            Intent fileIntent =
                    new Intent(Intent.ACTION_GET_CONTENT);

            fileIntent.setType("*/*");

            fileIntent.putExtra(
                    Intent.EXTRA_ALLOW_MULTIPLE, true);

            Intent chooser =
                    new Intent(Intent.ACTION_CHOOSER);

            chooser.putExtra(
                    Intent.EXTRA_INTENT, fileIntent);

            chooser.putExtra(
                    Intent.EXTRA_INITIAL_INTENTS,
                    new Intent[]{cameraIntent});

            startActivityForResult(chooser, 100);

            return true;
        }

        @Override
        public boolean onCreateWindow(WebView view,
                                      boolean dialog,
                                      boolean userGesture,
                                      Message resultMsg) {

            WebView.HitTestResult result =
                    view.getHitTestResult();

            String url = result.getExtra();

            Intent intent =
                    new Intent(MainActivity.this,
                            PopupActivity.class);

            intent.putExtra("url", url);

            startActivity(intent);

            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {

        if(fileCallback == null)
            return;

        Uri[] results = null;

        if(resultCode == RESULT_OK) {

            if(data == null) {

                File file =
                        new File(cameraPath);

                results =
                        new Uri[]{Uri.fromFile(file)};
            }

            else {

                results =
                        new Uri[]{data.getData()};
            }
        }

        fileCallback.onReceiveValue(results);

        fileCallback = null;
    }

    private void handleBack() {

        getOnBackPressedDispatcher()
                .addCallback(this,
                        new OnBackPressedCallback(true) {

            @Override
            public void handleOnBackPressed() {

                if(webView.canGoBack())
                    webView.goBack();

                else {

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Exit")
                            .setMessage("Do you want exit?")
                            .setPositiveButton("Yes",
                                    (d,i)->finish())
                            .setNegativeButton("No", null)
                            .show();
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        webView.saveState(outState);

        super.onSaveInstanceState(outState);
    }

}
