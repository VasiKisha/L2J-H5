#!/bin/bash
set -e

# ==========================================
# Konfigurace cest a proměnných
# ==========================================
BASE_DIR="/opt/l2j"
SRC_DIR="$BASE_DIR/src"
TARGET_BRANCH="master" # volba větne pro testování
DEPLOY_DIR="$BASE_DIR/l2j-h5"
BUILD_ZIP="$BASE_DIR/build/L2J_Mobius_CT_2.6_HighFive.zip"
TMP_DIR="$BASE_DIR/build_tmp"
JAVA_PATH="$BASE_DIR/jdk-25.0.4"
GIT_REPO_URL="https://VasiKisha:xxxtokenxxx@https://github.com/VasiKisha/L2J-H5.git"

# Název Docker kontejnerů podle Portaineru
GAMESERVER_CONTAINER="l2j_mobius_h5-gameserver-1"
LOGINSERVER_CONTAINER="l2j_mobius_h5-loginserver-1"

echo "=========================================="
echo " Starting L2J Deployment Pipeline"
echo "=========================================="

# 1. Nastavení prostředí Java JDK
export JAVA_HOME="$JAVA_PATH"
export PATH="$JAVA_HOME/bin:$PATH"

echo "[1/6] Checking Java version..."
java -version

# 2. Aktualizace zdrojového kódu z Gitu
echo "[2/6] Updating source code from Git (branch: $TARGET_BRANCH)..."
if [ ! -d "$SRC_DIR" ]; then
    echo "Source folder does not exist. Cloning repository..."
    git clone -b "$TARGET_BRANCH" "$GIT_REPO_URL" "$SRC_DIR"
else
    cd "$SRC_DIR"
    echo "Pulling latest changes from Git..."
    git fetch --all
    git checkout "$TARGET_BRANCH"
    git reset --hard "origin/$TARGET_BRANCH"
fi

# 3. Kompilace přes Ant
echo "[3/6] Compiling project with Ant..."
cd "$SRC_DIR"
ant

# 4. Rozbalení nově zkompilovaného buildu do dočasné složky
echo "[4/6] Unpacking build..."
rm -rf "$TMP_DIR"
mkdir -p "$TMP_DIR"
unzip -q "$BUILD_ZIP" -d "$TMP_DIR"

# 5. Kompletní přepsání produkčních složek (Login + Game + Data)
echo "[5/6] Overwriting production files with Git build..."
mkdir -p "$DEPLOY_DIR"

# Přepíše strukturu a smaže ze serveru soubory, které v novém buildu už nejsou
rsync -av --checksum --delete "$TMP_DIR/" "$DEPLOY_DIR/"

# Zabezpečení spustitelnosti skriptů
chmod +x "$DEPLOY_DIR/login/"*.sh 2>/dev/null || true
chmod +x "$DEPLOY_DIR/game/"*.sh 2>/dev/null || true

# Úklid dočasné složky
rm -rf "$TMP_DIR"

# 6. Restart Docker kontejnerů přes Portainer/Docker CLI
echo "[6/6] Restarting Docker containers..."
docker restart "$LOGINSERVER_CONTAINER"
docker restart "$GAMESERVER_CONTAINER"

echo "=========================================="
echo " Deployment successful! Server updated."
echo "=========================================="
