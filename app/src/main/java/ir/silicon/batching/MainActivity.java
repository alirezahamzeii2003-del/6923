package ir.silicon.batching;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.webkit.*;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class MainActivity extends Activity {
    private WebView web;
    private static final int NOTIFICATION_REQ = 7001;
    private static final int CREATE_FILE_REQ = 7101;
    private static final int OPEN_FILE_REQ = 7102;
    private static final String PREFS = "silicon_native";
    private static final String PENDING = "draft_pending";
    private byte[] pendingFileData;
    private String pendingFileName = "report";
    private String pendingMime = "application/octet-stream";
    private String pendingIncomingJson;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        pendingIncomingJson = readJsonFromIntent(getIntent());
        hideStatusBar();
        NativeNotifications.ensureChannel(this);
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQ);

        web = new WebView(this);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setBuiltInZoomControls(false);
        ws.setSupportZoom(false);
        web.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new AndroidBridge(this), "AndroidBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                hideStatusBar();
                if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(PENDING, false))
                    NativeNotifications.showDraft(thisActivity());
                if (pendingIncomingJson != null) {
                    String text = pendingIncomingJson;
                    pendingIncomingJson = null;
                    runJs("window.receiveEditableFileText(" + JSONObject.quote(text) + ")");
                }
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    private Activity thisActivity(){ return this; }

    private void hideStatusBar() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
            ViewFlags.FULLSCREEN | ViewFlags.IMMERSIVE_STICKY | ViewFlags.LAYOUT_STABLE
        );
    }

    // Kept local to avoid depending on API-specific constants at call sites.
    private static final class ViewFlags {
        static final int FULLSCREEN = 0x00000004;
        static final int LAYOUT_STABLE = 0x00000100;
        static final int IMMERSIVE_STICKY = 0x00001000;
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) new Handler(Looper.getMainLooper()).postDelayed(this::hideStatusBar, 3000);
    }

    @Override public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String text = readJsonFromIntent(intent);
        if (text != null) {
            if (web != null && web.getUrl() != null) runJs("window.receiveEditableFileText(" + JSONObject.quote(text) + ")");
            else pendingIncomingJson = text;
        }
    }

    private String readJsonFromIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return null;
        Uri uri = intent.getData();
        if (uri == null) return null;
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) return null;
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (Throwable e) { return null; }
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    private void createFile(byte[] data, String name, String mime) {
        pendingFileData = data;
        pendingFileName = sanitize(name);
        pendingMime = (mime == null || mime.isEmpty()) ? "application/octet-stream" : mime;
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType(pendingMime);
        i.putExtra(Intent.EXTRA_TITLE, pendingFileName);
        startActivityForResult(i, CREATE_FILE_REQ);
    }

    private void openEditableFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/plain", "*/*"});
        startActivityForResult(i, OPEN_FILE_REQ);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == CREATE_FILE_REQ) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new IOException("Cannot open destination");
                out.write(pendingFileData);
                out.flush();
                runJs("toast('فایل با موفقیت ذخیره شد')");
            } catch (Throwable e) {
                runJs("toast('ذخیره فایل انجام نشد',false)");
            } finally {
                pendingFileData = null;
            }
        } else if (requestCode == OPEN_FILE_REQ) {
            try (InputStream in = getContentResolver().openInputStream(uri);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                if (in == null) throw new IOException("Cannot open source");
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
                String text = out.toString(StandardCharsets.UTF_8.name());
                runJs("window.receiveEditableFileText(" + JSONObject.quote(text) + ")");
            } catch (Throwable e) {
                runJs("toast('خواندن فایل انجام نشد',false)");
            }
        }
    }

    private void runJs(String script) {
        if (web != null) web.post(() -> web.evaluateJavascript(script, null));
    }

    public static class AndroidBridge {
        private final MainActivity activity;
        private final Context context;
        AndroidBridge(MainActivity c){ activity=c; context=c.getApplicationContext(); }

        @JavascriptInterface public void setDraftPending(boolean pending) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(PENDING, pending).apply();
        }

        @JavascriptInterface public void notifyIfDraftPending() {
            if (context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PENDING, false))
                NativeNotifications.showDraft(context);
        }

        @JavascriptInterface public void saveFile(String base64, String fileName, String mime) {
            try {
                byte[] data = decode(base64);
                activity.runOnUiThread(() -> activity.createFile(data, fileName, mime));
            } catch (Throwable e) {
                activity.runJs("toast('ساخت فایل برای ذخیره انجام نشد',false)");
            }
        }

        @JavascriptInterface public void exitApp() {
            activity.runOnUiThread(() -> {
                try { activity.finishAndRemoveTask(); } catch (Throwable e) { activity.finish(); }
            });
        }

        @JavascriptInterface public void pickEditableFile() {
            activity.runOnUiThread(activity::openEditableFile);
        }

        @JavascriptInterface public void shareFile(String base64, String fileName, String mime, String text) {
            try {
                byte[] data = decode(base64);
                File dir = new File(context.getCacheDir(), "shared");
                if (!dir.exists()) dir.mkdirs();
                File out = new File(dir, sanitize(fileName));
                try (FileOutputStream fos = new FileOutputStream(out)) { fos.write(data); fos.flush(); }
                Uri uri = FileProvider.getUriForFile(context, context.getPackageName()+".fileprovider", out);
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType(mime == null || mime.isEmpty() ? "application/octet-stream" : mime);
                send.putExtra(Intent.EXTRA_STREAM, uri);
                if (text != null && !text.isEmpty()) send.putExtra(Intent.EXTRA_TEXT, text);
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent chooser = Intent.createChooser(send, "اشتراک‌گذاری");
                activity.startActivity(chooser);
            } catch (Throwable e) {
                activity.runJs("toast('اشتراک‌گذاری فایل انجام نشد',false)");
            }
        }

        private static byte[] decode(String base64) {
            if (Build.VERSION.SDK_INT >= 26) return Base64.getDecoder().decode(base64);
            return android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
        }

        private static String sanitize(String n) {
            String x = n == null ? "report" : n.replaceAll("[\\/:*?\"<>]", "_");
            return x.length() > 100 ? x.substring(0,100) : x;
        }
    }

    private static String sanitize(String n) {
        String x = n == null ? "report" : n.replaceAll("[\\/:*?\"<>]", "_");
        return x.length() > 100 ? x.substring(0,100) : x;
    }
}
