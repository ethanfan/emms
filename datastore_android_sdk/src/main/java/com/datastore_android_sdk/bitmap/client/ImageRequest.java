/*
 * Copyright (C) 2011 The Android Open Source Project, 张涛
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

package com.datastore_android_sdk.bitmap.client;

import android.graphics.Bitmap;

import com.datastore_android_sdk.bitmap.toolbox.CreateBitmap;
import com.datastore_android_sdk.rxvolley.http.HttpHeaderParser;
import com.datastore_android_sdk.rxvolley.http.NetworkResponse;
import com.datastore_android_sdk.rxvolley.http.Request;
import com.datastore_android_sdk.rxvolley.http.Response;
import com.datastore_android_sdk.rxvolley.toolbox.HttpParamsEntry;
import com.datastore_android_sdk.rxvolley.toolbox.Loger;
import com.datastore_android_sdk.rxvolley.client.HttpCallback;
import com.datastore_android_sdk.rxvolley.http.VolleyError;
import com.datastore_android_sdk.rxvolley.interf.IPersistence;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 从网络请求一张bitmap
 *
 */
public class ImageRequest extends Request<Bitmap> implements IPersistence {

    private final int mMaxWidth;
    private final int mMaxHeight;
    // 用来保证当前对象只有一个线程在访问
    private static final Object sDecodeLock = new Object();

    public ImageRequest(BitmapRequestConfig config, HttpCallback callback) {
        super(config, callback);
        mMaxWidth = config.maxWidth;
        mMaxHeight = config.maxHeight;
    }

    @Override
    public Priority getPriority() {
        return Priority.NORMAL;
    }

    @Override
    public String getCacheKey() {
        return getUrl();
    }

    @Override
    public ArrayList<HttpParamsEntry> getHeaders() {
        return ((BitmapRequestConfig) getConfig()).getHeaders();
    }

    @Override
    public Response<Bitmap> parseNetworkResponse(NetworkResponse response) {
        synchronized (sDecodeLock) {
            try {
                Bitmap bitmap = CreateBitmap.create(response.data, mMaxWidth, mMaxHeight);
                if (bitmap == null) {
                    return Response.error(new VolleyError(response));
                } else {
                    return Response.success(bitmap, response.headers, HttpHeaderParser
                            .parseCacheHeaders(getUseServerControl(), getCacheTime(), response));
                }
            } catch (OutOfMemoryError e) {
                Loger.debug(String.format("Caught OOM for %d byte image, url=%s",
                        response.data.length, getUrl()));
                return Response.error(new VolleyError(e));
            }
        }
    }

    @Override
    protected void deliverResponse(ArrayList<HttpParamsEntry> headers, Bitmap response) {
        if (mCallback != null) {
            HashMap<String, String> map = new HashMap<>(headers.size());
            for (HttpParamsEntry entry : headers) {
                map.put(entry.k, entry.v);
            }
            mCallback.onSuccess(map, response);
        }
    }
}
