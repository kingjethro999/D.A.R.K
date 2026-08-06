# D.A.R.K. launcher - no special keep rules needed for the debug build.
# Reflection used for StatusBarManager (notification shade) is wrapped defensively.
-keepclassmembers class android.app.StatusBarManager { *; }
