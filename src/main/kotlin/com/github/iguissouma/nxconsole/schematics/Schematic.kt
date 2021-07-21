// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.iguissouma.nxconsole.schematics

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Schematic {

    @SerializedName("name")
    @Expose
    var name: String? = null

    @SerializedName("description")
    @Expose
    var description: String? = null

    @SerializedName("options")
    @Expose
    var options: MutableList<Option> = ArrayList()

    @SerializedName("arguments")
    @Expose
    var arguments: MutableList<Option> = ArrayList()

    @SerializedName("error")
    @Expose
    var error: String? = null

    @SerializedName("hidden")
    @Expose
    var hidden: Boolean = false

    constructor()

    constructor(name: String, description: String?, options: MutableList<Option>, arguments: MutableList<Option>) {
        this.name = name
        this.description = description
        this.options = options
        this.arguments = arguments
    }

    override fun toString(): String {
        return name!!
    }
}

class Option {

    @SerializedName("name")
    @Expose
    var name: String? = null

    @SerializedName("default")
    @Expose
    var default: Any? = null

    @SerializedName("description")
    @Expose
    var description: String? = null

    @SerializedName("type")
    @Expose
    var type: String? = null

    @SerializedName("required")
    @Expose
    var isRequired: Boolean = false

    @SerializedName("visible")
    @Expose
    var isVisible: Boolean = true

    @SerializedName("enum")
    @Expose
    var enum: MutableList<String> = ArrayList()

    @SerializedName("format")
    @Expose
    var format: String? = null

    @SerializedName("positional")
    @Expose
    var positional: Int? = null

    @SerializedName("\$default")
    @Expose
    var _default: Map<String, Any>? = null

    @SerializedName("tooltip")
    @Expose
    var tooltip: String? = null

    @SerializedName("itemTooltips")
    @Expose
    var itemTooltips: Map<String, Any>? = null

    constructor()

    constructor(name: String) {
        this.name = name
    }

    override fun toString(): String {
        return name!!
    }
}
