#!/bin/bash
# by Claude — Initializes this template project with a new name and package.
# Usage: ./init-project.sh
# Prompts for project name and package name, then renames everything.

set -euo pipefail

echo "=== Lightning Server + KiteUI Project Initializer ==="
echo ""

# Prompt for project name
read -p "Project name (e.g. my-awesome-app): " PROJECT_NAME
if [[ -z "$PROJECT_NAME" ]]; then
    echo "Error: Project name is required."
    exit 1
fi

# Derive leaf name (no hyphens, lowercase) from project name
DEFAULT_LEAF=$(echo "$PROJECT_NAME" | tr -d '-' | tr '[:upper:]' '[:lower:]')
read -p "Package leaf name (default: $DEFAULT_LEAF): " PACKAGE_LEAF
PACKAGE_LEAF=${PACKAGE_LEAF:-$DEFAULT_LEAF}

# Prompt for base package
read -p "Base package (default: com.lightningkite.$PACKAGE_LEAF): " FULL_PACKAGE
FULL_PACKAGE=${FULL_PACKAGE:-com.lightningkite.$PACKAGE_LEAF}

echo ""
echo "Summary:"
echo "  Project name:  $PROJECT_NAME"
echo "  Package leaf:  $PACKAGE_LEAF"
echo "  Full package:  $FULL_PACKAGE"
echo ""
read -p "Proceed? (y/N): " CONFIRM
if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
    echo "Aborted."
    exit 0
fi

OLD_LEAF="lskiteuistarter"
OLD_PROJECT="ls-kiteui-starter"
OLD_PACKAGE="com.lightningkite.lskiteuistarter"

# Convert package to directory path
PACKAGE_DIR=$(echo "$FULL_PACKAGE" | tr '.' '/')
OLD_PACKAGE_DIR="com/lightningkite/lskiteuistarter"

echo ""
echo "Step 1: Renaming package references in source files..."
find . \( -name '*.kt' -o -name '*.kts' -o -name '*.json' -o -name '*.md' \
         -o -name '*.xml' -o -name '*.mjs' -o -name '*.html' -o -name '*.yaml' \
         -o -name '*.yml' -o -name '*.properties' \) \
    -not -path './.git/*' \
    -not -path '*/build/*' \
    -not -path '*/node_modules/*' \
    -not -path './.gradle/*' \
    -not -path './init-project.sh' \
    -exec sed -i '' "s|$OLD_PACKAGE|$FULL_PACKAGE|g" {} +

echo "Step 2: Renaming leaf name references..."
find . \( -name '*.kt' -o -name '*.kts' -o -name '*.json' -o -name '*.md' \
         -o -name '*.xml' -o -name '*.mjs' -o -name '*.html' -o -name '*.yaml' \
         -o -name '*.yml' -o -name '*.properties' \) \
    -not -path './.git/*' \
    -not -path '*/build/*' \
    -not -path '*/node_modules/*' \
    -not -path './.gradle/*' \
    -not -path './init-project.sh' \
    -exec sed -i '' "s|$OLD_LEAF|$PACKAGE_LEAF|g" {} +

echo "Step 3: Renaming project name references..."
find . \( -name '*.kt' -o -name '*.kts' -o -name '*.json' -o -name '*.md' \
         -o -name '*.xml' -o -name '*.mjs' -o -name '*.html' -o -name '*.yaml' \
         -o -name '*.yml' -o -name '*.properties' \) \
    -not -path './.git/*' \
    -not -path '*/build/*' \
    -not -path '*/node_modules/*' \
    -not -path './.gradle/*' \
    -not -path './init-project.sh' \
    -exec sed -i '' "s|$OLD_PROJECT|$PROJECT_NAME|g" {} +

echo "Step 4: Moving source directories..."
for BASE in \
    "shared/src/commonMain/kotlin" \
    "server/src/main/kotlin" \
    "server/src/test/kotlin" \
    "apps/src/commonMain/kotlin" \
    "apps/src/androidMain/kotlin" \
    "apps/src/iosMain/kotlin" \
    "apps/src/jsMain/kotlin"
do
    OLD_DIR="$BASE/$OLD_PACKAGE_DIR"
    NEW_DIR="$BASE/$PACKAGE_DIR"
    if [[ -d "$OLD_DIR" ]]; then
        mkdir -p "$NEW_DIR"
        # Move all contents
        if ls "$OLD_DIR"/* 1>/dev/null 2>&1; then
            cp -R "$OLD_DIR"/* "$NEW_DIR"/ 2>/dev/null || true
            cp -R "$OLD_DIR"/.* "$NEW_DIR"/ 2>/dev/null || true
            rm -rf "$OLD_DIR"
        fi
        # Clean up empty parent dirs
        OLD_PARENT=$(dirname "$OLD_DIR")
        while [[ "$OLD_PARENT" != "$BASE" ]] && [[ -d "$OLD_PARENT" ]] && [[ -z "$(ls -A "$OLD_PARENT" 2>/dev/null)" ]]; do
            rmdir "$OLD_PARENT" 2>/dev/null || true
            OLD_PARENT=$(dirname "$OLD_PARENT")
        done
    fi
done

echo "Step 5: Updating settings.gradle.kts project name..."
sed -i '' "s|rootProject.name = \"$OLD_PROJECT\"|rootProject.name = \"$PROJECT_NAME\"|g" settings.gradle.kts

echo "Step 6: Updating index.html script reference..."
INDEX_FILE="apps/src/jsMain/resources/index.html"
if [[ -f "$INDEX_FILE" ]]; then
    sed -i '' "s|$OLD_PROJECT-apps|$PROJECT_NAME-apps|g" "$INDEX_FILE"
fi

echo ""
echo "Done! Project renamed to '$PROJECT_NAME' with package '$FULL_PACKAGE'."
echo ""
echo "Next steps:"
echo "  1. Review the changes: git diff"
echo "  2. Run: ./gradlew :shared:compileKotlinJvm :server:compileKotlin"
echo "  3. Update settings.json with your configuration"
echo "  4. Update deployments.kt with your domain and AWS settings"
echo "  5. Delete this script (init-project.sh)"
