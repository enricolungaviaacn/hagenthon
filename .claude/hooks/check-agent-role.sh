#!/usr/bin/env bash
# Hook: check-agent-role.sh
# Solo java-react-developer puo' modificare app/ e pushare codice.
# Gli agenti funzionali scrivono solo in agents/requirements/.
# Trigger: PreToolUse su Edit, Write, Bash

set -euo pipefail

input=$(cat)
tool_name=$(echo "$input" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_name',''))" 2>/dev/null || echo "")

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
MARKER_FILE="$REPO_ROOT/.claude/active-agent"
TECHNICAL_AGENT="java-react-developer"

active_agent=""
if [ -f "$MARKER_FILE" ]; then
  active_agent=$(cat "$MARKER_FILE" 2>/dev/null | tr -d '[:space:]')
fi

# --- Edit / Write: blocca modifiche a app/ se non agente tecnico ---
if [ "$tool_name" = "Edit" ] || [ "$tool_name" = "Write" ]; then
  file_path=$(echo "$input" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('file_path',''))" 2>/dev/null || echo "")

  normalized=$(realpath -m "$file_path" 2>/dev/null || echo "$file_path")
  app_dir=$(realpath -m "$REPO_ROOT/app" 2>/dev/null || echo "$REPO_ROOT/app")

  if [[ "$normalized" == "$app_dir"* ]]; then
    if [ "$active_agent" != "$TECHNICAL_AGENT" ]; then
      echo "BLOCCATO: Solo java-react-developer puo' modificare file in app/"
      echo "  File richiesto : $file_path"
      echo "  Agente attivo  : ${active_agent:-nessuno}"
      echo ""
      echo "  Se sei java-react-developer, attiva prima il marker:"
      echo "    echo 'java-react-developer' > .claude/active-agent"
      echo "  Gli agenti funzionali scrivono SOLO in agents/requirements/"
      exit 2
    fi
  fi
fi

# --- Bash: blocca git push con file app/ se non agente tecnico ---
if [ "$tool_name" = "Bash" ]; then
  command=$(echo "$input" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('command',''))" 2>/dev/null || echo "")

  if ! echo "$command" | grep -qE "git push"; then
    exit 0
  fi

  # Cerca file in app/ nei commit da pushare (fallback su test come base)
  has_app_files=$(git diff --name-only "@{u}...HEAD" 2>/dev/null | grep -E "^app/" | head -1 \
    || git diff --name-only "origin/test...HEAD" 2>/dev/null | grep -E "^app/" | head -1 \
    || git diff --name-only "origin/main...HEAD" 2>/dev/null | grep -E "^app/" | head -1 \
    || echo "")

  if [ -n "$has_app_files" ]; then
    if [ "$active_agent" != "$TECHNICAL_AGENT" ]; then
      echo "BLOCCATO: Solo java-react-developer puo' pushare codice in app/"
      echo "  File di codice nel push: $has_app_files"
      echo "  Agente attivo         : ${active_agent:-nessuno}"
      echo ""
      echo "  Gli agenti funzionali possono solo proporre requisiti."
      echo "  Il push del codice spetta esclusivamente a java-react-developer."
      exit 2
    fi
  fi
fi

exit 0
