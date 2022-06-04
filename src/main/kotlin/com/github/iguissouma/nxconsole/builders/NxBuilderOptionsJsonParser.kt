package com.github.iguissouma.nxconsole.builders

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

object NxBuilderOptionsJsonParser {
    fun parse(output: String?): List<NxBuilderOptions> {
        val listType = object : TypeToken<ArrayList<NxBuilderOptions?>?>() {}.type
        return GsonBuilder().create().fromJson(output, listType)
    }
}
