/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.print.PrintHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.os.Handler;

import com.android.gallery3d.R;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootView;
import com.android.gallery3d.util.PanoramaViewHelper;
import com.android.gallery3d.util.ThreadPool;
import com.android.photos.data.GalleryBitmapPool;

import java.io.FileNotFoundException;

public class AbstractGalleryActivity extends Activity implements GalleryContext {
    private static final String TAG = "AbstractGalleryActivity";
    private GLRootView mGLRootView;
    private StateManager mStateManager;
    private GalleryActionBar mActionBar;
    private OrientationManager mOrientationManager;
    private TransitionStore mTransitionStore = new TransitionStore();
    private boolean mDisableToggleStatusBar;
    private PanoramaViewHelper mPanoramaViewHelper;
    private static final int ONRESUME_DELAY = 50;

    private AlertDialog mAlertDialog = null;
    //定义 安装BroadcaseReceiver
    private BroadcastReceiver mMountReceiver = new BroadcastReceiver() {
        @Override
        //当接收到相对应的广播时候调用
        public void onReceive(Context context, Intent intent) {
            //获得系统文件的绝对路径（能放置缓存文件的地方）
            if (getExternalCacheDir() != null)
                //准备存储设备
                onStorageReady();
        }
    };
    //定义过滤器，当媒体安装的时候调用
    private IntentFilter mMountFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //排列方向管理者
        mOrientationManager = new OrientationManager(this);
        mPanoramaViewHelper = new PanoramaViewHelper(this);
        mPanoramaViewHelper.onCreate();
        getWindow().setBackgroundDrawable(null);
        //切换状态条
        toggleStatusBarByOrientation();
        //绑定批service
        doBindBatchService();
    }

    @Override
    //存储一些状态来进行恢复
    protected void onSaveInstanceState(Bundle outState) {
        mGLRootView.lockRenderThread();
        try {
            super.onSaveInstanceState(outState);
            //调用StateManager来saveState
            getStateManager().saveState(outState);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    @Override
    //当前Acitity配置改变时，调用StateManager与GalleryActionBar的onConfigurationChange
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        mStateManager.onConfigurationChange(config);
        getGalleryActionBar().onConfigurationChanged();
        //重新进行状态配置
        //生命菜单项已经改变，这个函数执行玩会调用onCreateOptionsMenu
        invalidateOptionsMenu();
        //重新创建状态条
        toggleStatusBarByOrientation();
    }

    @Override
    //创建按钮
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return getStateManager().createOptionsMenu(menu);
    }

    @Override
    //返回AndroidContext
    public Context getAndroidContext() {
        return this;
    }

    @Override
    //get dateManager
    public DataManager getDataManager() {
        return ((GalleryApp) getApplication()).getDataManager();
    }

    @Override
    //get ThreadPool
    public ThreadPool getThreadPool() {
        return ((GalleryApp) getApplication()).getThreadPool();
    }

    //get StateManager
    public synchronized StateManager getStateManager() {
        if (mStateManager == null) {
            mStateManager = new StateManager(this);
        }
        return mStateManager;
    }

    //get GLRoot
    public GLRoot getGLRoot() {
        return mGLRootView;
    }

    public void GLRootResume(boolean isResume) {
        if (isResume) {
            mGLRootView.onResume();
            mGLRootView.lockRenderThread();
        } else {
            mGLRootView.unlockRenderThread();
        }
    }

    //get OrientationManager
    public OrientationManager getOrientationManager() {
        return mOrientationManager;
    }

    @Override
    //进行view文件的设置
    public void setContentView(int resId) {
        super.setContentView(resId);
        mGLRootView = (GLRootView) findViewById(R.id.gl_root_view);
    }

    //储存装置准备
    protected void onStorageReady() {
        if (mAlertDialog != null) {
            //驳回对话框，并且在屏幕上消失
            mAlertDialog.dismiss();
            mAlertDialog = null;
            //注销MounReceiver
            unregisterReceiver(mMountReceiver);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        /*
         * 当没有外部存储设备的时候
         *  弹出dialog并且当选择确认的时候关闭当前acitivity
         */
        if (getExternalCacheDir() == null) {
            OnCancelListener onCancel = new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    //关掉当前的acitivity
                    finish();
                }
            };
            OnClickListener onClick = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.no_external_storage_title)
                    .setMessage(R.string.no_external_storage)
                    .setNegativeButton(android.R.string.cancel, onClick)
                    .setOnCancelListener(onCancel);
            //如果SDK_INT版本 > 11
            if (ApiHelper.HAS_SET_ICON_ATTRIBUTE) {
                setAlertDialogIconAttribute(builder);
            } else {
                //设置图标
                builder.setIcon(android.R.drawable.ic_dialog_alert);
            }
            //创建Alertdialog并且show
            mAlertDialog = builder.show();
            //注册安装监听器
            registerReceiver(mMountReceiver, mMountFilter);
        }
        mPanoramaViewHelper.onStart();
    }

    @TargetApi(ApiHelper.VERSION_CODES.HONEYCOMB)
    //当API>11时候设置alert图标
    private static void setAlertDialogIconAttribute(
            AlertDialog.Builder builder) {
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAlertDialog != null) {
            //取消监听
            unregisterReceiver(mMountReceiver);
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
        mPanoramaViewHelper.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //延迟onresume
        delayedOnResume(ONRESUME_DELAY);
    }

    //每次resume的时候都延迟50ms
    private void delayedOnResume(final int delay){
        final Handler handler = new Handler();
            //延迟任务，将一些费时的东西resume
           Runnable delayTask = new Runnable() {
              @Override
              public void run() {
                  //50ms之后执行Runnbale
                   handler.postDelayed(new Runnable() {
                       @Override
                       public void run() {
                           mGLRootView.lockRenderThread();
                           try {
                                getStateManager().resume();
                                getDataManager().resume();
                            } finally {
                                mGLRootView.unlockRenderThread();
                            }
                            mGLRootView.onResume();
                            mOrientationManager.resume();
                       }
                   }, delay);
             }
          };
        Thread delayThread = new Thread(delayTask);
        delayThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mOrientationManager.pause();
        mGLRootView.onPause();
        mGLRootView.lockRenderThread();
        try {
            getStateManager().pause();
            getDataManager().pause();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        GalleryBitmapPool.getInstance().clear();
        MediaItem.getBytesBufferPool().clear();
    }

    @Override
    //注销，解绑
    protected void onDestroy() {
        super.onDestroy();
        mGLRootView.lockRenderThread();
        try {
            getStateManager().destroy();
        } finally {
            mGLRootView.unlockRenderThread();
        }
        doUnbindBatchService();
    }

    @Override
    //呆结果的返回activity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mGLRootView.lockRenderThread();
        try {
            getStateManager().notifyActivityResult(
                    requestCode, resultCode, data);
        } finally {
            mGLRootView.unlockRenderThread();
        }
    }

    @Override
    //冲定义返回按键
    //返回上一个状态
    public void onBackPressed() {
        // send the back event to the top sub-state
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            getStateManager().onBackPressed();
        } finally {
            root.unlockRenderThread();
        }
    }

    //get GalleryActionBar
    public GalleryActionBar getGalleryActionBar() {
        if (mActionBar == null) {
            mActionBar = new GalleryActionBar(this);
        }
        return mActionBar;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        GLRoot root = getGLRoot();
        root.lockRenderThread();
        try {
            return getStateManager().itemSelected(item);
        } finally {
            root.unlockRenderThread();
        }
    }

    //不显示切换状态条
    protected void disableToggleStatusBar() {
        mDisableToggleStatusBar = true;
    }

    // Shows status bar in portrait view, hide in landscape view
    // 如果是没有状态栏就不用管了，如果有状态栏，横屏显示隐藏状态栏，竖屏显示状态栏
    private void toggleStatusBarByOrientation() {
        if (mDisableToggleStatusBar) return;

        Window win = getWindow();
        //方向不变，值对应端口的资源定定位符
        //portrait 竖排
        //landscape 横排
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            //清楚全屏
            win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            //开始全屏
            win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    //get方法
    public TransitionStore getTransitionStore() {
        return mTransitionStore;
    }

    //get方法
    public PanoramaViewHelper getPanoramaViewHelper() {
        return mPanoramaViewHelper;
    }

    protected boolean isFullscreen() {
        //是否时全屏
        return (getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
    }

    private BatchService mBatchService;
    //创建状态码为false
    private boolean mBatchServiceIsBound = false;
    //定义绑定监听函数
    private ServiceConnection mBatchServiceConnection = new ServiceConnection() {
        @Override
        //传参进去，组件名字，交换的数据
        public void onServiceConnected(ComponentName className, IBinder service) {
            //获得当前进行的批service
            mBatchService = ((BatchService.LocalBinder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mBatchService = null;
        }
    };

    //绑定service
    private void doBindBatchService() {
        //绑定Service并且指定连接监听者mBatchServiceConnection，自动创建
        bindService(new Intent(this, BatchService.class), mBatchServiceConnection, Context.BIND_AUTO_CREATE);
        //绑定Service的状态码
        mBatchServiceIsBound = true;
    }

    //解绑service
    private void doUnbindBatchService() {
        if (mBatchServiceIsBound) {
            // Detach our existing connection.
            //接触绑定
            unbindService(mBatchServiceConnection);
            mBatchServiceIsBound = false;
        }
    }

    public ThreadPool getBatchServiceThreadPoolIfAvailable() {
        if (mBatchServiceIsBound && mBatchService != null) {
            return mBatchService.getThreadPool();
        } else {
            throw new RuntimeException("Batch service unavailable");
        }
    }

    public void printSelectedImage(Uri uri) {
        //根据uri打印选择的图片
        if (uri == null) {
            return;
        }
        String path = ImageLoader.getLocalPathFromUri(this, uri);
        if (path != null) {
            Uri localUri = Uri.parse(path);
            path = localUri.getLastPathSegment();
        } else {
            path = uri.getLastPathSegment();
        }
        PrintHelper printer = new PrintHelper(this);
        try {
            printer.printBitmap(path, uri);
        } catch (FileNotFoundException fnfe) {
            Log.e(TAG, "Error printing an image", fnfe);
        }
    }
}
