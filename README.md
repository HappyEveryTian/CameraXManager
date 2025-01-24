# CameraXManager
CameraXManager is a library that helps you to use CameraX simply in your Android project.

## how to get
1. add the JitPack repository to your build file
    ```groovy
    // settings.gradle.kts
    dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			......
			maven { url 'https://jitpack.io' }
		}
	}
    ```
2. add the dependency
    ```groovy
    // build.gradle.kts
    dependencies {
        ......
        implementation("com.github.HappyEveryTian:CameraXManager:1.0.2")
    }
    ```

## usage
1. init the camera engin
    ```Java
    // first call init() method to init the camera engin
    // @param Context context should be a LifecycleOwner
    CameraManager.getInstance().init(context);
    // @param PreviewView previewView is a layout view that can show the preview of the camera which defined in your layout file
    // CameraManager.getInstance().init(context, previewView);
    ```
2. add the callback lisener
    ```Java
    CameraManager.getInstance().addRecordingCallback(new RecordingCallback() {
                    @Override
                    public void onInit() {
                        // when init() finished will call this method
                        // you can bind some click method on your button to control the camera
                    }

                    @Override
                    public void onStart() {
                        // logic after camera record start 
                    }

                    @Override
                    public void onPause() {
                        // logic after camera record pause
                    }

                    @Override
                    public void onResume() {
                        // logic after camera record resume
                    }

                    @Override
                    public void onStop() {
                        // logic after camera record stop
                    }

                    @Override
                    public void onError() {
                        // logic when camera record stop with error
                    }
                });
    ```
3. when you want to switch the camera, just call the method below:
   ```Java
   CameraManager.getInstance().switchCamera();
   ```
4. get the recording object to control
    ```Java
    // the param is the file name which is nullable
    Recording recording = CameraManager.getInstance().startRecord(null);
    ......
    recording.pause();
    recording.resume();
    recording.stop();
    ```    

5. **remember to release the camera engin when you don't need it**, for example:
    ```Java
    // MainActivity.java
    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraManager.getInstance().release();
    }
    ```
6. simple usage case is in the module app

## contact me
*if you have any question or suggestion, please contact me by email: Cy1901753749@gmail.com*