# CLMediaMetadataRetriever
<h4 align="left"><strong>English</strong> | 
<a href="https://github.com/microink/CLMediaMetadataRetriever/blob/main/README_zh.md">简体中文</a></h4>
<div>

[![License](https://img.shields.io/github/license/microink/CLMediaMetadataRetriever)](https://github.com/microink/CLMediaMetadataRetriever/blob/main/LICENSE)
[![GitHub release](https://img.shields.io/github/release/microink/CLMediaMetadataRetriever)](https://github.com/microink/CLMediaMetadataRetriever/releases)

</div>
&nbsp; Alternatives to Android's official MediaMetadataRetriever and FFMediaMetadataRetriever, to avoid issues when using their getFrameAtTime() method for frame extraction.

### Feature

- getFrameAtTime()，consistent with the acquisition method of the official library, allowing for seamless migration.
- Video source supports both path and Uri.
- Includes methods for obtaining common video parameters, such as video duration, video width and height, and rotation angle.

### Different from other libraries

- Avoid the issue where using the official MediaMetadataRetriever to retrieve video frames becomes slower and consumes increasing amounts of memory when retrieving frames multiple times, especially as the timeline progresses.
- Avoid the issue where the FFmpegMediaMetadataRetriever library sometimes returns null for video frames.
- Avoid memory leak issues when extracting frames multiple times.

### Usage

1. Import jitpack maven
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
2. Import CLMediaMetadataRetriever
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
3. Create and init CLMediaMetadataRetriever
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
4. Release 
```kotlin
    override fun onDestroy() {
        super.onDestroy()
        retriever.release()
    }
```

### Technical reference
- Android [MediaMetadataRetriever](https://developer.android.com/reference/android/media/MediaMetadataRetriever)
- [FFmpegMediaMetadataRetriever](https://github.com/wseemann/FFmpegMediaMetadataRetriever)
- [AV_FrameCapture](https://stackoverflow.com/questions/12772547/mediametadataretriever-getframeattime-returns-only-first-frame/60633395#60633395)