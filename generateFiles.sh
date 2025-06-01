#!/usr/bin/env bash
cd src/main/resources/files
NUM=10000000
BATCH=100000
for MODE in structural typological referential functional; do
  echo "🔧 Generando $MODE → $NUM filas…"
  python3 generate.py $MODE $NUM $BATCH
done
