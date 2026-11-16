package app.vdvoem.feature.dates

import androidx.annotation.StringRes

/** Бюджет идеи свидания — фильтр на экране «Идеи свиданий». */
enum class Budget(@StringRes val labelRes: Int) {
    /** Бесплатно / дома. */
    HOME(R.string.dates_budget_home),

    /** До 1500 ₽. */
    CHEAP(R.string.dates_budget_cheap),

    /** До 5000 ₽. */
    MEDIUM(R.string.dates_budget_medium),

    /** Без лимита. */
    SPLURGE(R.string.dates_budget_splurge)
}

/** Одна идея свидания: тексты живут в ресурсах, здесь только id и оформление. */
data class DateIdea(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val textRes: Int,
    val budget: Budget,
    val emoji: String
)

/**
 * Курируемый список идей для пары. Статичная коллекция: без сети, без синка,
 * без пользовательских данных — поэтому живёт прямо в коде модуля.
 */
object DateIdeas {

    val all: List<DateIdea> = listOf(
        idea("home_cinema", R.string.dates_idea_home_cinema_title, R.string.dates_idea_home_cinema_text, Budget.HOME, "🍿"),
        idea("evening_walk", R.string.dates_idea_evening_walk_title, R.string.dates_idea_evening_walk_text, Budget.HOME, "🌆"),
        idea("picnic", R.string.dates_idea_picnic_title, R.string.dates_idea_picnic_text, Budget.CHEAP, "🧺"),
        idea("board_games", R.string.dates_idea_board_games_title, R.string.dates_idea_board_games_text, Budget.HOME, "🎲"),
        idea("cook_together", R.string.dates_idea_cook_together_title, R.string.dates_idea_cook_together_text, Budget.CHEAP, "🍳"),
        idea("spa_evening", R.string.dates_idea_spa_evening_title, R.string.dates_idea_spa_evening_text, Budget.HOME, "🛁"),
        idea("day_trip", R.string.dates_idea_day_trip_title, R.string.dates_idea_day_trip_text, Budget.MEDIUM, "🚗"),
        idea("concert", R.string.dates_idea_concert_title, R.string.dates_idea_concert_text, Budget.MEDIUM, "🎶"),
        idea("exhibition", R.string.dates_idea_exhibition_title, R.string.dates_idea_exhibition_text, Budget.CHEAP, "🖼"),
        idea("skating", R.string.dates_idea_skating_title, R.string.dates_idea_skating_text, Budget.CHEAP, "⛸"),
        idea("breakfast_bed", R.string.dates_idea_breakfast_bed_title, R.string.dates_idea_breakfast_bed_text, Budget.HOME, "🥐"),
        idea("photoshoot", R.string.dates_idea_photoshoot_title, R.string.dates_idea_photoshoot_text, Budget.CHEAP, "📸"),
        idea("quest", R.string.dates_idea_quest_title, R.string.dates_idea_quest_text, Budget.MEDIUM, "🔍"),
        idea("masterclass", R.string.dates_idea_masterclass_title, R.string.dates_idea_masterclass_text, Budget.MEDIUM, "🎨"),
        idea("bike_ride", R.string.dates_idea_bike_ride_title, R.string.dates_idea_bike_ride_text, Budget.CHEAP, "🚲"),
        idea("planetarium", R.string.dates_idea_planetarium_title, R.string.dates_idea_planetarium_text, Budget.CHEAP, "🪐"),
        idea("oceanarium", R.string.dates_idea_oceanarium_title, R.string.dates_idea_oceanarium_text, Budget.MEDIUM, "🐠"),
        idea("karaoke", R.string.dates_idea_karaoke_title, R.string.dates_idea_karaoke_text, Budget.CHEAP, "🎤"),
        idea("bowling", R.string.dates_idea_bowling_title, R.string.dates_idea_bowling_text, Budget.CHEAP, "🎳"),
        idea("bookstore", R.string.dates_idea_bookstore_title, R.string.dates_idea_bookstore_text, Budget.CHEAP, "📚"),
        idea("hotel_night", R.string.dates_idea_hotel_night_title, R.string.dates_idea_hotel_night_text, Budget.SPLURGE, "🏨"),
        idea("stargazing", R.string.dates_idea_stargazing_title, R.string.dates_idea_stargazing_text, Budget.HOME, "✨"),
        idea("museum_evening", R.string.dates_idea_museum_evening_title, R.string.dates_idea_museum_evening_text, Budget.CHEAP, "🏛"),
        idea("sushi_home", R.string.dates_idea_sushi_home_title, R.string.dates_idea_sushi_home_text, Budget.CHEAP, "🍣"),
        idea("pottery", R.string.dates_idea_pottery_title, R.string.dates_idea_pottery_text, Budget.MEDIUM, "🏺"),
        idea("cafe_breakfast", R.string.dates_idea_cafe_breakfast_title, R.string.dates_idea_cafe_breakfast_text, Budget.CHEAP, "☕"),
        idea("theatre", R.string.dates_idea_theatre_title, R.string.dates_idea_theatre_text, Budget.MEDIUM, "🎭"),
        idea("sauna", R.string.dates_idea_sauna_title, R.string.dates_idea_sauna_text, Budget.CHEAP, "🧖"),
        idea("sunrise", R.string.dates_idea_sunrise_title, R.string.dates_idea_sunrise_text, Budget.HOME, "🌅"),
        idea("train_weekend", R.string.dates_idea_train_weekend_title, R.string.dates_idea_train_weekend_text, Budget.SPLURGE, "🚂")
    )

    /** Идеи под выбранный бюджет; `null` — весь список. */
    fun byBudget(budget: Budget?): List<DateIdea> = when (budget) {
        null -> all
        else -> all.filter { it.budget == budget }
    }
}

private fun idea(id: String, titleRes: Int, textRes: Int, budget: Budget, emoji: String) =
    DateIdea(id = id, titleRes = titleRes, textRes = textRes, budget = budget, emoji = emoji)