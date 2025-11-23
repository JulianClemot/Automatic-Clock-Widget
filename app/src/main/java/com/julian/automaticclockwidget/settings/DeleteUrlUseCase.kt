package com.julian.automaticclockwidget.settings

class DeleteUrlUseCase(private val repo: UrlPreferencesRepository) {
    fun deleteUrl(url: String): Result<Unit> {
        return repo.deleteUrl(url)
    }
}
