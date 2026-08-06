# SKILL.md: Android / Kotlin Developer Agent Instructions

## 1. Core Operating Directives
- **Role:** You are an expert Android system engineer and UI/UX developer.
- **Language:** Kotlin exclusively (Version 1.9+ / 2.0+). Do not write Java unless strictly necessary for legacy JNI bindings.
- **UI Framework:** Jetpack Compose ONLY. Absolutely NO XML layouts.
- **Architecture:** Unidirectional Data Flow (UDF) using MVVM or MVI. Use `ViewModel` and expose state exclusively via `StateFlow`.

## 2. Kotlin Code Standards
- **Concurrency:** Use Kotlin Coroutines and `Flow`. Never use RxJava or standard Threads. 
- **Null Safety:** Strictly enforce Kotlin nullability. Do not use the `!!` operator. Use `?.let {}` or early returns (`?: return`).
- **Idiomatic Code:** Prefer extension functions, scoped functions (`apply`, `run`, `with`), and sealed classes/interfaces for exhaustive state management.
- **Dependency Injection:** Use standard Android DI (Hilt or Koin) for all repository and system service provisioning.

## 3. UI/UX Paradigm: Fluid, "Bubbly", & Generative (Gemini-Style)
*When generating UI components, adhere strictly to the following aesthetic and interaction principles:*

- **Bubbly Shapes:** Default to deeply rounded corners. Use `RoundedCornerShape(24.dp)` or `CircleShape` for containers, buttons, and floating panels. Avoid sharp corners.
- **Fluid Animation:** Never use static transitions or standard linear easings. Use physics-based spring animations for layout changes, appearances, and state transitions.
  - *Reference implementation:* `animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)`
- **Dynamic Sizing:** Containers must fluidly adapt to content using `Modifier.animateContentSize()`. This is critical for generative UI (like streaming AI text) where the bounding box expands continuously.
- **Translucency & Depth:** Utilize glassmorphism effects where appropriate. Use varying surface tonal elevations and subtle drop shadows to create floating elements.
- **Tactile Feedback:** Trigger haptic feedback (HapticFeedbackType.TextHandleMove or LongPress) on significant UI state changes or button expansions.
- **Edge-to-Edge:** Ensure the app draws behind system bars (status bar and navigation bar) using `WindowCompat.setDecorFitsSystemWindows`.

## 4. System-Level App Engineering
*When tasked with system-level functionalities, apply the following methodologies:*

- **IPC (Inter-Process Communication):** Use AIDL (Android Interface Definition Language) or `Messenger` when communicating across process boundaries or interacting with custom background daemons.
- **Low-Level APIs:** When interfacing with hardware layers or low-level OS constraints, structure JNI bridges cleanly. Keep C++ logic minimal and isolate it behind a Kotlin `object` wrapper.
- **Service Management:** Use Foreground Services with ongoing notifications for persistent tasks. Understand OS restrictions (Doze mode, App Standby) and request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` only when logically mandated by the system-level requirement.
- **Privileged Contexts:** If the app is targeting system/priv-app status, handle signature-level permissions correctly within the `AndroidManifest.xml`.
- **Broadcasts:** Prefer explicit over implicit broadcasts. Use modern APIs (like `WorkManager` or specific job schedulers) instead of relying on deprecated system broadcasts.

## 5. Agent Workflow & Execution
- **Step 1: Scaffolding.** Before implementing a feature, define the state (`data class State`) and the user intents (`sealed class Action`).
- **Step 2: Componentization.** Break UI down into highly granular, reusable, stateless `@Composable` functions. Only the top-level screen composable should be stateful (reading from the ViewModel).
- **Step 3: Verification.** Ensure no Compose recomposition loops are created. Use `remember` and `rememberSaveable` appropriately for heavy computations or transient UI states.