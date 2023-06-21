//
// MIT License
//
// Copyright (c) 2023 Segment
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

package com.segment.analytics.kotlin.destinations.adobeanalytics

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.segment.analytics.kotlin.android.plugins.AndroidLifecycle
import com.segment.analytics.kotlin.core.*
import com.segment.analytics.kotlin.core.Analytics.Companion.debugLogsEnabled
import com.segment.analytics.kotlin.core.platform.DestinationPlugin
import com.segment.analytics.kotlin.core.platform.Plugin
import com.segment.analytics.kotlin.core.platform.plugins.logger.log
import kotlinx.serialization.Serializable

class AdobeAnalyticsDestination constructor(
    private val adobeAppID: String,
    private val adobeAnalyticsClient: DefaultAnalyticsClient = DefaultAnalyticsClient()
) :
    DestinationPlugin(), AndroidLifecycle {

    private val ADOBE_ANALYTICS_FULL_KEY = "Adobe Analytics"
    internal var adobeAnalyticsSettings: AdobeAnalyticsSettings? = null
    private var contextDataConfiguration: ContextDataConfiguration? = null
    internal lateinit var ecommerceAnalytics: EcommerceAnalytics
    internal lateinit var videoAnalytics: VideoAnalytics

    override val key: String = ADOBE_ANALYTICS_FULL_KEY

    override fun update(settings: Settings, type: Plugin.UpdateType) {
        super.update(settings, type)
        this.adobeAnalyticsSettings =
            settings.destinationSettings(key, AdobeAnalyticsSettings.serializer())
        if (type == Plugin.UpdateType.Initial) {
            adobeAnalyticsClient.initAdobeMobileCore(adobeAppID,analytics.configuration.application as Application,
                debugLogsEnabled)
            contextDataConfiguration = ContextDataConfiguration(
                adobeAnalyticsSettings?.customDataPrefix,
                adobeAnalyticsSettings?.contextValues
            )
            ecommerceAnalytics = EcommerceAnalytics(
                adobeAnalyticsClient,
                analytics,
                adobeAnalyticsSettings?.productIdentifier,
                contextDataConfiguration!!
            )
            videoAnalytics = VideoAnalytics(
                analytics.configuration.application as Context,
                contextDataConfiguration!!,
                analytics )
        }
    }

    override fun identify(payload: IdentifyEvent): BaseEvent {
        val userId: String = payload.userId
        if (userId.isEmpty()) {
            return payload
        }
        adobeAnalyticsClient.setVisitorIdentifier(userId)
        analytics.log("Analytics.setVisitorIdentifier($userId)")
        return payload
    }

    override fun screen(payload: ScreenEvent): BaseEvent {
        val properties: Properties = payload.properties
        if (properties.isEmpty()) {
            adobeAnalyticsClient.trackState(payload.name, null)
            analytics.log("MobileCore.trackState(${payload.name},null)")
            return payload
        }

        val contextDataMap: Map<String, String> = getContextData(payload, payload.properties)
        adobeAnalyticsClient.trackState(payload.name, contextDataMap)
        analytics.log("MobileCore.trackState(${payload.name}, $contextDataMap)")
        return payload
    }

    override fun track(payload: TrackEvent): BaseEvent {
        // if event is for ecommerce event
        if (EcommerceAnalytics.EventEnum.isEcommerceEvent(payload.event)) {
            if (adobeAnalyticsSettings?.eventsV2 != null &&
                adobeAnalyticsSettings?.eventsV2!!.containsKey(payload.event)) {
                analytics.log(
                    "Segment currently does not support mapping spec ecommerce events to "
                            + "custom Adobe events."
                )
                return payload
            }
            ecommerceAnalytics.track(payload)
            return payload
        }

        // if event is for video event
        if (VideoAnalytics.EventVideoEnum.isVideoEvent(payload.event)) {
            videoAnalytics.track(payload)
            return payload
        }
        if (adobeAnalyticsSettings?.eventsV2 == null ||
            adobeAnalyticsSettings?.eventsV2!!.isEmpty() ||
            !adobeAnalyticsSettings?.eventsV2!!.containsKey(payload.event)
        ) {
            analytics.log("Event must be either configured in Adobe and in the Segment EventsV2 setting, a reserved Adobe Ecommerce or Video event.")
            return payload
        }
        val event: String = adobeAnalyticsSettings?.eventsV2!![payload.event].toString()
        val contextDataMap: Map<String, String> = getContextData(payload, payload.properties)

        adobeAnalyticsClient.trackAction(event, contextDataMap)
        analytics.log("MobileCore.trackAction(${payload.event}, $contextDataMap)")
        return payload
    }

    override fun reset() {
        super.reset()
        adobeAnalyticsClient.setVisitorIdentifier(null)
        analytics.log("Analytics.setVisitorIdentifier(null)")
    }

    override fun flush() {
        super.flush()
        adobeAnalyticsClient.flushQueue()
        analytics.log("Analytics.sendQueuedHits()")
    }

    /**
     * AndroidActivity Lifecycle Methods
     */
    override fun onActivityResumed(activity: Activity?) {
        super.onActivityResumed(activity)
        adobeAnalyticsClient.lifecycleStart(null)
        analytics.log("MobileCore.lifecycleStart()")
    }

    override fun onActivityPaused(activity: Activity?) {
        super.onActivityPaused(activity)
        adobeAnalyticsClient.lifecyclePause()
        analytics.log("MobileCore.lifecyclePause()")
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        super.onActivityCreated(activity, savedInstanceState)
        adobeAnalyticsClient.setApplication(activity!!.application)
        analytics.log("MobileCore.lifecyclePause()")
    }

    private fun getContextData(event: BaseEvent, properties: Properties): Map<String, String> {
        val extraProperties = properties.asStringMap() as MutableMap

        // Remove products just in case
        extraProperties.remove("products")

        val contextData: MutableMap<String, String> = mutableMapOf()
        for (field in contextDataConfiguration?.eventFieldNames!!) {
            var value: Any? = null
            try {
                value = contextDataConfiguration!!.searchValue(field, event)
            } catch (e: IllegalArgumentException) {
                if (debugLogsEnabled) {
                    e.printStackTrace()
                }
            }
            if (value != null) {
                val variable = contextDataConfiguration!!.getVariableName(field)
                if (!variable.isNullOrEmpty()) {
                    contextData[variable] = value as String
                }
                extraProperties.remove(field)
            }
        }

        // Add all extra properties
        for (extraProperty in extraProperties.keys) {
            if (extraProperty.isNotEmpty()) {
                val variable: String = contextDataConfiguration!!.getPrefix() + extraProperty
                if (variable.isNotEmpty()) {
                    contextData[variable] = extraProperties[extraProperty] ?: ""
                }
            }
        }
        return if (contextData.isEmpty()) {
            mapOf()
        } else {
            contextData.toMap()
        }
    }
}

/**
 * Adobe Analytics Settings data class.
 */
@Serializable
data class AdobeAnalyticsSettings(
//    Adobe SSL boolean
    var ssl: Boolean = false,
//    Adobe Product Identifier
    var productIdentifier: String,
//    Adobe eventsV2 Map which are mapped with Adobe Events in Segment.
    var eventsV2: Map<String, String>,
//    Adobe Context Values in form of Map
    var contextValues: Map<String, String>,
//    Adobe custom data prefix
    var customDataPrefix: String
)
