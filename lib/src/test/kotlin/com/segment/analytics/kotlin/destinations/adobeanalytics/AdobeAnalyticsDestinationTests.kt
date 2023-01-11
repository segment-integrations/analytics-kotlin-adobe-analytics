package com.segment.analytics.kotlin.destinations.adobeanalytics

import android.app.Activity
import android.app.Application
import android.content.Context
import com.segment.analytics.kotlin.core.*
import com.segment.analytics.kotlin.core.platform.Plugin
import com.segment.analytics.kotlin.core.utilities.LenientJson
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkClass
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AdobeAnalyticsDestinationTests {
    @MockK
    lateinit var mockApplication: Application
    @MockK
    lateinit var mockedContext: Context

    @MockK(relaxUnitFun = true)
    lateinit var mockedAnalytics: Analytics

    @MockK(relaxUnitFun = true)
    lateinit var mockedDefaultAdobeAnalyticsClient: DefaultAnalyticsClient

    @MockK(relaxUnitFun = true)
    lateinit var mockedVideoAnalytics: VideoAnalytics

    @MockK(relaxUnitFun = true)
    lateinit var mockedEcommerceAnalytics: EcommerceAnalytics

    lateinit var mockedAdobeAnalyticsDestination: AdobeAnalyticsDestination
    private val sampleAdobeAnalyticsSettings: Settings = LenientJson.decodeFromString(
        """
            {
              "integrations": {
                "Adobe Analytics": {
                  "contextValues": {
                    "testing": "myapp.testing.Testing",
                    ".context.library": "myapp.library"
                    },
                    "customDataPrefix": "myapp.",
                    "productIdentifier": "name",
                    "enableTrackPageName": true,
                    "eventsV2": {
                      "TestEvent": "event1"
                    }
                }
              }
            }
        """.trimIndent()
    )
    init {
        MockKAnnotations.init(this)
    }

    @Before
    fun setUp() {
        mockedAdobeAnalyticsDestination = AdobeAnalyticsDestination("Adobe_APP_ID", mockedDefaultAdobeAnalyticsClient)
        every { mockedAnalytics.configuration.application } returns mockApplication
        every { mockApplication.applicationContext } returns mockedContext
        every { mockApplication.packageName } returns "unknown"
        mockedAnalytics.configuration.application = mockedContext
        mockedAdobeAnalyticsDestination.analytics = mockedAnalytics
        mockedAdobeAnalyticsDestination.ecommerceAnalytics = mockedEcommerceAnalytics
        mockedAdobeAnalyticsDestination.videoAnalytics = mockedVideoAnalytics
    }

    @Test
    fun `settings are updated correctly`() {
        // An Adobe Analytics example settings
        val adobeAnalyticsSettings: Settings = sampleAdobeAnalyticsSettings
        mockedAdobeAnalyticsDestination.update(adobeAnalyticsSettings, Plugin.UpdateType.Initial)

        /* assertions Adobe config */
        Assertions.assertNotNull(mockedAdobeAnalyticsDestination.adobeAnalyticsSettings)
        with(mockedAdobeAnalyticsDestination.adobeAnalyticsSettings!!) {
            assertEquals(mockedAdobeAnalyticsDestination.adobeAnalyticsSettings!!.productIdentifier, "name")
            assertEquals(mockedAdobeAnalyticsDestination.adobeAnalyticsSettings!!.customDataPrefix, "myapp.")
            assertEquals(mockedAdobeAnalyticsDestination.adobeAnalyticsSettings!!.eventsV2.size, 1)
            assertEquals(mockedAdobeAnalyticsDestination.adobeAnalyticsSettings!!.contextValues.size, 2)
        }
    }

    @Test
    fun `onActivityResumed() handled correctly`() {
        mockedAdobeAnalyticsDestination.onActivityResumed(mockkClass(Activity::class))
        verify { mockedDefaultAdobeAnalyticsClient.lifecycleStart(null) }
    }

    @Test
    fun `onActivityPaused() handled correctly`() {
        mockedAdobeAnalyticsDestination.onActivityPaused(mockkClass(Activity::class))
        verify { mockedDefaultAdobeAnalyticsClient.lifecyclePause() }
    }

    @Test
    fun `reset is handled correctly`() {
        mockedAdobeAnalyticsDestination.reset()
        verify { mockedDefaultAdobeAnalyticsClient.setVisitorIdentifier(null) }
    }

    @Test
    fun `flush is handled correctly`() {
        mockedAdobeAnalyticsDestination.flush()
        verify { mockedDefaultAdobeAnalyticsClient.flushQueue() }
    }

    @Test
    fun `identify is handled correctly with userId`() {
        val sampleIdentifyEvent = IdentifyEvent(
            userId = "adobe-UserID-123",
            traits = buildJsonObject {
            }
        )
        val identifyEvent = mockedAdobeAnalyticsDestination.identify(sampleIdentifyEvent)
        Assertions.assertNotNull(identifyEvent)
        verify { mockedDefaultAdobeAnalyticsClient.setVisitorIdentifier("adobe-UserID-123") }
    }

    @Test
    fun `identify is handled correctly with empty userId`() {
        val sampleIdentifyEvent = IdentifyEvent(
            userId = "",
            traits = buildJsonObject {
            }
        )
        val identifyEvent = mockedAdobeAnalyticsDestination.identify(sampleIdentifyEvent)
        Assertions.assertNotNull(identifyEvent)
        Assertions.assertEquals(identifyEvent.userId, "")
    }

    @Test
    fun `screen is handled correctly`() {
        val sampleEvent = ScreenEvent(
            name = "Viewed a Screen",
            category ="",
            properties = buildJsonObject {
            }
        )
        mockedAdobeAnalyticsDestination.screen(sampleEvent)
        verify { mockedDefaultAdobeAnalyticsClient.trackState("Viewed a Screen", null) }
    }

    @Test
    fun `screen is handled correctly with context data`() {
        // An Adobe Analytics example settings
        val adobeAnalyticsSettings: Settings = sampleAdobeAnalyticsSettings
        mockedAdobeAnalyticsDestination.update(adobeAnalyticsSettings, Plugin.UpdateType.Initial)

        val sampleEvent = ScreenEvent(
            name = "Viewed a Screen",
            category ="",
            properties = buildJsonObject {
                put("testing", "testing value")
            }
        )
        mockedAdobeAnalyticsDestination.screen(sampleEvent)

        val contextData: MutableMap<String, String> = HashMap()
        contextData["myapp.testing.Testing"] = "testing value"
        verify { mockedDefaultAdobeAnalyticsClient.trackState("Viewed a Screen", contextData) }
    }

    @Test
    fun `track is handled correctly`() {
        // An Adobe Analytics example settings
        val adobeAnalyticsSettings: Settings = sampleAdobeAnalyticsSettings
        mockedAdobeAnalyticsDestination.update(adobeAnalyticsSettings, Plugin.UpdateType.Initial)
        val sampleEvent = TrackEvent(
            event = "TestEvent",
            properties = buildJsonObject {
            }
        )
        mockedAdobeAnalyticsDestination.track(sampleEvent)
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction("event1",  mapOf<String, String>())}
    }

    @Test
    fun `track is handled correctly for Video Event`() {
        val sampleEvent = TrackEvent(
            event = "Video Playback Started",
            properties = buildJsonObject {
            }
        ).apply {
            userId = "user_UserID-123"
            anonymousId = "anonymous_UserID-123"
            messageId = "messageID-123"
            context = emptyJsonObject
            integrations = emptyJsonObject
            timestamp = "2023-01-10T01:59:09"
        }
        mockedAdobeAnalyticsDestination.track(sampleEvent)
        verify {  mockedVideoAnalytics.track(sampleEvent)}
    }

    @Test
    fun `track is handled correctly for Ecommerce Event`() {
        val sampleEvent = TrackEvent(
            event = "Product Added",
            properties = buildJsonObject {
            }
        ).apply {
            userId = "user_UserID-123"
            anonymousId = "anonymous_UserID-123"
            messageId = "messageID-123"
            context = emptyJsonObject
            integrations = emptyJsonObject
            timestamp = "2023-01-10T01:59:09"
        }
        mockedAdobeAnalyticsDestination.track(sampleEvent)
        verify {  mockedEcommerceAnalytics.track(sampleEvent)}
    }

    @Test
    fun `track is handled correctly with Context Data and extra value`() {
        // An Adobe Analytics example settings
        val adobeAnalyticsSettings: Settings = sampleAdobeAnalyticsSettings
        mockedAdobeAnalyticsDestination.update(adobeAnalyticsSettings, Plugin.UpdateType.Initial)

        val sampleEvent = TrackEvent(
            event = "TestEvent",
            properties = buildJsonObject {
                put("testing", "testing value")
                put("extra", "extra value")
            },
        )
        mockedAdobeAnalyticsDestination.track(sampleEvent)
        val contextData: MutableMap<String, String> = mutableMapOf()
        contextData["myapp.testing.Testing"] = "testing value"
        contextData["myapp.extra"] = "extra value"
        verify { mockedDefaultAdobeAnalyticsClient.trackAction("event1", contextData) }
    }

    @Test
    fun `track is handled correctly with Context Data with extra value and invalid prefix`() {
        // An Adobe Analytics example settings
        val adobeAnalyticsSettings: Settings  = LenientJson.decodeFromString(
            """
            {
              "integrations": {
                "Adobe Analytics": {
                  "contextValues": {
                    "testing": "myapp.testing.Testing"
                    },
                    "customDataPrefix": "a.",
                    "productIdentifier": "name",
                    "enableTrackPageName": true,
                    "eventsV2": {
                      "TestEvent": "event1"
                    }
                }
              }
            }
        """.trimIndent()
        )
        mockedAdobeAnalyticsDestination.update(adobeAnalyticsSettings, Plugin.UpdateType.Initial)

        val sampleEvent = TrackEvent(
            event = "TestEvent",
            properties = buildJsonObject {
                put("testing", "testing value")
                put("extra", "extra value")
            },
        )
        mockedAdobeAnalyticsDestination.track(sampleEvent)
        val contextData: MutableMap<String, String> = mutableMapOf()
        contextData["myapp.testing.Testing"] = "testing value"
        contextData["extra"] = "extra value"
        verify { mockedDefaultAdobeAnalyticsClient.trackAction("event1", contextData) }
    }

    @Test
    fun `track is handled correctly with Context Data with extra value and empty prefix`() {
        // An Adobe Analytics example settings
        val adobeAnalyticsSettings: Settings  = LenientJson.decodeFromString(
            """
            {
              "integrations": {
                "Adobe Analytics": {
                  "contextValues": {
                    "testing": "myapp.testing.Testing"
                    },
                    "customDataPrefix": "",
                    "productIdentifier": "name",
                    "enableTrackPageName": true,
                    "eventsV2": {
                      "TestEvent": "event1"
                    }
                }
              }
            }
        """.trimIndent()
        )
        mockedAdobeAnalyticsDestination.update(adobeAnalyticsSettings, Plugin.UpdateType.Initial)

        val sampleEvent = TrackEvent(
            event = "TestEvent",
            properties = buildJsonObject {
                put("testing", "testing value")
                put("extra", "extra value")
            },
        )
        mockedAdobeAnalyticsDestination.track(sampleEvent)
        val contextData: MutableMap<String, String> = mutableMapOf()
        contextData["myapp.testing.Testing"] = "testing value"
        contextData["extra"] = "extra value"
        verify { mockedDefaultAdobeAnalyticsClient.trackAction("event1", contextData) }
    }

}