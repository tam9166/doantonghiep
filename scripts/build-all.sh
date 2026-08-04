#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND="$ROOT/Frontend/nha-hang-frontend"
BACKEND="$ROOT/quanlynhahang"

cd "$FRONTEND"
rm -rf dist
if [ -d node_modules ]; then
  npm install
else
  npm ci
fi
npm run lint
npm run test
npm run build

cd "$BACKEND"
./mvnw clean test
./mvnw clean package

echo "Build completed."
