import os

yaml_path = ".github/workflows/android_build.yml"

with open(yaml_path, "r") as f:
    content = f.read()

# Fix 1: Inject the missing keystore generation into the Ubuntu runner
if "Generate Dummy Debug Keystore" not in content:
    keystore_step = """      - name: Generate Dummy Debug Keystore
        run: keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "C=US, O=Android, CN=Android Debug"

      - name: Setup Gradle"""
    content = content.replace("      - name: Setup Gradle", keystore_step)

# Fix 2: Force Java headless mode for KSP to prevent the AWT-EventQueue crash
if "-Djava.awt.headless=true" not in content:
    content = content.replace("gradle assembleDebug --no-daemon", "gradle assembleDebug --no-daemon -Djava.awt.headless=true")

with open(yaml_path, "w") as f:
    f.write(content)

print("✅ Surgical Repair Complete: Injected Keystore Generator and KSP Headless Mode into YAML.")
