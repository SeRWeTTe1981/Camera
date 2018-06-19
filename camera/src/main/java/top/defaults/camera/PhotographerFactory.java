package top.defaults.camera;

import android.app.Activity;

public class PhotographerFactory {

    public static Photographer createPhotographerWithCamera2(Activity activity, CameraPreview preview) {
        InternalPhotographer photographer = new Camera2Photographer();
        photographer.initWithViewfinder(activity, preview);
        return photographer;
    }
}