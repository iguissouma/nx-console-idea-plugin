package com.github.iguissouma.nxconsole.schematics

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.util.ArrayList

object NxSchematicsJsonParser {
    fun parse(output: String?): List<NxSchematicCollection> {
        val listType = object : TypeToken<ArrayList<NxSchematicCollection?>?>() {}.type
        return GsonBuilder().create().fromJson(output, listType)
    }
}
