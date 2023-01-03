package com.segment.analytics.kotlin.destinations.adobeanalytics

import com.segment.analytics.kotlin.core.utilities.toContent
import kotlinx.serialization.json.JsonObject

internal fun JsonObject.asStringMap(): Map<String, String> = this.mapValues { (_, value) ->
    value.toContent().toString()
}