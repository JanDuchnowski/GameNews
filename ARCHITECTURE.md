# Architecture

Game News lists free-to-play titles from the FreeToGame API and shows details for one of
them, reading everything through a local database so the app works without a network.

## Layers

```
ui      Compose + ViewModel + StateFlow      ──┐
                                               ├──> domain    models, repository interfaces
data    Retrofit, Room, mappers, repositories ─┘
```

- `domain` imports nothing from `data` or `ui`, and no Android or framework types.
- `data` implements the `domain` interfaces and never imports `ui`.
- `ui` sees `domain` only — never a DTO, an Entity, or a Retrofit or Room type.
- `di` is the only package that knows about all three.

Arrows point inward: the database and the network are details that `domain` does not know
about.

## Decisions

**One Gradle module, split by package.** Two screens and one data source. The package
structure maps one-to-one onto modules, so the split is mechanical if the project ever
grows enough to need compile-time enforcement of the rules above.

**No use cases.** Every ViewModel call maps one-to-one onto a repository call, so a
`GetGamesUseCase` would be a pass-through that exists only to satisfy a diagram. When
logic starts being shared between screens — filtering, sorting, combining sources — it
belongs in `domain` rather than duplicated in two ViewModels, and that is when the layer
earns its place.

**Three representations of a game** (`GameListItemDto`, `GameEntity`, `Game`). The DTO's
shape is dictated by someone else's server — snake_case, everything nullable. The
Entity's is dictated by SQLite. The domain model's is dictated by what the UI needs:
non-null strings. The mappers in `data/mapper` are where "the API omitted this field"
becomes a decision made once, instead of `String?` leaking into Compose.

**Two domain models** (`Game`, `GameDetails`). The list endpoint cannot return a
description, screenshots or system requirements. One model with those nullable would make
every detail screen defensive about nulls that only exist because of where the object was
loaded from.

**Offline-first, with Room as the single source of truth.** The repository exposes Flows
that come from the DAO; network calls are one-shot `suspend` functions that only write
into Room. There is exactly one path a screen update can take, cold start shows cached
content immediately, and a failed refresh cannot blank out content that is already on
screen, because the failure is a return value rather than an emission on the read stream.

**Sealed interface for UI state**, not a data class of flags. `Loading`, `Empty`,
`Content`, `Error` are mutually exclusive, so `when` is exhaustive and
"loading and error and has data" cannot be represented. `Content` carries `isRefreshing`
as a flag because refreshing is a modifier on having content, not a state of its own.

**Hand-written fakes instead of a mocking framework.** The fakes are backed by
`MutableStateFlow`, so writing through them really does push a value down the read Flow —
which is the offline-first property the tests exist to assert, rather than a stubbed
return value restating the assumption.

## One thing worth knowing about

`refreshGames()` returns `Result<Int>` — how many games the API listed — rather than
`Result<Unit>`. Room announces writes asynchronously, so after a successful refresh there
is a window where the rows are in the database but the Flow has not delivered them yet.
Deriving "the cache is empty" from the Flow in that window made the list flash
"No games to show yet" on every cold start. The count distinguishes "the API returned
nothing" from "the rows have not arrived yet", so the state machine no longer depends on
which of two asynchronous sources wins a race.

Similarly, an HTTP 404 on the detail endpoint becomes `GameNotFoundException` at the data
boundary: "this game does not exist" and "we could not reach the server" want different
screens, because only one of them has a sensible retry button.

## What I would add next

- **CI** — detekt, unit tests and `assembleDebug` on every push. The checks exist locally;
  nothing enforces them yet.
- **A sealed `DataError`** instead of `Result<Throwable>`. A 404 is already translated,
  but everything else reaches the UI as a raw `Throwable` that gets inspected with
  `is IOException`. Mapping it in `data/` would let the UI decide what is retryable
  through an exhaustive `when`.
- **Compose UI tests.** Both screens are stateless functions of a sealed state, so each
  branch is directly assertable.
- **Paging**, once the list endpoint's single response stops being a reasonable size.
- **A real date type.** `releaseDate` is a `String` because `java.time` needs desugaring
  at `minSdk 24`. Parsing to `LocalDate` in the mapper is a prerequisite for anything
  locale-aware.

## Build notes

AGP 9.2.1 / Kotlin 2.2.10 / Gradle 9.4.1 on JDK 21. Two constraints worth knowing before
bumping anything:

- **AGP 9 has built-in Kotlin support**, so `org.jetbrains.kotlin.android` must not be
  applied, and KSP has to come from the version-decoupled `2.3.x` line.
- **`compileSdk` is 36.1 and several current releases require 37**, so OkHttp, Lifecycle,
  Navigation Compose, `hilt-navigation-compose` and `core-ktx` are each pinned one step
  back. Raising `compileSdk` unblocks them all at once.

Room schemas are exported to `app/schemas/` and committed, so migrations show up in diffs
rather than being discovered at runtime.
