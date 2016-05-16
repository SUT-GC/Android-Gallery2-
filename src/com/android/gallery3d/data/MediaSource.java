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

import com.android.gallery3d.data.MediaSet.ItemConsumer;

import java.util.ArrayList;

//看，Mediasource是根类
public abstract class MediaSource {
    //设置一个final tag，应该是log用的
    private static final String TAG = "MediaSource";
    //定义一个String 前缀
    private String mPrefix;

    //构造参数是初始化这个前缀
    protected MediaSource(String prefix) {
        mPrefix = prefix;
    }

    //前缀get
    public String getPrefix() {
        return mPrefix;
    }

    //通过uri来find路径
    public Path findPathByUri(Uri uri, String type) {
        return null;
    }

    //根据path来创建一个MediaOnject对象
    public abstract MediaObject createMediaObject(Path path);

    //暂停
    public void pause() {
    }

    //重新开始
    public void resume() {
    }

    //分局Path得到一个缺省的set
    public Path getDefaultSetOf(Path item) {
        return null;
    }

    //得到全部可用的cache
    public long getTotalUsedCacheSize() {
        return 0;
    }

    //得到全部目标cache的大小
    public long getTotalTargetCacheSize() {
        return 0;
    }


    public static class PathId {
        public PathId(Path path, int id) {
            this.path = path;
            this.id = id;
        }
        public Path path;
        public int id;
    }

    // Maps a list of Paths (all belong to this MediaSource) to MediaItems,
    // and invoke consumer.consume() for each MediaItem with the given id.
    //
    // This default implementation uses getMediaObject for each Path. Subclasses
    // may override this and provide more efficient implementation (like
    // batching the database query).
    //借助consumer.consume() 和 被给予的id 将一个Path(全部属于MediaSource）的链表隐射到MediaItems
    //此默认用每个path的getMediaObject来实现。子类可以重写此并提供更有效的实现（如数据库查询的批处理）。
    public void mapMediaItems(ArrayList<PathId> list, ItemConsumer consumer) {
        int n = list.size();
        //遍历list的size
        for (int i = 0; i < n; i++) {
            //得到每个PathId
            PathId pid = list.get(i);
            MediaObject obj;
            synchronized (DataManager.LOCK) {
                obj = pid.path.getObject();
                if (obj == null) {
                    try {
                        obj = createMediaObject(pid.path);
                    } catch (Throwable th) {
                        Log.w(TAG, "cannot create media object: " + pid.path, th);
                    }
                }
            }
            if (obj != null) {
                //使用者。消费者
                //当创建了MediaObject的对象之后，将此强制转换成MediaItem之后带着PathId.id进行消费
                consumer.consume(pid.id, (MediaItem) obj);
            }
        }
    }
}
