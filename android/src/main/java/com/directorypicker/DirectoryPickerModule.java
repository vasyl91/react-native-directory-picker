package com.directorypicker;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import android.support.v4.provider.DocumentFile;
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
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.ArrayAdapter;

public class DirectoryPickerModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private final ReactApplicationContext mReactContext;

    private Callback mCallback;
    WritableMap response;

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

    @Override
    public String getName() {
        return "DirectoryPickerManager";
    }    

    //Directory Chooser
    static final int REQUEST_LAUNCH_DIRECTORY_CHOOSER = 42;
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
        Intent libraryIntent;
        response = Arguments.createMap();
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            response.putString("error", "can't find current Activity");
            callback.invoke(response);
            return;
        }

        requestCode = REQUEST_LAUNCH_DIRECTORY_CHOOSER;
        libraryIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
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

    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

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
      if (requestCode == REQUEST_LAUNCH_DIRECTORY_CHOOSER) {
        Uri treeUri = data.getData();
        DocumentFile pickedDir = DocumentFile.fromTreeUri(mReactContext, treeUri);
        response.putString("path", getPath(mReactContext, treeUri)); 
        response.putString("dirname", pickedDir.getName());
        response.putString("decodedUri", Uri.decode(treeUri.toString()));
        mCallback.invoke(response);
        return;
      }     
    }       

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if (isKitKat) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = uri.getPath();
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("/tree/primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }                
            }           
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }    

    // Required for RN 0.30+ modules than implement ActivityEventListener
    public void onNewIntent(Intent intent) { }

}
