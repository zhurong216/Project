package dji.v5.ux.sample.showcase.defaultlayout;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.manager.datacenter.camera.CameraStreamManager;
import dji.v5.manager.interfaces.ICameraStreamManager;
import dji.v5.utils.common.JsonUtil;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;

public class CaptureActivity extends AppCompatActivity {
    private static String TAG = "CaptureActivity";

    protected SurfaceView mVideoSurface = null; // 演示窗口
    private Button mCaptureBtn; // 拍照
    private Button mSaveBtn;    // 保存
    private Button mDeleteBtn;  // 删除
    private ToggleButton mRecordBtn;    // 录像模式
    private TextView recordingTime; // 录制时间

    Surface surface = null;

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


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uxsdk_activity_capture);
        // 注册工作已在 DJIAircraftMainActivity 中完成
        // 初始化控件
        initUi();
        // 添加可用相机源
        MediaDataCenter.getInstance().getCameraStreamManager().addAvailableCameraUpdatedListener(new ICameraStreamManager.AvailableCameraUpdatedListener() {
            @Override
            public void onAvailableCameraUpdated(@NonNull List<ComponentIndexType> availableCameraList) {
                if (availableCameraList.size() > 0) {
                    MediaDataCenter.getInstance().getCameraStreamManager().putCameraStreamSurface(availableCameraList.get(0), surface, 300,600, ICameraStreamManager.ScaleType.CENTER_INSIDE);
                }
            }
        });
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
                
            }
        });
        // 保存点击事件
        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        // 删除点击事件
        mDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        // 录像点击事件
        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaDataCenter.getInstance().getCameraStreamManager().removeCameraStreamSurface(surface);
    }
}