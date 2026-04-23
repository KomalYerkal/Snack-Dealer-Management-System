#!/bin/bash
# ============================================================
#  build.sh — Compile and run the Snack Dealer Management System
#
#  Prerequisites:
#    1. JDK 17+ installed  (java / javac in PATH)
#    2. MySQL 8+ running    (adjust DB credentials in DatabaseConnection.java)
#    3. mysql-connector-j-8.x.jar in the lib/ folder
#       Download: https://dev.mysql.com/downloads/connector/j/
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"
BIN_DIR="$SCRIPT_DIR/bin"
LIB_DIR="$SCRIPT_DIR/lib"
JAR="$LIB_DIR/mysql-connector-j.jar"

# ── Check JDBC jar ────────────────────────────────────────────
if [ ! -f "$JAR" ]; then
  echo "ERROR: MySQL JDBC driver not found at $JAR"
  echo "Download from https://dev.mysql.com/downloads/connector/j/"
  echo "and save as: $JAR"
  exit 1
fi

# ── Compile ───────────────────────────────────────────────────
echo "Compiling sources..."
mkdir -p "$BIN_DIR"
find "$SRC_DIR" -name "*.java" > /tmp/sources.txt
javac -cp "$JAR" -d "$BIN_DIR" @/tmp/sources.txt
echo "Compilation successful."

# ── Run ───────────────────────────────────────────────────────
echo "Launching Snack Dealer Management System..."
java -cp "$BIN_DIR:$JAR" snack.Main