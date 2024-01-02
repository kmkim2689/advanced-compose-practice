# Compose UI Practice
> Compose에서 사용될 수 있는 UI 구현법을 정리하고 연습하기 위한 Repository

## 1. Light & Dark Theme

* Libraries Needed : Hilt
```
implementation("com.google.dagger:hilt-android:2.44")
kapt("com.google.dagger:hilt-android-compiler:2.44")

// for hiltviewmodel
implementation("androidx.hilt:hilt-navigation-compose:1.0.0")
```

### 사전 작업 : Hilt 이용을 위한 Application 재정의 작업
```
@HiltAndroidApp
class MyApplication : Application() {
}

// AndroidManifest의 application 태그에 설정
```

### Theme in Jetpack Compose
* ThemeSetting.kt
```
enum class AppTheme {
    MODE_AUTO, // 0
    MODE_DAY, // 1
    MODE_NIGHT; // 2

    companion object {
        // index로 enum요소에 접근
        fun fromOrdinal(ordinal: Int) = values()[ordinal]
    }
}

interface ThemeSetting {
    val themeStream: StateFlow<AppTheme>
    val theme: AppTheme
}
```

* ThemeSettingPreference
```
class ThemeSettingPreference @Inject constructor(
    @ApplicationContext appContext: Context
) : ThemeSetting {

    override val theme: AppTheme by AppThemePreferenceDelegate("app_theme", AppTheme.MODE_AUTO)
    
    override val themeStream: MutableStateFlow<AppTheme>
        get() = MutableStateFlow(theme)
    
    private val preferences: SharedPreferences = appContext.getSharedPreferences("sample_theme", Context.MODE_PRIVATE)

    inner class AppThemePreferenceDelegate(
        private val name: String,
        private val default: AppTheme
    ) : ReadWriteProperty<Any?, AppTheme> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): AppTheme {
            return AppTheme.fromOrdinal(preferences.getInt(name, default.ordinal))
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: AppTheme) {
            themeStream.value = value
            preferences.edit { 
                putInt(name, value.ordinal)
            }
        }
    }
}
```

* SettingModule
```
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingModule {
    @Binds
    @Singleton
    abstract fun provideThemeSetting(
        themeSettingPreference: ThemeSettingPreference
    ): ThemeSetting
}
```

* 액티비티에서 테마 결정 반영
```
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject lateinit var themeSetting: ThemeSetting
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            
            val theme = themeSetting.themeStream.collectAsState()
            // boolean value whether to use dark theme or not
            val useDarkColors = when (theme.value) {
                AppTheme.MODE_AUTO -> isSystemInDarkTheme()
                AppTheme.MODE_DAY -> false
                AppTheme.MODE_NIGHT -> true
            }
            
            MyTheme(
                // darktheme
                darkTheme = useDarkColors
            ) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ComposeThemeScreen(
                        onItemSelected = { theme ->
                            themeSetting.theme = theme
                        }
                    )
                }
            }
        }
    }
}

```