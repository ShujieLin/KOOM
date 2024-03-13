/*
 * Copyright (c) 2021. Kwai, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author KOOM Team
 *
 */
package com.kwai.koom.base

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import org.json.JSONObject
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

object MonitorManager {
    internal val MONITOR_MAP = ConcurrentHashMap<Class<*>, Monitor<*>>()

    internal lateinit var commonConfig: CommonConfig

    @JvmStatic
    fun initCommonConfig(commonConfig: CommonConfig) = apply {
        this.commonConfig = commonConfig
    }

    @JvmStatic
    fun <M : MonitorConfig<*>> addMonitorConfig(config: M) = apply {
        var supperType: Type? = config.javaClass.genericSuperclass
        while (supperType is Class<*>) {
            supperType = supperType.genericSuperclass
        }

        if (supperType !is ParameterizedType) {
            throw java.lang.RuntimeException("config must be parameterized")
        }

        val monitorType = supperType.actualTypeArguments[0] as Class<Monitor<M>>

        if (MONITOR_MAP.containsKey(monitorType)) {
            return@apply
        }

        val monitor = try {
            monitorType.getDeclaredField("INSTANCE").get(null) as Monitor<M>
        } catch (e: Throwable) {
            monitorType.newInstance() as Monitor<M>
        }

        MONITOR_MAP[monitorType] = monitor

        monitor.init(commonConfig, config)

        monitor.logMonitorEvent()
    }

    @JvmStatic
    fun getApplication() = commonConfig.application

    @Deprecated("Use Monitor Directly")
    @JvmStatic
    fun <M : Monitor<*>> getMonitor(clazz: Class<M>): M {
        return MONITOR_MAP[clazz] as M
    }

    @Deprecated("Use Monitor#isInitialized Directly")
    @JvmStatic
    fun <M : Monitor<*>> isInitialized(clazz: Class<M>): Boolean {
        return MONITOR_MAP[clazz] != null
    }

    @JvmStatic
    fun onApplicationCreate() {
        registerApplicationExtension()

        registerMonitorEventObserver()
    }

    private fun registerMonitorEventObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleEventObserver {
            private var mHasLogMonitorEvent = false

            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_START) {
                    logAllMonitorEvent()
                }
            }

            /**
             * **函数功能详细描述：**
             *
             * 该`logAllMonitorEvent`函数的主要目的是收集并记录所有的监控事件信息。
             *
             * 1. **条件判断与状态标记：**
             *    函数首先检查成员变量`mHasLogMonitorEvent`的值。如果为true，表示已执行过日志记录操作，此时直接返回以避免重复记录。否则，将其设置为true，表明即将进行一次日志记录。
             *
             * 2. **构建日志参数Map：**
             *    创建一个可变的`mutableMapOf<Any?, Any?>()`，然后使用`apply`函数遍历名为`MONITOR_MAP`的对象。对于`MONITOR_MAP`中的每个条目（可能是键值对或其他数据结构），通过调用其value部分的`getLogParams()`方法获取日志相关参数，并将这些参数以键值对的形式全部存入新创建的Map中。
             *
             * 3. **转换并记录日志：**
             *    使用`also`函数处理上一步得到的包含所有监控事件参数的Map。这里，它将整个Map对象转换成JSON格式，通过调用`JSONObject(it).toString()`实现。最后，将这个JSON格式化的字符串作为自定义状态事件的参数，调用`MonitorLogger.addCustomStatEvent("switch-stat", ...)`方法将事件记录到监控日志系统中，事件类型为"switch-stat"。
             *
             * 总结来说，此函数整合了多个监控点的日志参数，并将整合后的信息以自定义状态事件的形式一次性写入到监控日志中。
             */
            private fun logAllMonitorEvent() {
                if (mHasLogMonitorEvent) return
                mHasLogMonitorEvent = true

                mutableMapOf<Any?, Any?>().apply { MONITOR_MAP.forEach { putAll(it.value.getLogParams()) } }
                    .also {
                        MonitorLogger.addCustomStatEvent("switch-stat", JSONObject(it).toString())
                    }
            }
        })
    }

    private fun <C> Monitor<C>.logMonitorEvent() {
        if (!getApplication().isForeground) return

        mutableMapOf<Any?, Any?>().apply { putAll(this@logMonitorEvent.getLogParams()) }.also {
                MonitorLogger.addCustomStatEvent("switch-stat", JSONObject(it).toString())
            }
    }
}