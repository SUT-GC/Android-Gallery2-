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

import java.util.ArrayList;
import java.util.HashMap;

//路径匹配类
public class PathMatcher {
    public static final int NOT_FOUND = -1;

    private ArrayList<String> mVariables = new ArrayList<String>();
    //创建一个根node
    private Node mRoot = new Node();

    public PathMatcher() {
        mRoot = new Node();
    }

    public void add(String pattern, int kind) {
        //根据pattern分开成几段
        String[] segments = Path.split(pattern);
        Node current = mRoot;
        for (int i = 0; i < segments.length; i++) {
            current = current.addChild(segments[i]);
        }
        //给此时的node设置一个kind
        current.setKind(kind);
    }

    public int match(Path path) {
        String[] segments = path.split();
        mVariables.clear();
        Node current = mRoot;
        for (int i = 0; i < segments.length; i++) {
            Node next = current.getChild(segments[i]);
            //如果根node中没有对应的值，则看看有没有*的值，如果没有的话返回-1，如果有的话，把对应的段放到mVariables里去
            if (next == null) {
                next = current.getChild("*");
                if (next != null) {
                    mVariables.add(segments[i]);
                } else {
                    return NOT_FOUND;
                }
            }
            current = next;
        }
        return current.getKind();
    }

    //返回mVaribale对应的值
    public String getVar(int index) {
        return mVariables.get(index);
    }

    public int getIntVar(int index) {
        return Integer.parseInt(mVariables.get(index));
    }

    public long getLongVar(int index) {
        return Long.parseLong(mVariables.get(index));
    }

    private static class Node {
        private HashMap<String, Node> mMap;
        private int mKind = NOT_FOUND;

        Node addChild(String segment) {
            //查看map是不是为空
            //如果为空，创建新的map
            //如果不为空，查看map里是否存在段
            //如果存在，返回存在的node，如果不存在，创建一个新的node
            //讲新node添加进入map中
            //返回新的node
            if (mMap == null) {
                mMap = new HashMap<String, Node>();
            } else {
                Node node = mMap.get(segment);
                if (node != null) return node;
            }

            Node n = new Node();
            mMap.put(segment, n);
            return n;
        }

        //根据段查找node
        Node getChild(String segment) {
            if (mMap == null) return null;
            return mMap.get(segment);
        }

        //设置kind
        void setKind(int kind) {
            mKind = kind;
        }

        //得到kind
        int getKind() {
            return mKind;
        }
    }
}
