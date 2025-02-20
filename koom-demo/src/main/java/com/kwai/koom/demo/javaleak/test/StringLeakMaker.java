package com.kwai.koom.demo.javaleak.test;

import android.content.Context;
import android.util.Log;

import com.kwai.koom.demo.javaleak.test.LeakMaker;

/**
 * Copyright 2020 Kwai, Inc. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Rui Li <lirui05@kuaishou.com>
 */
public class StringLeakMaker extends LeakMaker<String> {
  private static final String TAG = "StringLeakMaker";
  @Override
  void startLeak(Context context) {
    String largeStr = new String(new byte[512 * 1024]);
    uselessObjectList.add(largeStr);
    Log.d(TAG, "startLeak: uselessObjectList = " + uselessObjectList.size());
  }
}
