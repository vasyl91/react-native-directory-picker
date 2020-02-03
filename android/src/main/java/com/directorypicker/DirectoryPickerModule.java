package com.directorypicker;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import javax.annotation.Nonnull;

public class DirectoryPickerModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private final ReactApplicationContext mReactContext;

    private Callback mCallback;
    private WritableMap response;

    public DirectoryPickerModule(ReactApplicationContext reactContext) {
        super(reactContext);

        reactContext.addActivityEventListener(this);

        mReactContext = reactContext;
    }

    private boolean permissionsCheck(Activity activity) {
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (readPermission != PackageManager.PERMISSION_GRANTED) {
            String[] PERMISSIONS = {
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
            ActivityCompat.requestPermissions(activity, PERMISSIONS, 1);
            return false;
        }
        return true;
    }

    @Nonnull
    @Override
    public String getName() {
        return "DirectoryPickerManager";
    }

    //Directory Chooser
    private static final int REQUEST_LAUNCH_DIRECTORY_CHOOSER = 42;

    @ReactMethod
    public void showDirectoryPicker(final ReadableMap options, final Callback callback) {
        Activity currentActivity = getCurrentActivity();
        response = Arguments.createMap();

        if (!permissionsCheck(currentActivity)) {
            response.putBoolean("didRequestPermission", true);
            response.putString("option", "launchFileChooser");
            callback.invoke(response);
            return;
        }

        if (currentActivity == null) {
            response.putString("error", "can't find current Activity");
            callback.invoke(response);
            return;
        }

        launchDirectoryChooser(callback);
    }

    @ReactMethod
    public void launchDirectoryChooser(final Callback callback) {
        int requestCode;
        Intent libraryIntent = null;
        response = Arguments.createMap();
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            response.putString("error", "can't find current Activity");
            callback.invoke(response);
            return;
        }

        requestCode = REQUEST_LAUNCH_DIRECTORY_CHOOSER;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            libraryIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        }
        if(libraryIntent == null) return;
        libraryIntent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        libraryIntent.putExtra("android.content.extra.SHOW_FILESIZE", true);

        if (libraryIntent.resolveActivity(mReactContext.getPackageManager()) == null) {
            response.putString("error", "Cannot launch file library");
            callback.invoke(response);
            return;
        }

        mCallback = callback;

        try {
            currentActivity.startActivityForResult(libraryIntent, requestCode);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    // R.N > 33
    public void onActivityResult(final Activity activity, final int requestCode, final int resultCode, final Intent data) {
        onActivityResult(requestCode, resultCode, data);
    }

    private void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        //robustness code
        if (mCallback == null || requestCode != REQUEST_LAUNCH_DIRECTORY_CHOOSER) {
            return;
        }
        // user cancel
        if (resultCode != Activity.RESULT_OK) {
            response.putBoolean("didCancel", true);
            mCallback.invoke(response);
            return;
        }

        //Handle Directory
        Uri treeUri = data.getData();
        if(treeUri == null) return;
        DocumentFile pickedDir = DocumentFile.fromTreeUri(mReactContext, treeUri);
        response.putString("path", getPath(mReactContext, treeUri));
        if(pickedDir == null) return;
        response.putString("dirname", pickedDir.getName());
        response.putString("decodedUri", Uri.decode(treeUri.toString()));
        mCallback.invoke(response);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if (isKitKat) {
            try {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = uri.getPath();
                    if (docId == null) return null;
                    final String[] split = docId.split(":");
                    if (split.length <= 0) return null;
                    final String type = split[0];

                    if ("/tree/primary".equalsIgnoreCase(type)) {
					  String path = Environment.getExternalStorageDirectory().getPath();
					  if (split.length > 1) {
						path += "/" + split[1];
					  }
					  return path;
                    }
                }
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    // Required for RN 0.30+ modules than implement ActivityEventListener
    public void onNewIntent(Intent intent) {
    }

}
