#!/usr/bin/env bash
# Hook: impone il workflow feature-branch
# Blocca push e merge diretti su main, stable, test
# Trigger: PreToolUse su Bash

set -euo pipefail

input=$(cat)
tool_name=$(echo "$input" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_name',''))" 2>/dev/null || echo "")

if [ "$tool_name" != "Bash" ]; then exit 0; fi

command=$(echo "$input" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('command',''))" 2>/dev/null || echo "")

if ! echo "$command" | grep -qE "git (push|merge)"; then
  exit 0
fi

PROTECTED="main stable test"
current_branch=$(git branch --show-current 2>/dev/null || echo "")

for branch in $PROTECTED; do
  # Blocca push quando il branch corrente e' gia' un branch protetto
  if echo "$command" | grep -qE "git push" && [ "$current_branch" = "$branch" ]; then
    echo "BLOCCATO: Push diretto su '$branch' non consentito."
    echo "  Branch corrente: $current_branch"
    echo "  Azione richiesta: crea un branch feature/nome e apri una Pull Request su GitHub."
    exit 2
  fi

  # Blocca push espliciti verso branch protetti (es. git push origin main)
  if echo "$command" | grep -qE "push.+\b${branch}\b"; then
    echo "BLOCCATO: Push diretto su '$branch' non consentito."
    echo "  Usa una Pull Request per promuovere le modifiche su '$branch'."
    exit 2
  fi

  # Blocca merge diretti su branch protetti
  if echo "$command" | grep -qE "git merge" && [ "$current_branch" = "$branch" ]; then
    echo "BLOCCATO: Merge diretto su '$branch' non consentito."
    echo "  Apri una Pull Request su GitHub per fare il merge."
    exit 2
  fi
done
