# react-native-directory-picker
A React Native module that allows you to use native UI to select a directory from the device library

###  :warning: Using this component is not recommended. This is a workaround.

## Install

### Android

```bash
$ npm install git+https://github.com/lokdevp/react-native-directory-picker.git
$ react-native link
```

```xml
<!-- file: android/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.myApp">
    <!-- add following permissions -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <!-- -->
    ...
```

## Usage
1. In your React Native javascript code, bring in the native module:

  ```javascript
import DirectoryPickerManager from 'react-native-directory-picker';
  ```
2. Use it like so:

  When you want to display the picker:
  ```javascript

  DirectoryPickerManager.showDirectoryPicker(null, (response) => {
    console.log('Response = ', response);

    if (response.didCancel) {
      console.log('User cancelled directory picker');
    }
    else if (response.error) {
      console.log('DirectoryPickerManager Error: ', response.error);
    }
    else {
      this.setState({
        directory: response
      });
    }
  });
  ```

