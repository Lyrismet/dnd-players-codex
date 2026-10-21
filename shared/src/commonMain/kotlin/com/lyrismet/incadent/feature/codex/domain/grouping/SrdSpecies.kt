package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.core.format.foldForSearch
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType

/** a creature name from the SRD 5.1, only the name and the type, no stats */
data class SrdSpecies(
    val nameRu: String,
    val nameEn: String,
    val type: CreatureType,
) {
    fun name(russian: Boolean): String = if (russian) nameRu else nameEn
}

const val SRD_SUGGESTION_LIMIT = 4

/** the SRD names that match what is typed, names that start with it first, an exact match is not offered again */
fun srdSuggestions(
    query: String,
    russian: Boolean,
    limit: Int = SRD_SUGGESTION_LIMIT,
): List<SrdSpecies> {
    val typed = query.trim().foldForSearch()
    if (typed.isEmpty()) return emptyList()
    val matching =
        SrdCatalog.species
            .map { it to it.name(russian).foldForSearch() }
            .filter { (_, name) -> typed in name && name != typed }
    return matching
        .sortedBy { (_, name) -> if (name.startsWith(typed)) 0 else 1 }
        .map { it.first }
        .take(limit)
}

/** the SRD names not yet among [existingNames], each with a negative select id that can't meet a stored species id */
fun srdNotAdded(existingNames: List<String>): List<Pair<Long, SrdSpecies>> {
    val have = existingNames.map { it.foldForSearch() }.toSet()
    return SrdCatalog.species
        .withIndex()
        .filter { (_, srd) -> srd.nameRu.foldForSearch() !in have && srd.nameEn.foldForSearch() !in have }
        .map { (index, srd) -> -(index + 1L) to srd }
}

/** the built-in list the species form suggests from */
object SrdCatalog {
    val species: List<SrdSpecies> =
        listOf(
            SrdSpecies("Аболет", "Aboleth", CreatureType.ABERRATION),
            SrdSpecies("Летучая мышь", "Bat", CreatureType.BEAST),
            SrdSpecies("Чёрный медведь", "Black Bear", CreatureType.BEAST),
            SrdSpecies("Бурый медведь", "Brown Bear", CreatureType.BEAST),
            SrdSpecies("Волк", "Wolf", CreatureType.BEAST),
            SrdSpecies("Лютый волк", "Dire Wolf", CreatureType.BEAST),
            SrdSpecies("Гигантский паук", "Giant Spider", CreatureType.BEAST),
            SrdSpecies("Гигантская крыса", "Giant Rat", CreatureType.BEAST),
            SrdSpecies("Гигантская лягушка", "Giant Frog", CreatureType.BEAST),
            SrdSpecies("Кабан", "Boar", CreatureType.BEAST),
            SrdSpecies("Пантера", "Panther", CreatureType.BEAST),
            SrdSpecies("Лев", "Lion", CreatureType.BEAST),
            SrdSpecies("Крокодил", "Crocodile", CreatureType.BEAST),
            SrdSpecies("Орёл", "Eagle", CreatureType.BEAST),
            SrdSpecies("Сова", "Owl", CreatureType.BEAST),
            SrdSpecies("Пегас", "Pegasus", CreatureType.CELESTIAL),
            SrdSpecies("Единорог", "Unicorn", CreatureType.CELESTIAL),
            SrdSpecies("Коатль", "Couatl", CreatureType.CELESTIAL),
            SrdSpecies("Оживлённые доспехи", "Animated Armor", CreatureType.CONSTRUCT),
            SrdSpecies("Плотяной голем", "Flesh Golem", CreatureType.CONSTRUCT),
            SrdSpecies("Каменный голем", "Stone Golem", CreatureType.CONSTRUCT),
            SrdSpecies("Молодой красный дракон", "Young Red Dragon", CreatureType.DRAGON),
            SrdSpecies("Виверна", "Wyvern", CreatureType.DRAGON),
            SrdSpecies("Огненный элементаль", "Fire Elemental", CreatureType.ELEMENTAL),
            SrdSpecies("Воздушный элементаль", "Air Elemental", CreatureType.ELEMENTAL),
            SrdSpecies("Земляной элементаль", "Earth Elemental", CreatureType.ELEMENTAL),
            SrdSpecies("Водный элементаль", "Water Elemental", CreatureType.ELEMENTAL),
            SrdSpecies("Горгулья", "Gargoyle", CreatureType.ELEMENTAL),
            SrdSpecies("Спрайт", "Sprite", CreatureType.FEY),
            SrdSpecies("Дриада", "Dryad", CreatureType.FEY),
            SrdSpecies("Сатир", "Satyr", CreatureType.FEY),
            SrdSpecies("Имп", "Imp", CreatureType.FIEND),
            SrdSpecies("Квазит", "Quasit", CreatureType.FIEND),
            SrdSpecies("Огр", "Ogre", CreatureType.GIANT),
            SrdSpecies("Тролль", "Troll", CreatureType.GIANT),
            SrdSpecies("Холмовой великан", "Hill Giant", CreatureType.GIANT),
            SrdSpecies("Гоблин", "Goblin", CreatureType.HUMANOID),
            SrdSpecies("Хобгоблин", "Hobgoblin", CreatureType.HUMANOID),
            SrdSpecies("Багбир", "Bugbear", CreatureType.HUMANOID),
            SrdSpecies("Кобольд", "Kobold", CreatureType.HUMANOID),
            SrdSpecies("Орк", "Orc", CreatureType.HUMANOID),
            SrdSpecies("Гнолл", "Gnoll", CreatureType.HUMANOID),
            SrdSpecies("Людоящер", "Lizardfolk", CreatureType.HUMANOID),
            SrdSpecies("Сахуагин", "Sahuagin", CreatureType.HUMANOID),
            SrdSpecies("Бандит", "Bandit", CreatureType.HUMANOID),
            SrdSpecies("Культист", "Cultist", CreatureType.HUMANOID),
            SrdSpecies("Стражник", "Guard", CreatureType.HUMANOID),
            SrdSpecies("Головорез", "Thug", CreatureType.HUMANOID),
            SrdSpecies("Разведчик", "Scout", CreatureType.HUMANOID),
            SrdSpecies("Шпион", "Spy", CreatureType.HUMANOID),
            SrdSpecies("Убийца", "Assassin", CreatureType.HUMANOID),
            SrdSpecies("Рыцарь", "Knight", CreatureType.HUMANOID),
            SrdSpecies("Маг", "Mage", CreatureType.HUMANOID),
            SrdSpecies("Простолюдин", "Commoner", CreatureType.HUMANOID),
            SrdSpecies("Совомедведь", "Owlbear", CreatureType.MONSTROSITY),
            SrdSpecies("Мимик", "Mimic", CreatureType.MONSTROSITY),
            SrdSpecies("Василиск", "Basilisk", CreatureType.MONSTROSITY),
            SrdSpecies("Гарпия", "Harpy", CreatureType.MONSTROSITY),
            SrdSpecies("Мантикора", "Manticore", CreatureType.MONSTROSITY),
            SrdSpecies("Минотавр", "Minotaur", CreatureType.MONSTROSITY),
            SrdSpecies("Медуза", "Medusa", CreatureType.MONSTROSITY),
            SrdSpecies("Ржавый монстр", "Rust Monster", CreatureType.MONSTROSITY),
            SrdSpecies("Желатиновый куб", "Gelatinous Cube", CreatureType.OOZE),
            SrdSpecies("Серая слизь", "Gray Ooze", CreatureType.OOZE),
            SrdSpecies("Охровое желе", "Ochre Jelly", CreatureType.OOZE),
            SrdSpecies("Трент", "Treant", CreatureType.PLANT),
            SrdSpecies("Ползучий курган", "Shambling Mound", CreatureType.PLANT),
            SrdSpecies("Скелет", "Skeleton", CreatureType.UNDEAD),
            SrdSpecies("Зомби", "Zombie", CreatureType.UNDEAD),
            SrdSpecies("Гуль", "Ghoul", CreatureType.UNDEAD),
            SrdSpecies("Призрак", "Ghost", CreatureType.UNDEAD),
            SrdSpecies("Умертвие", "Wight", CreatureType.UNDEAD),
            SrdSpecies("Вампир", "Vampire", CreatureType.UNDEAD),
        )
}
