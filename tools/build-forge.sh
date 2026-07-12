#!/bin/bash
#
# ControlFlex API - Build Forge JAR
#
# 用法:
#   chmod +x tools/build-forge.sh
#   ./tools/build-forge.sh
#
# 功能: 构建 Forge / NeoForge 1.20.1 对应的 API JAR
#       产物: build/libs/controlflex-api-${api_version}-forge.jar
#       该 JAR 仅包含 META-INF/mods.toml，不含 fabric.mod.json

set -euo pipefail

# ── 配置 ──────────────────────────────────────
TOOLS_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "${TOOLS_DIR}/.." && pwd)"
PROPS_FILE="${PROJECT_DIR}/gradle.properties"
BUILD_DIR="${PROJECT_DIR}/build/libs"

# ── 辅助函数 ──────────────────────────────────
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

# ── Step 1: 读取版本号 ─────────────────────────
log_step "1/3" "读取版本号"

API_VER=$(grep '^api_version=' "${PROPS_FILE}" | cut -d'=' -f2 | tr -d ' ')

if [ -z "${API_VER}" ]; then
    echo "❌ 无法从 gradle.properties 读取 api_version"
    exit 1
fi

log_info "api_version = ${API_VER}"

# ── Step 2: 构建 Forge JAR ────────────────────
log_step "2/3" "编译 Forge JAR (jarForge)"

cd "${PROJECT_DIR}"
if ! ./gradlew jarForge; then
    echo ""
    echo "❌ 编译失败！"
    exit 1
fi

# ── Step 3: 校验产物 ──────────────────────────
log_step "3/3" "校验产物"

FORGE_JAR="${BUILD_DIR}/controlflex-api-${API_VER}-forge.jar"

if [ ! -f "${FORGE_JAR}" ]; then
    echo "❌ JAR 文件未找到: ${FORGE_JAR}"
    log_info "目录内容:"
    ls -lh "${BUILD_DIR}" || true
    exit 1
fi

JAR_SIZE=$(du -h "${FORGE_JAR}" | cut -f1 | tr -d ' ')

# 验证 JAR 不含 fabric.mod.json
if jar tf "${FORGE_JAR}" | grep -q "fabric.mod.json"; then
    echo "❌ Forge JAR 不应包含 fabric.mod.json！"
    exit 1
fi

# 验证 JAR 含 mods.toml
if ! jar tf "${FORGE_JAR}" | grep -q "mods.toml"; then
    echo "❌ Forge JAR 缺少 META-INF/mods.toml！"
    exit 1
fi

echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║  ✅ Forge API JAR 构建成功！                     ║"
echo "╠══════════════════════════════════════════════════╣"
echo "║  版本:   ${API_VER}"
echo "║  JAR:    controlflex-api-${API_VER}-forge.jar (${JAR_SIZE})"
echo "║  路径:   ${BUILD_DIR}"
echo "╚══════════════════════════════════════════════════╝"
echo ""
