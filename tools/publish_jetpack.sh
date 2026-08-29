#!/usr/bin/env bash
#
# Publish control-flex-api to JitPack by tagging api_version and pushing to GitHub.
# JitPack builds from the git tag; the consumer coordinate stays:
#   com.github.ControlFlexMC:control-flex-api:<api_version>
#
# Usage:
#   ./tools/publish_jetpack.sh              # tag + push + GitHub release + wait for JitPack
#   ./tools/publish_jetpack.sh --no-release # skip `gh release create`
#   ./tools/publish_jetpack.sh --rebuild    # do not retag; only trigger / wait for JitPack
#   ./tools/publish_jetpack.sh --no-wait    # do not poll JitPack (still tag/push)
#
set -euo pipefail

TOOLS_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "${TOOLS_DIR}/.." && pwd)"
PROPS_FILE="${PROJECT_DIR}/gradle.properties"

GROUP_ID="com.github.ControlFlexMC"
ARTIFACT_ID="control-flex-api"
GIT_REMOTE="origin"
JITPACK_OWNER="ControlFlexMC"
JITPACK_REPO="control-flex-api"

SKIP_RELEASE=false
REBUILD_ONLY=false
NO_WAIT=false
for arg in "$@"; do
    case "${arg}" in
        --no-release) SKIP_RELEASE=true ;;
        --rebuild)    REBUILD_ONLY=true ;;
        --no-wait)    NO_WAIT=true ;;
        -h|--help)
            sed -n '2,14p' "$0"
            exit 0
            ;;
        *)
            echo "❌ Unknown argument: ${arg}"
            echo "Usage: $0 [--no-release] [--rebuild] [--no-wait]"
            exit 1
            ;;
    esac
done

log_step() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  [$1] $2"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

log_info() {
    echo "  $1"
}

log_warning() {
    echo "  ⚠️  $1"
}

fail() {
    echo "❌ $1" >&2
    exit 1
}

read_prop() {
    grep "^${1}=" "${PROPS_FILE}" | cut -d'=' -f2- | tr -d ' '
}

detect_java_home() {
    if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
        return 0
    fi
    local candidate
    for candidate in \
        /Library/Java/JavaVirtualMachines/microsoft-17.jdk/Contents/Home \
        /Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
        /Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home \
        /Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
    do
        if [[ -x "${candidate}/bin/java" ]]; then
            export JAVA_HOME="${candidate}"
            export PATH="${JAVA_HOME}/bin:${PATH}"
            return 0
        fi
    done
}

require_clean_worktree() {
    if [[ -n "$(git status --porcelain)" ]]; then
        fail "Working tree is dirty. Commit (or stash) before publishing to JitPack."
    fi
}

wait_for_jitpack() {
    local version="$1"
    local pom_url="https://jitpack.io/com/github/${JITPACK_OWNER}/${JITPACK_REPO}/${version}/${ARTIFACT_ID}-${version}.pom"
    local log_url="https://jitpack.io/com/github/${JITPACK_OWNER}/${JITPACK_REPO}/${version}/build.log"
    local tmp_pom
    tmp_pom="$(mktemp)"
    local tmp_log
    tmp_log="$(mktemp)"
    local elapsed=0
    local timeout=360
    local interval=10

    log_info "POM: ${pom_url}"
    log_info "log: ${log_url}"

    # First request kicks off the JitPack build.
    curl -fsSL "${log_url}" -o "${tmp_log}" || true

    while (( elapsed <= timeout )); do
        if curl -fsSL "${pom_url}" -o "${tmp_pom}" 2>/dev/null; then
            if grep -q "com.github.ControlFlexMC.control-flex-api" "${tmp_pom}"; then
                rm -f "${tmp_pom}" "${tmp_log}"
                fail "JitPack POM is a multi-artifact wrapper. Keep a single maven-publish publication."
            fi
            if ! grep -q "<artifactId>${ARTIFACT_ID}</artifactId>" "${tmp_pom}"; then
                rm -f "${tmp_pom}" "${tmp_log}"
                fail "JitPack POM artifactId is not ${ARTIFACT_ID}."
            fi
            echo ""
            echo "  JitPack POM:"
            sed 's/^/    /' "${tmp_pom}"
            rm -f "${tmp_pom}" "${tmp_log}"
            return 0
        fi
        log_info "waiting for JitPack… ${elapsed}s / ${timeout}s"
        sleep "${interval}"
        elapsed=$((elapsed + interval))
        curl -fsSL "${log_url}" -o "${tmp_log}" 2>/dev/null || true
        if grep -q "Build failed\|BUILD FAILED" "${tmp_log}" 2>/dev/null; then
            echo ""
            echo "----- JitPack build.log (tail) -----"
            tail -n 40 "${tmp_log}"
            rm -f "${tmp_pom}" "${tmp_log}"
            fail "JitPack build failed. See ${log_url}"
        fi
    done

    rm -f "${tmp_pom}" "${tmp_log}"
    fail "Timed out waiting for JitPack (${timeout}s). Check ${log_url}"
}

[[ -f "${PROPS_FILE}" ]] || fail "gradle.properties not found: ${PROPS_FILE}"

VERSION="$(read_prop api_version)"
[[ -n "${VERSION}" ]] || fail "api_version is empty in gradle.properties"

cd "${PROJECT_DIR}"

log_step "1/4" "Read version"
log_info "api_version: ${VERSION}"
log_info "coordinate:  ${GROUP_ID}:${ARTIFACT_ID}:${VERSION}"
log_info "tag:         ${VERSION}"

if [[ "${REBUILD_ONLY}" == "true" ]]; then
    log_step "2/4" "Skip tag/push (--rebuild)"
    if ! git rev-parse -q --verify "refs/tags/${VERSION}" >/dev/null; then
        fail "Tag ${VERSION} does not exist locally. Run without --rebuild first."
    fi
    log_info "existing tag: $(git rev-parse --short "refs/tags/${VERSION}")"
else
    require_clean_worktree

    log_step "2/4" "Verify build, then tag and push"
    detect_java_home
    if [[ -n "${JAVA_HOME:-}" ]]; then
        log_info "JAVA_HOME: ${JAVA_HOME}"
    fi
    ./gradlew build

    if git rev-parse -q --verify "refs/tags/${VERSION}" >/dev/null; then
        fail "Git tag ${VERSION} already exists. Use --rebuild to only refresh JitPack."
    fi
    if git ls-remote --tags "${GIT_REMOTE}" "refs/tags/${VERSION}" | grep -q .; then
        fail "Git tag ${VERSION} already exists on ${GIT_REMOTE}."
    fi

    git tag -a "${VERSION}" -m "control-flex-api ${VERSION}"
    log_info "created annotated tag ${VERSION}"

    git push "${GIT_REMOTE}" HEAD
    git push "${GIT_REMOTE}" "${VERSION}"
    log_info "pushed HEAD and tag ${VERSION} to ${GIT_REMOTE}"

    if [[ "${SKIP_RELEASE}" == "true" ]]; then
        log_info "skipping GitHub release (--no-release)"
    elif command -v gh >/dev/null 2>&1; then
        if gh release view "${VERSION}" >/dev/null 2>&1; then
            log_warning "GitHub release ${VERSION} already exists"
        else
            gh release create "${VERSION}" \
                --title "control-flex-api ${VERSION}" \
                --notes "control-flex-api ${VERSION}"
            log_info "created GitHub release ${VERSION} (CI uploads the JAR)"
        fi
    else
        log_warning "gh not found; skipped GitHub release. Tag push is enough for JitPack."
    fi
fi

log_step "3/4" "Trigger JitPack"
if [[ "${NO_WAIT}" == "true" ]]; then
    log_info "skipping wait (--no-wait)"
    log_info "https://jitpack.io/#${JITPACK_OWNER}/${JITPACK_REPO}/${VERSION}"
else
    wait_for_jitpack "${VERSION}"
fi

log_step "4/4" "Done"

echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║  ✅ JitPack publish triggered                          ║"
echo "╠════════════════════════════════════════════════════════╣"
echo "║  ${GROUP_ID}:${ARTIFACT_ID}:${VERSION}"
echo "║  https://jitpack.io/#${JITPACK_OWNER}/${JITPACK_REPO}/${VERSION}"
echo "╚════════════════════════════════════════════════════════╝"
echo ""
echo "Consumer:"
echo "  repositories { maven { url 'https://jitpack.io' } }"
echo "  compileOnly '${GROUP_ID}:${ARTIFACT_ID}:${VERSION}'"
echo ""
