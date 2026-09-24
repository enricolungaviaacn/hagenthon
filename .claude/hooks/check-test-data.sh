#!/bin/bash
# Blocca commit/push di file in test-data/ verso i branch main e stable.
# I dati di test non devono mai finire in produzione.

TOOL_INPUT="${CLAUDE_TOOL_INPUT:-}"
BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")

# Attivo solo su main e stable
case "$BRANCH" in
  main|stable) ;;
  *) exit 0 ;;
esac

# Controlla se il comando tocca test-data/
if echo "$TOOL_INPUT" | grep -q "test-data"; then
  echo "BLOCCATO: i file in test-data/ non possono essere committati o pushati su '$BRANCH'." >&2
  echo "I dati di test sono riservati ai branch feature/* e test." >&2
  exit 1
fi

# Controlla anche se ci sono file staged in test-data/ (per git add / git commit)
if echo "$TOOL_INPUT" | grep -qE "git (add|commit|push)"; then
  STAGED=$(git diff --cached --name-only 2>/dev/null | grep "^test-data/" || true)
  if [ -n "$STAGED" ]; then
    echo "BLOCCATO: file di test in staging su branch '$BRANCH': $STAGED" >&2
    echo "Esegui: git reset HEAD test-data/" >&2
    exit 1
  fi
fi

exit 0
