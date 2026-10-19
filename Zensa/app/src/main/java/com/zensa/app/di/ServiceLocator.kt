package com.zensa.app.di


import android.content.Context
import com.zensa.app.BuildConfig
import com.zensa.app.core.network.NetworkMonitor
import com.zensa.app.data.local.ZensaDatabase
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.preferences.UserPreferences
import com.zensa.app.data.remote.AuthInterceptor
import com.zensa.app.data.remote.ZensaApiService
import com.zensa.app.data.repository.AchievementRepository
import com.zensa.app.data.repository.AssignmentRepository
import com.zensa.app.data.repository.AuthRepository
import com.zensa.app.data.repository.ModuleRepository
import com.zensa.app.data.repository.NoteRepository
import com.zensa.app.data.repository.SettingsRepository
import com.zensa.app.data.repository.StudySessionRepository
import com.zensa.app.data.repository.SubtaskRepository
import com.zensa.app.data.repository.SyncRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit


object ServiceLocator {

    private lateinit var tokenStorageInstance: TokenStorage
    private lateinit var userPreferences: UserPreferences
    private lateinit var database: ZensaDatabase
    private lateinit var api: ZensaApiService
    private lateinit var networkMonitor: NetworkMonitor

    lateinit var authRepository: AuthRepository
        private set
    lateinit var moduleRepository: ModuleRepository
        private set
    lateinit var assignmentRepository: AssignmentRepository
        private set
    lateinit var subtaskRepository: SubtaskRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var studySessionRepository: StudySessionRepository
        private set
    lateinit var noteRepository: NoteRepository
        private set
    lateinit var achievementRepository: AchievementRepository
        private set
    lateinit var syncRepository: SyncRepository
        private set

    fun init(context: Context) {
        if (::tokenStorageInstance.isInitialized) return
        val app = context.applicationContext

        tokenStorageInstance = TokenStorage(app)
        userPreferences = UserPreferences(app)
        database = ZensaDatabase.get(app)
        networkMonitor = NetworkMonitor(app)

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStorageInstance))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ZensaApiService::class.java)

        authRepository = AuthRepository(api, tokenStorageInstance)

        moduleRepository = ModuleRepository(
            dao = database.moduleDao(),
            api = api
        )

        assignmentRepository = AssignmentRepository(
            dao = database.assignmentDao(),
            moduleDao = database.moduleDao(),
            api = api
        )

        subtaskRepository = SubtaskRepository(
            dao = database.subtaskDao(),
            assignmentDao = database.assignmentDao(),
            api = api
        )

        settingsRepository = SettingsRepository(
            preferences = userPreferences,
            api = api
        )

        studySessionRepository = StudySessionRepository(
            dao = database.studySessionDao(),
            moduleDao = database.moduleDao(),
            api = api
        )

        noteRepository = NoteRepository(
            dao = database.noteDao(),
            moduleDao = database.moduleDao(),
            api = api
        )

        achievementRepository = AchievementRepository(
            dao = database.achievementDao()
        )

        syncRepository = SyncRepository(
            appContext = app,
            queueDao = database.syncQueueDao(),
            moduleDao = database.moduleDao(),
            assignmentDao = database.assignmentDao(),
            subtaskDao = database.subtaskDao(),
            studySessionDao = database.studySessionDao(),
            noteDao = database.noteDao(),
            api = api,
            networkMonitor = networkMonitor
        )

        // Wire the sync repository back into the other repositories so
        // every failed server write is queued for background sync.
        moduleRepository.syncRepository = syncRepository
        assignmentRepository.syncRepository = syncRepository
        subtaskRepository.syncRepository = syncRepository
        studySessionRepository.syncRepository = syncRepository
        noteRepository.syncRepository = syncRepository
    }

    fun tokenStorage(): TokenStorage = tokenStorageInstance
    fun userPreferences(): UserPreferences = userPreferences
    fun networkMonitor(): NetworkMonitor = networkMonitor
}
