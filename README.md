# com.kit.cordova.amaplocation
使用高德Android定位SDK进行定位，以解决webapp中定位不准的问题

## 安装
`cordova plugin add https://github.com/namedjw/com.kit.cordova.amaplocation.git --variable KEY=您申请的高德地图androidkey --save`
ps:_此插件android定位功能使用高德定位,ios定位功能使用苹果系统自带的定位功能,所以只需要配置android key,不需要配置ios key_

## 配置

### ionic3调用方法

```
import {Injectable} from '@angular/core';
declare var LocationPlugin;

@Injectable()
export class NativeService {
  constructor() { }
  /**
   * 获得用户当前坐标/坐标系为火星坐标系
   * @return {Promise<any>}
   */
  getUserLocation(): Promise<any> {
    return new Promise((resolve) => {
        LocationPlugin.getLocation(data => {
          resolve({'lng': data.longitude, 'lat': data.latitude});
        }, msg => {
          alert(msg.indexOf('缺少定位权限') == -1 ? ('错误消息：' + msg) : '缺少定位权限，请在手机设置中开启');
        });
    });
  }
}
```
