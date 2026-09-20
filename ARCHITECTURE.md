# Architecture

Game News lists free-to-play titles from the FreeToGame API and shows details for one of
them, reading everything through a local database so the app works without a network.

## Layers

```
        +-------------------------------------------+
        |                    ui                     |
        |  list / detail / components / theme       |
        |  Compose + ViewModel + StateFlow          |
        +---------------------+---------------------+
                              |  depends on
                              v
        +-------------------------------------------+
        |                  domain                   |
        |  model (plain data classes)               |
        |  repository (interfaces)                  |
        +---------------------+---------------------+
                              ^  implements
                              |
        +---------------------+---------------------+
        |                   data                    |
        |  remote  ->  mapper  ->  local            |
        |  repository (implementations)             |
        +-------------------------------------------+

  di/ wires data implementations into domain interfaces at the composition root.
```

Dependency rules, enforced by convention and reviewed in code review:

- `domain` imports nothing from `data` or `ui`, and no Android or framework types.
- `data` imports `domain` (to implement its interfaces) but never `ui`.
- `ui` imports `domain` only. It never sees a DTO, an Entity, or Retrofit/Room types.
- `di` is the only package allowed to know about all three.

Arrows point inward. The database and the network are details that `domain` does not know
about; `data` depends on `domain`, not the other way around.

---

## Decisions

### One Gradle module instead of a multi-module setup

**Decision.** Everything lives in `:app`, separated by package.

**Why.** The app has two screens, one data source and roughly forty files. Module
boundaries buy compile-time enforcement of the dependency rules and parallel/incremental
build wins, and both of those are close to worthless at this size — a clean build is
under a minute. What they cost is real: a `:core:model`, `:core:database`,
`:core:network`, `:feature:list`, `:feature:detail` split needs convention plugins, five
more build files, and a reviewer has to open six directories to follow one request end to
end.

**Rejected.** A `:core` / `:feature` split as in Now in Android. It is the right shape for
an app with many teams and many features; here it would be ceremony that hides a small
codebase behind build logic.

**When this stops being good.** When a second feature team needs to work without stepping
on the first, when the clean build passes a couple of minutes, or when the package-level
dependency rules start being violated in review — at that point the compiler should be
enforcing them, not a human. The package structure above maps one-to-one onto modules, so
the split is mechanical when it is needed.

### Three representations (DTO, Entity, domain model) instead of one class

**Decision.** `GameListItemDto` / `GameEntity` / `Game` are separate types, with mapper
functions between them in `data/mapper`.

**Why.** The three have genuinely different obligations. The DTO's shape is dictated by
someone else's server: snake_case names, and every field nullable because the API omits
them freely. The Entity's shape is dictated by SQLite: a primary key, flattened embedded
objects, a JSON column for screenshots. The domain model's shape is dictated by what the
UI needs: non-null strings, names that read well at the call site. Fusing them means one
class carrying `@SerialName`, `@Entity` and `@PrimaryKey` at once, where a rename on the
server changes a database column, and a schema migration changes JSON parsing. It also
means the UI handling `String?` everywhere, because the API said so.

The mapper is where "absent field" becomes a decision made once, in a place that is
trivially unit-testable without Android, Room or a network.

**Rejected.** A single annotated class shared across layers. It is roughly 150 fewer lines
and it is the reason a lot of small apps cannot change their database without touching
their JSON. Also rejected: keeping DTO and Entity separate but exposing the Entity to the
UI — that leaks Room into Compose and makes the UI untestable without a database.

**When this stops being good.** If a model were a pure pass-through with no naming, no
nullability and no structural differences across all three layers, the mappers would be
noise. That is not the case here: the detail payload alone needs a nullable embedded
object and a JSON-encoded list.

### Two domain models (`Game`, `GameDetails`) instead of one nullable one

**Decision.** The list endpoint and the detail endpoint get their own domain model and
their own table.

**Why.** `GET /games` cannot produce `description`, `status`, `screenshots` or
`minimum_system_requirements`. A single model would have to declare those nullable, and
then every detail screen would be written defensively against nulls that can only occur
because of where the object was loaded from. Two types make the guarantee structural: if
you are holding a `GameDetails`, the description is there.

**Rejected.** One `Game` with nullable detail fields. It saves a class and a table and
moves the cost into every consumer. Also rejected: a `Game` plus a separate
`GameExtras`, which is the same nullability problem with an extra indirection.

**When this stops being good.** If the API ever returned the full payload from the list
endpoint, the two models would collapse into one and the second table would go away.

### Offline-first with Room as the single source of truth

**Decision.** The repository exposes `Flow`s that come from the DAO. Network calls are
one-shot `suspend` functions that only write into Room and return `Result<Unit>`. The UI
has no path to the network.

**Why.** There is exactly one place where data comes from, so there is exactly one path a
screen update can take: something wrote to the database, the DAO's `Flow` emitted, the
screen re-rendered. Cold start shows cached content immediately instead of a spinner. A
failed refresh cannot blank out content that is already on screen, because the failure is
a return value on a separate call, not an emission on the read stream. Correct behaviour
on a flaky connection falls out of the structure rather than being handled case by case.

**Rejected.** Fetching from the network into the ViewModel and treating the database as an
optional cache: that produces two sources of truth and the classic bug where a slow
network response overwrites fresher local state. Also rejected: `NetworkBoundResource`,
which emits loading/success/error wrappers down the same stream as the data and ends up
re-implementing the state machine that the sealed UI state already covers.

**When this stops being good.** When writes go the other way — if the app ever posts
user-generated data, it needs a sync/outbox layer with conflict handling, and
"network only writes to the database" becomes too simple. Also if a payload were large
enough that caching it whole is wasteful, paging would need to enter the picture
(Room + Paging keeps the same shape, so this is an extension rather than a rewrite).

### StateFlow instead of LiveData

**Decision.** ViewModels expose `StateFlow<UiState>`; Compose collects with
`collectAsStateWithLifecycle()`.

**Why.** The rest of the data path is already `Flow` — Room returns `Flow`, the repository
maps `Flow`. Exposing `LiveData` would mean converting at the last step for no gain.
`StateFlow` always has a value, which matches "the screen is always in some state", and it
is a plain Kotlin type, so ViewModel tests need no Android framework and no
`InstantTaskExecutorRule`. `collectAsStateWithLifecycle` gives the lifecycle-aware
collection that was `LiveData`'s main advantage.

**Rejected.** `LiveData` (lifecycle awareness is now available to `Flow`, and it drags in
an Android dependency that makes the ViewModel harder to test). Also rejected: exposing
Compose `State` directly from the ViewModel, which couples the ViewModel to Compose and
blocks reuse from any non-Compose consumer or test.

**When this stops being good.** It does not, for this shape of app. If a screen ever needs
one-shot events (navigate once, show one snackbar), `StateFlow` is the wrong tool for
*that* part and a `Channel` should be added alongside it — state and events are different
things and conflating them is where `SingleLiveEvent` came from.

### Sealed interface for UI state instead of a flags data class

**Decision.** `GameListUiState` is a sealed interface with `Loading`, `Empty`, `Content`
and `Error`.

**Why.** The states are mutually exclusive, and a sealed hierarchy is how you say that to
the compiler. `data class UiState(isLoading, games, error)` can represent
"loading and error and has data", which is meaningless, and every composable then has to
decide in what order to check the flags — usually inconsistently. With a sealed interface
a `when` is exhaustive, adding a fifth state produces compile errors at exactly the places
that must handle it, and the data each state needs lives on that state (`Content` holds a
non-empty list; `Error` holds a cause) instead of being nullable on a shared class.

`Content` carries `isRefreshing` as a flag deliberately: refreshing is not a separate
state, it is a modifier on having content, and that is what lets a failed background
refresh leave existing content on screen.

**Rejected.** A flags data class (representable-but-impossible states). Also rejected: a
generic `Resource<T>` wrapper — it is the same four cases with less type information, and
`Resource.Success(emptyList())` still leaves "empty" to be re-derived at every call site.

**When this stops being good.** When a screen accumulates several independent axes of
state — say filter + sort + selection + load status — the hierarchy starts to multiply
combinatorially, and the right answer becomes a data class of small sealed properties
rather than one big sealed root.

### Hand-written fakes instead of MockK

**Decision.** `FakeGameDao`, `FakeGameApi` and `FakeGameRepository` implement the real
interfaces, backed by `MutableStateFlow`. No mocking framework.

**Why.** The interfaces being faked are small and they are ours, so a fake is a few dozen
lines. What the fake gives that a mock does not is *behaviour*: `FakeGameDao` really
stores rows, so `upsertGames` followed by `observeGames` emits the new rows — which is
precisely the offline-first property the repository test needs to assert. Mocking that
means stubbing a `Flow` per call and hand-wiring the relationship between write and read,
which is both more code and a re-statement of assumptions rather than a test of them.
Mocks also couple tests to call sequences, so refactoring the implementation breaks tests
that should not care. And MockK's bytecode manipulation is a measurable share of unit test
runtime.

**Rejected.** MockK (verbose for stateful collaborators, brittle, slow). Also rejected:
Mockito, which additionally fights Kotlin's final-by-default classes. For the network
layer, `MockWebServer` was considered and rejected — it tests Retrofit and the JSON
converter, which are already tested by their authors; a fake at the API interface is a
faster test of *our* code. That changes if custom interceptors or error-code handling
appear.

**When this stops being good.** When faking an interface stops being cheap — a third-party
interface with thirty methods, or one whose contract we do not control. At that point the
cost of a fake exceeds the cost of a stub, and a mocking library earns its place.

---

## Dependencies

| Library | What it is for | Alternative considered | Why this one |
| --- | --- | --- | --- |
| Jetpack Compose + Material 3 | UI | XML views + Fragments | Less boilerplate, state-driven rendering that pairs naturally with `StateFlow`; Material 3 is the current design system and gets dynamic color for free. |
| Navigation Compose | Screen navigation | Manual `when` on a state enum | Handles back stack, argument passing and process death. Type-safe routes (`@Serializable` destinations) mean the compiler checks navigation arguments instead of string parsing at runtime. |
| ViewModel + StateFlow | Screen state, survives rotation | Holding state in composables (`rememberSaveable`) | State must survive configuration change and be driven from a coroutine scope tied to the screen, not the composition. |
| Coroutines / Flow | Async and streams | RxJava | Kotlin-native, the language has structured concurrency, and Room/Retrofit both speak `suspend`/`Flow` directly — no adapters. |
| Retrofit | HTTP client | Ktor client, bare OkHttp | Declarative interface definitions mean the API surface is readable in one file, and `suspend` support is first-class. Ktor is a good choice for KMP, which is not a goal here. |
| kotlinx.serialization | JSON | Moshi, Gson | Compile-time generated, reflection-free serializers (works cleanly with R8, no `@Keep` rules). Same library serializes the navigation routes and the screenshots JSON column. Gson is unmaintained and null-unsafe from Kotlin. |
| OkHttp + logging interceptor | Transport, debug visibility | Default `HttpURLConnection` | Retrofit's engine anyway; connection pooling and interceptors. Logging is `BODY` in debug and `NONE` in release so no payloads reach logcat in production. |
| Room | Local persistence, source of truth | SQLDelight, DataStore, plain SQLite | Compile-time verified SQL, `Flow` return types (which is what makes single-source-of-truth observable), first-party migrations. DataStore is for key-value preferences, not a queryable list. |
| Hilt | Dependency injection | Koin, manual construction | Compile-time validated graph — a missing binding is a build error, not a crash. Standard Android integration for `Application`, `Activity` and `ViewModel` scoping. Koin resolves at runtime; manual DI works but the wiring code grows faster than the app. |
| KSP | Annotation processing for Room and Hilt | KAPT | KAPT generates Java stubs and is roughly twice as slow; it is in maintenance mode. KSP is also the only option here — AGP 9's built-in Kotlin support does not apply the Kotlin Gradle Plugin that KAPT needs. |
| JUnit 4 | Test runner | JUnit 5 | The Android tooling, Compose test rules and Hilt test rules are all built around JUnit 4 rules. JUnit 5 on Android still needs a third-party plugin. |
| kotlinx-coroutines-test | Virtual time, `Dispatchers.Main` swap | `runBlocking` + real delays | `runTest` skips delays, so tests run in milliseconds and deterministically. |
| Turbine | Asserting on `Flow` emissions | Collecting into a list in a background job | Makes "this Flow emitted exactly these values and nothing else" a one-liner, and fails loudly on unconsumed emissions instead of silently passing. |

### Version notes

The project was generated with AGP 9.2.1 / Kotlin 2.2.10 / Gradle 9.4.1 on JDK 21. Two
constraints followed from that and are worth knowing before bumping anything:

- **AGP 9 has built-in Kotlin support.** Applying `org.jetbrains.kotlin.android` now fails
  with `Cannot add extension with name 'kotlin'`. Only the compiler-plugin plugins
  (`kotlin.plugin.compose`, `kotlin.plugin.serialization`) are applied. KSP must come from
  the version-decoupled `2.3.x` line, which does not require the Kotlin Gradle Plugin;
  the older `2.2.10-2.0.2` scheme does and is therefore unusable here.
- **`compileSdk` is 36.1, and several current releases require 37.** Pinned one step back
  for that reason: OkHttp 5.4.0 (5.5.0+ needs 37), Lifecycle 2.10.0 (2.11.0 needs 37),
  Navigation Compose 2.9.8 (2.10.x needs 37), `hilt-navigation-compose` 1.3.0 (1.4.0 needs
  37), `core-ktx` 1.18.0 (1.19.0 needs 37). Raising `compileSdk` to 37 unblocks all of
  them at once and is the first thing to do when that platform is installed.

Room schemas are exported to `app/schemas/` and should be committed, so migrations show up
in diffs rather than being discovered at runtime.

---

## Decisions made while implementing

The sections above were written against the skeleton. These are the choices the
implementation forced, and they are the ones I would most expect to be asked about.

### `refreshGames()` returns `Result<Int>`, not `Result<Unit>`

**Decision.** A successful refresh reports how many games the API listed.

**Why.** With `Result<Unit>`, the list screen flashed "No games to show yet" on every cold
start. The cause is that Room announces a write *asynchronously*: the sequence is

1. Room emits `[]` — the cache is empty → `Loading`
2. `refreshGames()` writes 417 rows and returns success → refresh state settles
3. `combine` recomputes: games are still `[]`, the refresh succeeded → **`Empty` renders**
4. Room's invalidation tracker delivers the rows → `Content`

Step 3 is the bug. `Empty` is defined as "the cache is empty *and* the API genuinely
returned nothing", and `Result<Unit>` cannot distinguish that from "rows were written and
the database has not announced them yet". The count can: a success carrying `> 0` means
the rows are on their way, so the screen stays in `Loading`; a success carrying `0` is the
only thing that means `Empty`. The state machine no longer depends on which of two
asynchronous sources happens to win a race.

**Rejected.** Debouncing the transition into `Empty` by a few hundred milliseconds. It
works, it is three lines, and it makes correctness a function of how fast the device is.
Also rejected: deriving `Empty` only from a database emission that arrives after the
refresh settles — correct for the common case, but if the API legitimately returns an
empty list into an already-empty cache, Room never emits again and the spinner never ends.

**When this stops being good.** If refreshes ever become partial (paging, incremental
sync), a single count stops describing the outcome and this wants to be a small result
type rather than an `Int`.

The detail screen has the identical race and needed no signature change: a successful
detail refresh always writes exactly one row, so a settled success with no cached row can
only mean "not delivered yet", and the screen stays in `Loading`.

### A 404 is a domain fact, so `GameNotFoundException` lives in `domain`

**Decision.** `refreshGameDetails` converts an HTTP 404 into `GameNotFoundException`; every
other failure passes through unchanged.

**Why.** `GameDetailUiState.Empty` means "this game does not exist" and `Error` means "we
could not reach it" — those want different screens, because only one of them has a
sensible retry button. Only the data layer can see a 404, and the UI layer is not allowed
to know Retrofit exists, so the classification has to happen at the data boundary. It is
in `domain` rather than `data` because "this game is gone" is a fact about the domain, not
about transport; the repository interface is where that promise belongs.

**Rejected.** Letting `HttpException` reach the ViewModel. It would work and it would put
`retrofit2` in the import list of a Compose ViewModel, which is exactly the dependency
rule this project claims to follow.

**When this stops being good.** At the third distinguishable failure (a 5xx worth
retrying, a serialization failure worth reporting) this stops being one exception and
becomes the sealed `DataError` described under future work. Two cases do not justify the
hierarchy; four would.

### `runCatchingCancellable` instead of `runCatching`

**Decision.** The repository catches `Throwable`, but rethrows `CancellationException`
first.

**Why.** `runCatching` catches `Throwable`, and in a coroutine that includes the
`CancellationException` used to unwind a cancelled scope. Swallowing it turns "the user
left the screen" into `Result.failure`, and the coroutine machinery stops being able to
tell that cancellation happened — structured concurrency quietly breaks. The repository's
refresh functions are called from `viewModelScope`, which is cancelled on every
`ViewModel.onCleared()`, so this is the normal path, not an edge case.

**Rejected.** `runCatching` as-is, which is the version most samples ship.

### The UI layer classifies `Throwable`, in exactly one place

**Decision.** `Throwable.toUiErrorCause()` sits next to `UiErrorCause` in `ui/`, and both
screens use it.

**Why.** Two screens need the same mapping and the detail state originally imported the
enum out of `ui.list`, which is a package boundary violation waiting to become a cycle. A
shared file makes the mapping exhaustive in one place: adding a cause produces compile
errors at both screens' `when`.

**When this stops being good.** The moment the data layer gains the sealed `DataError`
above, this function becomes a `when` over a closed type instead of an `is IOException`
guess, and `UiErrorCause` may collapse into it entirely.

### Detekt configured as deltas, not a dumped default

**Decision.** `config/detekt/detekt.yml` contains only rules that differ from the default,
with `buildUponDefaultConfig = true`, and `ignoreFailures = false`.

**Why.** A generated 22 KB default config is unreviewable: nobody can tell which lines are
intentional. Every entry in the file is a decision with a comment explaining it — that
`@Composable` functions are PascalCase and long by nature, that an ARGB literal is the
colour rather than a magic number, that the repository's broad catch is deliberate. A
finding fails the build, because a warning that scrolls past in a log is not a check.

### Robolectric for one test class only

**Decision.** `GameDetailViewModelTest` runs on Robolectric; every other JVM test does not.

**Why.** Reading the type-safe navigation argument goes
`SavedStateHandle` → `Bundle`, and a plain JVM test fails with
`Method putInt in android.os.BaseBundle not mocked`. That is the only Android dependency
in the class, so it gets the runner and the rest of the suite stays fast. It is configured
with a plain `Application` so Hilt is not dragged into a test that constructs the
ViewModel directly.

**Rejected.** Injecting the id as a constructor parameter purely to dodge the framework.
That would make the test pass by deleting the thing under test — how the destination
argument is read is exactly what could break.

---

## What I would do differently on a larger project

- **Modularise.** `:core:model`, `:core:database`, `:core:network`, `:core:designsystem`,
  then `:feature:list` and `:feature:detail`. The point is not build speed, it is that
  `:feature:detail` physically cannot import `:feature:list`, and `:core:model` cannot
  import Room. Convention plugins in `build-logic` to stop the build files diverging.
- **Add use cases where they earn their place.** Right now every ViewModel call maps
  one-to-one onto a repository call, so a `GetGamesUseCase` would be a pass-through. Once
  logic is shared across screens — filtering, sorting, combining two sources — that logic
  belongs in `domain`, not duplicated in two ViewModels.
- **A real error type instead of `Result<Throwable>`.** Half-done: a 404 is already
  translated into `GameNotFoundException` at the data boundary, but everything else still
  reaches the UI as a raw `Throwable` that `toUiErrorCause()` inspects with
  `is IOException`. A sealed `DataError` (`Network`, `Server(code)`, `Serialization`,
  `Unknown`) mapped in `data/` lets the UI decide what is retryable and what message to
  show via an exhaustive `when`, without guessing from exception classes.
- **Paging.** The list endpoint returns every game in one response. That is fine at the
  current size and will not stay fine; Paging 3 with a `RemoteMediator` keeps Room as the
  source of truth while bounding memory.
- **Background sync.** Refresh currently happens when a screen opens. WorkManager with a
  periodic constraint-aware job would keep the cache warm and make "offline-first" true on
  cold start after days offline, not just minutes.
- **Screenshots as a child table.** They are a JSON column today because they are only
  ever read with their game. If anything ever needs to query across screenshots, that
  becomes a `screenshots` table with a foreign key and a `@Relation`.
- **CI.** Detekt, the unit suite and `assembleDebug` on every PR. Detekt is wired in and
  fails the build locally; what is missing is the machine that runs it for you.
- **A proper date type.** `releaseDate` is a `String` because the API returns
  `"2022-10-04"` and `java.time` needs desugaring at `minSdk 24`. Enabling core library
  desugaring and parsing to `LocalDate` at the mapper boundary makes sorting and
  formatting honest, and is a prerequisite for anything locale-aware.

---

## Proposals

These were listed as gaps in the skeleton. The first three were adopted while
implementing; the rest remain open, in the order I would add them.

**Adopted**

1. **Coil** (`io.coil-kt.coil3:coil-compose` + `coil-network-okhttp`). Both screens show
   remote images and fetch/decode/cache/cancel-on-scroll is not worth hand-rolling. The
   `ImageLoader` is registered explicitly in `GameNewsApplication` via
   `SingletonImageLoader.Factory` rather than relying on Coil's classpath auto-discovery,
   so image loading fails at compile time if the fetcher is ever dropped, not at runtime.
2. **Instrumented tests** (`androidx.room:room-testing`, `androidx.test.ext:junit`).
   `GameDaoTest` runs against a real in-memory SQLite database. These are the assertions
   the JVM suite cannot make: that the queries are valid SQL, that the JSON type converter
   and the nullable `@Embedded` group round-trip, and that a write really does push a new
   value down the read `Flow` — the property the whole offline-first design rests on.
3. **Detekt** as a build-failing check, configured as deltas from the default — see the
   decision above.

**Still open**

4. **MockWebServer** (`com.squareup.okhttp3:mockwebserver`). Worth it once interceptors or
   HTTP status-code mapping grow beyond the single 404 case. Faking at the API interface
   covers today's needs.
5. **Compose UI tests** (`ui-test-junit4`). The two screens are stateless functions of a
   sealed state, so each branch is directly assertable without Hilt or a network.
6. **CI.** GitHub Actions running `detekt`, `testDebugUnitTest` and `assembleDebug` on
   every push. The checks exist; nothing enforces them yet.
7. **Core library desugaring** (`com.android.tools:desugar_jdk_libs`), to allow
   `java.time` at `minSdk 24` — see the date-type point above.
