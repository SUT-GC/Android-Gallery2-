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

package com.android.gallery3d.anim;

import android.view.animation.Interpolator;

import com.android.gallery3d.common.Utils;

// Animation calculates a value according to the current input time.
// 动画计算一个值是根据当前的输入时间
// 1. First we need to use setDuration(int) to set the duration of the
//    animation. The duration is in milliseconds.
// 首先，我们需要调用setDuration（设置持续时间）来设置动画的持续时间，以毫秒为单位
// 2. Then we should call start(). The actual start time is the first value
//    passed to calculate(long).
// 然后我们调用start函数，这个时机的开始时间是第一次传入calculate的时间
// 3. Each time we want to get an animation value, we call
//    calculate(long currentTimeMillis) to ask the Animation to calculate it.
//    The parameter passed to calculate(long) should be nonnegative.
// 每次我们想得到一个动画的值，我们调用calculate来告诉Animation计算它，这个传入的参数非负
// 4. Use get() to get that value.
// 我们用get函数得到这个值
//
// In step 3, onCalculate(float progress) is called so subclasses can calculate
// the value according to progress (progress is a value in [0,1]).
// 在第三部，oncalculate函数被子类调用，能够计算传入的参数值（0~1）
//
// Before onCalculate(float) is called, There is an optional interpolator which
// can change the progress value. The interpolator can be set by
// setInterpolator(Interpolator). If the interpolator is used, the value passed
// to onCalculate may be (for example, the overshoot effect).
// 在调用onCalculate函数之前，有一个可选择的工具来改变这个传入的值
// 这个工作能够被set在setInterpolator函数
// 如果这个工具被调用了，通过计算出的值可能是超过效果
//
// The isActive() method returns true after the animation start() is called and
// before calculate is passed a value which reaches the duration of the
// animation.
// 这个isActive方法返回true在这个动画start之后被调用，在计算出动画持续时间之前
//
// The start() method can be called again to restart the Animation.
// 这个start方法能够被再次调用，当restart这个动画时候
//
abstract public class Animation {
    private static final long ANIMATION_START = -1;
    private static final long NO_ANIMATION = -2;

    private long mStartTime = NO_ANIMATION;
    private int mDuration;
    private Interpolator mInterpolator;

    public void setInterpolator(Interpolator interpolator) {
        mInterpolator = interpolator;
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    public void start() {
        mStartTime = ANIMATION_START;
    }

    public void setStartTime(long time) {
        mStartTime = time;
    }

    public boolean isActive() {
        return mStartTime != NO_ANIMATION;
    }

    //强行停止
    public void forceStop() {
        mStartTime = NO_ANIMATION;
    }

    public boolean calculate(long currentTimeMillis) {
        if (mStartTime == NO_ANIMATION) return false;
        if (mStartTime == ANIMATION_START) mStartTime = currentTimeMillis;
        //定义消逝变量 = 当前的时间-startTime
        int elapse = (int) (currentTimeMillis - mStartTime);
        //将 消逝的时间/持续时间 的值控制在0~1之间
        float x = Utils.clamp((float) elapse / mDuration, 0f, 1f);
        Interpolator i = mInterpolator;
        //计算传入的值
        onCalculate(i != null ? i.getInterpolation(x) : x);
        //如果消逝时间 > 持续时间
        //  停止动画，返回false
        if (elapse >= mDuration) mStartTime = NO_ANIMATION;
        return mStartTime != NO_ANIMATION;
    }

    abstract protected void onCalculate(float progress);
}
