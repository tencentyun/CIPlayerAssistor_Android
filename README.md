### 简介
CIPlayerAssistor 为数据万象提供给客户希望使用第三方播放器或自研播放器开发播放万象自研私有加密m3u8资源文件，常用于有自定义播放器功能需求的用户。

### 相关资源
CIPlayerAssistor Android SDK 以及 Demo 项目，请参见 [CIPlayerAssistor_Android](https://github.com/tencentyun/CIPlayerAssistor_Android)

### 集成指引
#### 环境要求
Android系统版本：4.4 及以上。

#### 集成 CIPlayerAssistor SDK
1. 目前以 aar 的方式提供 SDK：[ci-assistor.aar](https://github.com/tencentyun/CIPlayerAssistor_Android/blob/main/app/libs/ci-assistor.aar)
2. 依赖 nanohttpd
```
implementation 'org.nanohttpd:nanohttpd:2.3.1'
```

### 示例代码
```
// 初始化万象播放协助器
CIPlayerAssistor.getInstance().init();

// 原始的媒体url
String orgUrl = "https://ci-1258100000.cos.ap-beijing.myqcloud.com/hls/test.m3u8";
// 是否是私有加密
boolean isPrivate = true;

// CIMediaInfo在实例化时,如果是私有加密，会自动生成了公钥，可用于请求token
CIMediaInfo ciMediaInfo = new CIMediaInfo(orgUrl, isPrivate);

// 从业务服务器获取token和授权信息: 自行实现getTokenAndAuthoriz方法
Pair<String, String> pair = getTokenAndAuthoriz(ciMediaInfo);
// 获取到token
String token = pair.first;
// 获取到授权信息，例如 q-sign-algorithm=sha1&q-ak=XXXXXXX&q-sign-time=1706098529;1706102129&q-key-time=1706098529;1706102129&q-header-list=&q-url-param-list=ci-process&q-signature=XXXXXXX
String authorization = pair.second;

// 给ciMediaInfo设置获取到的token和授权信息
ciMediaInfo.setJwtToken(token);
ciMediaInfo.setAuthorization(authorization);

// 获取最终的播放url
String playerUrl = CIPlayerAssistor.getInstance().buildPlayerUrl(ciMediaInfo);

// 将playerUrl设置给播放器即可，demo中演示了exoplayer和腾讯云播放器

// 以下以exoplayer作为代码示例
PlayerView playerView = view.findViewById(R.id.video_view);
ExoPlayer player = new ExoPlayer.Builder(getActivity()).build();
playerView.setPlayer(player);
MediaItem mediaItem = MediaItem.fromUri(playerUrl);
HlsMediaSource mediaSource = new HlsMediaSource.Factory(dataType ->
        new DefaultHttpDataSource.Factory().createDataSource()createMediaSource(mediaItem);
player.prepare(mediaSource);
player.play();

// 注意：播放结束后请释放CIPlayerAssistor资源
CIPlayerAssistor.getInstance().destroy();
```

### 示例图片
![](https://github.com/tencentyun/CIPlayerAssistor_Android/blob/main/screenshot/1.png)
![](https://github.com/tencentyun/CIPlayerAssistor_Android/blob/main/screenshot/2.png)
