package model.mge

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class Bases(
    val map: List<MapElement> = listOf()
)

@Serializable
data class MapElement(
    @SerializedName("Sector")
    val sector: Sector,
)

@Serializable
data class Sector(
    @SerializedName("Type")
    val type: String,
    val data: SectorData,
)

@Serializable
data class SectorData(
    val dynamicData: SectorDynData?,
)

@Serializable
data class SectorDynData(
    val structures: Structures,
    val controlledBy: String,
)

@Serializable
data class Structures(
    val familyClub: FamilyClub,
    val garage: Garage,
    val arsenal: Arsenal,
    val stock: Stock,
    val pub: Pub,
    val headquarter: Headquarter,
    val gamblingClub: GamblingClub,
)

@Serializable
data class FamilyClub(
    val level: Int,
) {
    val name: String
        get() = "Семейный клуб"
}

@Serializable
data class Garage(
    val level: Int,
) {
    val name: String
        get() = "Гараж"
}

@Serializable
data class Arsenal(
    val level: Int,
) {
    val name: String
        get() = "Арсенал"
}

@Serializable
data class Stock(
    val level: Int,
) {
    val name: String
        get() = "Склад"
}

@Serializable
data class Pub(
    val level: Int,
) {
    val name: String
        get() = "Бар"
}

@Serializable
data class Headquarter(
    val level: Int,
) {
    val name: String
        get() = "Штаб-квартира"
}

@Serializable
data class GamblingClub(
    val level: Int,
) {
    val name: String
        get() = "Игорный клуб"
}