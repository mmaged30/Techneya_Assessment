# Zippopotam API + Wikipedia Mobile Automation

A single Java automation framework covering two targets through one TestNG suite:

- **API** — the [Zippopotam](https://zippopotam.us/) postal-code service, `GET /{country}/{postalCode}`
- **Mobile** — the [Wikipedia Android app](https://play.google.com/store/apps/details?id=org.wikipedia), reading-list behaviour

Data-driven from Excel workbooks, reported through Allure.

---

## Why one project rather than two

API and mobile automation are different technologies, but they are not different *projects*.
They share configuration, driver/session lifecycle, screenshot capture, logging, reporting
wiring and a build. Splitting them would duplicate all of that and produce two reports for one
deliverable; a multi-module Maven build would fix the duplication but add four POMs, a reactor
ordering constraint and an Allure results-merging step to support roughly a dozen tests.

So this is **one module with hard package boundaries** — `org.utils`, `org.api`, `org.mobile`.
Nothing in `org.api` imports `org.mobile` or the reverse; both use `org.utils`, and `org.utils`
imports neither. That is the compromise: the separation of a multi-module build without its
build tax, and if this ever needs to become multi-module, it is a POM change with no code moves.

---

## Architecture

The rule that tells you where new code goes: **`org.utils` knows nothing about Wikipedia or
Zippopotam; `org.api` and `org.mobile` know nothing about each other; the test layer knows
nothing about a platform.**

```
src/main/java/org/
├── utils/                     The engine. App-agnostic, layer-agnostic.
│   ├── ConfigManager          -D flag > config/<platform>.properties > config/global.properties
│   ├── DriverFactory          the only place a platform picks a driver
│   ├── DriverManager          thread-local session holder
│   ├── Waits                  the only place a WebDriverWait is built
│   ├── ExcelReader            .xlsx -> header-keyed rows for a @DataProvider
│   ├── ScreenshotUtils        failure evidence, collision-proof file names
│   └── ExecutionFootprint     what the run actually touched, for the report
│
├── Listeners/                 AllureEnvironmentWriter (ISuiteListener)
│                              FailureListener (ITestListener)
│
├── enums/                     Platform
│
├── api/                       Zippopotam under test
│   ├── controllers/           BaseController, ZippopotamController
│   ├── enums/                 Schema
│   └── models/                PostalCode, Place
│
└── mobile/                    Wikipedia under test
    ├── interfaces/            MainScreen, SearchScreen, ArticleScreen, …  (7 contracts)
    ├── base/                  MobileScreen (portable base), Screens (platform selector)
    ├── android/               Android*Screen + AndroidScreenBase
    └── ios/                   IosScreenBase, IosSearchScreen (reference — see iOS below)

src/test/java/
├── base/                      BaseApiTest, BaseMobileTest — lifecycle by inheritance
├── data/                      TestData — every @DataProvider, the only place a sheet is named
└── tests/api/, tests/mobile/  the tests themselves

src/test/resources/
├── data/                      api-data.xlsx, mobile-data.xlsx
├── config/                    global.properties, android.properties, ios.properties
├── schemas/                   JSON Schema contracts
└── suites/                    api-suite.xml, mobile-suite.xml, master-suite.xml
```

**How a run flows:** Maven profile picks a TestNG suite → the suite lists test classes → a class
extending `BaseMobileTest` gets an Appium session from `@BeforeMethod`, one extending
`BaseApiTest` does not → `@DataProvider` methods in `TestData` read rows from Excel → the test
calls controllers (API) or screen interfaces (mobile) → Allure records each method with its
`@Step`-annotated calls nested inside.

**How a platform is chosen.** Exactly twice, both driven by the `platform` config key:
`DriverFactory.create()` picks the driver, and `Screens.onboarding()` picks the screen
implementations. Both use an **exhaustive `switch`** over `Platform`, so adding a platform is a
compile error until it is handled. Everything downstream is polymorphic — an Android screen
returns Android screens *typed as interfaces* — so no test names a platform.

**Lifecycle is inheritance, not configuration.** Extending `BaseMobileTest` is what gives a test
a device; a test that does not extend it never opens one. There is no tag matching and no
string to get wrong.

---


## Prerequisites

| Requirement | Version used | Needed for |
|---|---|---|
| JDK | 21 | everything |
| Maven | 3.9+ | everything |
| Node.js | 18+ | Appium only |
| Appium server | 3.x | mobile only |
| Appium `uiautomator2` driver | 7.x | mobile only |
| Android SDK + platform-tools | API 34 | mobile only |
| Android emulator or device | Android 11+ | mobile only |

**The API suite needs none of the mobile tooling** — only a JDK, Maven and a network connection.

`ANDROID_HOME` must point at the Android SDK, and `platform-tools` must be on `PATH` so
`adb devices` works.

---

## Installation

```bash
git clone <repository-url>
cd zippopotam-wikipedia-automation
mvn clean compile
```

For the mobile suite, additionally:

```bash
npm install -g appium
appium driver install uiautomator2
```

### The app under test

The Wikipedia APK is **not committed** to this repository. Install it on the target device
before running the mobile suite, either from the Play Store on the emulator, or with:

```bash
adb install path/to/wikipedia.apk
```

Confirm it is present:

```bash
adb shell pm list packages | grep wikipedia
```

---

## Configuration

Everything configurable lives in `src/test/resources/config/`. Nothing there has to be edited
to change a run — every key is overridable with `-D`.

**Resolution order, highest first:** `-Dkey=value` → `config/<platform>.properties` →
`config/global.properties`.

| File | Holds |
|---|---|
| `global.properties` | API base URI, Appium server URL, timeouts, screenshot toggle, active platform |
| `android.properties` | device name, app package/activity, reset behaviour |
| `ios.properties` | device name, bundle id, reset behaviour |

Common overrides:

```bash
mvn test -Papi -Dapi.base.uri=https://api.zippopotam.us
```

```bash
mvn test -Pmobile -Ddevice.name=Pixel_7_API_34 -Dtimeout.explicit=30
```

---

## Running the tests

### API only — no device required

```bash
mvn clean test -Papi
```

### Mobile only

Start the Appium server in one terminal:

```bash
appium
```

Start an emulator (or attach a device) in another, and confirm it is visible:

```bash
adb devices
```

Then run:

```bash
mvn clean test -Pmobile
```

### Everything

```bash
mvn clean test
```

This needs a running Appium server and an attached device, because it includes the mobile suite.

### Running a subset by group

Every test carries TestNG groups. They work independently of the profiles:

```bash
mvn test -Papi -Dgroups=negative
```

```bash
mvn test -Papi -Dgroups=positive -DexcludedGroups=contract
```

Available groups: `api`, `mobile`, `positive`, `negative`, `contract`, `smoke`, `search`.

### Re-running only what failed

TestNG writes `target/surefire-reports/testng-failed.xml` after any failing run. Feed it back:

```bash
mvn test -DsuiteXmlFile=target/surefire-reports/testng-failed.xml
```

This is used instead of an `IRetryAnalyzer` because an automatic retry hides flakiness behind a
silent second attempt, whereas an explicit rerun makes it visible.

---

## Reports

### Allure — the primary report

```bash
mvn allure:serve
```

Or generate it to disk:

```bash
mvn allure:report
```

The report contains each test method and its status, the controller and screen actions nested
inside it as `@Step` entries, the full HTTP request and response for every API call, failure
screenshots and the device page source for mobile failures, and an Environment table listing
only what the run actually touched. Data-driven methods appear once per row, each carrying the
values it ran with.

### On other reporters

**Extent Reports is deliberately not included.** Allure already covers everything it would show
and more — request/response attachments, step nesting, history, an Environment table — and a
second reporter would add a dependency and a second wiring path without adding information.

---


## Test coverage

24 test executions from 11 test methods. Where data genuinely varies it comes from a workbook;
where it does not, the values stay inline, because a one-row spreadsheet is a file to open
rather than information.

### API — 8 methods, 20 executions

| Method | Runs | Data | What it proves |
|---|---|---|---|
| `knownPostalCodeResolvesToItsPlace` | 5 | `KnownPostalCodes` | US, DE, GB, CA — five-digit, alphanumeric outward and forward-sortation formats |
| `postalCodeCoveringSeveralDistrictsReturnsEveryOne` | 1 | inline | `de/01067` returns 3 places, each fully described |
| `countryCodeIsAcceptedInAnyCasing` | 3 | `CountryCasing` | `us`, `US`, `Us` all resolve |
| `successfulLookupMatchesTheLocationSchema` | 1 | inline | JSON Schema, `additionalProperties: false` |
| `lookupIsRejected` | 5 | `RejectedLookups` | unknown country, uncovered country, non-existent code, non-numeric code, too-short code |
| `rejectedLookupStillAnswersAsJson` | 1 | inline | 404 carries `{}` and the JSON content type |
| `requestMissingAPathSegmentNeverReachesTheApi` | 3 | `MissingPathSegments` | `/us`, `/us/`, `/90210` → 404 as **HTML**, not JSON |
| `endpointRefusesToBeWrittenTo` | 1 | inline | POST → 405 |

**Two findings about the service, built into the tests rather than worked around:**

1. **There is no error contract.** An unknown country, an uncovered country, a non-existent code
   and a malformed code all return exactly the same empty `{}` with a 404. `lookupIsRejected`
   asserts that sameness, because it is what a client actually has to handle. Asserting an error
   message would be asserting behaviour the service does not have.

2. **The German dataset returns broken coordinates.** `de/01067` answers with
   `"longitude": "51.05", "latitude": "14612"` — transposed and out of range. Coordinates are
   therefore asserted to be *numeric*, but deliberately **not** to be within valid latitude and
   longitude ranges: that assertion would fail against correct, live service behaviour, and
   excluding Germany to make it pass would quietly hide a real data defect. Instead the bad data
   is attached to the Allure report, so the test stays green on the contract while the defect
   stays visible.

### Mobile — 3 methods, 4 executions

| Method | Runs | Data | Steps covered |
|---|---|---|---|
| `searchingSurfacesTheExpectedArticle` | 2 | `SearchTerms` | search and result verification |
| `articleSavedToANewListCanBeFoundThereAndRemoved` | 1 | inline | launch → search → open article → save → create list → find list → verify → remove → verify gone → list empty |
| `savingTheSameArticleTwiceLeavesOnlyOneCopy` | 1 | inline | the duplicate-prevention check |

**Duplicate prevention** is verified two independent ways, because the app turned out to handle
it in a way worth pinning down. Adding an article to a list that already holds it is not blocked
in the UI — the list is still offered in the chooser — and the app instead detects the duplicate
after the choice and answers with *"All good! AI Research already contains Artificial
intelligence."* So the test asserts both:

- that message, read from the snackbar; and
- that the list still holds **exactly one** copy, waited on rather than sampled, so the app is
  given the full timeout to produce a second row before its absence is accepted as a result.

---


## Design notes worth knowing

**Data-driven testing uses one mechanism.** A TestNG `@DataProvider` reading an `.xlsx` sheet,
for both layers, with every provider declared in `data.TestData` — the only class that names a
workbook or a sheet. Rows are keyed by the sheet's **header**, not by column position, so a test
reads `row.get("postalCode")` and stays correct when a column is reordered or inserted. Adding a
case is a row in Excel; no Java changes.

Cells are written and read as **text**. Excel would store `01067` as the number 1067 and drop
the leading zero, and a numeric cell would arrive as `90210.0`; the API takes strings, so text is
both safer and closer to what is actually sent.

Where parameterisation earns its place it is used — five country/postal-code combinations, five
rejection reasons, three casings, two search terms — and where it does not, the values stay
inline in the test: the reading-list tests each exercise one behaviour, and a one-row spreadsheet
is a file to open rather than information.

**Every mobile test starts from cleared app data.** That is what keeps a list created by one
test invisible to the next, and it is why no test has a cleanup step that could itself fail. The cost is that the app treats every run as a first run and interrupts with promotions —
a search-widget advert, a games dialog, a share tooltip, a recommendations card. These are not
tied to any one screen and can arrive *after* the screen beneath them has rendered, so clearing
them is folded into the waiting itself rather than done once up front.

**Failure evidence is captured by a listener, not by teardown.** `FailureListener.onTestFailure`
fires *before* the `@AfterMethod` that quits the driver, so the screenshot is taken while the
session is still alive. That ordering is a guarantee of `ITestListener`, not a trick.

**Synchronisation is explicit throughout — there is no `Thread.sleep` anywhere in the project.**
Two durations look like sleeps and are not: the hold of a long press and the travel time of a
swipe. Both are part of the gesture's definition — a press too short is a tap, and that exact
failure was observed before the duration was raised.

**Screenshots cannot collide.** A timestamp alone is not unique under parallel execution, so
filenames carry the thread id and a monotonic counter as well.

**Waits carry their own description.** A timeout on a lambda condition would otherwise report
`Lambda$$0x00007f...`, which tells a reader nothing. Descriptions are attached by rethrowing
rather than through `withMessage`, which mutates the wait it is called on and would leak the
description onto every later timeout on that screen.

---

## Bonus features

**Parallel execution — implemented for API, deliberately not for mobile.**
The API providers are declared `@DataProvider(parallel = true)` and run four at a time
(`data-provider-thread-count` in `api-suite.xml`). That is safe because nothing is shared: each
call derives a fresh request specification, and every test method holds its own state in local
variables rather than in a shared context object.

Mobile is left serial, and that is a decision rather than an omission. The framework underneath
it *is* thread-safe — the driver is thread-local, each session claims its own device-side port,
and screenshot names cannot collide — but one emulator hosts one session, so parallelism here
would fail for reasons that have nothing to do with the tests. With a second device attached it
becomes `parallel = true` on the `searchTerms` provider plus a thread count in the suite XML; no
other code would change.

**iOS — architected and wired, never executed.**
The framework is built for two platforms rather than retrofitted for one:

- `DriverFactory` and `Screens` are the *only* two places a platform is resolved, both keyed on
  the `platform` config value, both an exhaustive `switch` — so adding a platform will not
  compile until it is handled.
- Every screen the tests touch is an **interface** in `org.mobile.interfaces`. `MobileScreen` holds
  what is genuinely portable (tap, type, swipe, restart, wait-past-interruptions) and declares
  abstract the three things that are not: the long-press gesture, the app identifier
  (package name vs bundle id), and which interruptions to clear.
- `IosScreenBase` and the iOS half of `DriverFactory` are complete and correct *as XCUITest
  mechanics* — accessibility identifiers, NSPredicate and class-chain locators,
  `mobile: touchAndHold`, WebDriverAgent's port, bundle-id identity.

**What does not exist is the iOS screens themselves, and nothing on iOS has ever been run.**
This was built on Windows; XCUITest needs macOS with Xcode. `IosSearchScreen` is a deliberate
*reference template* — its structure is real, its identifiers are placeholders that must be
read off a running build. The `IOS` arm of `Screens.onboarding()` therefore throws with a message naming
every screen still to be written, rather than letting a session be driven with the wrong
selectors. (On a non-macOS host `-Dplatform=ios` will not get that far — the driver
fails first, because XCUITest cannot start. The screen-level guard is what a *macOS* run hits.)

Verified on this machine: with `-Dplatform=ios`, `ConfigManager` loads `config/ios.properties`
and `Screens.onboarding()` raises exactly that message. That is
the platform-switching mechanism proven end to end; it is *not* a claim that iOS tests run.

Completing iOS is: write six screens against the existing interfaces, return
`IosOnboardingScreen` from the `IOS` arm of `Screens.onboarding()`, and replace the placeholder
identifiers. No interface, test, base class, suite or Android class changes.

---

## Test execution results

Both suites were executed on the machine this was developed on. These are actual results, not
expected ones.

**Combined suite — `mvn clean test`**

```
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
Time elapsed: 132.6 s
BUILD SUCCESS
```

**API only — `mvn test -Papi`**

```
Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
Time elapsed: 7.4 s   (4 parallel threads)
BUILD SUCCESS
```

**Mobile only — `mvn test -Pmobile`**

```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
Time elapsed: 128.6 s   (serial, one emulator)
BUILD SUCCESS
```

Elapsed times vary widely with how warm the emulator is — the same suite has taken anywhere
from 128 s to 215 s on this machine. The counts are the part that matters, and they are
unchanged from before the architectural refactor: the same 24 test executions, all passing.

Environment: Windows 10, JDK 21.0.3, Maven 3.9.6, Appium 3.5.0, uiautomator2 7.6.1,
Android 14 (API 34) x86_64 emulator, Wikipedia 50600-r-2026-07-28.

---

## Known limitations

- **The mobile suite is only as stable as its emulator.** Four executions that each clear app
  data and reload an article are heavy going for an emulator, and under sustained load the app
  can stall to the point of an ANR. The suite passes; a machine with less headroom may need
  `-Dtimeout.explicit=30`, or the rerun command above.
- **Mobile locators are pinned to a UI that is current, not permanent.** They were read from the
  running app rather than guessed, but Wikipedia's search results are Jetpack Compose and expose
  no resource ids at all, so those rows can only be matched on their text. A redesign or a
  renamed article title will break them, and the failure will point at the text.
- **The article title is not the search term.** Searching `Artificial Intelligence` returns an
  article called `Artificial intelligence`; the `SearchTerms` sheet keeps the two in separate
  columns for that reason.
- **iOS has no screen implementations and has never been executed** — see above. The
  architecture is in place and the driver, gesture and locator mechanics are written; six screen
  classes and a macOS machine are what is missing.
- **One platform per JVM.** `ConfigManager` merges the platform overlay into a single set of
  properties for the whole run, so a single JVM cannot drive Android and iOS simultaneously.
  This is a deliberate stopping point, not an oversight: running both at once also needs two
  devices, two concurrent Appium sessions of different types and per-thread platform
  resolution, and none of that is exercised by the current suite. Running them one after the
  other — `mvn test -Pmobile -Dplatform=android` then `-Dplatform=ios` — works today.
- **No CI workflow is included**, by agreement. The API suite is the part that would run well on
  a hosted runner; an Android job needs an emulator action and is slow enough to be worth putting
  behind a manual trigger.
- **Test data lives in binary files.** `.xlsx` does not diff in git, so a data change cannot be
  reviewed in a pull request and a merge conflict means picking one file wholesale. CSV would
  avoid this at the cost of Excel's multi-sheet workbooks; the trade was made deliberately.
