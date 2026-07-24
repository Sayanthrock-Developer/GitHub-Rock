#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 5 ]; then
  echo "Usage: package-arch.sh <binary> <icon> <output> <version> <architecture>" >&2
  exit 1
fi

binary="$1"
icon="$2"
output="$3"
version="$4"
architecture="$5"

test -x "$binary" || {
  echo "The Tauri binary is missing or not executable: $binary" >&2
  exit 1
}
test -s "$icon" || {
  echo "The application icon is missing: $icon" >&2
  exit 1
}
[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][A-Za-z0-9.-]+)?$ ]] || {
  echo "Invalid package version: $version" >&2
  exit 1
}

package_root="$(mktemp -d)"
trap 'rm -rf "$package_root"' EXIT

install -Dm755 "$binary" "$package_root/usr/bin/github-rock"
install -Dm644 "$icon" "$package_root/usr/share/icons/hicolor/512x512/apps/github-rock.png"
mkdir -p "$package_root/usr/share/applications"
cat > "$package_root/usr/share/applications/github-rock.desktop" <<'DESKTOP'
[Desktop Entry]
Type=Application
Name=GitHub Rock
Comment=GitHub Rock releases and project companion
Exec=github-rock
Icon=github-rock
Terminal=false
Categories=Development;
DESKTOP

installed_size="$(du -sb "$package_root" | cut -f1)"
arch_version="${version//-/_}"
cat > "$package_root/.PKGINFO" <<PKGINFO
pkgname = github-rock
pkgbase = github-rock
pkgver = ${arch_version}-1
pkgdesc = GitHub Rock releases and project companion
url = https://github.com/Sayanthrock-Developer/GitHub-Rock
builddate = $(date +%s)
packager = GitHub Rock release workflow
size = ${installed_size}
arch = ${architecture}
license = MIT
depend = webkit2gtk-4.1
depend = gtk3
PKGINFO

mkdir -p "$(dirname "$output")"
tar --zstd -cf "$output" -C "$package_root" .
test -s "$output"
echo "Created Arch Linux package: $output"
