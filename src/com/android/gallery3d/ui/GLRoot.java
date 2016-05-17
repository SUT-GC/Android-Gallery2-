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

package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Matrix;

import com.android.gallery3d.anim.CanvasAnimation;
import com.android.gallery3d.glrenderer.GLCanvas;

public interface GLRoot {

    // Listener will be called when GL is idle AND before each frame.
    // Mainly used for uploading textures(文理）.
    public static interface OnGLIdleListener {
        public boolean onGLIdle(
                GLCanvas canvas, boolean renderRequested);
    }

    //添加事件监听器
    public void addOnGLIdleListener(OnGLIdleListener listener);
    //注册开始的动画
    public void registerLaunchedAnimation(CanvasAnimation animation);
    //请求生成焦点
    public void requestRenderForced();
    //请求生成
    public void requestRender();
    //请求布局面板
    public void requestLayoutContentPane();

    //对渲染线程进行lock操作
    public void lockRenderThread();
    public void unlockRenderThread();

    //设置内容面板
    public void setContentPane(GLView content);
    //设置取向源
    public void setOrientationSource(OrientationSource source);
    //设置显示旋转
    public int getDisplayRotation();
    //修正
    public int getCompensation();
    public Matrix getCompensationMatrix();
    //冻结
    public void freeze();
    public void unfreeze();
    public void setLightsOutMode(boolean enabled);

    public Context getContext();
}
