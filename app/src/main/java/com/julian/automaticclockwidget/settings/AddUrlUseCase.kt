package com.julian.automaticclockwidget.settings

class AddUrlUseCase(private val repo: UrlPreferencesRepository) {
    fun addUrl(url: String): Result<Unit> {
        return repo.addUrl(url)
    }
}
