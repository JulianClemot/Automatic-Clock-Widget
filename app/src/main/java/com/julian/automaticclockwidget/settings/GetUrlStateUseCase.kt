package com.julian.automaticclockwidget.settings

/** Snapshot of the current URLs and the selected one. */
data class UrlState(
    val urls: List<String>,
    val selected: String?
)

class GetUrlStateUseCase(private val repo: UrlPreferencesRepository) {
    fun getUrlState(): Result<UrlState> = runCatching {
        val urls = repo.getUrls().getOrElse { throw it }
        val selected = repo.getSelectedUrl().getOrElse { throw it }
        UrlState(urls = urls, selected = selected)
    }
}
