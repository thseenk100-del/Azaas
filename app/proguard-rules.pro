# Project-specific R8 rules.
# The app currently uses only AndroidX and Compose public APIs.
# Keep Kotlin metadata for safe reflection and diagnostics.
-keep class kotlin.Metadata { *; }
