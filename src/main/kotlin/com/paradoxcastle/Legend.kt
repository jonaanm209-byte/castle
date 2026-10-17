package com.paradoxcastle

/** The two summonable legendaries and everything that differs between them. */
enum class Legend(
    /** Cobblemon species id used with PokemonProperties. */
    val species: String,
    /** Level the legendary spawns at. */
    val level: Int,
    /** RGB colour of the key's charge bar. */
    val barColor: Int,
    val displayKey: String
) {
    KORAIDON("koraidon", 70, 0xE0452F, "legend.paradoxcastle.koraidon"),
    MIRAIDON("miraidon", 70, 0x9B4DE0, "legend.paradoxcastle.miraidon")
}
