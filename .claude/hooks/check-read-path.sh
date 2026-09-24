#!/usr/bin/env bash
# Hook: blocca la lettura di file al di fuori del repository
# Trigger: PreToolUse su Read, Edit, Write

set -euo pipefail

input=$(cat)
tool_name=$(echo "$input" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_name',''))" 2>/dev/null || echo "")
file_path=$(echo "$input" | python3 -c "import sys,json; d=json.load(sys.stdin); ti=d.get('tool_input',{}); print(ti.get('file_path',''))" 2>/dev/null || echo "")

if [ -z "$file_path" ]; then exit 0; fi

repo_root=$(git rev-parse --show-toplevel 2>/dev/null || echo "")
if [ -z "$repo_root" ]; then exit 0; fi

real_file=$(realpath -m "$file_path" 2>/dev/null || echo "$file_path")
real_repo=$(realpath "$repo_root" 2>/dev/null || echo "$repo_root")

if [[ "$real_file" != "$real_repo"* ]]; then
  echo "BLOCCATO: Non puoi accedere a file al di fuori del repository."
  echo "  Path richiesto : $file_path"
  echo "  Repository root: $repo_root"
  echo "Per leggere un file esterno, condividilo direttamente in chat."
  exit 2
fi
