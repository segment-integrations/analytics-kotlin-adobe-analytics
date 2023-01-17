package com.segment.analytics.kotlin.destinations.adobeanalytics

import com.segment.analytics.kotlin.core.Analytics
import com.segment.analytics.kotlin.core.TrackEvent
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EcommerceAnalyticsTests {

    @MockK(relaxUnitFun = true)
    lateinit var mockedAnalytics: Analytics

    @MockK(relaxUnitFun = true)
    lateinit var mockedDefaultAdobeAnalyticsClient: DefaultAnalyticsClient

    private lateinit var mockedEcommerceAnalytics: EcommerceAnalytics

    init {
        MockKAnnotations.init(this)
    }

    @Before
    fun setUp() {
        val config = ContextDataConfiguration(null, mapOf())
        mockedEcommerceAnalytics = EcommerceAnalytics(mockedDefaultAdobeAnalyticsClient, mockedAnalytics, "", config)
    }

    @Test
    fun `ecommerce initialize`() {
        val contextDataVariables: MutableMap<String, String> = HashMap()
        contextDataVariables["field"] = "myapp.prop"
        val config = ContextDataConfiguration("myapp.", contextDataVariables)
        mockedEcommerceAnalytics = EcommerceAnalytics(mockedDefaultAdobeAnalyticsClient, mockedAnalytics, "identifier", config)

        Assert.assertEquals("identifier", mockedEcommerceAnalytics.productIdentifier)
    }

    @Test
    fun `track for checkout started handled correctly`() {
        val eventEnum = EcommerceAnalytics.EventEnum.CheckoutStarted
        val sampleEvent = TrackEvent(
            event = eventEnum.segmentEvent,
            properties = buildJsonObject {
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedEcommerceAnalytics.track(sampleEvent)
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction(eventEnum.adobeAnalyticsEvent, null)}
    }

    @Test
    fun `track for order completed handled correctly`() {
        val contextDataVariables: MutableMap<String, String> = HashMap()
        contextDataVariables["field"] = "myapp.prop"
        val config = ContextDataConfiguration("", contextDataVariables)

        val mockedEcommerceAnalytics = EcommerceAnalytics(mockedDefaultAdobeAnalyticsClient, mockedAnalytics, "name", config)
        val eventEnum = EcommerceAnalytics.EventEnum.OrderCompleted

        val sampleEvent = TrackEvent(
            event = eventEnum.segmentEvent,
            properties = buildJsonObject {
                put("orderId", "ORDERID1234")
                put("field", "test1")
                put("products", buildJsonArray {
                    add(buildJsonObject {
                        put("id", "123")
                        put("sku", "ABC")
                        put("price", 10.0)
                        put("name", "shoes")
                        put("category", "athletic")
                        put("quantity", 2)
                    })
                })
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }

        mockedEcommerceAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = mutableMapOf()
        contextData["purchaseid"] = "ORDERID1234"
        contextData["&&events"] = eventEnum.adobeAnalyticsEvent
        contextData["myapp.prop"] = "test1"
        contextData["&&products"] = "athletic;shoes;2;20.0"
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction(eventEnum.adobeAnalyticsEvent, contextData.toMap())}
    }

    @Test
    fun `track for order completed with extra properties handled correctly`() {
        val contextDataVariables: MutableMap<String, String> = HashMap()
        contextDataVariables["field1"] = "myapp.prop"
        contextDataVariables["field2.fieldA"] = "myapp.prop1"
        contextDataVariables["field3.fieldA"] = "myapp.prop2.propA"
        contextDataVariables[".context.context1"] = "myapp.context1"
        val config = ContextDataConfiguration("myapp.", contextDataVariables)

        val mockedEcommerceAnalytics = EcommerceAnalytics(mockedDefaultAdobeAnalyticsClient, mockedAnalytics, "name", config)
        val eventEnum = EcommerceAnalytics.EventEnum.OrderCompleted
        val sampleEvent = TrackEvent(
            event = eventEnum.segmentEvent,
            properties = buildJsonObject {
                put("orderId", "ORDERID1234")
                put("extraField", "extra test1")
                put("field1", "test1")
                put("field3", buildJsonObject {
                    put("fieldA", "field3 subfield A value")
                    put("fieldB", "field3 subfield B value")
                    put("fieldC", "field3 subfield C value")
                })
                put("products", buildJsonArray {
                    add(buildJsonObject {
                        put("id", "123")
                        put("sku", "ABC")
                        put("price", 10.0)
                        put("name", "shoes")
                        put("category", "athletic")
                        put("quantity", 2)
                    })
                })
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
            context = buildJsonObject {
                put("context1", "testing context a")
            }
        }

        mockedEcommerceAnalytics.track(sampleEvent)

        val contextData: MutableMap<String, String> = mutableMapOf()
        val fieldData: MutableMap<String, String> = mutableMapOf()
        fieldData["fieldA"] = "field3 subfield A value"
        fieldData["fieldB"] = "field3 subfield B value"
        fieldData["fieldC"] = "field3 subfield C value"

        contextData["purchaseid"] = "ORDERID1234"
        contextData["&&events"] = eventEnum.adobeAnalyticsEvent
        contextData["myapp.extraField"] = "extra test1"
        contextData["myapp.prop"] = "test1"
        contextData["myapp.field3"] = fieldData.toString()
        contextData["myapp.prop2.propA"] = "field3 subfield A value"
        contextData["&&products"] = "athletic;shoes;2;20.0"
        contextData["myapp.context1"] = "testing context a"
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction(eventEnum.adobeAnalyticsEvent, contextData.toMap())}
    }

    @Test
    fun `track for product added in properties handled correctly`() {
        mockedEcommerceAnalytics.productIdentifier = "name"
        val eventEnum = EcommerceAnalytics.EventEnum.ProductAdded
        val sampleEvent = TrackEvent(
            event = eventEnum.segmentEvent,
            properties = buildJsonObject {
                put("sku", "ABC")
                put("price", 10.0)
                put("name", "shoes")
                put("category", "athletic")
                put("quantity", 2)
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedEcommerceAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = mutableMapOf()
        contextData["&&events"] = eventEnum.adobeAnalyticsEvent
        contextData["sku"] = "ABC"
        contextData["&&products"] = "athletic;shoes;2;20.0"
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction(eventEnum.adobeAnalyticsEvent, contextData.toMap())}
    }

    @Test
    fun `track for product added in properties with extra property handled correctly`()
    {
        mockedEcommerceAnalytics.productIdentifier = "name"
        val eventEnum = EcommerceAnalytics.EventEnum.ProductAdded
        val sampleEvent = TrackEvent(
            event = eventEnum.segmentEvent,
            properties = buildJsonObject {
                put("sku", "ABC")
                put("price", 10.0)
                put("name", "shoes")
                put("category", "athletic")
                put("quantity", 2)
                put("extraField", "extra value")
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedEcommerceAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = mutableMapOf()
        contextData["&&events"] = eventEnum.adobeAnalyticsEvent
        contextData["sku"] = "ABC"
        contextData["&&products"] = "athletic;shoes;2;20.0"
        contextData["extraField"] = "extra value"
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction(eventEnum.adobeAnalyticsEvent, contextData.toMap())}
    }

    @Test
    fun `track for product removed in properties handled correctly`() {
        mockedEcommerceAnalytics.productIdentifier = "name"
        val eventEnum = EcommerceAnalytics.EventEnum.ProductRemoved
        val sampleEvent = TrackEvent(
            event = eventEnum.segmentEvent,
            properties = buildJsonObject {
                put("sku", "ABC")
                put("price", 10.0)
                put("name", "shoes")
                put("category", "athletic")
                put("quantity", 2)
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedEcommerceAnalytics.track(sampleEvent)

        val contextData: MutableMap<String, String> = mutableMapOf()
        contextData["&&events"] = eventEnum.adobeAnalyticsEvent
        contextData["sku"] = "ABC"
        contextData["&&products"] = "athletic;shoes;2;20.0"
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction(eventEnum.adobeAnalyticsEvent, contextData.toMap())}
    }

    @Test
    fun `track for product viewed in properties handled correctly`() {
        mockedEcommerceAnalytics.productIdentifier = "name"
        val eventEnum = EcommerceAnalytics.EventEnum.ProductView
        val sampleEvent = TrackEvent(
            event = eventEnum.segmentEvent,
            properties = buildJsonObject {
                put("sku", "ABC")
                put("price", 10.0)
                put("name", "shoes")
                put("category", "athletic")
                put("quantity", 2)
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedEcommerceAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = mutableMapOf()
        contextData["&&events"] = eventEnum.adobeAnalyticsEvent
        contextData["sku"] = "ABC"
        contextData["&&products"] = "athletic;shoes;2;20.0"
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction(eventEnum.adobeAnalyticsEvent, contextData.toMap())}
    }

    @Test
    fun `track for product viewed in properties with product id handled correctly`() {
        mockedEcommerceAnalytics.productIdentifier = "id"
        val eventEnum = EcommerceAnalytics.EventEnum.ProductView
        val sampleEvent = TrackEvent(
            event = eventEnum.segmentEvent,
            properties = buildJsonObject {
                put("productId", "123")
                put("sku", "ABC")
                put("price", 10.0)
                put("name", "shoes")
                put("category", "athletic")
                put("quantity", 2)
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }

        mockedEcommerceAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = mutableMapOf()
        contextData["&&events"] = eventEnum.adobeAnalyticsEvent
        contextData["sku"] = "ABC"
        contextData["name"] = "shoes"
        contextData["&&products"] = "athletic;123;2;20.0"
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction(eventEnum.adobeAnalyticsEvent, contextData.toMap())}
    }

    @Test
    fun `track for checkout started with multiple products handled correctly`() {
        mockedEcommerceAnalytics.productIdentifier = "name"
        val eventEnum = EcommerceAnalytics.EventEnum.CheckoutStarted
        val sampleEvent = TrackEvent(
            event = eventEnum.segmentEvent,
            properties = buildJsonObject {
                put("products", buildJsonArray {
                    add(buildJsonObject {
                        put("id", "123")
                        put("sku", "ABC")
                        put("price", 10.0)
                        put("name", "shoes")
                        put("category", "athletic")
                        put("quantity", 2)
                    })
                    add(buildJsonObject {
                        put("id", "456")
                        put("sku", "XYZ")
                        put("price", 20.0)
                        put("name", "shirt")
                        put("category", "formal")
                        put("quantity", 3)
                    })
                    add(buildJsonObject {
                        put("id", "789")
                        put("sku", "PQR")
                        put("price", 30.0)
                        put("category", "casual")
                        put("quantity", 1)
                    })
                })
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedEcommerceAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = mutableMapOf()
        contextData["&&events"] = eventEnum.adobeAnalyticsEvent
        contextData["&&products"] = "athletic;shoes;2;20.0,formal;shirt;3;60.0,casual;789;1;30.0"
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction(eventEnum.adobeAnalyticsEvent, contextData.toMap())}
    }

    @Test
    fun trackCartViewed() {
        mockedEcommerceAnalytics.productIdentifier = "name"
        val eventEnum = EcommerceAnalytics.EventEnum.CartViewed
        val sampleEvent = TrackEvent(
            event = eventEnum.segmentEvent,
            properties = buildJsonObject {
                put("products", buildJsonArray {
                    add(buildJsonObject {
                        put("id", "123")
                        put("sku", "ABC")
                        put("price", 10.0)
                        put("name", "shoes")
                        put("category", "athletic")
                        put("quantity", 2)
                    })
                    add(buildJsonObject {
                        put("id", "456")
                        put("sku", "XYZ")
                        put("price", 20.0)
                        put("name", "shirt")
                        put("category", "formal")
                        put("quantity", 3)
                    })
                })
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedEcommerceAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = mutableMapOf()
        contextData["&&events"] = eventEnum.adobeAnalyticsEvent
        contextData["&&products"] = "athletic;shoes;2;20.0,formal;shirt;3;60.0"
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction(eventEnum.adobeAnalyticsEvent, contextData.toMap())}
    }

    @Test
    fun `track for product removed in properties with no name handled correctly`() {
        mockedEcommerceAnalytics.productIdentifier = "name"
        val eventEnum = EcommerceAnalytics.EventEnum.ProductRemoved
        val sampleEvent = TrackEvent(
            event = eventEnum.segmentEvent,
            properties = buildJsonObject {
                put("sku", "ABC")
                put("price", 10.0)
                put("category", "athletic")
                put("quantity", 2)
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }

        mockedEcommerceAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = mutableMapOf()
        contextData["&&events"] = eventEnum.adobeAnalyticsEvent
        contextData["sku"] = "ABC"
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction(eventEnum.adobeAnalyticsEvent, contextData.toMap())}
    }

    @Test
    fun `track for product added with no products and properties handled correctly`() {
        mockedEcommerceAnalytics.productIdentifier = "name"
        val eventEnum = EcommerceAnalytics.EventEnum.ProductAdded
        val sampleEvent = TrackEvent(
            event = eventEnum.segmentEvent,
            properties = buildJsonObject {
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }

        mockedEcommerceAnalytics.track(sampleEvent)
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction(eventEnum.adobeAnalyticsEvent,null)}
    }

    @Test
    fun  `track for order completed with defaults handled correctly`() {
        mockedEcommerceAnalytics.productIdentifier = "sku"
        val eventEnum = EcommerceAnalytics.EventEnum.OrderCompleted
        val sampleEvent = TrackEvent(
            event = eventEnum.segmentEvent,
            properties = buildJsonObject {
                put("products", buildJsonArray {
                    add(buildJsonObject {
                        put("id", "123")
                        put("sku", "ABC")
                        put("price", 0)
                    })
                })
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }
        mockedEcommerceAnalytics.track(sampleEvent)

        val contextData: MutableMap<String, String> = mutableMapOf()
        contextData["&&events"] = eventEnum.adobeAnalyticsEvent
        contextData["&&products"] = ";ABC;1;0.0"
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction(eventEnum.adobeAnalyticsEvent, contextData.toMap())}
    }

    @Test
    fun `track for product removed with defaults handled correctly`() {
        mockedEcommerceAnalytics.productIdentifier = "name"
        val eventEnum = EcommerceAnalytics.EventEnum.ProductRemoved
        val sampleEvent = TrackEvent(
            event = eventEnum.segmentEvent,
            properties = buildJsonObject {
                put("orderId", "123")
            }
        ).apply {
            userId = "UserID-123"
            anonymousId = "anonymous_UserID-123"
        }

        mockedEcommerceAnalytics.track(sampleEvent)
        val contextData: MutableMap<String, String> = mutableMapOf()
        contextData["&&events"] = eventEnum.adobeAnalyticsEvent
        contextData["purchaseid"] = "123"
        verify {  mockedDefaultAdobeAnalyticsClient.trackAction(eventEnum.adobeAnalyticsEvent, contextData.toMap())}
    }
}