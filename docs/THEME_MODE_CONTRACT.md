# GitHub Rock Theme Mode Contract

GitHub Rock has three explicit color modes:

- **Dark** — app surfaces use the graphite GitHub Rock palette. Manual dark mode does not consume Android dynamic wallpaper colors.
- **Light** — app surfaces use the light GitHub Rock palette. Manual light mode does not consume Android dynamic wallpaper colors.
- **System** — follows the Android system light/dark setting and, on Android 12+, uses the system dynamic Material palette derived from the current wallpaper when dynamic color is enabled.

**AMOLED** is an explicit Dark-mode variant and is the only mode that intentionally uses true black (`#000000`) for its surfaces.

Dynamic color is therefore a System-mode feature, not a way for manual Dark or Light mode to override the user's explicit choice.
