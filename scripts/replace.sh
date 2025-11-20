#!/bin/bash

# 1. 先查看哪些文件会被修改
echo "Files that will be modified:"
grep -r "^package application$" --include="*.scala" .
grep -r "^package domain$" --include="*.scala" .
grep -r "^package infrastructure$" --include="*.scala" .
grep -r "^package presentation$" --include="*.scala" .

echo -e "\nPress Enter to continue or Ctrl+C to cancel..."
read

# 2. 执行替换（带备份）
find modules/nexus/src -type f -name "*.scala" -exec perl -i.bak -pe '
  s/^package application$/package app.mosia.nexus\n package application/
' {} \;

find modules/nexus/src -type f -name "*.scala" -exec perl -i.bak -pe '
  s/^package domain$/package app.mosia.nexus\n package domain/
' {} \;

find modules/nexus/src -type f -name "*.scala" -exec perl -i.bak -pe '
  s/^package infrastructure$/package app.mosia.nexus\n package infrastructure/
' {} \;

find modules/nexus/src -type f -name "*.scala" -exec perl -i.bak -pe '
  s/^package presentation$/package app.mosia.nexus\n package presentation/
' {} \;

echo -e "\n✅ Replacement completed!"
echo "Original files backed up with .bak extension"