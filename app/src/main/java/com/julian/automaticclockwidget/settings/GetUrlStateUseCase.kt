package com.julian.automaticclockwidget.settings

/** Snapshot of the current calendar entries and the selected URL. */
data class UrlState(
    val entries: List<CalendarEntry>,
    val selected: String?,
)

class GetUrlStateUseCase(private val repo: UrlPreferencesRepository) {
    fun getUrlState(): Result<UrlState> = runCatching {
        val entries = repo.getEntries().getOrElse { throw it }
        val selected = repo.getSelectedUrl().getOrElse { throw it }
        UrlState(entries = entries, selected = selected)
    }
}
