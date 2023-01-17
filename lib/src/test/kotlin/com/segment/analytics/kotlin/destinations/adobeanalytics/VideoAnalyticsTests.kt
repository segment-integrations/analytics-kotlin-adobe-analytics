package com.segment.analytics.kotlin.destinations.adobeanalytics

import android.content.Context
import com.adobe.marketing.mobile.Media
import com.adobe.marketing.mobile.MediaConstants
import com.adobe.marketing.mobile.MediaConstants.AdMetadataKeys.ADVERTISER
import com.adobe.marketing.mobile.MediaTracker
import com.segment.analytics.kotlin.core.Analytics
import com.segment.analytics.kotlin.core.TrackEvent
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class VideoAnalyticsTests {
    @MockK(relaxUnitFun = true)
    lateinit var mockedAnalytics: Analytics

    @MockK(relaxUnitFun = true)
    lateinit var mockedMediaTrackerFactory: VideoAnalytics.MediaTrackerFactory


    @MockK(relaxUnitFun = true)
    lateinit var mockedMediaTracker: MediaTracker

    @MockK
    lateinit var mockedContext: Context

    private lateinit var mockedVideoAnalytics: VideoAnalytics

    init {
        MockKAnnotations.init(this)
    }

    @Before
    fun setUp() {
        val config = ContextDataConfiguration(null, mapOf())
        every { mockedContext.packageName } returns "unknown"
        every { mockedMediaTrackerFactory.get(any()) } returns mockedMediaTracker

        mockedVideoAnalytics = VideoAnalytics(mockedContext, config, mockedMediaTrackerFactory, mockedAnalytics)
    }

    @Test
    fun `track for Video Playback Started handled correctly`() {
        val variables: MutableMap<String, String> = HashMap()
        variables["random metadata"] = "adobe.random"
        mockedVideoAnalytics.contextDataConfiguration = ContextDataConfiguration("", variables)
        startVideoSession()

        val mediaMap: MutableMap<String, String> = HashMap()
        mediaMap[MediaConstants.VideoMetadataKeys.ASSET_ID] = "123"
        mediaMap[MediaConstants.VideoMetadataKeys.SHOW] = "Program 1"
        mediaMap[MediaConstants.VideoMetadataKeys.GENRE] = "Fiction"
        mediaMap[MediaConstants.VideoMetadataKeys.SEASON] = "1"
        mediaMap[MediaConstants.VideoMetadataKeys.FIRST_AIR_DATE] = "2023"
        mediaMap[MediaConstants.VideoMetadataKeys.STREAM_FORMAT] = MediaConstants.StreamType.VOD
        mediaMap[MediaConstants.VideoMetadataKeys.EPISODE] = "1"
        mediaMap[MediaConstants.VideoMetadataKeys.NETWORK] = "Channel 1"

        val contextData: MutableMap<String, String> = HashMap()
        contextData["adobe.random"] = "something random"

        verify {  mockedMediaTracker.trackSessionStart(mediaMap.toMap(), contextData)}
    }

    @Test
    fun `track for Video Playback Paused handled correctly`() {
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.PlaybackPaused.eventName,
            properties = buildJsonObject {
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        verify {  mockedMediaTracker.trackPause()}
    }

    @Test
    fun `track for Video Playback Resumed handled correctly`() {
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.PlaybackResumed.eventName,
            properties = buildJsonObject {
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        verify {  mockedMediaTracker.trackPlay()}
    }

    @Test
    fun `track for Video Content Started handled correctly`() {
        val variables: MutableMap<String, String> = java.util.HashMap()
        variables["title"] = "adobe.title"
        mockedVideoAnalytics.contextDataConfiguration = ContextDataConfiguration("", variables)
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.ContentStarted.eventName,
            properties = buildJsonObject {
                put("title", "Video 1")
                put("contentAssetId", "123")
                put("totalLength", 100.0)
                put("startTime", 10.0)
                put("indexPosition", 1L)
                put("position", 35)
                put("assetId", "123")//
                put("program", "Program 1")
                put("season", "1")
                put("episode", "1")
                put("genre", "Fiction")
                put("channel", "Channel 1")
                put("airdate", "2023")
                put("livestream", false)
                put("publisher", "Publisher 1")
                put("rating", "MA")

            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = java.util.HashMap()
        contextData["adobe.title"] = "Video 1"

        val mediaMap: MutableMap<String, String> = java.util.HashMap()
        mediaMap[MediaConstants.VideoMetadataKeys.ASSET_ID] = "123"
        mediaMap[MediaConstants.VideoMetadataKeys.SHOW] = "Program 1"
        mediaMap[MediaConstants.VideoMetadataKeys.GENRE] = "Fiction"
        mediaMap[MediaConstants.VideoMetadataKeys.SEASON] = "1"
        mediaMap[MediaConstants.VideoMetadataKeys.FIRST_AIR_DATE] = "2023"
        mediaMap[MediaConstants.VideoMetadataKeys.STREAM_FORMAT] = MediaConstants.StreamType.VOD
        mediaMap[MediaConstants.VideoMetadataKeys.ORIGINATOR] = "Publisher 1"
        mediaMap[MediaConstants.VideoMetadataKeys.EPISODE] = "1"
        mediaMap[MediaConstants.VideoMetadataKeys.RATING] = "MA"
        mediaMap[MediaConstants.VideoMetadataKeys.NETWORK] = "Channel 1"
        verify {  mockedMediaTracker.updateCurrentPlayhead(35.0)}
        verify {  mockedMediaTracker.trackPlay()}
        verify {  mockedMediaTracker.trackEvent(
            Media.Event.ChapterStart,
            mediaMap as Map<String, Any>?,
            contextData
        )}
    }

    @Test
    fun `track for Video Content Started with snake_case keys handled correctly`() {
        val variables: MutableMap<String, String> = java.util.HashMap()
        variables["title"] = "adobe.title"
        mockedVideoAnalytics.contextDataConfiguration = ContextDataConfiguration("", variables)
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.ContentStarted.eventName,
            properties = buildJsonObject {
                put("title", "Video 1")
                put("content_asset_id", "123")
                put("total_length", 100.0)
                put("start_time", 10.0)
                put("index_position", 1L)
                put("position", 35)
                put("asset_id", "123")//
                put("program", "Program 1")
                put("season", "1")
                put("episode", "1")
                put("genre", "Fiction")
                put("channel", "Channel 1")
                put("airdate", "2023")
                put("livestream", false)
                put("publisher", "Publisher 1")
                put("rating", "MA")

            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = java.util.HashMap()
        contextData["adobe.title"] = "Video 1"

        val mediaMap: MutableMap<String, String> = java.util.HashMap()
        mediaMap[MediaConstants.VideoMetadataKeys.ASSET_ID] = "123"
        mediaMap[MediaConstants.VideoMetadataKeys.SHOW] = "Program 1"
        mediaMap[MediaConstants.VideoMetadataKeys.GENRE] = "Fiction"
        mediaMap[MediaConstants.VideoMetadataKeys.SEASON] = "1"
        mediaMap[MediaConstants.VideoMetadataKeys.FIRST_AIR_DATE] = "2023"
        mediaMap[MediaConstants.VideoMetadataKeys.STREAM_FORMAT] = MediaConstants.StreamType.VOD
        mediaMap[MediaConstants.VideoMetadataKeys.ORIGINATOR] = "Publisher 1"
        mediaMap[MediaConstants.VideoMetadataKeys.EPISODE] = "1"
        mediaMap[MediaConstants.VideoMetadataKeys.RATING] = "MA"
        mediaMap[MediaConstants.VideoMetadataKeys.NETWORK] = "Channel 1"
        verify {  mockedMediaTracker.updateCurrentPlayhead(35.0)}
        verify {  mockedMediaTracker.trackPlay()}
        verify {  mockedMediaTracker.trackEvent(
            Media.Event.ChapterStart,
            mediaMap as Map<String, Any>?,
            contextData
        )}
    }

    @Test
    fun `track for Video Content Started with extra properties handled correctly`() {
        val contextDataVariables: MutableMap<String, String> = java.util.HashMap()
        contextDataVariables["title"] = "adobe.title"
        contextDataVariables[".context.context1"] = "adobe.context1"
        mockedVideoAnalytics.contextDataConfiguration = ContextDataConfiguration("", contextDataVariables)
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.ContentStarted.eventName,
            properties = buildJsonObject {
                put("title", "Video 1")
                put("contentAssetId", "123")
                put("totalLength", 100.0)
                put("startTime", 10.0)
                put("indexPosition", 1L)
                put("position", 35)
                put("assetId", "123")//
                put("program", "Program 1")
                put("season", "1")
                put("episode", "1")
                put("genre", "Fiction")
                put("channel", "Channel 1")
                put("airdate", "2023")
                put("livestream", false)
                put("publisher", "Publisher 1")
                put("rating", "MA")
                put("extra", "extra value")
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
            context = buildJsonObject {
                put("context1", "Context 1 Value")
            }
        }
        mockedVideoAnalytics.track(sampleEvent)

        val contextData: MutableMap<String, String> = java.util.HashMap()
        contextData["adobe.title"] = "Video 1"
        contextData["extra"] = "extra value"
        contextData["adobe.context1"] = "Context 1 Value"

        val mediaMap: MutableMap<String, String> = java.util.HashMap()
        mediaMap[MediaConstants.VideoMetadataKeys.ASSET_ID] = "123"
        mediaMap[MediaConstants.VideoMetadataKeys.SHOW] = "Program 1"
        mediaMap[MediaConstants.VideoMetadataKeys.GENRE] = "Fiction"
        mediaMap[MediaConstants.VideoMetadataKeys.SEASON] = "1"
        mediaMap[MediaConstants.VideoMetadataKeys.FIRST_AIR_DATE] = "2023"
        mediaMap[MediaConstants.VideoMetadataKeys.STREAM_FORMAT] = MediaConstants.StreamType.VOD
        mediaMap[MediaConstants.VideoMetadataKeys.ORIGINATOR] = "Publisher 1"
        mediaMap[MediaConstants.VideoMetadataKeys.EPISODE] = "1"
        mediaMap[MediaConstants.VideoMetadataKeys.RATING] = "MA"
        mediaMap[MediaConstants.VideoMetadataKeys.NETWORK] = "Channel 1"
        verify {  mockedMediaTracker.updateCurrentPlayhead(35.0)}
        verify {  mockedMediaTracker.trackPlay()}
        verify {  mockedMediaTracker.trackEvent(
            Media.Event.ChapterStart,
            mediaMap as Map<String, Any>?,
            contextData
        )}
    }

    @Test
    fun `track for Video Content Completed handled correctly`() {
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.ContentCompleted.eventName,
            properties = buildJsonObject {
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        verify {  mockedMediaTracker.trackEvent(Media.Event.ChapterComplete, mapOf(), null)}
    }

    @Test
    fun `track for Video Playback Completed handled correctly`() {
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.PlaybackCompleted.eventName,
            properties = buildJsonObject {
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        verify {  mockedMediaTracker.trackComplete()}
        verify {  mockedMediaTracker.trackSessionEnd()}
    }

    @Test
    fun `track for Video Buffer Started handled correctly`() {
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.PlaybackBufferStarted.eventName,
            properties = buildJsonObject {
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        verify {  mockedMediaTracker.trackPause()}
        verify {  mockedMediaTracker.trackEvent(Media.Event.BufferStart, mapOf(), null)}
    }

    @Test
    fun `track for Video Buffer Completed handled correctly`() {
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.PlaybackBufferCompleted.eventName,
            properties = buildJsonObject {
                put("seekPosition", 45)
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        verify {  mockedMediaTracker.trackPlay()}
        verify {  mockedMediaTracker.updateCurrentPlayhead(45.0)}
        verify {  mockedMediaTracker.trackEvent(Media.Event.BufferComplete, mapOf(), null)}
    }

    @Test
    fun `track for Video Seek Started handled correctly`() {
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.PlaybackSeekStarted.eventName,
            properties = buildJsonObject {
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        verify {  mockedMediaTracker.trackPause()}
        verify {  mockedMediaTracker.trackEvent(Media.Event.SeekStart, mapOf(), null)}
    }

    @Test
    fun `track for Video Seek Completed handled correctly`() {
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.PlaybackSeekCompleted.eventName,
            properties = buildJsonObject {
                put("seekPosition", 45)
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        verify {  mockedMediaTracker.trackPlay()}
        verify {  mockedMediaTracker.updateCurrentPlayhead(45.0)}
        verify {  mockedMediaTracker.trackEvent(Media.Event.SeekComplete, mapOf(), null)}
    }

    @Test
    fun `track for Ad Break Started handled correctly`() {
        val variables: MutableMap<String, String> = java.util.HashMap()
        variables["contextValue"] = "adobe.context.value"
        mockedVideoAnalytics.contextDataConfiguration = ContextDataConfiguration("", variables)
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.AdBreakStarted.eventName,
            properties = buildJsonObject {
                put("title", "Ad 1")
                put("startTime", 10.0)
                put("indexPosition", 1L)
                put("contextValue", "value")

            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = java.util.HashMap()
        contextData["adobe.context.value"] = "value"
        val adBreakObject = Media.createAdBreakObject("Ad 1", 1L, 10.0)
        verify {  mockedMediaTracker.trackEvent(Media.Event.AdBreakStart, adBreakObject, contextData)}
    }

    @Test
    fun `track for Ad Break Started with snake_case keys handled correctly`() {
        val variables: MutableMap<String, String> = java.util.HashMap()
        variables["context_value"] = "adobe.context.value"
        mockedVideoAnalytics.contextDataConfiguration = ContextDataConfiguration("", variables)
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.AdBreakStarted.eventName,
            properties = buildJsonObject {
                put("title", "Ad 1")
                put("start_time", 10.0)
                put("index_position", 1L)
                put("context_value", "value")

            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = java.util.HashMap()
        contextData["adobe.context.value"] = "value"
        val adBreakObject = Media.createAdBreakObject("Ad 1", 1L, 10.0)
        verify {  mockedMediaTracker.trackEvent(Media.Event.AdBreakStart, adBreakObject, contextData)}
    }

    @Test
    fun `track for Ad Break Completed handled correctly`() {
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.AdBreakCompleted.eventName,
            properties = buildJsonObject {
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        verify {  mockedMediaTracker.trackEvent(Media.Event.AdBreakComplete, mapOf(), null)}
    }

    @Test
    fun `track for Video Ad Break Started handled correctly`() {
        val variables: MutableMap<String, String> = java.util.HashMap()
        variables["title"] = "adobe.title"

        mockedVideoAnalytics.contextDataConfiguration = ContextDataConfiguration("myapp.", variables)
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.AdStarted.eventName,
            properties = buildJsonObject {
                put("title", "Video Ad 1")
                put("assetId", "123")
                put("totalLength", 10.0)
                put("indexPosition", 1L)
                put("publisher", "Publisher 1")
                put("extra", "extra value")

            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }

        mockedVideoAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = java.util.HashMap()
        contextData["adobe.title"] = "Video Ad 1"
        contextData["myapp.extra"] = "extra value"

        val mediaMap: MutableMap<String, String> = java.util.HashMap()
        mediaMap[ADVERTISER] = "Publisher 1"
        verify {  mockedMediaTracker.trackEvent(Media.Event.AdStart,
            mediaMap as Map<String, Any>?, contextData)}
    }

    @Test
    fun `track for Video Ad Break Started with snake_case handled correctly`() {
        val variables: MutableMap<String, String> = java.util.HashMap()
        variables["title"] = "adobe.title"

        mockedVideoAnalytics.contextDataConfiguration = ContextDataConfiguration("myapp.", variables)
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.AdStarted.eventName,
            properties = buildJsonObject {
                put("title", "Video Ad 1")
                put("asset_id", "123")
                put("total_length", 10.0)
                put("index_position", 1L)
                put("publisher", "Publisher 1")
                put("extra", "extra value")

            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }

        mockedVideoAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = java.util.HashMap()
        contextData["adobe.title"] = "Video Ad 1"
        contextData["myapp.extra"] = "extra value"

        val mediaMap: MutableMap<String, String> = java.util.HashMap()
        mediaMap[ADVERTISER] = "Publisher 1"
        verify {  mockedMediaTracker.trackEvent(Media.Event.AdStart,
            mediaMap as Map<String, Any>?, contextData)}
    }

    @Test
    fun `track for Video Ad Break Skipped handled correctly`() {
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.AdSkipped.eventName,
            properties = buildJsonObject {
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        verify {  mockedMediaTracker.trackEvent(Media.Event.AdSkip, mapOf(), null)}
    }

    @Test
    fun `track for Video Ad Break Completed handled correctly`() {
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.AdCompleted.eventName,
            properties = buildJsonObject {
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        verify {  mockedMediaTracker.trackEvent(Media.Event.AdComplete, mapOf(), null)}
    }

    @Test
    fun `track for Video playback interrupted handled correctly`() {
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.PlaybackInterrupted.eventName,
            properties = buildJsonObject {
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        verify {  mockedMediaTracker.trackPause()}
    }

    @Test
    fun `track for Video Quality updated handled correctly`() {
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.QualityUpdated.eventName,
            properties = buildJsonObject {
                put("bitrate", 15000)
                put("startupTime", 1)
                put("fps", 60)
                put("droppedFrames", 2)
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        val mockedMediaQoeObject = Media.createQoEObject(15000, 1.0, 60.0, 2L)
        verify {  mockedMediaTracker.updateQoEObject(mockedMediaQoeObject)}
    }

    @Test
    fun `track for Video Quality updated with snake_case handled correctly`() {
        startVideoSession()
        val sampleEvent = TrackEvent(
            event = VideoAnalytics.EventVideoEnum.QualityUpdated.eventName,
            properties = buildJsonObject {
                put("bitrate", 15000)
                put("startup_time", 1)
                put("fps", 60)
                put("dropped_frames", 2)
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
        val mockedMediaQoeObject = Media.createQoEObject(15000, 1.0, 60.0, 2L)
        verify {  mockedMediaTracker.updateQoEObject(mockedMediaQoeObject)}
    }

    private fun startVideoSession() {
        val eventEnum = VideoAnalytics.EventVideoEnum.PlaybackStarted

        val sampleEvent = TrackEvent(
            event = eventEnum.eventName,
            properties = buildJsonObject {
                put("title", "Video 1")
                put("contentAssetId", "123")
                put("totalLength", 100.0)
                put("assetId", "123")
                put("program", "Program 1")
                put("season", "1")
                put("episode", "1")
                put("genre", "Fiction")
                put("channel", "Channel 1")
                put("airdate", "2023")
                put("livestream", false)
                put("random metadata", "something random")

            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedVideoAnalytics.track(sampleEvent)
    }


}