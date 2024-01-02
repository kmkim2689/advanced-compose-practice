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

## 2. Drag and Drop List in Compose LazyList
### 키워드
* LazyListState : 스크롤 관련 동작을 Observable하기 위한 변수 : LazyList 구현 시, 스크롤과 관련한 데이터들을 관찰할 수 있도록 하는 state 변수
  * firstVisibleItemIndex : 현재 화면의 시점에서 첫 번째로 보이는 아이템의 인덱스
  * firstVisibleItemScrollOffset : 현재 시점에서 첫 번째로 보이는 아이템이 어느 정도로 스크롤이 발생했는지 나타낸다. -> 아이템 경계를 넘어 스크롤할 때 자동 재설정
  * 예시 : https://onlyfor-me-blog.tistory.com/690
  * 이것으로부터 얻을 수 있는 것들
    * layoutInfo : LazyListLayoutInfo를 제공
```
@Stable
class LazyListState constructor(
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0
) : ScrollableState
```

* LazyListLayoutInfo : 
  * Contains useful information about the currently displayed layout state of lazy lists like LazyColumn or LazyRow. For example you can get the list of currently displayed item.
  * 얻을 수 있는 정보들
    * visibleItemsInfo : 현재 시점에서 보이는 아이템들에 대한 LazyListItemInfo의 List 형태를 반환
    * totalItemsCount : LazyColumn으로 렌더링되는 모든 아이템의 개수
    * 등등

* LazyListItemInfo : LazyList의 "각 아이템"에 대한 유용한 정보를 제공한다.
```
interface LazyListItemInfo {
    // The index of the item in the list.
    val index: Int

    // The key of the item which was passed to the item() or items() function.
    val key: Any

    // The main axis offset of the item in pixels. It is relative to the start of the lazy list container.
    val offset: Int

    /**
     * The main axis size of the item in pixels. Note that if you emit multiple layouts in the composable
     * slot for the item then this size will be calculated as the sum of their sizes.
     */
    val size: Int
}
```

### 구현
* DragDropListExtensions : drag drop을 위한 확장함수를 정의
```
// 여기서 absolute란, Drag의 대상이 되는 / 선택된 아이템의 인덱스 번호를 가리킨다. 
fun LazyListState.getVisibleItemInfoFor(absolute: Int): LazyListItemInfo? {
    return this.layoutInfo.visibleItemsInfo.getOrNull(absolute - this.layoutInfo.visibleItemsInfo.first().index)
}

// 선택된 LazyList의 아이템에 대해 하단 끝 부분의 offset을 픽셀 단위로 얻어오기 위함
val LazyListItemInfo.offsetEnd: Int
    get() = this.offset + this.size // offset은 상단의 offset 픽셀을 가리키고, size는 해당 아이템의 길이(높이)의 픽셀을 가리킨다.

fun <T> MutableList<T>.move(from: Int, to: Int) {
    if (from == to) return
    
    // removeAt : 리스트에서 n번 인덱스의 요소를 제거하고(drag), 그 요소를 "반환"
    val element = this.removeAt(from) ?: return
    
    // 임시로 element에 받아놓은 것을 목표로 하는 인덱스(drop한 곳)으로 위치 변경
    this.add(to, element)
}
```

* DragDropListState
