package com.segment.analytics.kotlin.destinations.adobeanalytics

import com.segment.analytics.kotlin.core.TrackEvent
import com.segment.analytics.kotlin.core.emptyJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ContextDataConfigurationTests {
    @Test
    fun `initialize with default prefix`() {
        val contextDataVariables: MutableMap<String, String> = HashMap()
        contextDataVariables["field"] = "prop"
        val config = ContextDataConfiguration("a.", contextDataVariables)
        assertEquals("", config.getPrefix())
    }

    @Test
    fun `initialize with null prefix`() {
        val contextDataVariables: MutableMap<String, String> = HashMap()
        contextDataVariables["field"] = "prop"
        val config = ContextDataConfiguration(null, contextDataVariables)
        assertEquals("", config.getPrefix())
    }

    @Test
    fun `searchValue() with Fields`() {
        val sampleEvent = TrackEvent(
            event = "TestEvent",
            properties = buildJsonObject {
                put("field1", "testing a")
                put("field2", "testing b")
            },
        )
        val config = ContextDataConfiguration(null, mapOf())

        assertEquals("testing a", config.searchValue("field1", sampleEvent))
        assertEquals("testing b", config.searchValue("field2", sampleEvent))
    }

    @Test
    fun `searchValue() with Nested Fields`() {
        val sampleEvent = TrackEvent(
            event = "TestEvent",
            properties = buildJsonObject {
                put("field1", "testing a")
                put("field2",  buildJsonObject {
                    put("nestedField1", "testing b")
                })
            }
        )
        val config = ContextDataConfiguration(null, mapOf())

        assertEquals("testing a", config.searchValue("field1", sampleEvent))
        assertEquals("testing b", config.searchValue("field2.nestedField1", sampleEvent))
    }

    @Test
    fun `searchValue() with Parent Fields`() {
        val sampleEvent = TrackEvent(
            event = "TestEvent",
            properties = buildJsonObject {
                put("field1", "testing a")
                put("field2", buildJsonObject {
                    put("nestedField1", "testing b")
                })
            },
        ).apply {
            context = buildJsonObject {
                put("context1", "testing context a")
            }
            integrations = emptyJsonObject
            timestamp = "2023-01-10T01:59:09"
            anonymousId = "anonymous_UserID-123"
        }

        val config = ContextDataConfiguration(null, mapOf())

        assertEquals("anonymous_UserID-123", config.searchValue(".anonymousId", sampleEvent))
        assertEquals("testing context a", config.searchValue(".context.context1", sampleEvent))
    }

    @Test
    fun `searchValue() with Invalid Fields`() {
        val sampleEvent = TrackEvent(
            event = "TestEvent",
            properties = buildJsonObject {
                put("field1", "testing a")
                put("field2", buildJsonObject {
                    put("nestedField1", "testing b")
                })
            },
        ).apply {
            context = buildJsonObject {
                put("context1", "testing context a")
            }
            integrations = emptyJsonObject
            timestamp = "2023-01-10T01:59:09"
            anonymousId = "anonymous_UserID-123"
        }

        val config = ContextDataConfiguration(null, mapOf())

        for (invalidField in arrayOf("..an..onymousId", ".context.context1.    .")) {
            var e: IllegalArgumentException? = null
            try {
                config.searchValue(invalidField, sampleEvent)
            } catch (ex: IllegalArgumentException) {
                e = ex
            }
            assertNotNull(e)
        }
    }


    @Test
    fun `searchValue() with Missing Fields`() {
        val sampleEvent = TrackEvent(
            event = "TestEvent",
            properties = buildJsonObject {
                put("field1", "testing a")
                put("field2", buildJsonObject {
                    put("nestedField1", "testing b")
                })
                put("field4", "testing c")
            },
        ).apply {
            context = buildJsonObject {
                put("context1", "testing context a")
            }
            integrations = emptyJsonObject
            timestamp = "2023-01-10T01:59:09"
            anonymousId = "anonymous_UserID-123"
        }

        val config = ContextDataConfiguration(null, mapOf())

        for (missingField in arrayOf(".context.test", "field3", "field4.id")) {
            Assert.assertNull(config.searchValue(missingField, sampleEvent))
        }
    }
}