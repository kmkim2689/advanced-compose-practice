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
  * recomposition 와중에도 LazyColumn/Row/Grid에 대한 정보를 유지할 수 있도록 하기 위해 제작
  * 매개 변수
    * lazyListState : rememberLazyListState() -> compose 상에서 제공
    * onMove : (startIndex: Int, endIndex: Int) -> Unit // 옮기고자 하는 아이템의 본래 인덱스 -> 이동하고자 하는 인덱스로 이동을 구현
```
@Composable
fun rememberDragDropListState(
    lazyListState: LazyListState = rememberLazyListState(),
    onMove: (Int, Int) -> Unit
): DragDropListState {
    return remember {
        DragDropListState(lazyListState, onMove)
    }
}

class DragDropListState(
    val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit
) {
    // 해당 아이템이 얼마만큼 드래그 되었는지를 거리로 나타냄(픽셀)
    var draggedDistance by mutableStateOf(0f)

    // 드래그가 되는 대상의 아이템에 대한 정보 : LazyListItemInfo
    var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)

    // 드래그가 되는 대상의 아이템이 현재 어느 인덱스에 위치하는지에 대한 정보 : wjdtn
    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)

    // 드래그 되는 대상의 드래그 전 상단 끝, 하단 끝 오프셋을 얻어옴
    val initialOffsets: Pair<Int, Int>?
        get() = initiallyDraggedElement?.let { lazyListItemInfo ->
            // 현재 드래그 되고 있는 아이템의 상단 끝 오프셋과 하단 끝 오프셋을 얻어옴
            Pair(lazyListItemInfo.offset, lazyListItemInfo.offsetEnd)
        }

    val elementDisplacement: Float?
        get() = currentIndexOfDraggedItem?.let {
            lazyListState.getVisibleItemInfoFor(absolute = it)
        }?.let { lazyListItemInfo ->
            (initiallyDraggedElement?.offset?.toFloat() ?: 0f) + draggedDistance - lazyListItemInfo.offset
        }

    val currentElement: LazyListItemInfo?
        get() = currentIndexOfDraggedItem?.let {
            lazyListState.getVisibleItemInfoFor(it)
        }

    var overScrollJob by mutableStateOf<Job?>(null)

    fun onDragStart(offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { lazyListItemInfo ->
            offset.y.toInt() in lazyListItemInfo.offset..(lazyListItemInfo.offset + lazyListItemInfo.size)
        }?.also {
            currentIndexOfDraggedItem = it.index
            initiallyDraggedElement = it
        }
    }

    fun onDragInterrupted() {
        draggedDistance = 0f
        currentIndexOfDraggedItem = null
        initiallyDraggedElement = null
        overScrollJob?.cancel()
    }

    fun onDrag(offset: Offset) {
        draggedDistance += offset.y

        initialOffsets?.let { (topOffset, bottomOffset) ->
            val startOffset = topOffset + draggedDistance
            val endOffset = bottomOffset + draggedDistance

            currentElement?.let { hovered ->
                lazyListState.layoutInfo.visibleItemsInfo.filterNot { lazyListItemInfo ->
                    lazyListItemInfo.offsetEnd < startOffset || lazyListItemInfo.offset > endOffset || hovered.index == lazyListItemInfo.index
                }.firstOrNull { lazyListItemInfo ->
                    val delta = startOffset - hovered.offset
                    when {
                        delta > 0 -> (endOffset > lazyListItemInfo.offsetEnd)
                        else -> (startOffset < lazyListItemInfo.offset)
                    }
                }?.also { lazyListItemInfo ->
                    currentIndexOfDraggedItem?.let { current ->
                        onMove.invoke(current, lazyListItemInfo.index)
                    }
                    currentIndexOfDraggedItem = lazyListItemInfo.index
                }
            }
        }
    }

    fun checkForOverScroll(): Float {
        return initiallyDraggedElement?.let {
            val startOffset = it.offset + draggedDistance
            val endOffset = it.offsetEnd + draggedDistance

            return@let when {
                draggedDistance > 0 -> (endOffset - lazyListState.layoutInfo.viewportEndOffset).takeIf { diff ->
                    diff > 0
                }
                draggedDistance < 0 -> (startOffset - lazyListState.layoutInfo.viewportStartOffset).takeIf { diff ->
                    diff < 0
                }
                else -> null
            }
        } ?: 0f
    }
}
```

* DragDropList
```
@Composable
fun DragDropList(
    items: MutableList<String>,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var overScrollJob by remember {
        mutableStateOf<Job?>(null)
    }
    val dragDropListState = rememberDragDropListState(onMove = onMove)

    LazyColumn(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDropListState.onDrag(offset = dragAmount)

                        if (overScrollJob?.isActive == true) return@detectDragGesturesAfterLongPress

                        dragDropListState
                            .checkForOverScroll()
                            .takeIf { it != 0f }
                            ?.let {
                                overScrollJob = scope.launch {
                                    dragDropListState.lazyListState.scrollBy(it)
                                }
                            } ?: kotlin.run { overScrollJob?.cancel() }
                    },
                    onDragStart = { offset ->
                        dragDropListState.onDragStart(offset)
                    },
                    onDragEnd = { dragDropListState.onDragInterrupted() },
                    onDragCancel = { dragDropListState.onDragInterrupted() }
                )
            }
            .fillMaxSize()
            .padding(10.dp),
        state = dragDropListState.lazyListState
    ) {
        itemsIndexed(items) {index, item ->
            Column(
                modifier = Modifier
                    .composed {
                        val offsetOrNull = dragDropListState.elementDisplacement.takeIf {
                            index == dragDropListState.currentIndexOfDraggedItem
                        }
                        graphicsLayer {
                            translationY = offsetOrNull ?: 0f
                        }
                    }
                    .background(
                        color = Color.Gray,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(text = item, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
```

## 3. Shorts Video Template(Sliding Up & Down)

### 필요 라이브러리들
```
implementation 'com.google.android.exoplayer:exoplayer:2.14.1'

    implementation 'com.github.bumptech.glide:glide:4.12.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.12.0'
```

### 키워드
* inline, crossinline, noinline
* Modifier.composed
* mutableStateOf()의 policy parameter => structuralEqualityPolicy()
* Int/Long.coerceIn(minimumValue: Int/Long, maximumValue: Int/Long): Int/Long
  * ensures that the value lies in the specific range between minimumValue and maximumValue
  * 만약 해당 값이 minimumValue보다 작다면, minimumValue를 반환하고, 해당 값이 maximumValue보다 크다면, maximumValue 반환
* 함수의 매개변수로 특정 클래스의 확장함수를 넘겨주기 : 클래스명.() -> 리턴타입
* Animatable : 클래스명이 아니라 함수이다.
  * 

### 구현

* ModifierExtensions.kt : 클릭 시 ripple 효과가 없는 clickable을 구현하기 위함
```
inline fun Modifier.clickableWithoutRipple(
    crossinline onClick: () -> Unit
): Modifier = composed { 
    this.clickable(
        indication = null,
        interactionSource = remember {
            MutableInteractionSource()
        },
        onClick = {
            onClick()
        }
    )
}
```

* PagerState
