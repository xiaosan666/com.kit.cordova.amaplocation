package com.kit.cordova.AMapLocation;

import java.util.Locale;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;

public class LocationPlugin extends CordovaPlugin {

	private static final String ACTION_GETLOCATION = "getlocation";

	private AMapLocationClient locationClient = null;
	private AMapLocationClientOption locationOption = null;

	private CallbackContext callbackContext = null;
	private KITLocation kitLocation = new KITLocation();
	private Context context;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		context = this.cordova.getActivity().getApplicationContext();
		super.initialize(cordova, webView);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {
		this.callbackContext = callbackContext;
		if (ACTION_GETLOCATION.equals(action.toLowerCase(Locale.CHINA))) {
			kitLocation.startSingleLocation(context);
			PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
			r.setKeepCallback(true);
			callbackContext.sendPluginResult(r);
			return true;
		}
		return false;
	}

	public class KITLocation implements AMapLocationListener {
		@Override
		public void onLocationChanged(AMapLocation amapLocation) {

			if (amapLocation != null && amapLocation.getErrorCode() == 0) {
				// 获取位置信息
				Double latitude = amapLocation.getLatitude();
				Double longitude = amapLocation.getLongitude();
				boolean hasAccuracy = amapLocation.hasAccuracy();
				float accuracy = amapLocation.getAccuracy();
				String address = amapLocation.getAddress();
				String province = amapLocation.getProvince();
				String road = amapLocation.getRoad();
				// 速度
				float speed = amapLocation.getSpeed();
				// 角度
				float bearing = amapLocation.getBearing();
				// 星数
				int satellites = amapLocation.getExtras().getInt("satellites",0);
				// 时间
				long time = amapLocation.getTime();

				JSONObject jo = new JSONObject();
				try {
					jo.put("latitude", latitude);
					jo.put("longitude", longitude);
					jo.put("hasAccuracy", hasAccuracy);
					jo.put("accuracy", accuracy);
					jo.put("address", address);
					jo.put("province", province);
					jo.put("road", road);
					jo.put("speed", speed);
					jo.put("bearing", bearing);
					jo.put("satellites", satellites);
					jo.put("time", time);

				} catch (JSONException e) {
					jo = null;
					e.printStackTrace();
				}
				callbackContext.success(jo);
			} else {
				callbackContext.error(amapLocation.getErrorInfo());
			}
		}

		public void startSingleLocation(Context context) {
			locationClient = new AMapLocationClient(context);
			locationOption = new AMapLocationClientOption();			
           /*			
                                           低功耗   Battery_Saving
			高精度   Hight_Accuracy
			GPS    Device_Sensors
			*/
			// 设置定位模式为高精度模式
			locationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
			// 设置定位监听
			locationClient.setLocationListener(this);
			// 设置为单次定位
			locationOption.setOnceLocation(true);
			// 设置定位参数
			locationClient.setLocationOption(locationOption);
			// 启动定位
			locationClient.startLocation();

		}
	}

}
