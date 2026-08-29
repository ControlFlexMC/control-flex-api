#!/usr/bin/env bash
#
# Publish control-flex-api to ~/.m2 at the JitPack consumer coordinate:
#   com.github.ControlFlexMC:control-flex-api:<api_version>
#
# Usage:
#   ./tools/publish_maven_local.sh
#
set -euo pipefail

TOOLS_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "${TOOLS_DIR}/.." && pwd)"
PROPS_FILE="${PROJECT_DIR}/gradle.properties"

GROUP_ID="com.github.ControlFlexMC"
ARTIFACT_ID="control-flex-api"

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

[[ -f "${PROPS_FILE}" ]] || fail "gradle.properties not found: ${PROPS_FILE}"

VERSION="$(read_prop api_version)"
[[ -n "${VERSION}" ]] || fail "api_version is empty in gradle.properties"

M2_DIR="${HOME}/.m2/repository/com/github/ControlFlexMC/${ARTIFACT_ID}/${VERSION}"
LEGACY_DIR="${HOME}/.m2/repository/com/ifels/controlflex/controlflex-api/${VERSION}"
POM="${M2_DIR}/${ARTIFACT_ID}-${VERSION}.pom"
JAR="${M2_DIR}/${ARTIFACT_ID}-${VERSION}.jar"

log_step "1/3" "Read version"
log_info "api_version: ${VERSION}"
log_info "coordinate:  ${GROUP_ID}:${ARTIFACT_ID}:${VERSION}"

detect_java_home
if [[ -n "${JAVA_HOME:-}" ]]; then
    log_info "JAVA_HOME:   ${JAVA_HOME}"
fi

log_step "2/3" "publishToMavenLocal"
cd "${PROJECT_DIR}"
./gradlew publishToMavenLocal

log_step "3/3" "Verify local artifact"
[[ -f "${JAR}" ]] || fail "JAR not found: ${JAR}"
[[ -f "${POM}" ]] || fail "POM not found: ${POM}"

if grep -q "com.github.ControlFlexMC.control-flex-api" "${POM}"; then
    fail "POM looks like a JitPack wrapper (multi-artifact). Check publishing {} in build.gradle."
fi
if [[ -d "${LEGACY_DIR}" ]]; then
    fail "Also published legacy coordinate com.ifels.controlflex:controlflex-api:${VERSION}. Keep a single publication."
fi

JAR_SIZE="$(du -h "${JAR}" | cut -f1 | tr -d ' ')"

echo ""
echo "╔════════════════════════════════════════════════════════╗"
echo "║  ✅ Maven Local publish succeeded                      ║"
echo "╠════════════════════════════════════════════════════════╣"
echo "║  ${GROUP_ID}:${ARTIFACT_ID}:${VERSION}"
echo "║  ${JAR}  (${JAR_SIZE})"
echo "╚════════════════════════════════════════════════════════╝"
echo ""
echo "Consumer:"
echo "  compileOnly '${GROUP_ID}:${ARTIFACT_ID}:${VERSION}'"
echo ""
