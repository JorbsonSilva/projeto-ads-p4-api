#!/usr/bin/env bash
# ==============================================================================
# Script de Reset do Banco de Dados para a Banca — Orienta API
# Executa limpeza e população do seed em ~15 segundos
# ==============================================================================
set -e

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-orientadb}
DB_USER=${DB_USER:-postgres}
DB_PASS=${DB_PASS:-postgres}

echo "🔄 [BANCA] Iniciando reset rápido do banco de dados orientadb..."

export PGPASSWORD=$DB_PASS

# Executa migrações Flyway ou reset direto
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f src/main/resources/db/seed/V10__seed_banca_orienta.sql

echo "✅ [BANCA] Banco populado com sucesso!"
echo "📋 Contas de teste ativas:"
echo "   - Aluno: aluno.banca@orienta.edu.br (Senha@123)"
echo "   - Professor: prof.carlos@orienta.edu.br (Senha@123)"
echo "   - Professora: profa.juliana@orienta.edu.br (Senha@123)"
