package com.kit.cordova.AMapLocation;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * 高德地图 api http://lbs.amap.com/api/android-location-sdk/guide/android-location/getlocation
 */
public class LocationPlugin extends CordovaPlugin {

    protected final static String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    public static final int ACCESS_LOCATION = 1;

    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = null;
    private Context context;
    private CallbackContext callbackContext = null;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        context = this.cordova.getActivity().getApplicationContext();
        locationClient = new AMapLocationClient(context);
        locationClient.setLocationListener(mLocationListener);
        super.initialize(cordova, webView);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if ("getlocation".equals(action.toLowerCase(Locale.CHINA))) {
            if (context.getApplicationInfo().targetSdkVersion < 23) {
                this.getLocation();
            } else {
                boolean access_fine_location = PermissionHelper.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
                boolean access_coarse_location = PermissionHelper.hasPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                if (access_fine_location && access_coarse_location) {
                    this.getLocation();
                } else {
                    PermissionHelper.requestPermissions(this, ACCESS_LOCATION, permissions);
                }
            }
            return true;
        }
        return false;
    }

    private void getLocation() {
        locationOption = new AMapLocationClientOption();
        locationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.SignIn);// 使用签到定位场景
        locationClient.setLocationOption(locationOption); // 设置定位参数
        // 设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
        locationClient.stopLocation();
        locationClient.startLocation(); // 启动定位
        PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
        r.setKeepCallback(true);
        callbackContext.sendPluginResult(r);
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        switch (requestCode) {
            case ACCESS_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.getLocation();
                } else {
                    Toast.makeText(this.cordova.getActivity(), "请开启应用定位权限", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private AMapLocationListener mLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if (amapLocation != null) {
                if (amapLocation.getErrorCode() == 0) {
                    int locationType = amapLocation.getLocationType();//获取当前定位结果来源 定位类型对照表: http://lbs.amap.com/api/android-location-sdk/guide/utilities/location-type/
                    Double latitude = amapLocation.getLatitude();//获取纬度
                    Double longitude = amapLocation.getLongitude();//获取经度
                    float accuracy = amapLocation.getAccuracy();//获取精度信息
                    String address = amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                    String country = amapLocation.getCountry();//国家信息
                    String province = amapLocation.getProvince();//省信息
                    String city = amapLocation.getCity();//城市信息
                    String district = amapLocation.getDistrict();//城区信息
                    String street = amapLocation.getStreet();//街道信息
                    String streetNum = amapLocation.getStreetNum();//街道门牌号信息
                    String cityCode = amapLocation.getCityCode();//城市编码
                    String adCode = amapLocation.getAdCode();//地区编码
                    String aoiName = amapLocation.getAoiName();//获取当前定位点的AOI信息
                    String floor = amapLocation.getFloor();//获取当前室内定位的楼层
                    int gpsAccuracyStatus = amapLocation.getGpsAccuracyStatus();//获取GPS的当前状态
                    long time = amapLocation.getTime();  // 时间
                    JSONObject jo = new JSONObject();
                    try {
                        jo.put("locationType", locationType);
                        jo.put("latitude", latitude);
                        jo.put("longitude", longitude);
                        jo.put("accuracy", accuracy);
                        jo.put("address", address);
                        jo.put("country", country);
                        jo.put("province", province);
                        jo.put("city", city);
                        jo.put("district", district);
                        jo.put("street", street);
                        jo.put("streetNum", streetNum);
                        jo.put("cityCode", cityCode);
                        jo.put("adCode", adCode);
                        jo.put("aoiName", aoiName);
                        jo.put("floor", floor);
                        jo.put("gpsAccuracyStatus", gpsAccuracyStatus);
                        jo.put("time", time);
                    } catch (JSONException e) {
                        jo = null;
                        e.printStackTrace();
                    }
                    callbackContext.success(jo);
                } else {
                    //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                    Log.e("AmapError", "location Error, ErrCode:"
                            + amapLocation.getErrorCode() + ", errInfo:"
                            + amapLocation.getErrorInfo());
                    callbackContext.error(amapLocation.getErrorInfo());
                }
            }
        }
    };

}
