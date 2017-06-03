#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import <CoreLocation/CoreLocation.h>
#import <Cordova/CDV.h>

static NSString* const LATITUDE_KEY = @"latitude";
static NSString* const LONGITUDE_KEY = @"longitude";

/**
 *IOS版本的定位采用CLLocationManager类进行定位
 */
 
@interface LocationPlugin : CDVPlugin <CLLocationManagerDelegate>{
    // Member variables go here.
    CLLocationManager* locationManager;
    CLLocationManager* curLocationManager;
    BOOL isStart;
    NSString* callbackId;
    int maxLength;
    int interval;
}

- (void)getlocation:(CDVInvokedUrlCommand*)command;

- (CLLocationCoordinate2D)transformFromWGSToGCJ:(CLLocationCoordinate2D) wgLoc;
- (CLLocationCoordinate2D)bd_encrypt:(CLLocationCoordinate2D)gcLoc;
- (CLLocationCoordinate2D)bd_decrypt:(CLLocationCoordinate2D) bdLoc;

@end

@implementation LocationPlugin

- (void)getlocation:(CDVInvokedUrlCommand*)command{
    callbackId = command.callbackId;
    if(curLocationManager == nil){
        curLocationManager = [[CLLocationManager alloc] init];
        curLocationManager.distanceFilter = kCLDistanceFilterNone;
        curLocationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters;//精度最佳
        curLocationManager.delegate = self;
        if([CLLocationManager authorizationStatus] == kCLAuthorizationStatusNotDetermined && [[[UIDevice currentDevice] systemVersion ] floatValue] >= 8.0){
            [curLocationManager requestAlwaysAuthorization];
        }
    }
    
    if(![CLLocationManager locationServicesEnabled]){
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"定位服务未打开"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    

    
    [curLocationManager startUpdatingLocation];
}

/**
 *定位成功，回调此方法
 */
- (void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray *)locations{
    CLLocation* location = (CLLocation*)[locations lastObject];
    CLLocationCoordinate2D gcjLocation = [self transformFromWGSToGCJ: location.coordinate];
    if(manager == curLocationManager){
        if(callbackId != nil){
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:[NSDictionary dictionaryWithObjectsAndKeys: [NSNumber numberWithDouble: gcjLocation.longitude], LONGITUDE_KEY, [NSNumber numberWithDouble: gcjLocation.latitude], LATITUDE_KEY, nil]];

            [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
            callbackId = nil;
        }
        [manager stopUpdatingLocation];
    }
}

/**
 *定位失败，回调此方法
 */
-(void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error{
	CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"定位失败"];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
    return;
}

- (void)initLocationManager{
    locationManager = [[CLLocationManager alloc] init];
    locationManager.delegate = self;
    locationManager.distanceFilter = kCLLocationAccuracyHundredMeters; //更新距离
    locationManager.desiredAccuracy = kCLLocationAccuracyBest;//精度最佳
    if([[[UIDevice currentDevice] systemVersion ] floatValue] >= 8.0){
        [locationManager requestAlwaysAuthorization];
    }
}


const double pi = 3.14159265358979324;
const double a = 6378245.0;
const double ee = 0.00669342162296594323;

bool outOfChina(double lat, double lon)
{
    if (lon < 72.004 || lon > 137.8347)
        return true;
    if (lat < 0.8293 || lat > 55.8271)
        return true;
    return false;
}

double transformLat(double x, double y)
{
    double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(fabs(x));
    ret += (20.0 * sin(6.0 * x * pi) + 20.0 *sin(2.0 * x * pi)) * 2.0 / 3.0;
    ret += (20.0 * sin(y * pi) + 40.0 * sin(y / 3.0 * pi)) * 2.0 / 3.0;
    ret += (160.0 * sin(y / 12.0 * pi) + 320 * sin(y * pi / 30.0)) * 2.0 / 3.0;
    return ret;
}

double transformLon(double x, double y)
{
    double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(fabs(x));
    ret += (20.0 * sin(6.0 * x * pi) + 20.0 * sin(2.0 * x * pi)) * 2.0 / 3.0;
    ret += (20.0 * sin(x * pi) + 40.0 * sin(x / 3.0 * pi)) * 2.0 / 3.0;
    ret += (150.0 * sin(x / 12.0 * pi) + 300.0 * sin(x / 30.0 * pi)) * 2.0 / 3.0;
    return ret;
}

///
///  WGS-84 到 GCJ-02 的转换
///
- (CLLocationCoordinate2D) transformFromWGSToGCJ:(CLLocationCoordinate2D) wgLoc
{
    CLLocationCoordinate2D mgLoc;
    if (outOfChina(wgLoc.latitude, wgLoc.longitude))
    {
        mgLoc = wgLoc;
        return mgLoc;
    }
    double dLat = transformLat(wgLoc.longitude - 105.0, wgLoc.latitude - 35.0);
    double dLon = transformLon(wgLoc.longitude - 105.0, wgLoc.latitude - 35.0);
    double radLat = wgLoc.latitude / 180.0 * pi;
    double magic = sin(radLat);
    magic = 1 - ee * magic * magic;
    double sqrtMagic = sqrt(magic);
    dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
    dLon = (dLon * 180.0) / (a / sqrtMagic * cos(radLat) * pi);
    mgLoc = CLLocationCoordinate2DMake(wgLoc.latitude + dLat, wgLoc.longitude + dLon);
    
    return mgLoc;
}

///
///  GCJ-02 坐标转换成 BD-09 坐标
///

const double x_pi = 3.14159265358979324 * 3000.0 / 180.0;
- (CLLocationCoordinate2D)bd_encrypt:(CLLocationCoordinate2D)gcLoc
{
    double x = gcLoc.longitude, y = gcLoc.latitude;
    double z = sqrt(x * x + y * y) + 0.00002 * sin(y * x_pi);
    double theta = atan2(y, x) + 0.000003 * cos(x * x_pi);
    return CLLocationCoordinate2DMake(z * cos(theta) + 0.0065, z * sin(theta) + 0.006);
}

///
///   BD-09 坐标转换成 GCJ-02坐标
///
///
- (CLLocationCoordinate2D)bd_decrypt:(CLLocationCoordinate2D) bdLoc
{
    double x = bdLoc.longitude - 0.0065, y = bdLoc.latitude - 0.006;
    double z = sqrt(x * x + y * y) - 0.00002 * sin(y * x_pi);
    double theta = atan2(y, x) - 0.000003 * cos(x * x_pi);
    return CLLocationCoordinate2DMake(z * cos(theta), z * sin(theta));

}


@end