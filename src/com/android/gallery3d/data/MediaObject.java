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

package com.android.gallery3d.data;

import android.net.Uri;

public abstract class MediaObject {
    @SuppressWarnings("unused")
    private static final String TAG = "MediaObject";
    public static final long INVALID_DATA_VERSION = -1;

    // These are the bits returned from getSupportedOperations():
    //返回允许的操作码
    public static final int SUPPORT_DELETE = 1 << 0;
    public static final int SUPPORT_ROTATE = 1 << 1;//旋转
    public static final int SUPPORT_SHARE = 1 << 2;
    public static final int SUPPORT_CROP = 1 << 3;//裁剪
    public static final int SUPPORT_SHOW_ON_MAP = 1 << 4;
    public static final int SUPPORT_SETAS = 1 << 5;
    public static final int SUPPORT_FULL_IMAGE = 1 << 6;
    public static final int SUPPORT_PLAY = 1 << 7;
    public static final int SUPPORT_CACHE = 1 << 8;
    public static final int SUPPORT_EDIT = 1 << 9;
    public static final int SUPPORT_INFO = 1 << 10;
    public static final int SUPPORT_TRIM = 1 << 11;//裁剪
    public static final int SUPPORT_UNLOCK = 1 << 12;
    public static final int SUPPORT_BACK = 1 << 13;
    public static final int SUPPORT_ACTION = 1 << 14;
    public static final int SUPPORT_CAMERA_SHORTCUT = 1 << 15;
    public static final int SUPPORT_MUTE = 1 << 16;//减轻，消除
    public static final int SUPPORT_PRINT = 1 << 17;//打印
    public static final int SUPPORT_DRM_INFO = 1 << 18;//管理
    public static final int SUPPORT_ALL = 0xffffffff;

    // These are the bits returned from getMediaType():
    //getMedioType的返回值
    public static final int MEDIA_TYPE_UNKNOWN = 1;
    public static final int MEDIA_TYPE_IMAGE = 2;
    public static final int MEDIA_TYPE_VIDEO = 4;
    public static final int MEDIA_TYPE_DRM_VIDEO = 5;//版权管理 vedio
    public static final int MEDIA_TYPE_DRM_IMAGE = 6;
    public static final int MEDIA_TYPE_ALL = MEDIA_TYPE_IMAGE | MEDIA_TYPE_VIDEO;

    public static final String MEDIA_TYPE_IMAGE_STRING = "image";
    public static final String MEDIA_TYPE_VIDEO_STRING = "video";
    public static final String MEDIA_TYPE_ALL_STRING = "all";

    // These are flags for cache() and return values for getCacheFlag():
    //cache的标志和返回cacheFlag
    public static final int CACHE_FLAG_NO = 0;
    public static final int CACHE_FLAG_SCREENNAIL = 1;
    public static final int CACHE_FLAG_FULL = 2;

    // These are return values for getCacheStatus():
    public static final int CACHE_STATUS_NOT_CACHED = 0;
    public static final int CACHE_STATUS_CACHING = 1;
    public static final int CACHE_STATUS_CACHED_SCREENNAIL = 2;
    public static final int CACHE_STATUS_CACHED_FULL = 3;

    //版本串行号
    private static long sVersionSerial = 0;

    //数据版本
    protected long mDataVersion;

    protected final Path mPath;

    public interface PanoramaSupportCallback {
        void panoramaInfoAvailable(MediaObject mediaObject, boolean isPanorama,
                boolean isPanorama360);
    }

    public MediaObject(Path path, long version) {
        path.setObject(this);
        mPath = path;
        mDataVersion = version;
    }

    //返回Path
    public Path getPath() {
        return mPath;
    }

    //得到支持的操作码
    public int getSupportedOperations() {
        return 0;
    }

    //得到全景支持
    public void getPanoramaSupport(PanoramaSupportCallback callback) {
        //全景照片展开
        callback.panoramaInfoAvailable(this, false, false);
    }

    public void clearCachedPanoramaSupport() {
    }

    public void delete() {
        throw new UnsupportedOperationException();
    }

    //旋转
    public void rotate(int degrees) {
        throw new UnsupportedOperationException();
    }

    public Uri getContentUri() {
        String className = getClass().getName();
        Log.e(TAG, "Class " + className + "should implement getContentUri.");
        Log.e(TAG, "The object was created from path: " + getPath());
        throw new UnsupportedOperationException();
    }

    public Uri getPlayUri() {
        throw new UnsupportedOperationException();
    }

    public int getMediaType() {
        return MEDIA_TYPE_UNKNOWN;
    }

    public MediaDetails getDetails() {
        MediaDetails details = new MediaDetails();
        return details;
    }

    public long getDataVersion() {
        return mDataVersion;
    }

    public int getCacheFlag() {
        return CACHE_FLAG_NO;
    }

    public int getCacheStatus() {
        throw new UnsupportedOperationException();
    }

    public long getCacheSize() {
        throw new UnsupportedOperationException();
    }

    public void cache(int flag) {
        throw new UnsupportedOperationException();
    }

    public static synchronized long nextVersionNumber() {
        return ++MediaObject.sVersionSerial;
    }

    //根据String来返回状态码
    public static int getTypeFromString(String s) {
        if (MEDIA_TYPE_ALL_STRING.equals(s)) return MediaObject.MEDIA_TYPE_ALL;
        if (MEDIA_TYPE_IMAGE_STRING.equals(s)) return MediaObject.MEDIA_TYPE_IMAGE;
        if (MEDIA_TYPE_VIDEO_STRING.equals(s)) return MediaObject.MEDIA_TYPE_VIDEO;
        throw new IllegalArgumentException(s);
    }

    //将type码转换为String
    public static String getTypeString(int type) {
        switch (type) {
            case MEDIA_TYPE_IMAGE: return MEDIA_TYPE_IMAGE_STRING;
            case MEDIA_TYPE_VIDEO: return MEDIA_TYPE_VIDEO_STRING;
            case MEDIA_TYPE_ALL: return MEDIA_TYPE_ALL_STRING;
        }
        throw new IllegalArgumentException();
    }
}
