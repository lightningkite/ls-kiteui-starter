#!/usr/bin/env sh
# Create a new Lightning Server + KiteUI project from lskiteuistarter.
#
#   curl -fsSL https://<your-host>/init.sh | sh -s -- my-app com.mycompany.myapp
#
# The result is a fresh directory with its own git history and nothing
# linking it back to the template. The template's package name and project
# name are rewritten to yours.
#
# Environment overrides:
#   LSKITE_REF      branch or tag of the starter to use (default: main)
#   LSKITE_TARBALL  path to a local .tar.gz instead of downloading (testing)
set -eu

# ---- Template facts: set these to match the starter repo -------------------
REPO="lightningkite/ls-kiteui-starter"
TEMPLATE_PACKAGE="com.lightningkite.lskiteuistarter"
TEMPLATE_NAME="lskiteuistarter"
# Files that make sense in the template repo but not in a user's project.
TEMPLATE_ONLY_FILES="init.sh"
# -----------------------------------------------------------------------------

REF="${LSKITE_REF:-main}"

die() { printf 'error: %s\n' "$1" >&2; exit 1; }
need() { command -v "$1" >/dev/null 2>&1 || die "'$1' is required but not installed"; }

[ $# -ge 1 ] || {
  echo "usage: init.sh <project-name> [package]   e.g. init.sh my-app com.acme.myapp" >&2
  exit 1
}

NAME="$1"
case "$NAME" in
  *[!A-Za-z0-9_-]* | -*) die "project name may only contain letters, digits, '-' and '_'" ;;
esac

# Default package: com.example.<name without dashes/underscores, lowercased>
SIMPLE=$(printf '%s' "$NAME" | tr -d '_-' | tr '[:upper:]' '[:lower:]')
PACKAGE="${2:-com.example.$SIMPLE}"
printf '%s' "$PACKAGE" | grep -Eq '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$' \
  || die "package must look like com.company.app (lowercase, dot-separated)"

need tar; need git; need perl
[ -n "${LSKITE_TARBALL:-}" ] || need curl
[ ! -e "$NAME" ] || die "'$NAME' already exists"

echo "Creating $NAME ($PACKAGE) from $REPO@$REF ..."
mkdir "$NAME"
trap 'rm -rf "$NAME"; echo "aborted; removed $NAME" >&2' INT TERM
if [ -n "${LSKITE_TARBALL:-}" ]; then
  tar -xzf "$LSKITE_TARBALL" -C "$NAME" --strip-components=1
else
  # Archive download: no .git directory, so no link to the template's history.
  curl -fsSL "https://codeload.github.com/$REPO/tar.gz/$REF" \
    | tar -xz -C "$NAME" --strip-components=1 \
    || { rm -rf "$NAME"; die "download failed (does $REPO@$REF exist?)"; }
fi
cd "$NAME"

for f in $TEMPLATE_ONLY_FILES; do rm -f "$f"; done

# 1. Rewrite package and project name inside text files only.
OLD_PKG_RE=$(printf '%s' "$TEMPLATE_PACKAGE" | sed 's/\./\\./g')
find . -type f \( -name '*.kt' -o -name '*.kts' -o -name '*.gradle' -o -name '*.java' \
  -o -name '*.swift' -o -name '*.xml' -o -name '*.json' -o -name '*.properties' \
  -o -name '*.plist' -o -name '*.pbxproj' -o -name '*.xcscheme' -o -name '*.yml' \
  -o -name '*.yaml' -o -name '*.toml' -o -name '*.md' -o -name '*.html' \
  -o -name '*.tf' -o -name '*.conf' -o -name '*.env*' \) \
  -exec perl -pi -e "s/$OLD_PKG_RE/$PACKAGE/g; s/\Q$TEMPLATE_NAME\E/$NAME/g" {} +

# 2. Move source directories com/lightningkite/lskiteuistarter -> new package path.
OLD_PATH=$(printf '%s' "$TEMPLATE_PACKAGE" | tr . /)
NEW_PATH=$(printf '%s' "$PACKAGE" | tr . /)
if [ "$OLD_PATH" != "$NEW_PATH" ]; then
  find . -type d -path "*/$OLD_PATH" > .lskite-dirs
  while IFS= read -r dir; do
    base="${dir%/"$OLD_PATH"}"
    tmp="$base/.lskite-move-$$"
    mv "$dir" "$tmp"
    # Remove now-empty parents of the old path (com/lightningkite), ignore errors.
    rmdir -p "$(dirname "$dir")" 2>/dev/null || true
    mkdir -p "$base/$(dirname "$NEW_PATH")"
    mv "$tmp" "$base/$NEW_PATH"
  done < .lskite-dirs
  rm -f .lskite-dirs
fi

# 3. Fresh repository with a single initial commit.
git init -q
git checkout -q -b main 2>/dev/null || true
git add -A
if git commit -q -m "Initial commit from $REPO@$REF" 2>/dev/null; then
  committed=yes
else
  committed=no
fi

trap - INT TERM
echo
echo "Done. Created ./$NAME with package $PACKAGE."
[ "$committed" = yes ] || echo "(git repo initialised; commit skipped - set git user.name/user.email and commit)"
echo
echo "Next:"
echo "  cd $NAME"
echo "  ./gradlew build"
