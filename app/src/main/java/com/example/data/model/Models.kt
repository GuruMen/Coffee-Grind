package com.example.data.model

data class Category(
    val id: String,
    val name: String,
    val nameRu: String,
    val minUm: Int,
    val maxUm: Int
)

data class BrewMethod(
    val id: String,
    val nameEn: String,
    val nameRu: String,
    val minUm: Int,
    val maxUm: Int,
    val ratio: Float,
    val dose: Float,
    val tip: String,
    val star: Boolean = false,
    val desc: String = ""
)

data class MethodRange(
    val str: String,
    val lowPos: Float,
    val highPos: Float,
    val midPos: Float,
    val midStr: String,
    val lowUm: Int,
    val highUm: Int,
    val midUm: Int
)

data class Grinder(
    val idx: Int,
    val brand: String,
    val model: String,
    val url: String,
    val unit: String,
    val note: String,
    val minUm: Int,
    val maxUm: Int,
    val fmt: String,
    val step: Float,
    val keepZero: Boolean,
    val minPos: Float,
    val maxPos: Float,
    val minScale: String,
    val maxScale: String,
    val anchors: List<Pair<Int, Float>>, // Pair<microns, position>
    val methods: Map<String, String>, // methodId -> displayString
    val methodRanges: Map<String, MethodRange?> // methodId -> range
) {
    val key: String get() = "$brand::$model"
    val fullName: String get() = "$brand $model"
}
