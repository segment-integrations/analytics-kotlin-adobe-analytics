package com.segment.analytics.kotlin.destinations.adobeanalytics

import com.segment.analytics.kotlin.core.Analytics
import com.segment.analytics.kotlin.core.TrackEvent
import com.segment.analytics.kotlin.core.platform.plugins.logger.log
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.util.*

/**
 * Generate events for all ecommerce actions.
 * @param productIdentifier Field that represents the product id.
 * @param contextDataConfiguration New context data configuration.
 */
class EcommerceAnalytics internal constructor(
    private val adobeAnalytics: AdobeAnalyticsClient,
    private val analytics: Analytics,
    productIdentifier: String?,
    contextDataConfiguration: ContextDataConfiguration
) {
    internal enum class EventEnum(
        /**
         * Retrieves Segment's ecommerce event name. This is different from `enum.name()`.
         *
         * @return Ecommerce event name.
         */
        val segmentEvent: String,
        /**
         * Retrieves Adobe Analytics' ecommerce event name. This is different from `enum.name()`.
         *
         * @return Ecommerce event name.
         */
        val adobeAnalyticsEvent: String
    ) {
        OrderCompleted("Order Completed", "purchase"),
        ProductAdded("Product Added", "scAdd"),
        ProductRemoved("Product Removed", "scRemove"),
        CheckoutStarted("Checkout Started", "scCheckout"),
        CartViewed("Cart Viewed", "scView"),
        ProductView("Product Viewed", "prodView");

        companion object {
            private var names: MutableMap<String, EventEnum>? = null

            init {
                names = mutableMapOf()
                for (e in values()) {
                    names!![e.segmentEvent] = e
                }
            }

            /**
             * Retrieves the event using Segment's ecommerce event name.
             *
             * @param name Segment's ecommerce event name.
             * @return The event.
             */
            operator fun get(name: String): EventEnum? {
                if (names!!.containsKey(name)) {
                    return names!![name]
                }
                throw IllegalArgumentException("$name is not a valid ecommerce event")
            }

            /**
             * Identifies if the event is part of Segment's ecommerce spec.
             *
             * @param eventName Event name
             * @return `true` if it's a ecommerce event, `false` otherwise.
             */
            fun isEcommerceEvent(eventName: String): Boolean {
                return names!!.containsKey(eventName)
            }
        }
    }

    private var contextDataConfiguration: ContextDataConfiguration
    var productIdentifier: String?

    init {
        this.contextDataConfiguration = contextDataConfiguration
        this.productIdentifier = productIdentifier
    }

    fun track(payload: TrackEvent) {
        val event = EventEnum[payload.event]
        val eventName = event!!.adobeAnalyticsEvent
        val cdata = getContextData(eventName, payload)
        adobeAnalytics.trackAction(eventName, cdata)
        analytics.log("Analytics.trackAction($eventName, $cdata)")
    }

    private fun getContextData(eventName: String, payload: TrackEvent): Map<String, String>? {
        val contextData: MutableMap<String, String> = HashMap()
        contextData["&&events"] = eventName
        val extraProperties = payload.properties.asStringMap() as MutableMap
        val properties = payload.properties.asStringMap() as MutableMap
        val productsJsonArray = (payload.properties.toMap()["products"] as JsonArray)

        val products: Products
        if (properties.containsKey("products") && productsJsonArray.size > 0) {
            products = Products(productsJsonArray)
            extraProperties.remove("products")
        } else {
            val propertiesToRemove: MutableList<String> = LinkedList()
            propertiesToRemove.add("category")
            propertiesToRemove.add("quantity")
            propertiesToRemove.add("price")
            products = Products(properties)
            val idKey = productIdentifier
            if (idKey == null || idKey == "id") {
                propertiesToRemove.add("productId")
                propertiesToRemove.add("product_id")
            } else {
                propertiesToRemove.add(idKey)
            }
            for (key in propertiesToRemove) {
                extraProperties.remove(key)
            }
        }
        if (!products.isEmpty) {
            contextData["&&products"] = products.toString()
        }
        if (properties.containsKey("orderId")) {
            contextData["purchaseid"] = properties["orderId"]!!
            extraProperties.remove("orderId")
        }
        if (properties.containsKey("order_id")) {
            contextData["purchaseid"] = properties["order_id"]!!
            extraProperties.remove("order_id")
        }

        // add all customer-mapped properties to ecommerce context data map
        for (field in contextDataConfiguration.eventFieldNames) {
            var value: Any? = null
            try {
                value = contextDataConfiguration.searchValue(field, payload.properties)
            } catch (e: IllegalArgumentException) {
                // Ignore.
            }
            if (value != null) {
                val variable = contextDataConfiguration.getVariableName(field)
                if (variable != null) {
                    contextData[variable] = value.toString()
                }
                extraProperties.remove(field)
            }
        }

        // Add extra properties.
        for (extraProperty in extraProperties.keys) {
            val variable = contextDataConfiguration.getPrefix() + extraProperty
            contextData[variable] = extraProperties[extraProperty]!!
        }

        // If we only have events, we return null;
        return if (contextData.size == 1) {
            null
        } else contextData.toMap()
    }

    /**
     * Defines a Adobe Analytics ecommerce product.
     * Creates a product.
     *
     * @param eventProductMap Product as defined in the event.
     */
    internal inner class Product(eventProductMap: Map<String, String>) {
        private val category: String?
        private var id: String? = null
        private var quantity: Int
        private var price: Double

        init {
            setProductId(eventProductMap)
            this.category = eventProductMap["category"]

            // Default to 1.
            quantity = 1
            val q: String? = eventProductMap["quantity"]
            if (q != null) {
                try {
                    quantity = q.toInt()
                } catch (e: NumberFormatException) {
                    // Default.
                }
            }

            // Default to 0.
            price = 0.0
            val p: String? = eventProductMap["price"]
            if (p != null) {
                try {
                    price = p.toDouble()
                } catch (e: NumberFormatException) {
                    // Default.
                }
            }
            price *= quantity
        }

        /**
         * Sets the product ID using productIdentifier setting if present (supported values are `
         * name`, `sku` and `id`. If the field is not present, it fallbacks
         * to "productId" and "id".
         *
         *
         * Currently we do not allow to have products without IDs. Adobe Analytics allows to send an
         * extra product for merchandising evars and event serialization, as seen in the last example of
         * the [docs](https://marketing.adobe.com/resources/help/en_US/sc/implement/products.html),
         * but it is not well documented and does not conform Segment's spec.
         *
         *
         * **NOTE: V2 Ecommerce spec defines "product_id" instead of "id". We fallback to "id" to
         * keep backwards compatibility.**
         *
         * @param eventProductMap Event's product.
         * @throws IllegalArgumentException if the product does not have an ID.
         */
        private fun setProductId(eventProductMap: Map<String, String>) {
            if (productIdentifier != null) {
                // When productIdentifier is "id" use the default behavior.
                if (productIdentifier != "id") {
                    id = eventProductMap.get(productIdentifier)
                }
            }

            // Fallback to "productId" as V2 ecommerce spec
            if (id == null || id!!.trim { it <= ' ' }.isEmpty()) {
                id = eventProductMap["productId"]
            }

            // Fallback to "product_id" as V2 ecommerce spec
            if (id == null || id!!.trim { it <= ' ' }.isEmpty()) {
                id = eventProductMap["product_id"]
            }

            // Fallback to "id" as V1 ecommerce spec
            if (id == null || id!!.trim { it <= ' ' }.isEmpty()) {
                id = eventProductMap["id"]
            }
            require(!(id == null || id!!.trim { it <= ' ' }
                .isEmpty())) { "Product id is not defined." }
        }

        /**
         * Builds a string out of product properties category, name, quantity and price to send to
         * Adobe.
         *
         * @return A single string of product properties, in the format `category;name;quantity;price;
         * examples: `athletic;shoes;1;10.0`, `;shoes;1;0.0`, `;123;;`
         */
        override fun toString(): String {
            val builder = StringBuilder()

            // Category
            if (category != null && category.trim { it <= ' ' }.isNotEmpty()) {
                builder.append(category)
            }
            builder.append(";")

            // Id
            if (id != null && id!!.trim { it <= ' ' }.isNotEmpty()) {
                builder.append(id)
            }
            builder.append(";")

            // Quantity
            builder.append(quantity)
            builder.append(";")

            // Price
            builder.append(price)
            return builder.toString()
        }
    }

    /**
     * Defines an array of products.
     */
    internal inner class Products {
        private var products: ArrayList<Product> = arrayListOf()

        constructor(eventProductsJsonArray: JsonArray) {
            products = ArrayList(eventProductsJsonArray.size)
            for (eventProductJsonObject in eventProductsJsonArray) {
                try {
                    products.add(Product((eventProductJsonObject as JsonObject).asStringMap()))
                } catch (e: IllegalArgumentException) {
                    // We ignore the product
                    analytics.log(
                        "You must provide a name for each product to pass an ecommerce event"
                                + "to Adobe Analytics."
                    )
                }
            }
        }

        constructor(productsMap: MutableMap<String, String>) {
            products = ArrayList(1)
            try {
                products.add(Product(productsMap))
            } catch (e: IllegalArgumentException) {
                // We ignore the product
                analytics.log(
                    "You must provide a name for each product to pass an ecommerce event"
                            + "to Adobe Analytics."
                )
            }
        }

        val isEmpty: Boolean
            get() = products.isEmpty()

        /**
         * Builds a string out of product properties category, name, quantity and price to send to
         * Adobe.
         *
         * @return A single string of product properties, in the format `category;name;quantity;price;
         * examples: `athletic;shoes;1;10.0`, `;shoes;1;0.0`
         * */
        override fun toString(): String {
            val builder = StringBuilder()
            for (i in products.indices) {
                builder.append(products[i].toString())
                if (i < products.size - 1) {
                    builder.append(',')
                }
            }
            return builder.toString()
        }
    }
}