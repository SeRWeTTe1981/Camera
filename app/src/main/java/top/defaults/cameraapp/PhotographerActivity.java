package top.defaults.cameraapp;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import timber.log.Timber;
import top.defaults.camera.CameraPreview;
import top.defaults.camera.CanvasDrawer;
import top.defaults.camera.Error;
import top.defaults.camera.Keys;
import top.defaults.camera.Photographer;
import top.defaults.camera.PhotographerFactory;
import top.defaults.cameraapp.dialog.PickerDialog;
import top.defaults.cameraapp.dialog.SimplePickerDialog;
import top.defaults.cameraapp.options.VideoSize;
import top.defaults.view.TextButton;

public class PhotographerActivity extends AppCompatActivity {

    Photographer photographer;
    private boolean isRecordingVideo;
    private int lensFacing = CameraCharacteristics.LENS_FACING_BACK;
    private Size[] videoSizes;
    private VideoSize selectedSize;

    @BindView(R.id.preview) CameraPreview preview;
    @BindView(R.id.status) TextView statusTextView;
    @BindView(R.id.video) TextButton videoButton;
    @BindView(R.id.switch_mode) TextButton switchButton;
    @BindView(R.id.flip) TextButton flipButton;

    @OnClick(R.id.chooseVideoSize)
    void chooseVideoSize() {
        if (videoSizes != null && videoSizes.length > 0) {
            SimplePickerDialog<VideoSize> dialog = SimplePickerDialog.create(new PickerDialog.ActionListener<VideoSize>() {
                @Override
                public void onCancelClick(PickerDialog<VideoSize> dialog) {

                }

                @Override
                public void onDoneClick(PickerDialog<VideoSize> dialog) {
                    VideoSize videoSize = dialog.getSelectedItem(VideoSize.class);
                    selectedSize = videoSize;
                    photographer.setVideoSize(videoSize.size);
                }
            });
            List<VideoSize> supportedSizes = VideoSize.supportedSizes(videoSizes);
            dialog.setItems(supportedSizes);
            dialog.setInitialItem(VideoSize.findEqual(supportedSizes, selectedSize));
            dialog.show(getFragmentManager(), "videoSize");
        }
    }

    @OnCheckedChanged(R.id.fillSpace)
    void onFillSpaceChecked(boolean checked) {
        preview.setFillSpace(checked);
    }

    @OnClick(R.id.video)
    void video() {
        if (isRecordingVideo) {
            finishRecordingIfNeeded();
        } else {
            photographer.startRecording(null);
            isRecordingVideo = true;
            videoButton.setEnabled(false);
            switchButton.setVisibility(View.INVISIBLE);
            flipButton.setVisibility(View.INVISIBLE);
        }
    }

    @OnClick(R.id.switch_mode)
    void switchMode() {

    }

    @OnClick(R.id.flip)
    void flip() {
        photographer.stopPreview();
        if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
            lensFacing = CameraCharacteristics.LENS_FACING_FRONT;
        } else {
            lensFacing = CameraCharacteristics.LENS_FACING_BACK;
        }
        photographer.startPreview(Collections.singletonMap(Keys.LENS_FACING, lensFacing));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_record);
        enterFullscreen();

        ButterKnife.bind(this);

        preview.setFocusIndicatorDrawer(new CanvasDrawer() {
            private static final int SIZE = 300;
            private static final int LINE_LENGTH = 50;

            @Override
            public Paint[] initPaints() {
                Paint focusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                focusPaint.setStyle(Paint.Style.STROKE);
                focusPaint.setStrokeWidth(2);
                focusPaint.setColor(Color.WHITE);
                return new Paint[]{ focusPaint };
            }

            @Override
            public void draw(Canvas canvas, Point point, Paint[] paints) {
                if (paints == null || paints.length == 0) return;

                int left = point.x - (SIZE / 2);
                int top = point.y - (SIZE / 2);
                int right = point.x + (SIZE / 2);
                int bottom = point.y + (SIZE / 2);

                Paint paint = paints[0];

                canvas.drawLine(left, top + LINE_LENGTH, left, top, paint);
                canvas.drawLine(left, top, left + LINE_LENGTH, top, paint);

                canvas.drawLine(right - LINE_LENGTH, top, right, top, paint);
                canvas.drawLine(right, top, right, top + LINE_LENGTH, paint);

                canvas.drawLine(right, bottom - LINE_LENGTH, right, bottom, paint);
                canvas.drawLine(right, bottom, right - LINE_LENGTH, bottom, paint);

                canvas.drawLine(left + LINE_LENGTH, bottom, left, bottom, paint);
                canvas.drawLine(left, bottom, left, bottom - LINE_LENGTH, paint);
            }
        });
        photographer = PhotographerFactory.createPhotographerWithCamera2(this, preview);
        photographer.setOnEventListener(new Photographer.OnEventListener() {
            @Override
            public void onDeviceConfigured() {
                videoSizes = photographer.getSupportedRecordSize();
            }

            @Override
            public void onPreviewStarted() {

            }

            @Override
            public void onPreviewStopped() {

            }

            @Override
            public void onStartRecording() {
                videoButton.setEnabled(true);
                videoButton.setText(R.string.finish);
                statusTextView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPauseRecording() {

            }

            @Override
            public void onResumeRecording() {

            }

            @Override
            public void onFinishRecording(String filePath) {
                Toast.makeText(PhotographerActivity.this, "File: " + filePath, Toast.LENGTH_SHORT).show();
                Intent data = new Intent();
                data.putExtra(MainActivity.EXTRA_RECORDED_VIDEO_FILE_PATH, filePath);
                setResult(RESULT_OK, data);
            }

            @Override
            public void onError(Error error) {
                Timber.e("Error happens: %s", error.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        photographer.startPreview(Collections.singletonMap(Keys.LENS_FACING, lensFacing));
    }

    @Override
    protected void onPause() {
        finishRecordingIfNeeded();
        photographer.stopPreview();
        super.onPause();
    }

    private void enterFullscreen() {
        View decorView = getWindow().getDecorView();
        decorView.setBackgroundColor(Color.BLACK);
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void finishRecordingIfNeeded() {
        if (isRecordingVideo) {
            isRecordingVideo = false;
            photographer.finishRecording();
            statusTextView.setVisibility(View.INVISIBLE);
            switchButton.setVisibility(View.VISIBLE);
            flipButton.setVisibility(View.VISIBLE);
            videoButton.setEnabled(true);
            videoButton.setText(R.string.record_video);
        }
    }
}