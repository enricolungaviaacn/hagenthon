#!/usr/bin/env bash
# Hook: controllo accesso al database
# Solo gli sviluppatori in .claude/developers.txt possono modificare il DB
# Trigger: PreToolUse su Bash, Edit, Write

set -euo pipefail

input=$(cat)
tool_name=$(echo "$input" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_name',''))" 2>/dev/null || echo "")

repo_root=$(git rev-parse --show-toplevel 2>/dev/null || echo "")
developers_file="$repo_root/.claude/developers.txt"

# Se la lista non esiste, nessun blocco
if [ ! -f "$developers_file" ]; then exit 0; fi

git_email=$(git config user.email 2>/dev/null || echo "")

# Controlla se l'utente e' nella lista sviluppatori
if [ -n "$git_email" ] && grep -qi "^${git_email}$" "$developers_file" 2>/dev/null; then
  exit 0  # sviluppatore autorizzato
fi

# Determina se l'operazione riguarda il DB
is_db_op=false

if [ "$tool_name" = "Bash" ]; then
  cmd=$(echo "$input" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('command',''))" 2>/dev/null || echo "")
  if echo "$cmd" | grep -qiE "\b(INSERT INTO|UPDATE .+ SET|DELETE FROM|DROP TABLE|DROP DATABASE|ALTER TABLE|TRUNCATE|CREATE TABLE|CREATE DATABASE)\b|psql\s|mysql\s|sqlite3\s|liquibase|flyway"; then
    is_db_op=true
  fi
fi

if [ "$tool_name" = "Edit" ] || [ "$tool_name" = "Write" ]; then
  fpath=$(echo "$input" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('file_path',''))" 2>/dev/null || echo "")
  if echo "$fpath" | grep -qiE "(migration|schema|flyway|liquibase|V[0-9]+__.*\.sql|\.sql$|/db/|/database/)"; then
    is_db_op=true
  fi
fi

if [ "$is_db_op" = "true" ]; then
  echo "BLOCCATO: Solo gli sviluppatori autorizzati possono modificare il database."
  echo "  Account attuale: ${git_email:-non configurato}"
  echo "  Lista autorizzati: $developers_file"
  echo "  Per visualizzare il DB (sola lettura) non e' richiesta autorizzazione."
  exit 2
fi
