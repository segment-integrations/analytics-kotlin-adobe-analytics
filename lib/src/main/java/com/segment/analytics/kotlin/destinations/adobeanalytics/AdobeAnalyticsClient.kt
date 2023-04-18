package com.segment.analytics.kotlin.destinations.adobeanalytics

import android.app.Application
import com.adobe.marketing.mobile.Analytics
import com.adobe.marketing.mobile.Identity
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.Media
import com.adobe.marketing.mobile.MobileCore

internal interface AdobeAnalyticsClient {
    fun initAdobeMobileCore(
        adobeAppId: String,
        application: Application,
        debugLogging: Boolean = false
    )

    fun setApplication(application: Application)

    fun setVisitorIdentifier(identifier: String?)

    fun lifecycleStart(contextData: Map<String, String>?)

    fun lifecyclePause()

    fun trackAction(action: String, contextData: Map<String, String>?)

    fun trackState(state: String, contextData: Map<String, String>?)

    fun flushQueue()
}


/**
 * Default implementation of Adobe Analytics client.
 */
class DefaultAnalyticsClient : AdobeAnalyticsClient{
    override fun initAdobeMobileCore(
        adobeAppId: String,
        application: Application,
        debugLogging: Boolean
    ) {
        MobileCore.setApplication(application)
        MobileCore.configureWithAppID(adobeAppId)
        val extensions = listOf(Media.EXTENSION, Analytics.EXTENSION, Identity.EXTENSION)
        MobileCore.registerExtensions(extensions) {
//            AEP Mobile SDK is initialized
        }
        if (debugLogging) {
            MobileCore.setLogLevel(LoggingMode.DEBUG)
        }
    }

    override fun setApplication(application: Application) {
        MobileCore.setApplication(application)
    }

    override fun setVisitorIdentifier(identifier: String?) {
        Analytics.setVisitorIdentifier(identifier)
    }

    override fun lifecycleStart(contextData: Map<String, String>?) {
        MobileCore.lifecycleStart(contextData)
    }
    override fun lifecyclePause() {
        MobileCore.lifecyclePause()
    }

    override fun trackAction(action: String, contextData: Map<String, String>?) {
        MobileCore.trackAction(action, contextData)
    }

    override fun trackState(state: String, contextData: Map<String, String>?) {
        MobileCore.trackState(state, contextData)
    }

    override fun flushQueue() {
        Analytics.sendQueuedHits()
    }
}