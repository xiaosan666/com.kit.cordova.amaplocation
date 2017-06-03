var cordova = require('cordova');

function LocationPlugin(){}

LocationPlugin.prototype.getLocation = function(successCallback,errorCallback) {
    cordova.exec(successCallback,errorCallback,"LocationPlugin","getlocation",[]);
};

module.exports = new LocationPlugin();
