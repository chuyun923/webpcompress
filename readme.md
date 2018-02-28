##webp图片压缩插件

webp相对于png 和 jpg来说，拥有更高的压缩比，并且提供有损压缩和
无损压缩，即便是在无损压缩的情况下，webp也比png和jpg有更小的体积。本插件在打包时，自动将项目中满足条件的图片进行webp压缩

###引入

       classpath 'com.chuyun923.android.plugin:webpcompress:3.2.0'


       apply plugin: 'webpcompress'

###配置

       webpCompress {
           q = 80  //压缩比例 0～100，100是无损压缩，默认80
           skipDebug = true //debug下是否开启webp压缩 默认不开启
           cwebpPath = "/Users/pengliang/Downloads/libwebp-0.6.0-rc3-mac-10.12/bin/cwebp" //cwebp命令的路径
           openLog ＝ true //是否打开log
           filterAlpha ＝ true //是否过滤有透明通道的图片
           whiteList = [
                   "notify_panel_notification_icon_bg.png"
           ]

           //配置压缩白名单
       }

> * cwebp命令下载地址：https://storage.googleapis.com/downloads.webmproject.org/releases/webp/index.html
* filterAplph:默认情况下，需要打开，如果你的minSDK升级到了18，那么可以关闭，原因是在Android 4.2.1及以下版本上面对于有透明度的
webp图片显示有问题

###输出

       project.buildDir/outputs/webpcompressoutput.txt 文件中会输出这次替换的结果
###特别感谢

美丽联合集团的项目：https://github.com/meili/WebpConvert_Gradle_Plugin