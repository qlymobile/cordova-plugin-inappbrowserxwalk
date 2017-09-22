package com.example.plugin.InAppBrowserXwalk;

import com.example.plugin.InAppBrowserXwalk.BrowserDialog;

import android.content.res.Resources;

import org.apache.cordova.*;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import org.json.JSONStringer;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.internal.XWalkViewInternal;
import org.xwalk.core.XWalkCookieManager;

import android.util.JsonReader;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.graphics.Typeface;
import android.widget.Toast;

import android.webkit.WebResourceResponse;
import android.webkit.ValueCallback;

import android.util.Log;

public class InAppBrowserXwalk extends CordovaPlugin {

    private BrowserDialog dialog;
    private XWalkView xWalkWebView;
    private CallbackContext callbackContext;

    public static final String LOG_TAG = "InAppBrowserXwalk";

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        Log.d(LOG_TAG, "Action " + action);

        if (action.equals("open")) {
            this.callbackContext = callbackContext;
            this.openBrowser(data);
        }

        if (action.equals("close")) {
            this.closeBrowser();
        }

        if (action.equals("show")) {
            this.showBrowser();
        }

        if (action.equals("hide")) {
            this.hideBrowser();
        }

        if (action.equals("injectScriptCode")) {
            this.injectJS(data.getString(0), callbackContext);
        }

        return true;
    }

    class MyResourceClient extends XWalkResourceClient {
        MyResourceClient(XWalkView view) {
            super(view);
        }

        @Override
        public void onLoadStarted(XWalkView view, String url) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", "loadstart");
                obj.put("url", url);
                PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);
            } catch (JSONException ex) {
            }
        }

        @Override
        public void onLoadFinished(XWalkView view, String url) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", "loadstop");
                obj.put("url", url);
                PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);
            } catch (JSONException ex) {
            }
        }
    }

    private void openBrowser(final JSONArray data) throws JSONException {
        final String url = data.getString(0);
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog = new BrowserDialog(cordova.getActivity(), android.R.style.Theme_NoTitleBar);
                xWalkWebView = new XWalkView(cordova.getActivity(), cordova.getActivity());
                XWalkCookieManager mCookieManager = new XWalkCookieManager();
                mCookieManager.setAcceptCookie(true);
                mCookieManager.setAcceptFileSchemeCookies(true);
                xWalkWebView.setResourceClient(new MyResourceClient(xWalkWebView));
                xWalkWebView.load(url, "");

                String toolbarColor = "#FFFFFF";
                int toolbarHeight = 80;
                String closeButtonText = "< Close";
                int closeButtonSize = 25;
                String closeButtonColor = "#000000";
                boolean openHidden = false;

                if (data != null && data.length() > 1) {
                    try {
                        JSONObject options = new JSONObject(data.getString(1));

                        if (!options.isNull("toolbarColor")) {
                            toolbarColor = options.getString("toolbarColor");
                        }
                        if (!options.isNull("toolbarHeight")) {
                            toolbarHeight = options.getInt("toolbarHeight");
                        }
                        if (!options.isNull("closeButtonText")) {
                            closeButtonText = options.getString("closeButtonText");
                        }
                        if (!options.isNull("closeButtonSize")) {
                            closeButtonSize = options.getInt("closeButtonSize");
                        }
                        if (!options.isNull("closeButtonColor")) {
                            closeButtonColor = options.getString("closeButtonColor");
                        }
                        if (!options.isNull("openHidden")) {
                            openHidden = options.getBoolean("openHidden");
                        }
                    } catch (JSONException ex) {

                    }
                }

                LinearLayout main = new LinearLayout(cordova.getActivity());
                main.setOrientation(LinearLayout.VERTICAL);

                RelativeLayout toolbar = new RelativeLayout(cordova.getActivity());
                toolbar.setBackgroundColor(android.graphics.Color.parseColor(toolbarColor));
                toolbar.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, toolbarHeight));
                toolbar.setPadding(5, 5, 5, 5);

                TextView closeButton = new TextView(cordova.getActivity());
                closeButton.setText(closeButtonText);
                closeButton.setTextSize(closeButtonSize);
                closeButton.setTextColor(android.graphics.Color.parseColor(closeButtonColor));
                closeButton.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
                toolbar.addView(closeButton);

                closeButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        closeBrowser();
                    }
                });

                //main.addView(toolbar);
                main.addView(xWalkWebView);

                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
                dialog.setCancelable(true);
                LayoutParams layoutParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
                dialog.addContentView(main, layoutParams);
                if (!openHidden) {
                    dialog.show();
                }
            }
        });
    }

    public void hideBrowser() {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialog != null) {
                    dialog.hide();
                }
            }
        });
    }

    public void showBrowser() {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialog != null) {
                    dialog.show();
                }
            }
        });
    }

    public void closeBrowser() {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                xWalkWebView.onDestroy();
                dialog.dismiss();
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("type", "exit");
                    PluginResult result = new PluginResult(PluginResult.Status.OK, obj);
                    result.setKeepCallback(true);
                    callbackContext.sendPluginResult(result);
                } catch (JSONException ex) {
                }
            }
        });
    }

    public void injectJS(final String source, CallbackContext callbackContext) {
        final CallbackContext cbContext = callbackContext;
        String jsWrapper = "(function(){return [eval(%s)];})()";
        JSONArray jsonEsc = new org.json.JSONArray();
        jsonEsc.put(source);
        String jsonRepr = jsonEsc.toString();
        String jsonSourceString = jsonRepr.substring(1, jsonRepr.length() - 1);
        final String finalScriptToInject = String.format(jsWrapper, jsonSourceString);

        Log.d(LOG_TAG, "Inject JS: " + finalScriptToInject);
        this.cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                xWalkWebView.evaluateJavascript(finalScriptToInject, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String scriptResult) {
                        Log.d(LOG_TAG, "Inject JS result: " + scriptResult);
                        PluginResult result;
                        try {
                            JSONArray jsonArray = new JSONArray(scriptResult);
                            result = new PluginResult(PluginResult.Status.OK, jsonArray);
                        } catch (JSONException e) {
                            result = new PluginResult(PluginResult.Status.OK, scriptResult);
                        }
                        result.setKeepCallback(true);
                        cbContext.sendPluginResult(result);
                    }
                });
            }
        });
    }
}
