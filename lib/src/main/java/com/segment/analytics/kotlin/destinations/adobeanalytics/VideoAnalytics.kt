package com.segment.analytics.kotlin.destinations.adobeanalytics

import android.content.Context
import com.adobe.marketing.mobile.Media
import com.adobe.marketing.mobile.MediaConstants
import com.adobe.marketing.mobile.MediaConstants.AdMetadataKeys.ADVERTISER
import com.adobe.marketing.mobile.MediaConstants.VideoMetadataKeys.*
import com.adobe.marketing.mobile.MediaTracker
import com.segment.analytics.kotlin.core.Analytics
import com.segment.analytics.kotlin.core.Properties
import com.segment.analytics.kotlin.core.TrackEvent
import com.segment.analytics.kotlin.core.platform.plugins.logger.log
import kotlinx.serialization.json.JsonObject


/**
 * Generate events for all video actions.
 */
internal class VideoAnalytics(
    context: Context,
    contextDataConfiguration: ContextDataConfiguration,
    private val mediaTrackerFactory: MediaTrackerFactory,
    private val analytics: Analytics
) {
    internal enum class EventVideoEnum(
        /**
         * Retrieves the Adobe Analytics video event name. This is different from `enum.name()`
         *
         * @return Event name.
         */
        val eventName: String
    ) {
        PlaybackStarted("Video Playback Started"),
        ContentStarted("Video Content Started"),
        PlaybackPaused("Video Playback Paused"),
        PlaybackResumed("Video Playback Resumed"),
        ContentCompleted("Video Content Completed"),
        PlaybackCompleted("Video Playback Completed"),
        PlaybackBufferStarted("Video Playback Buffer Started"),
        PlaybackBufferCompleted("Video Playback Buffer Completed"),
        PlaybackSeekStarted("Video Playback Seek Started"),
        PlaybackSeekCompleted("Video Playback Seek Completed"),
        AdBreakStarted("Video Ad Break Started"),
        AdBreakCompleted("Video Ad Break Completed"),
        AdStarted("Video Ad Started"),
        AdSkipped("Video Ad Skipped"),
        AdCompleted("Video Ad Completed"),
        PlaybackInterrupted("Video Playback Interrupted"),
        QualityUpdated("Video Quality Updated");

        companion object {
            private var names: MutableMap<String, EventVideoEnum>? = null

            init {
                names = HashMap()
                for (e in values()) {
                    (names as HashMap<String, EventVideoEnum>)[e.name] = e
                }
            }

            operator fun get(name: String): EventVideoEnum? {
                if (names!!.containsKey(name)) {
                    return names!![name]
                }
                throw IllegalArgumentException("$name is not a valid video event")
            }

            /**
             * Identifies if the event is a video event.
             *
             * @param eventName Event name
             * @return `true` if it's a video event, `false` otherwise.
             */
            fun isVideoEvent(eventName: String): Boolean {
                return names!!.containsKey(eventName)
            }
        }
    }
    /**
     * Creates MediaHeartbeats with the provided delegate.
     */
    internal class MediaTrackerFactory {
        fun get(config: Map<String, Any>): MediaTracker {
            return Media.createTracker(config)
        }
        companion object {
            init {
                VIDEO_METADATA_KEYS["assetId"] = ASSET_ID
                VIDEO_METADATA_KEYS["asset_id"] = ASSET_ID
                VIDEO_METADATA_KEYS["contentAssetId"] = ASSET_ID
                VIDEO_METADATA_KEYS["content_asset_id"] = ASSET_ID
                VIDEO_METADATA_KEYS["program"] = SHOW
                VIDEO_METADATA_KEYS["season"] = SEASON
                VIDEO_METADATA_KEYS["episode"] = EPISODE
                VIDEO_METADATA_KEYS["genre"] = GENRE
                VIDEO_METADATA_KEYS["channel"] = NETWORK
                VIDEO_METADATA_KEYS["airdate"] = FIRST_AIR_DATE
                VIDEO_METADATA_KEYS["publisher"] = ORIGINATOR
                VIDEO_METADATA_KEYS["rating"] = RATING
                AD_METADATA_KEYS["publisher"] = ADVERTISER
            }
        }
    }

    var contextDataConfiguration: ContextDataConfiguration
    var isSessionStarted: Boolean
    private var packageName: String
    private var mediaTracker: MediaTracker? = null

    constructor(
        context: Context,
        contextDataConfiguration: ContextDataConfiguration,
        analytics: Analytics,
    ) : this(context, contextDataConfiguration, MediaTrackerFactory(), analytics)

    init {
        this.contextDataConfiguration = contextDataConfiguration
        isSessionStarted = false
        packageName = context.packageName
        if (packageName == null) {
            // default app version to "unknown" if not otherwise present b/c Adobe requires this value
            packageName = "unknown"
        }
    }

    fun track(payload: TrackEvent) {
        val event = EventVideoEnum[payload.event]
       /* if (heartbeatTrackingServerUrl == null) {
            analytics.log(
                "Please enter a Heartbeat Tracking Server URL in your Segment UI "
                        + "Settings in order to send video events to Adobe Analytics"
            )
            return
        }*/
        check(!(event != EventVideoEnum.PlaybackStarted && !isSessionStarted)) { "Video session has not started yet." }
        when (event) {
            EventVideoEnum.PlaybackStarted -> trackVideoPlaybackStarted(payload)
            EventVideoEnum.PlaybackPaused -> trackVideoPlaybackPaused()
            EventVideoEnum.PlaybackResumed -> trackVideoPlaybackResumed()
            EventVideoEnum.PlaybackCompleted -> trackVideoPlaybackCompleted()
            EventVideoEnum.ContentStarted -> trackVideoContentStarted(payload)
            EventVideoEnum.ContentCompleted -> trackVideoContentCompleted()
            EventVideoEnum.PlaybackBufferStarted -> trackVideoPlaybackBufferStarted()
            EventVideoEnum.PlaybackBufferCompleted -> trackVideoPlaybackBufferCompleted(payload)
            EventVideoEnum.PlaybackSeekStarted -> trackVideoPlaybackSeekStarted()
            EventVideoEnum.PlaybackSeekCompleted -> trackVideoPlaybackSeekCompleted(payload)
            EventVideoEnum.AdBreakStarted -> trackVideoAdBreakStarted(payload)
            EventVideoEnum.AdBreakCompleted -> trackVideoAdBreakCompleted()
            EventVideoEnum.AdStarted -> trackVideoAdStarted(payload)
            EventVideoEnum.AdSkipped -> trackVideoAdSkipped()
            EventVideoEnum.AdCompleted -> trackVideoAdCompleted()
            EventVideoEnum.PlaybackInterrupted -> trackVideoPlaybackInterrupted()
            EventVideoEnum.QualityUpdated -> trackVideoQualityUpdated(payload)
            else -> {}
        }
    }

    private fun trackVideoPlaybackStarted(track: TrackEvent) {
        val eventProperties: Properties = track.properties
        val config: MutableMap<String, Any> = mutableMapOf()
        config[MediaConstants.Config.CHANNEL] = eventProperties["channel"] ?: ""
        config[MediaConstants.Config.DOWNLOADED_CONTENT] = true // Creates downloaded content tracker

        mediaTracker = mediaTrackerFactory.get(config)
        isSessionStarted = true
        val event = VideoEvent(track)
        val mediaObjectMap = event.mediaObject
        val contextDataMap = event.contextData

        mediaTracker?.trackSessionStart(mediaObjectMap, contextDataMap)

        analytics.log("mediaTracker.trackSessionStart(MediaObject)")
    }

    private fun trackVideoPlaybackPaused() {
        mediaTracker?.trackPause()
        analytics.log("mediaTracker.trackPause()")
    }

    private fun trackVideoPlaybackResumed() {
        mediaTracker?.trackPlay()
        analytics.log("mediaTrackermediaTracker.trackPlay()")
    }

    private fun trackVideoContentStarted(track: TrackEvent) {
        val event = VideoEvent(track)
        val properties: Map<String, String> = track.properties.asStringMap()
        val position = properties["position"]?.toDouble() ?:0.0
        if (position > 0.0) {
            mediaTracker?.updateCurrentPlayhead(position)
        }
        mediaTracker?.trackPlay()
        analytics.log("mediaTracker.trackPlay()")
        trackAdobeEvent(
            Media.Event.ChapterStart, event.chapterObject, event.contextData
        )
    }

    private fun trackVideoContentCompleted() {
        trackAdobeEvent(Media.Event.ChapterComplete, mapOf(), null)
    }

    //Upon playback complete, call trackComplete, and end session
    private fun trackVideoPlaybackCompleted() {
        mediaTracker?.trackComplete()
        analytics.log("mediaTracker.trackComplete()")
        mediaTracker?.trackSessionEnd()
        analytics.log("mediaTracker.trackSessionEnd()")
    }

    private fun trackVideoPlaybackBufferStarted() {
        mediaTracker?.trackPause()
        trackAdobeEvent(Media.Event.BufferStart, mapOf(), null)
    }

    private fun trackVideoPlaybackBufferCompleted(track: TrackEvent) {
        val seekProperties: Map<String, String> = track.properties.asStringMap()
        var seekPosition: Long = seekProperties["seekPosition"]?.toLong() ?: 0
        if (seekPosition == 0L) {
            seekPosition = seekProperties["seek_position"]?.toLong() ?: 0
        }
        if (seekPosition == 0L) {
            seekPosition = seekProperties["position"]?.toLong() ?: 0
        }
        mediaTracker?.trackPlay()
        mediaTracker?.updateCurrentPlayhead(seekPosition.toDouble())
        trackAdobeEvent(Media.Event.BufferComplete, mapOf(), null)
    }

    private fun trackAdobeEvent(
        eventName: Media.Event, mediaObjectMap: Map<String, Any>, cdata: Map<String?, String>?
    ) {
        mediaTracker?.trackEvent(eventName, mediaObjectMap, cdata)
        analytics.log("mediaTracker.trackEvent($eventName, $mediaObjectMap, $cdata)")
    }

    private fun trackVideoPlaybackSeekStarted() {
        mediaTracker?.trackPause()
        trackAdobeEvent(Media.Event.SeekStart, mapOf(), null)
    }

    private fun trackVideoPlaybackSeekCompleted(track: TrackEvent) {
        val seekProperties: Map<String, String> = track.properties.asStringMap()
        var seekPosition: Long = seekProperties["seekPosition"]?.toLong() ?: 0
        if (seekPosition == 0L) {
            seekPosition = seekProperties["seek_position"]?.toLong() ?: 0
        }
        if (seekPosition == 0L) {
            seekPosition = seekProperties["position"]?.toLong() ?: 0
        }
        mediaTracker?.trackPlay()
        mediaTracker?.updateCurrentPlayhead(seekPosition.toDouble())
        trackAdobeEvent(Media.Event.SeekComplete, mapOf(), null)
    }

    private fun trackVideoAdBreakStarted(track: TrackEvent) {
        val event = VideoEvent(track, true)
        trackAdobeEvent(
            Media.Event.AdBreakStart, event.adBreakObject, event.contextData
        )
    }

    private fun trackVideoAdBreakCompleted() {
        trackAdobeEvent(Media.Event.AdBreakComplete, mapOf(), null)
    }

    private fun trackVideoAdStarted(track: TrackEvent) {
        val event = VideoEvent(track, true)
        trackAdobeEvent(Media.Event.AdStart, event.adObject, event.contextData)
    }

    private fun trackVideoAdSkipped() {
        trackAdobeEvent(Media.Event.AdSkip, mapOf(), null)
    }

    private fun trackVideoAdCompleted() {
        trackAdobeEvent(Media.Event.AdComplete, mapOf(), null)
    }

    private fun trackVideoPlaybackInterrupted() {
        mediaTracker?.trackPause()
        analytics.log("mediaTracker.trackPause()")
    }

    private fun trackVideoQualityUpdated(track: TrackEvent) {
        val event = VideoEvent(track, true)
        mediaTracker?.updateQoEObject(event.qoeObject)
    }

    /**
     * A wrapper for video metadata and properties.
     * Creates video properties from the ones provided in the event.
     *
     * @param payload Event Payload.
     * @param isAd Determines if the video is an ad.
     */
    internal inner class VideoEvent(payload: TrackEvent, isAd: Boolean) {
        private val metadata: MutableMap<String?, String>
        //        private var properties: Properties? = null
        private var propertiesMap: MutableMap<String, String> = mutableMapOf()

        private val payload: TrackEvent

        /**
         * Creates video properties from the ones provided in the event.
         *
         * @param payload Event Payload.
         */
        constructor(payload: TrackEvent) : this(payload, false)

        init {
            this.payload = payload
            metadata = HashMap()
            val properties = JsonObject(payload.properties.toMap())
            propertiesMap.putAll(properties.asStringMap())
            val eventProperties = payload.properties
            if (isAd) {
                mapAdProperties(eventProperties.asStringMap())
            } else {
                mapVideoProperties(eventProperties.asStringMap())
            }
        }

        private fun mapVideoProperties(eventProperties: Map<String, String>) {
            for (key in eventProperties.keys) {
                if (VIDEO_METADATA_KEYS.containsKey(key)) {
                    val propertyKey = VIDEO_METADATA_KEYS[key]
                    metadata[propertyKey] = eventProperties[key].toString()
                    propertiesMap.remove(key)
                }
            }
            if (propertiesMap.containsKey("livestream")) {
                var format: String = MediaConstants.StreamType.LIVE
                if (!propertiesMap["livestream"].equals("false")) {
                    format = MediaConstants.StreamType.VOD
                }
                metadata[STREAM_FORMAT] = format
                propertiesMap.remove("livestream")
            }
        }

        private fun mapAdProperties(eventProperties: Map<String, String>) {
            for (key in eventProperties.keys) {
                if (AD_METADATA_KEYS.containsKey(key)) {
                    val propertyKey = AD_METADATA_KEYS[key]
                    metadata[propertyKey] = java.lang.String.valueOf(eventProperties[key])
                    propertiesMap.remove(key)
                }
            }
        }

        val contextData: Map<String?, String>
            get() {
                val extraProperties = mutableMapOf<String, String>()
                extraProperties.putAll(propertiesMap)

                // Remove products from extra properties
                extraProperties.remove("products")

                // Remove video metadata keys
                for (key in VIDEO_METADATA_KEYS.keys) {
                    extraProperties.remove(key)
                }

                // Remove ad metadata keys
                for (key in AD_METADATA_KEYS.keys) {
                    extraProperties.remove(key)
                }

                // Remove media object keys
                for (key in arrayOf(
                    "title",
                    "indexPosition",
                    "index_position",
                    "position",
                    "totalLength",
                    "total_length",
                    "startTime",
                    "start_time"
                )) {
                    extraProperties.remove(key)
                }
                val cdata: MutableMap<String?, String> = HashMap()
                for (field in contextDataConfiguration.eventFieldNames) {
                    var value: Any? = null
                    try {
                        value = contextDataConfiguration.searchValue(field, payload.properties)
                    } catch (e: IllegalArgumentException) {
                        // Ignore.
                    }
                    if (value != null) {
                        val variable = contextDataConfiguration.getVariableName(field)
                        cdata[variable] = value.toString()
                        extraProperties.remove(field)
                    }
                }

                // Add extra properties.
                for (extraProperty in extraProperties.keys) {
                    val variable = contextDataConfiguration.getPrefix() + extraProperty
                    cdata[variable] = extraProperties[extraProperty].toString()
                }
                return cdata
            }

        // Segment does not spec this
        val chapterObject: Map<String, Any>
            get() {
                if (payload.properties.asStringMap().isEmpty()) {
                    return mapOf()
                }
                val eventPropertiesMap: Map<String, String> = payload.properties.asStringMap()
                val title: String? = eventPropertiesMap["title"]
                var indexPosition: Long = eventPropertiesMap["indexPosition"]?.toLong() ?: 1 // Segment does not spec this
                if (indexPosition == 1L) {
                    indexPosition = eventPropertiesMap["index_position"]?.toLong() ?: 1
                }
                var totalLength: Double = eventPropertiesMap["totalLength"]?.toDouble()?: 0.0
                if (totalLength == 0.0) {
                    totalLength = eventPropertiesMap["total_length"]?.toDouble() ?: 0.0
                }
                var startTime: Double = eventPropertiesMap["startTime"]?.toDouble() ?: 0.0
                if (startTime == 0.0) {
                    startTime = eventPropertiesMap["start_time"]?.toDouble() ?: 0.0
                }
                val mediaObjectMap =
                    Media.createChapterObject(title, indexPosition, totalLength, startTime)
                 for (key in metadata.keys) {
                    mediaObjectMap[key!!] = metadata[key]!!
                }
                return mediaObjectMap
            }

        val mediaObject: Map<String, Any>
            get() {
                if (payload.properties.asStringMap().isEmpty()) {
                    return mapOf()
                }
                val eventPropertiesMap: Map<String, String> = payload.properties.asStringMap()
                val title: String = eventPropertiesMap["title"].toString()
                var contentAssetId: String? = eventPropertiesMap["contentAssetId"]
                if (contentAssetId == null || contentAssetId.trim { it <= ' ' }.isEmpty()) {
                    contentAssetId = eventPropertiesMap["content_asset_id"]
                }
                var totalLength: Double = eventPropertiesMap["totalLength"]?.toDouble() ?: 0.0
                if (totalLength == 0.0) {
                    totalLength = eventPropertiesMap["total_length"]?.toDouble() ?: 0.0
                }
                var format: String = MediaConstants.StreamType.LIVE
                if (!eventPropertiesMap["livestream"].equals("false")) {
                    format = MediaConstants.StreamType.VOD
                }
                val mediaObjectMap = Media.createMediaObject(title, contentAssetId, totalLength, format, Media.MediaType.Video)
                for (key in metadata.keys) {
                    mediaObjectMap[key!!] = metadata[key]!!
                }
//                mediaObject.setValue(MediaHeartbeat.MediaObjectKey.StandardVideoMetadata, metadata)
                return mediaObjectMap
            }
        val adObject: Map<String, Any>
            get() {
                if (payload.properties.asStringMap().isEmpty()) {
                    return mapOf()
                }
                val eventPropertiesMap: Map<String, String> = payload.properties.asStringMap()
                val title: String = eventPropertiesMap["title"].toString()
                var assetId: String? = eventPropertiesMap["assetId"]
                if (assetId == null || assetId.trim { it <= ' ' }.isEmpty()) {
                    assetId = eventPropertiesMap["asset_id"].toString()
                }
                var indexPosition: Long = eventPropertiesMap["indexPosition"]?.toLong() ?: 1
                if (indexPosition == 1L) {
                    indexPosition = eventPropertiesMap["index_position"]?.toLong() ?: 1
                }
                var totalLength: Double = eventPropertiesMap["totalLength"]?.toDouble() ?: 0.0
                if (totalLength == 0.0) {
                    totalLength = eventPropertiesMap["total_length"]?.toDouble() ?: 0.0
                }
                val mediaObjectMap: MutableMap<String, Any> =
                    Media.createAdObject(title, assetId, indexPosition, totalLength)
                for (key in metadata.keys) {
                    mediaObjectMap[key!!] = metadata[key]!!
                }
                return mediaObjectMap
            }

        // Segment does not spec this
        val adBreakObject: Map<String, Any>
            get() {
                if (payload.properties.asStringMap().isEmpty()) {
                    return mapOf()
                }
                val eventPropertiesMap: Map<String, String> = payload.properties.asStringMap()
                val title: String = eventPropertiesMap["title"].toString()
                var indexPosition: Long =
                    eventPropertiesMap.get("indexPosition")?.toLong() ?:1 // Segment does not spec this
                if (indexPosition == 1L) {
                    indexPosition = eventPropertiesMap["index_position"]?.toLong() ?: 1
                }
                var startTime: Double = eventPropertiesMap["startTime"]?.toDouble() ?: 0.0
                if (startTime == 0.0) {
                    startTime = eventPropertiesMap["start_time"]?.toDouble() ?: 0.0
                }
                return Media.createAdBreakObject(title, indexPosition, startTime)
            }

        val qoeObject: Map<String, Any>
            get() {
                if (payload.properties.asStringMap().isEmpty()) {
                    return mapOf()
                }
                val eventPropertiesMap: Map<String, String> = payload.properties.asStringMap()

                var startupTime: Double = eventPropertiesMap["startupTime"]?.toDouble() ?: 0.0
                if (startupTime == 0.0) {
                    startupTime = eventPropertiesMap["startup_time"]?.toDouble() ?: 0.0
                }
                var droppedFrames: Long = eventPropertiesMap["droppedFrames"]?.toLong() ?: 0
                if (droppedFrames == 0L) {
                    droppedFrames = eventPropertiesMap["dropped_frames"]?.toLong() ?: 0
                }
                val bitrate: Long = eventPropertiesMap["bitrate"]?.toLong() ?: 0
                val fps: Double = eventPropertiesMap["fps"]?.toDouble() ?: 0.0


                return Media.createQoEObject(bitrate, startupTime, fps, droppedFrames)
            }

        fun getMetadata(): Map<String?, String> {
            return metadata
        }

        fun getProperties(): Map<String, String> {
            return propertiesMap
        }

        val eventPayload: TrackEvent
            get() = payload
    }

    companion object {
        private val VIDEO_METADATA_KEYS: MutableMap<String, String> = HashMap()
        private val AD_METADATA_KEYS: MutableMap<String, String> = HashMap()
    }
}