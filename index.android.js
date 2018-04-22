'use strict'

const { NativeModules } = require('react-native');
const { DirectoryPickerManager } = NativeModules;

module.exports = {
  ...DirectoryPickerManager,
  showDirectoryPicker: function showDirectoryPicker(options, callback) {
    if (typeof options === 'function') {
      callback = options;
      options = {};
    }
    return DirectoryPickerManager.showDirectoryPicker(options, callback)
  }
}
