package com.segment.analytics.kotlin.destinations.adobeanalytics

import com.segment.analytics.kotlin.core.Properties

/**
 * Encapsulates Context Data settings:
 * @param prefix : Prefix for extra properties
 * @param contextDataVariables : Mappings for context data variables
 */
class ContextDataConfiguration(
    /**
     * Gets the prefix added to all extra properties not defined in the translation map.
     *
     * @return Prefix.
     */
    private var prefix: String?,
    /**
     * Retrieves context data variables map, using keys as Segment's event fields and values the
     * corresponding Adobe Analytics variable name.
     *
     * @return The translation map between Segment fields and Adobe Analytics variables.
     */
    private var contextDataVariables: Map<String, String>?
) {

    init {
        if (contextDataVariables == null) {
            contextDataVariables = HashMap()
        }
        // "a." is reserved by Adobe Analytics
        if (this.prefix == null || this.prefix == "a.") {
            this.prefix = ""
        }
    }

    /**
     * Retrieves a set of Segment fields that has associated a Adobe Analytics variable.
     *
     * @return Set of keys.
     */
    val eventFieldNames: Set<String>
        get() = contextDataVariables!!.keys

    /**
     * Gets the variable name associated with field name from contextDataVariables
     *
     * @return Context Value
     */
    fun getVariableName(fieldName: String): String? {
        return contextDataVariables!![fieldName]
    }

    /**
     * Gets the prefix added to all extra properties not defined in the translation map.
     *
     * @return Prefix.
     */
    fun getPrefix(): String? {
        return prefix
    }

    /**
     * Inspects the event payload and retrieves the value described in the field. Field respects dot
     * notation (myObject.name) for event properties. If there is a dot present at the beginning of
     * the field, it will retrieve the value from the root of the payload.
     *
     *
     * Examples:
     *
     *
     *  * `myObject.name` = `track.properties.myObject.name`
     *  * `.userId` = `identify.userId`
     *  * `.context.library` = `track.context.library`
     *
     *
     * @param field Field name.
     * @param payloadProperties Event payload.
     * @return The value if found, `null` otherwise.
     */
    fun searchValue(field: String?, payloadProperties: Properties): Any? {
        require(!(field == null || field.trim { it <= ' ' }
            .isEmpty())) { "The field name must be defined" }
        var searchPaths = field.split("\\.").toTypedArray()

        // Using the properties object as starting point by default.
        val values = payloadProperties.asStringMap()

        // Dot is present at the beginning of the field name
        if (searchPaths[0] == "") {
            // Using the root of the payload as starting point
            searchPaths = searchPaths.copyOfRange(1, searchPaths.size)
        }
        return searchValue(searchPaths, values)
    }

    private fun searchValue(searchPath: Array<String>, values: Map<String, String>): Any? {
        var currentValues: Map<String, String> = values
        for (i in searchPath.indices) {
            val path = searchPath[i]
            require(path.trim { it <= ' ' }.isNotEmpty()) { "Invalid field name" }
            if (!currentValues.containsKey(path)) {
                return null
            }
            val value: Any = currentValues.get(path) ?: return null
            if (i == searchPath.size - 1) {
                return value
            }
            if (value is Map<*, *>) {
                currentValues = value.toMap() as Map<String, String>
            } else if (value is Map<*, *>) {
                try {
                    currentValues = value.toMap() as Map<String, String>
                } catch (e: ClassCastException) {
                    return null
                }
            }
        }
        return null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ContextDataConfiguration
        return contextDataVariables == that.contextDataVariables && prefix == that.prefix
    }

    override fun hashCode(): Int {
        var hash = 31 + prefix.hashCode()
        hash = 31 * hash + contextDataVariables.hashCode()
        return hash
    }
}
