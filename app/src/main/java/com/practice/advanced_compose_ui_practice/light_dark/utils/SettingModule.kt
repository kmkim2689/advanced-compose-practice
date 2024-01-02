package com.practice.advanced_compose_ui_practice.light_dark.utils

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingModule {
    @Binds
    @Singleton
    abstract fun provideThemeSetting(
        themeSettingPreference: ThemeSettingPreference
    ): ThemeSetting
}