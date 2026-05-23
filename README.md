# Casio ClassWiz (fx-570CW / fx-991CW) Scientific Calculator Clone

This project is a high-performance, single-activity native Android application that reconstructs the iconic look, feel, and mathematical capabilities of the **Casio ClassWiz (fx-570CW / fx-991CW)** scientific calculator in Jetpack Compose and modern Kotlin.

---

## 🎨 Visual Identity & Premium Styling

The user interface adheres to a customized, ultra-premium aesthetic:
1. **True-Black Background**: Styled with a solid pitch-black backdrop (`Color.Black`) to optimize visual contrast and battery consumption.
2. **Glassmorphism / Liquid Glass Panels**: Keypads, menus, and overlays utilize translucent frosted-glass surfaces (`Color.White.copy(alpha = 0.08f)` up to `0.15f`) with explicit light borders (`Color.White.copy(alpha = 0.1f)`) to establish visual depth.
3. **Custom Adaptive Mascot Icon**: Includes custom adaptive icon drawables (`ic_launcher_foreground.xml` and `ic_launcher_background.xml`) representing a golden mathematical radical (`√x`) on a mineral-slate background.
4. **Natural Textbook LCD Screen**: Mimics the monochrome graphic screen of the ClassWiz series. Features animatable backlights (Solar Matte Gray, Warm Amber backlight, White backlight), high-res pixel layout, and distinct status bar indicators (`S` for Shift, `A` for Alpha, `Math`, `D`/`R` for Angle units, and battery status).

---

## ⚙️ Core Architecture & Math Engine

- **Mathematical Parser (`MathEngine.kt`)**: Reconstructs BODMAS/PEMDAS operation ordering, recursive nested parentheses, fractions, trig (sin, cos, tan, inverse-trig), logs (ln, log10), factorials, and variable bindings. Fully supports implicit multiplication rules (e.g., `2π` -> `2 * π`, `2(3+1)` -> `2*4`).
- **Variable Storage**: Provides variable list screens mapping values to registers `A`, `B`, `C`, `D`, `E`, `F`, `X`, `Y`, `Z`. Recalled or written in standard math expressions.
- **Base-N converter applet**: Calculates directly in HEX, DEC, BIN, and OCT modes, complete with custom hexadecimal character entry bindings on the core numpad.
- **Function Table Maker applet**: Evaluates custom register equations `f(x)` and `g(x)` over customizable loops of `Start`, `End`, and `Step` to print high-fidelity scrollable table rows.
- **Equation & System Solver applet**: Uses determinant structures and Cramer's rules to solve Simultaneous linear systems with up to 3 unknowns, and Polynomial quadratic real/complex roots complete with horizontal parabola minimum/maximum vertex tracking.
- **Math Box applet**: Simulates realistic Dice Rolls (1..3 dice) and Coin Tosses (1..3 coins) with Trial registries and Relative Frequency occurrences datasets.
- **Local SQLite Persistence**: Adheres to offline-first principles. Employs a local **Room Database** to persist variables, historical calculation logs, and configurations across launches.

---

## ⚙️ Build Process & Continuous Integration

This project uses modern Gradle Kotlin DSL (`build.gradle.kts`) and compiles with **Java 17**.

To build the project without compiling on local equipment, the project includes an automated Continuous Integration (CI) configuration in `.github/workflows/android_build.yml`. Each push or pull request to the repository automatically triggers a build at GitHub's secure servers, rendering a compiled production APK as an artifact.

---

## 🚀 Repository Setup & Pushing Code

Follow these clean steps to push this project to a new GitHub repository:

### 1. Initialize local repository and stage assets
```bash
git init
git add .
git commit -m "feat: Initial commit of Casio ClassWiz fx-991CW custom scientific calculator with Liquid Glass UI"
```

### 2. Connect and push to GitHub
1. Create a blank private or public repository on GitHub (do not initialize with README or license).
2. Grab the remote URL (e.g., `git@github.com:YOUR_USERNAME/classwiz-scientific-calculator.git`).
3. Set and push:
```bash
git branch -M main
git remote add origin git@github.com:YOUR_USERNAME/classwiz-scientific-calculator.git
git push -u origin main
```

---

## 📦 Extracting the Workflow Build APK
Upon pushing the code:
1. Navigate to the **Actions** tab on your GitHub repository.
2. Select the latest run of the **Android Compile Workflow**.
3. Under the **Artifacts** section at the bottom of the run page, click **classwiz-scientific-calculator-apk** to download the compiled `.apk` directly to your mobile device!
