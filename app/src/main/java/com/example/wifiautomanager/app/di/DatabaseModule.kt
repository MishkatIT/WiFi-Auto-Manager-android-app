package com.example.wifiautomanager.app.di

import android.content.Context
import androidx.room.Room
import com.example.wifiautomanager.data.local.db.AppDatabase
import com.example.wifiautomanager.data.local.db.dao.DecisionLogDao
import com.example.wifiautomanager.data.local.db.dao.RuleDao
import com.example.wifiautomanager.data.local.db.dao.WifiNetworkDao
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "wifi_auto_manager.db"
        )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideWifiNetworkDao(database: AppDatabase): WifiNetworkDao =
        database.wifiNetworkDao()

    @Provides
    fun provideRuleDao(database: AppDatabase): RuleDao =
        database.ruleDao()

    @Provides
    fun provideDecisionLogDao(database: AppDatabase): DecisionLogDao =
        database.decisionLogDao()

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()
}
