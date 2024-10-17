package dji.v5.ux.sample.showcase.defaultlayout;

import android.Manifest;
import android.graphics.Camera;
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
import androidx.lifecycle.MutableLiveData;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.sdk.keyvalue.key.CameraKey;
import dji.sdk.keyvalue.key.DJIKeyInfo;
import dji.sdk.keyvalue.key.KeyTools;
import dji.sdk.keyvalue.msdkkeyinfo.KeyCaptureCameraStreamSettings;
import dji.sdk.keyvalue.value.camera.CameraMode;
import dji.sdk.keyvalue.value.camera.CameraStreamSettingsInfo;
import dji.sdk.keyvalue.value.camera.GeneratedMediaFileInfo;
import dji.sdk.keyvalue.value.camera.MediaFileType;
import dji.sdk.keyvalue.value.camera.VideoRecordMode;
import dji.sdk.keyvalue.value.common.CameraLensType;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.sdk.keyvalue.value.common.EmptyMsg;
import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.KeyManager;
import dji.v5.manager.datacenter.MediaDataCenter;
import dji.v5.manager.datacenter.media.MediaFile;
import dji.v5.manager.datacenter.media.MediaFileDownloadListener;
import dji.v5.manager.datacenter.media.MediaFileListData;
import dji.v5.manager.datacenter.media.MediaFileListState;
import dji.v5.manager.datacenter.media.MediaFileListStateListener;
import dji.v5.manager.datacenter.media.MediaManager;
import dji.v5.manager.datacenter.media.PullMediaFileListParam;
import dji.v5.manager.interfaces.ICameraStreamManager;
import dji.v5.manager.interfaces.IKeyManager;
import dji.v5.manager.interfaces.IMediaDataCenter;
import dji.v5.manager.interfaces.IMediaManager;
import dji.v5.utils.common.ContextUtil;
import dji.v5.utils.common.DiskUtil;
import dji.v5.utils.common.LogUtils;
import dji.v5.utils.common.StringUtils;
import dji.v5.ux.R;
import dji.v5.ux.cameracore.widget.cameracapture.CameraCaptureWidget;
import dji.v5.ux.core.base.DJISDKModel;
import io.reactivex.rxjava3.core.Completable;

public class CaptureActivity extends AppCompatActivity {
    private static String TAG = "CaptureActivity";

    protected SurfaceView mVideoSurface = null; // 演示窗口
    private Button mCaptureBtn; // 拍照
    private Button mSaveBtn;    // 保存
    private Button mDeleteBtn;  // 删除
    private ToggleButton mRecordBtn;  // 录像模式
    private TextView recordingTime; // 录制时间
    private ComponentIndexType primarySource = null; // 主摄像头
    Surface surface = null;
    List<ComponentIndexType> cameraList = new ArrayList<>();    // 可用相机列表
    private KeyManager keyManager = null;
    private int statusCamera = -1;    // -1：表示不能拍照 // 0：可以拍照 // 1：正在拍照 // 2：拍照结束
    private int statusVideo = -1;   // -1：不能录制 // 0:可以录制 // 1:正在录制 // 2：录制结束

    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static final int REQUEST_PERMISSION_CODE = 12345;
    private final ICameraStreamManager.AvailableCameraUpdatedListener availableCameraUpdatedListener = availableCameraList -> {
        runOnUiThread(() -> updateSource(availableCameraList));
    };
    private DJISDKModel djisdkModel = null;
    private IMediaDataCenter mediaDataCenter = null;    // 相机预览
    private MediaFile mediaFile = null; // 下载媒体文件
    private IMediaManager mediaManager = null; // 媒体文件管理器
    MutableLiveData<MediaFileListData> mediaFileListData = null;
    MutableLiveData<MediaFileListState> fileListState = new MutableLiveData<MediaFileListState>();
    MutableLiveData<Boolean> isPlayBack = new MutableLiveData<Boolean>();
    private Integer newFileIndex = 0;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uxsdk_activity_capture);
        // 注册工作已在 DJIAircraftMainActivity 中完成
        // 初始化控件
        initUi();
        // 添加可用相机源
        mediaDataCenter.getCameraStreamManager().addAvailableCameraUpdatedListener(availableCameraUpdatedListener);

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
        djisdkModel = DJISDKModel.getInstance();
        keyManager = KeyManager.getInstance();   // 创建管理者
            // 下载相关组件
        mediaDataCenter = MediaDataCenter.getInstance();
        mediaFile = new MediaFile();
        mediaManager =  MediaDataCenter.getInstance().getMediaManager();
            // 更新文件列表状态：添加文件列表更新状态监听
        addMediaFileListStateListener();

        if (keyManager == null) Log.d(TAG, "initUi: 管理者创建未成功");
        else Log.d(TAG, "initUi: 创建成功");


        // 拍照点击事件
        mCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (statusCamera == 0)takePicture();
            }
        });
        // 保存点击事件
        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: ");
                if (statusCamera == 2 || statusVideo == 2) {
                    try {
                        saveResult();
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    Log.d(TAG, "onClick: 无数据");
                }
            }
        });
        // 删除点击事件
        mDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (statusCamera == 2)deleteResult();
                else Log.d(TAG, "onClick: 无数据");
            }
        });
        // 录像点击事件
        mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (statusVideo == 0) startRecord();
                else if (statusVideo == 1) {
                    stopRecord();
                }
            }
        });
    }
    private void addMediaFileListStateListener() {
        Log.d(TAG, "addMediaFileListStateListener: 文件列表状态信息："+fileListState);
        MediaDataCenter.getInstance().getMediaManager().addMediaFileListStateListener(new MediaFileListStateListener() {
            @Override
            public void onUpdate(MediaFileListState mediaFileListState) {
                fileListState.postValue(mediaFileListState);
                Log.d(TAG, "onUpdate: mediaFileListState："+fileListState.toString());

                if (mediaFileListState.equals(MediaFileListState.UP_TO_DATE) ) {
                    Log.d(TAG, "onUpdate: 状态为 up_to_date");
                }
                else if (mediaFileListState == MediaFileListState.UPDATING) {
                    Log.d(TAG, "onUpdate: 状态为 updating");
                }
                else if (mediaFileListState == MediaFileListState.IDLE) {
                    Log.d(TAG, "onUpdate: 状态为 idle");
                }
            }
        });
    }
    private void pullMediaFileListFromCamera() {
        Log.d(TAG, "pullMediaFileListFromCamera: 拉取文件");
        MediaDataCenter.getInstance().getMediaManager().pullMediaFileListFromCamera(
                new PullMediaFileListParam.Builder().mediaFileIndex(newFileIndex).count(10).build(),
                new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "onSuccess: 拉取文件列表成功！");
                        MediaFileListData date = MediaDataCenter.getInstance().getMediaManager().getMediaFileListData();
                        Log.d(TAG, "onSuccess: 输出长度："+date.getData().size());
                        // 执行下载任务
                        try {
                            for (MediaFile it : date.getData()) {
                                downloadFile(it);
                            }
                            Log.d(TAG, "onSuccess: 下载完成");
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull IDJIError idjiError) {
                        Log.d(TAG, "onFailure: 拉取文件列表失败！");
                    }
                }
        );
    }


    private void takePicture() {
        Log.d(TAG, "takePicture: 设置拍照模式");
        statusCamera = 1;   // 正在拍照
        keyManager.setValue(KeyTools.createKey(CameraKey.KeyCameraMode, primarySource), CameraMode.PHOTO_NORMAL, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess: 设置为单拍模式成功");
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                Log.d(TAG, "onFailure: 设置为单拍模式失败");
            }
        });

        keyManager.performAction(KeyTools.createCameraKey(CameraKey.KeyStartShootPhoto, primarySource, CameraLensType.CAMERA_LENS_DEFAULT), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
            @Override
            public void onSuccess(EmptyMsg emptyMsg) {
                Log.d(TAG, "onSuccess: 执行拍照动作成功");
                statusCamera = 2;   // 拍照结束
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                Log.d(TAG, "onFailure: 执行拍照动作失败");
            }
        });
    }

    // 保存
    private void saveResult() throws FileNotFoundException {

        if (fileListState.getValue().equals(MediaFileListState.IDLE)) {
            Log.d(TAG, "saveResult: 执行文件拉取动作");
            pullMediaFileListFromCamera();
        }
        // downloadFile(mediaManager.getMediaFileListData().getData().get(newFileIndex));
        statusCamera = 0;   // 重置相机状态
        statusVideo = 0;
    }
    // 下载功能实现
    private void downloadFile(MediaFile mediaFile) throws FileNotFoundException {
        File dirs = new File(DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(),  "/mediafile"));
        if (!dirs.exists()) {
            dirs.mkdirs();
        }
        String filepath = DiskUtil.getExternalCacheDirPath(ContextUtil.getContext(),  "/mediafile/"  + mediaFile.getFileName());
        File file = new File(filepath);
        if (file.exists()) {
            file.delete();
        }
        Long offset = 0L;
        FileOutputStream outputStream = new FileOutputStream(file, true);
        BufferedOutputStream bos = new BufferedOutputStream(outputStream);
        mediaFile.pullOriginalMediaFileFromCamera(offset, new MediaFileDownloadListener() {
            @Override
            public void onStart() {
                LogUtils.i("MediaFile" , "${mediaFile.fileIndex } start download"  );
            }

            @Override
            public void onProgress(long total, long current) {
                Long fullSize = offset + total;
                Long downloadedSize = offset + current;
                Double data = StringUtils.formatDouble((downloadedSize.doubleValue() / fullSize.doubleValue()));
                String result = StringUtils.formatDouble(data * 100, "#0").toString() + "%";
                LogUtils.i("MediaFile"  , "${mediaFile.fileIndex}  progress $result");
            }

            @Override
            public void onRealtimeDataUpdate(byte[] data, long position) {
                try {
                    bos.write(data);
                    bos.flush();
                } catch (IOException e) {
                    LogUtils.e("MediaFile", "write error" + e.getMessage());
                }
            }

            @Override
            public void onFinish() {
                try {
                    outputStream.close();
                    bos.close();
                } catch (IOException error) {
                    LogUtils.e("MediaFile", "close error$error");
                }
                LogUtils.i("MediaFile" , "${mediaFile.fileIndex }  download finish"  );
                statusVideo = 0;
                statusCamera = 0;
            }

            @Override
            public void onFailure(IDJIError error) {
                LogUtils.e("MediaFile", "download error$error");
            }
        });

    }
    private void deleteResult() {
        statusCamera = 0;   // 重置相机状态
        statusVideo = 0;
    }

    private void startRecord() {
        Log.d(TAG, "startRecord: 开始录像");
        recordingTime.setText("录制中···");
        keyManager.setValue(KeyTools.createCameraKey(CameraKey.KeyVideoRecordMode,primarySource,CameraLensType.CAMERA_LENS_DEFAULT), VideoRecordMode.NORMAL, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "onSuccess: 设置成功");
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                Log.d(TAG, "onFailure: 设置失败");
            }
        });
        keyManager.performAction(KeyTools.createCameraKey(CameraKey.KeyStartRecord, primarySource, CameraLensType.CAMERA_LENS_DEFAULT), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
            @Override
            public void onSuccess(EmptyMsg emptyMsg) {
                Log.d(TAG, "onSuccess: 执行录制动作成功");
                statusVideo = 1;
            }

            @Override
            public void onFailure(@NonNull IDJIError idjiError) {
                Log.d(TAG, "onFailure: 执行录制动作失败");
            }
        });
    }
    private void stopRecord() {
        if (statusVideo == 1) {
            keyManager.performAction(KeyTools.createCameraKey(CameraKey.KeyStopRecord, primarySource, CameraLensType.CAMERA_LENS_DEFAULT), new CommonCallbacks.CompletionCallbackWithParam<EmptyMsg>() {
                @Override
                public void onSuccess(EmptyMsg emptyMsg) {
                    Log.d(TAG, "onSuccess: 结束录制成功");
                    recordingTime.setText("");
                    statusVideo = 2;
                }

                @Override
                public void onFailure(@NonNull IDJIError idjiError) {
                    Log.d(TAG, "onFailure: 结束录制失败");
                }
            });
        }

    }

    private void updateSource(List<ComponentIndexType> availableCameraList) {
        Log.d(TAG, "updateSource: 个数："+availableCameraList.size());
        if (availableCameraList.size() > 0) {
            primarySource = getSuitableSource(cameraList, ComponentIndexType.LEFT_OR_MAIN);
            mediaDataCenter.getCameraStreamManager().putCameraStreamSurface(primarySource, surface, 2000,1000, ICameraStreamManager.ScaleType.CENTER_CROP);
            // 添加文件列表状态监听
            keyManager.listen(KeyTools.createCameraKey(CameraKey.KeyNewlyGeneratedMediaFile, primarySource, CameraLensType.CAMERA_LENS_DEFAULT), this, new CommonCallbacks.KeyListener<GeneratedMediaFileInfo>() {
                @Override
                public void onValueChange(@Nullable GeneratedMediaFileInfo generatedMediaFileInfo, @Nullable GeneratedMediaFileInfo t1) {
                    Log.d(TAG, "onValueChange: 媒体资源已更新:");
                    if (t1 != null) {
                        Log.d(TAG, "onValueChange: ");
                        newFileIndex = t1.getIndex();
                        Log.d(TAG, "onValueChange: 新文件下标："+String.valueOf(newFileIndex));
                    }
                    else Log.d(TAG, "onValueChange: 空指针");

                }
            });

            statusCamera = 0;     // 可以拍照
            statusVideo = 0;    // 可以录像
        }
        else {
            Log.d(TAG, "updateSource: 未获取到相机源");
            statusCamera = -1;    // 不能拍照
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
        mediaDataCenter.getCameraStreamManager().removeAvailableCameraUpdatedListener(availableCameraUpdatedListener);
        mediaDataCenter.getCameraStreamManager().removeCameraStreamSurface(surface);
        mediaManager.removeAllMediaFileListStateListener(); // 移除所有文件列表状态监听器
        keyManager.cancelListen(KeyTools.createCameraKey(CameraKey.KeyNewlyGeneratedMediaFile, primarySource, CameraLensType.CAMERA_LENS_DEFAULT), this);
        Log.d(TAG, "onDestroy: 回收监听器");
    }
}