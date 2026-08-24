package com.hehe.mushroom_app_v3

data class MushroomInfo(
    val speciesName: String,
    val isPoisonous: Boolean,
    val characteristics: List<String>
)

object MushroomData {

    private val data = mapOf(

        //BERACUN
        "Amanita_muscaria" to MushroomInfo(
            speciesName = "Amanita muscaria",
            isPoisonous = true,
            characteristics = listOf(
                "Bright red cap covered with white wart-like spots",
                "White stem with a ring and a volva at the base",
                "Contains ibotenic acid and muscimol, which may cause hallucinations and poisoning"
            )
        ),
        "Amanita_pantherina" to MushroomInfo(
            speciesName = "Amanita pantherina",
            isPoisonous = true,
            characteristics = listOf(
                "Brown cap with evenly distributed white warts",
                "White stem with a thin ring and bulbous base",
                "Contains potent toxins that may cause seizures and severe poisoning"
            )
        ),
        "Clitocybe_nebularis" to MushroomInfo(
            speciesName = "Clitocybe nebularis",
            isPoisonous = true,
            characteristics = listOf(
                "Large gray cap with a cloudy appearance",
                "Cream-colored gills extending slightly down the stem",
                "May cause gastrointestinal poisoning in sensitive individuals"
            )
        ),
        "Gyromitra_gigas" to MushroomInfo(
            speciesName = "Gyromitra gigas",
            isPoisonous = true,
            characteristics = listOf(
                "Brain-like wrinkled cap with a pale brown color",
                "Short white hollow stem",
                "Contains gyromitrin, a toxin harmful to the liver and kidneys"
            )
        ),
        "Lactarius_torminosus" to MushroomInfo(
            speciesName = "Lactarius torminosus",
            isPoisonous = true,
            characteristics = listOf(
                "Tudung merah muda hingga oranye dengan tepi berbulu dan bergelombang",
                "Mengeluarkan getah putih seperti susu saat dipotong",
                "Getah sangat pahit dan pedas, menyebabkan kram perut parah"
            )
        ),
        "Paxillus_involutus" to MushroomInfo(
            speciesName = "Paxillus involutus",
            isPoisonous = true,
            characteristics = listOf(
                "Olive-brown cap with rolled inward margins",
                "Brownish-yellow gills that separate easily",
                "Repeated consumption may cause fatal hemolytic anemia"
            )
        ),
        "Stropharia_aeruginosa" to MushroomInfo(
            speciesName = "Stropharia aeruginosa",
            isPoisonous = true,
            characteristics = listOf(
                "Blue-green slimy cap",
                "White fibrous ring on the stem",
                "May cause nausea and gastrointestinal poisoning"
            )
        ),
        "Fomitopsis_pinicola" to MushroomInfo(
            speciesName = "Fomitopsis pinicola",
            isPoisonous = true,
            characteristics = listOf(
                "Shelf-shaped fruiting body attached to tree trunks",
                "Concentric bands of red, orange, and brown on the upper surface",
                "Hard woody texture and not suitable for consumption"
            )
        ),
        "Amanita_rubescens" to MushroomInfo(
            speciesName = "Amanita rubescens",
            isPoisonous = true,
            characteristics = listOf(
                "Reddish-brown cap with grayish patches",
                "White flesh bruises reddish when cut or damaged",
                "Contains heat-sensitive toxins and should not be consumed"
            )
        ),
        "Pholiota_squarrosa" to MushroomInfo(
            speciesName = "Pholiota squarrosa",
            isPoisonous = false,
            characteristics = listOf(
                "Yellowish-brown cap densely covered with dark pointed scales",
                "Usually grows in dense clusters on tree trunks",
                "May cause digestive discomfort and should not be consumed"
            )
        ),

        //DAPAT DIMAKAN

        "Boletus_edulis" to MushroomInfo(
            speciesName = "Boletus edulis",
            isPoisonous = false,
            characteristics = listOf(
                "Large brown cap with a smooth surface",
                "White to yellow pores instead of gills",
                "Firm white flesh that does not change color when cut"
            )
        ),
        "Cantharellus_cibarius" to MushroomInfo(
            speciesName = "Cantharellus cibarius",
            isPoisonous = false,
            characteristics = listOf(
                "Bright golden-yellow color",
                "Forked ridges instead of true gills",
                "Pleasant fruity aroma similar to apricot"
            )
        ),
        "Coprinus_comatus" to MushroomInfo(
            speciesName = "Coprinus comatus",
            isPoisonous = false,
            characteristics = listOf(
                "Tall white shaggy cap with scales",
                "Gills turn black and liquefy as the mushroom matures",
                "Best consumed while young before blackening begins"
            )
        ),
        "Flammulina_velutipes" to MushroomInfo(
            speciesName = "Flammulina velutipes",
            isPoisonous = false,
            characteristics = listOf(
                "Small orange-yellow sticky cap",
                "Long dark velvety stem",
                "Commonly known as Enoki mushroom"
            )
        ),

        "Leccinum_scabrum" to MushroomInfo(
            speciesName = "Leccinum scabrum",
            isPoisonous = false,
            characteristics = listOf(
                "Grayish-brown cap",
                "White stem covered with dark rough scales",
                "White pores underneath the cap"
            )
        ),
        "Lycoperdon_perlatum" to MushroomInfo(
            speciesName = "Lycoperdon perlatum",
            isPoisonous = false,
            characteristics = listOf(
                "Round white puffball covered with tiny spines",
                "Interior is solid white when young",
                "Edible only while the inside remains pure white"
            )
        ),
        "Macrolepiota_procera" to MushroomInfo(
            speciesName = "Macrolepiota procera",
            isPoisonous = false,
            characteristics = listOf(
                "Large umbrella-shaped cap with brown scales",
                "Tall stem with snakeskin-like pattern",
                "Movable ring around the stem"
            )
        ),

        "Pleurotus_ostreatus" to MushroomInfo(
            speciesName = "Pleurotus ostreatus",
            isPoisonous = false,
            characteristics = listOf(
                "Fan-shaped gray to white cap",
                "White gills extending down the stem",
                "Commonly known as Oyster mushroom"
            )
        ),
        "Suillus_granulatus" to MushroomInfo(
            speciesName = "Suillus granulatus",
            isPoisonous = false,
            characteristics = listOf(
                "Sticky yellow-brown cap",
                "Yellow pores producing milky droplets",
                "Yellow stem with fine brown granules"
            )
        ),
        "Verpa_bohemica" to MushroomInfo(
            speciesName = "Verpa bohemica",
            isPoisonous = false,
            characteristics = listOf(
                "Bell-shaped wrinkled brown cap",
                "Cap attached only at the top of the stem",
                "Must be thoroughly cooked before consumption"
            )
        )
    )

    fun getInfo(speciesKey: String): MushroomInfo? = data[speciesKey]
}
