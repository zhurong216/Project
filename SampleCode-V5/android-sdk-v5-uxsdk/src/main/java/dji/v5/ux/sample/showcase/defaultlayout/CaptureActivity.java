package dji.v5.ux.sample.showcase.defaultlayout;

import android.Manifest;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.manager.interfaces.ICameraStreamManager;
import dji.v5.ux.R;
import dji.v5.ux.cameracore.widget.cameracapture.CameraCaptureWidget;

public class CaptureActivity extends AppCompatActivity {
    private static String TAG = "CaptureActivity";

    protected SurfaceView mVideoSurface = null; // 演示窗口
    private Button mCaptureBtn; // 拍照
    private Button mSaveBtn;    // 保存
    private Button mDeleteBtn;  // 删除
    private ToggleButton mRecordBtn;    // 录像模式
    private TextView recordingTime; // 录制时间

    Surface surface = null;
    List<ComponentIndexType> cameraList = new ArrayList<>();    // 可用相机列表

    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA
    };
    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static final int REQUEST_PERMISSION_CODE = 12345;
    private final ICameraStreamManager.AvailableCameraUpdatedListener availableCameraUpdatedListener = availableCameraList -> {
        runOnUiThread(() -> updateSource(availableCameraList));
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uxsdk_activity_capture);
        // 注册工作已在 DJIAircraftMainActivity 中完成
        // 初始化控件
        initUi();
        // 添加可用相机源
        MediaDataCenter.getInstance().getCameraStreamManager().addAvailableCameraUpdatedListener(availableCameraUpdatedListener);
    }
    // 初始化UI
    private void initUi() {
        mVideoSurface = findViewById(R.id.video_previewer_surface);
        mCaptureBtn = findViewById(R.id.btn_capture);
        mDeleteBtn = findViewById(R.id.btn_delete);
        mSaveBtn = findViewById(R.id.btn_save);
        mRecordBtn = findViewById(R.id.btn_record);
        recordingTime = findViewById(R.id.timer);
        // 实例化视图
        surface = mVideoSurface.getHolder().getSurface();

        // 拍照点击事件
        mCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
        // 保存点击事件
        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveResult();
            }
        });
        // 删除点击事件
        mDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteResult();
            }
        });
        // 录像点击事件
        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });
    }
    private void takePicture() {

    }
    private void saveResult() {

    }
    private void deleteResult() {

    }
    private void startRecord() {

    }
    private void stopRecord() {

    }

    private void updateSource(List<ComponentIndexType> availableCameraList) {
        Log.d(TAG, "updateSource: 个数："+availableCameraList.size());
        if (availableCameraList.size() > 0) {
            ComponentIndexType primarySource = getSuitableSource(cameraList, ComponentIndexType.LEFT_OR_MAIN);
            MediaDataCenter.getInstance().getCameraStreamManager().putCameraStreamSurface(primarySource, surface, 2000,1000, ICameraStreamManager.ScaleType.CENTER_CROP);
        }
        else {
            Log.d(TAG, "updateSource: 未获取到相机源");
        }
    }

    private ComponentIndexType getSuitableSource(List<ComponentIndexType> cameraList, ComponentIndexType defaultSource) {
        if (cameraList.contains(ComponentIndexType.LEFT_OR_MAIN)) {
            return ComponentIndexType.LEFT_OR_MAIN;
        } else if (cameraList.contains(ComponentIndexType.RIGHT)) {
            return ComponentIndexType.RIGHT;
        } else if (cameraList.contains(ComponentIndexType.UP)) {
            return ComponentIndexType.UP;
        }
        return defaultSource;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaDataCenter.getInstance().getCameraStreamManager().removeAvailableCameraUpdatedListener(availableCameraUpdatedListener);
        MediaDataCenter.getInstance().getCameraStreamManager().removeCameraStreamSurface(surface);
        Log.d(TAG, "onDestroy: 回收监听器");
    }
}