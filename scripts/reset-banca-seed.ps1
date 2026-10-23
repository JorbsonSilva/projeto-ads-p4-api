# ==============================================================================
# Script de Reset do Banco de Dados para a Banca (PowerShell) — Orienta API
# ==============================================================================
param(
    [string]$DbHost = "localhost",
    [string]$DbPort = "5432",
    [string]$DbName = "orientadb",
    [string]$DbUser = "postgres",
    [string]$DbPass = "postgres"
)

$env:PGPASSWORD = $DbPass

Write-Host "🔄 [BANCA] Iniciando reset rápido do banco orientadb..." -ForegroundColor Cyan

psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -f src/main/resources/db/seed/V10__seed_banca_orienta.sql

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ [BANCA] Banco populado com sucesso!" -ForegroundColor Green
    Write-Host "   - Aluno: aluno.banca@orienta.edu.br (Senha@123)" -ForegroundColor Yellow
    Write-Host "   - Professor: prof.carlos@orienta.edu.br (Senha@123)" -ForegroundColor Yellow
    Write-Host "   - Professora: profa.juliana@orienta.edu.br (Senha@123)" -ForegroundColor Yellow
} else {
    Write-Host "❌ [ERRO] Falha ao executar o seed da banca." -ForegroundColor Red
}
