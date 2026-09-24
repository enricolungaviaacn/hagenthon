#!/usr/bin/env bash
# Hook: apre automaticamente una PR su GitHub dopo ogni push su un branch feature
# Trigger: PostToolUse su Bash (git push)

set -euo pipefail

input=$(cat)
tool_name=$(echo "$input" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_name',''))" 2>/dev/null || echo "")

if [ "$tool_name" != "Bash" ]; then exit 0; fi

command=$(echo "$input" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('command',''))" 2>/dev/null || echo "")

if ! echo "$command" | grep -qE "git push"; then exit 0; fi

current_branch=$(git branch --show-current 2>/dev/null || echo "")

if ! echo "$current_branch" | grep -qE "^feature/"; then exit 0; fi

# Controlla se esiste già una PR aperta per questo branch
existing_pr=$(gh pr list --head "$current_branch" --state open --json number --jq '.[0].number' 2>/dev/null || echo "")

if [ -n "$existing_pr" ]; then
  echo "PR #$existing_pr già aperta per il branch '$current_branch'. Nessuna azione."
  exit 0
fi

echo "Apertura PR automatica: $current_branch -> test"
gh pr create \
  --base test \
  --head "$current_branch" \
  --title "$(git log -1 --pretty=%s)" \
  --body "PR aperta automaticamente dopo push su \`$current_branch\`.

## Commit inclusi
$(git log origin/test.."$current_branch" --oneline 2>/dev/null || git log --oneline -5)

---
> In caso di conflitti, l'agente **agent-lead** provvederà alla risoluzione prima del merge."
