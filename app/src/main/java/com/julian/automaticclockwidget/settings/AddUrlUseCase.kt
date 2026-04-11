package com.julian.automaticclockwidget.settings

class AddUrlUseCase(private val repo: UrlPreferencesRepository) {
    fun addUrl(name: String, url: String): Result<Unit> = repo.addEntry(name, url)
}
