#!/usr/bin/env bash
#
# Smart test picker for ImgEdt.
# Analyzes changed files and runs only the relevant tests.
#
# Usage:
#   scripts/run-smart-tests.sh                  # reads from git staged files
#   scripts/run-smart-tests.sh file1 file2 ...  # explicit file list
#   scripts/run-smart-tests.sh --all            # run every test
#
# Exit codes:
#   0  all tests passed (or nothing to test)
#   1  one or more tests failed

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# Collect changed files
if [[ "${1:-}" == "--all" ]]; then
    RUN_ALL=true
    CHANGED_FILES=()
elif [[ $# -gt 0 ]]; then
    RUN_ALL=false
    CHANGED_FILES=("$@")
else
    RUN_ALL=false
    mapfile -t CHANGED_FILES < <(git diff --cached --name-only 2>/dev/null || true)
fi

if [[ "$RUN_ALL" != true ]] && [[ ${#CHANGED_FILES[@]} -eq 0 ]]; then
    echo -e "${CYAN}No changed files detected. Nothing to test.${NC}"
    exit 0
fi

# Test buckets
declare -a JVM_TESTS=()
declare -a INSTRUMENTED_TESTS=()
declare -a SKIPPED_FILES=()

PKG="com.imgedt.editor"

# ── Mapping functions ──────────────────────────────────────────────

add_jvm_test() {
    local class="$1"
    for t in "${JVM_TESTS[@]:-}"; do
        [[ "$t" == "$class" ]] && return
    done
    JVM_TESTS+=("$class")
}

add_instrumented_test() {
    local class="$1"
    for t in "${INSTRUMENTED_TESTS[@]:-}"; do
        [[ "$t" == "$class" ]] && return
    done
    INSTRUMENTED_TESTS+=("$class")
}

add_all_tests() {
    add_jvm_test "${PKG}.filter.FilterParamsTest"
    add_jvm_test "${PKG}.crop.CropStateTest"
    add_instrumented_test "${PKG}.crop.CropAreaViewTest"
    add_instrumented_test "${PKG}.crop.CropViewTest"
    add_instrumented_test "${PKG}.paint.PaintViewTest"
    add_instrumented_test "${PKG}.filter.FilterRendererTest"
    add_instrumented_test "${PKG}.PerformanceTest"
}

map_source_to_tests() {
    local file="$1"
    local basename
    basename="$(basename "$file")"

    case "$basename" in
        # Direct JVM test mappings
        FilterParams.java)
            add_jvm_test "${PKG}.filter.FilterParamsTest"
            # FilterRenderer depends on FilterParams
            add_instrumented_test "${PKG}.filter.FilterRendererTest"
            ;;
        CropState.java)
            add_jvm_test "${PKG}.crop.CropStateTest"
            # CropView depends on CropState
            add_instrumented_test "${PKG}.crop.CropViewTest"
            ;;

        # Direct instrumented test mappings
        CropAreaView.java)
            add_instrumented_test "${PKG}.crop.CropAreaViewTest"
            ;;
        CropView.java)
            add_instrumented_test "${PKG}.crop.CropViewTest"
            ;;
        PaintView.java)
            add_instrumented_test "${PKG}.paint.PaintViewTest"
            ;;
        FilterRenderer.java)
            add_instrumented_test "${PKG}.filter.FilterRendererTest"
            ;;

        # Dependency mappings (no direct test, but triggers downstream)
        FilterShaders.java)
            add_instrumented_test "${PKG}.filter.FilterRendererTest"
            ;;

        # Test files themselves
        FilterParamsTest.java)
            add_jvm_test "${PKG}.filter.FilterParamsTest"
            ;;
        CropStateTest.java)
            add_jvm_test "${PKG}.crop.CropStateTest"
            ;;
        CropAreaViewTest.java)
            add_instrumented_test "${PKG}.crop.CropAreaViewTest"
            ;;
        CropViewTest.java)
            add_instrumented_test "${PKG}.crop.CropViewTest"
            ;;
        PaintViewTest.java)
            add_instrumented_test "${PKG}.paint.PaintViewTest"
            ;;
        FilterRendererTest.java)
            add_instrumented_test "${PKG}.filter.FilterRendererTest"
            ;;
        PerformanceTest.java)
            add_instrumented_test "${PKG}.PerformanceTest"
            ;;

        # Files with no tests yet
        EditorActivity.java|CropActivity.java|PaintActivity.java|\
        BrushShaders.java|CropRotationWheel.java|ImageUtils.java|TuneToolRow.java)
            SKIPPED_FILES+=("$basename")
            ;;

        *)
            # Not a recognized source file, check if it is a cross-cutting config
            ;;
    esac
}

# ── Analyze changed files ──────────────────────────────────────────

if [[ "$RUN_ALL" == true ]]; then
    add_all_tests
else
    for file in "${CHANGED_FILES[@]}"; do
        # Cross-cutting triggers: any build config or manifest change runs everything
        case "$file" in
            *build.gradle|*settings.gradle|*gradle.properties|*AndroidManifest.xml)
                add_all_tests
                ;;
            *.java)
                map_source_to_tests "$file"
                ;;
        esac
    done
fi

# ── Summary ────────────────────────────────────────────────────────

echo ""
echo -e "${CYAN}=== Smart Test Picker ===${NC}"

if [[ ${#SKIPPED_FILES[@]} -gt 0 ]]; then
    echo -e "${YELLOW}No tests for: ${SKIPPED_FILES[*]}${NC}"
fi

if [[ ${#JVM_TESTS[@]} -eq 0 ]] && [[ ${#INSTRUMENTED_TESTS[@]} -eq 0 ]]; then
    echo -e "${GREEN}No relevant tests to run. Commit proceeds.${NC}"
    echo ""
    exit 0
fi

if [[ ${#JVM_TESTS[@]} -gt 0 ]]; then
    echo -e "  JVM tests:          ${JVM_TESTS[*]##*.}"
fi
if [[ ${#INSTRUMENTED_TESTS[@]} -gt 0 ]]; then
    echo -e "  Instrumented tests: ${INSTRUMENTED_TESTS[*]##*.}"
fi
echo ""

FAILED=false

# ── Run JVM tests ──────────────────────────────────────────────────

if [[ ${#JVM_TESTS[@]} -gt 0 ]]; then
    TESTS_ARGS=""
    for t in "${JVM_TESTS[@]}"; do
        TESTS_ARGS+=" --tests $t"
    done

    echo -e "${CYAN}Running JVM unit tests...${NC}"
    if ./gradlew :editor:testDebugUnitTest $TESTS_ARGS --quiet 2>&1; then
        echo -e "${GREEN}  JVM tests passed.${NC}"
    else
        echo -e "${RED}  JVM tests FAILED.${NC}"
        FAILED=true
    fi
fi

# ── Run instrumented tests (if emulator available) ─────────────────

if [[ ${#INSTRUMENTED_TESTS[@]} -gt 0 ]]; then
    ADB="${ANDROID_HOME:-$HOME/Android/Sdk}/platform-tools/adb"
    DEVICE_COUNT=0
    if [[ -x "$ADB" ]]; then
        DEVICE_COUNT=$("$ADB" devices 2>/dev/null | grep -c 'device$' || true)
    fi

    if [[ "$DEVICE_COUNT" -gt 0 ]]; then
        # connectedAndroidTest uses -P for class filtering, not --tests
        CLASS_LIST=""
        for t in "${INSTRUMENTED_TESTS[@]}"; do
            if [[ -n "$CLASS_LIST" ]]; then
                CLASS_LIST+=","
            fi
            CLASS_LIST+="$t"
        done

        echo -e "${CYAN}Running instrumented tests (emulator detected)...${NC}"
        if ./gradlew :editor:connectedDebugAndroidTest \
            -Pandroid.testInstrumentationRunnerArguments.class="$CLASS_LIST" \
            --quiet 2>&1; then
            echo -e "${GREEN}  Instrumented tests passed.${NC}"
        else
            echo -e "${RED}  Instrumented tests FAILED.${NC}"
            FAILED=true
        fi
    else
        echo -e "${YELLOW}No emulator/device detected. Skipping instrumented tests.${NC}"
        echo -e "${YELLOW}  Skipped: ${INSTRUMENTED_TESTS[*]##*.}${NC}"
    fi
fi

# ── Result ─────────────────────────────────────────────────────────

echo ""
if [[ "$FAILED" == true ]]; then
    echo -e "${RED}=== TESTS FAILED. Commit blocked. ===${NC}"
    exit 1
else
    echo -e "${GREEN}=== All tests passed. ===${NC}"
    exit 0
fi
