# CLMediaMetadataRetriever
<h4 align="left"><strong>English</strong> | 
<a href="https://github.com/microink/CLMediaMetadataRetriever/blob/main/README.md">简体中文</a></h4>
<div>

[![License](https://img.shields.io/github/license/microink/CLMediaMetadataRetriever)](https://github.com/microink/CLMediaMetadataRetriever/blob/main/LICENSE)
[![GitHub release](https://img.shields.io/github/release/microink/CLMediaMetadataRetriever)](https://github.com/microink/CLMediaMetadataRetriever/releases)

</div>
&nbsp; Android官方MediaMetadataRetriever和FFmpegMediaMetadataRetriever的替代品，避免使用它们的getFrameAtTime()拆帧时的一些问题。

### 功能

- getFrameAtTime()，与官方库获取方式一致，可以无缝迁移
- 视频源支持路径和Uri
- 附带获取视频常用参数方法，如视频时长、视频宽高、旋转角度

### 与其他库的不同

- 避免官方MediaMetadataRetriever获取视频帧时，如果多次获取，时间轴越后获取越慢并且内存不断增长的问题
- 避免FFmpegMediaMetadataRetriever库在部分情况下，视频帧一直返回null的问题
- 避免在多次拆帧时内存泄漏问题

### 使用方式

1. 引入jitpack仓库
```kotlin
// settings.gradle
maven { url 'https://jitpack.io' }

// or settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        ...
        maven { url =  URI.create("https://jitpack.io") }
    }
```
2. 引入CLMediaMetadataRetriever
```kotlin
// build.gradle.kts
android {
    ...
    // need jdk11
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}
dependencies {
    ...
    implementation("com.github.microink:CLMediaMetadataRetriever:1.0.1")
}
```
3. 创建并初始化CLMediaMetadataRetriever
```kotlin
        // create
        private val retriever: CLMediaMetadataRetriever = CLMediaMetadataRetriever()

        ...

        // use data init
        lifecycleScope.launch(Dispatchers.IO) {
            retriever.init(this@MainActivity, uri,
                object : CLMediaMetadataRetriever.InitListener{
                override fun initSuccess() {
                    val seeTimeMs = 1000L
                    // ms to um
                    val bitmap = retriever.getFrameAtTime(seeTimeMs * 1000)
                    bitmap?.let {
                        launch(Dispatchers.Main) {
                            mIV.setImageBitmap(it)
                        }
                    }
                }

                override fun initFailed(e: Exception) {

                }

            })
        }
```
4. 释放资源
```kotlin
    override fun onDestroy() {
        super.onDestroy()
        retriever.release()
    }
```

### 技术参考
- Android [MediaMetadataRetriever](https://developer.android.com/reference/android/media/MediaMetadataRetriever)
- [FFmpegMediaMetadataRetriever](https://github.com/wseemann/FFmpegMediaMetadataRetriever)
- [AV_FrameCapture](https://stackoverflow.com/questions/12772547/mediametadataretriever-getframeattime-returns-only-first-frame/60633395#60633395)
