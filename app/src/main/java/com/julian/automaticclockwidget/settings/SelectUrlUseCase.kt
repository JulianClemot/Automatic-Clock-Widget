package com.julian.automaticclockwidget.settings

class SelectUrlUseCase(private val repo: UrlPreferencesRepository) {
    fun selectUrl(url: String): Result<Unit> {
        return repo.selectUrl(url)
    }
}
