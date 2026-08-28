# GitHub Rock Theme Mode Contract

GitHub Rock has three explicit color modes:

- **Dark** — app surfaces use true black (`#000000`). Manual dark mode does not consume Android dynamic wallpaper colors.
- **Light** — app surfaces use true white (`#FFFFFF`). Manual light mode does not consume Android dynamic wallpaper colors.
- **System** — follows the Android system light/dark setting and, on Android 12+, uses the system dynamic Material palette derived from the current wallpaper when dynamic color is enabled.

Dynamic color is therefore a System-mode feature, not a way for manual Dark or Light mode to override the user's explicit choice.
