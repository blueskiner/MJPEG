#### MJPEG
Android MJPEG播放  
支持http mjpeg直播流播放;  
支持编码MP4保存视频;
#### 资源
|名字|资源|
|-|-|
|jar|[下载](https://github.com/RelinRan/MJPEG/tree/main/jar)|
|GitHub |[查看](https://github.com/RelinRan/MJPEG)|
|Gitee|[查看](https://gitee.com/relin/MJPEG)|
#### Maven
1.build.grade
```
allprojects {
    repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```
2./app/build.grade
```
dependencies {
	implementation 'com.github.RelinRan:MJPEG:2022.2023.10.17.1'
}
```
#### 初始化
配置权限
```
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```
#### 使用
```
MJPEGSurface surface = findViewById(R.id.surface);//MJPEGImage同方法使用

surface.setDebug(true);//开启调试
surface.setDataSource("http://xxx");//视频资源
surface.setScaleType(ScaleType.CENTER_FIT);//显示模式

//编码保存MP4
surface.setEncodeMP4(true);
surface.setEncodePath("MJPEG","Video","mjpeg.mp4");
surface.setEncodeWidth(640);
surface.setEncodeHeight(480);
surface.setFrameRate(30);
surface.setBitRate(100000);
surface.setIFrameInterval(1);

//开始播放
surface.start();

//开始编码MP4
surface.startEncodeMP4();

//停止编码MP4
surface.endEncodeMP4();
```