package com.julian.automaticclockwidget.clocks

class ClearClocksUseCase(private val repo: ClocksPreferencesRepository) {
    fun clearClocks(): Result<Unit> = repo.clearClocks()
}
