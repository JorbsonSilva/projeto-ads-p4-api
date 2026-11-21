package com.equipe.projetoadsapi.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/**
 * SERVIÇO DE GEOLOCALIZAÇÃO E CÁLCULO DE DISTÂNCIA (Fórmula de Haversine)
 * Rastreabilidade: Fase 8 (f8-t3, f8-t4, f8-t5) / MAP-005, MAP-006, MAP-SEC-002
 * 
 * Executa cálculo determinístico de distâncias geográficas no servidor a custo zero,
 * sem requisições a APIs pagas (como Google Maps Geocoding) e garantindo
 * conformidade com a LGPD através de minimização de coordenadas.
 */
@Service
public class LocalizacaoService {

    /** Raio médio aproximado do planeta Terra em quilômetros */
    private static final double RAIO_TERRA_KM = 6371.0;

    /** Raios de busca oficialmente permitidos pelo produto (MAP-004 e Decisão 17) */
    public static final int[] RAIOS_PERMITIDOS_KM = {2, 5, 10, 20};

    /** Limite máximo de raio admitido pelo servidor (Decisão 17) */
    public static final int RAIO_MAXIMO_KM = 50;

    private static final Map<String, PontoAproximado> PONTOS_PRESENCIAIS = Map.ofEntries(
            ponto("Caruaru", "PE", "Centro", -8.2839, -35.9761),
            ponto("Caruaru", "PE", "Maurício de Nassau", -8.2750, -35.9730),
            ponto("Caruaru", "PE", "Indianópolis", -8.3030, -35.9670),
            ponto("Caruaru", "PE", "Boa Vista", -8.2900, -35.9630),
            ponto("Caruaru", "PE", "Salgado", -8.3000, -35.9800),
            ponto("Recife", "PE", "Boa Viagem", -8.1189, -34.8988),
            ponto("Recife", "PE", "Casa Forte", -8.0267, -34.9197),
            ponto("Recife", "PE", "Graças", -8.0416, -34.8974),
            ponto("Recife", "PE", "Madalena", -8.0560, -34.9110),
            ponto("Recife", "PE", "Várzea", -8.0539, -34.9511),
            ponto("Campina Grande", "PB", "Centro", -7.2307, -35.8817),
            ponto("Campina Grande", "PB", "Catolé", -7.2390, -35.8760),
            ponto("Campina Grande", "PB", "Bodocongó", -7.2180, -35.9100),
            ponto("Campina Grande", "PB", "Liberdade", -7.2390, -35.8910),
            ponto("Campina Grande", "PB", "Prata", -7.2240, -35.8900),
            ponto("João Pessoa", "PB", "Centro", -7.1195, -34.8450),
            ponto("João Pessoa", "PB", "Manaíra", -7.1040, -34.8340),
            ponto("João Pessoa", "PB", "Bessa", -7.0740, -34.8340),
            ponto("João Pessoa", "PB", "Tambaú", -7.1160, -34.8240),
            ponto("João Pessoa", "PB", "Bancários", -7.1550, -34.8300)
    );

    public record PontoAproximado(
            String cidade,
            String estado,
            String regiao,
            double latitude,
            double longitude
    ) {}

    private static Map.Entry<String, PontoAproximado> ponto(
            String cidade, String estado, String regiao, double latitude, double longitude
    ) {
        return Map.entry(
                chave(cidade, estado, regiao),
                new PontoAproximado(cidade, estado, regiao, latitude, longitude)
        );
    }

    private static String chave(String cidade, String estado, String regiao) {
        return normalizarLocalidade(cidade) + "|" + normalizarLocalidade(estado) + "|"
                + normalizarLocalidade(regiao);
    }

    private static String normalizarLocalidade(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Resolve cidade e bairro para um ponto público aproximado, sem armazenar endereço residencial.
     */
    public PontoAproximado resolverPontoAproximado(String cidade, String estado, String regiao) {
        PontoAproximado ponto = PONTOS_PRESENCIAIS.get(chave(cidade, estado, regiao));
        if (ponto == null) {
            throw new IllegalArgumentException(
                    "Selecione uma cidade e uma região disponíveis para atendimento presencial."
            );
        }
        return ponto;
    }

    /**
     * Calcula a distância em quilômetros entre dois pontos geográficos
     * utilizando a fórmula do grande círculo de Haversine.
     *
     * @param lat1 Latitude do ponto 1 (ex: Aluno)
     * @param lon1 Longitude do ponto 1 (ex: Aluno)
     * @param lat2 Latitude do ponto 2 (ex: Professor)
     * @param lon2 Longitude do ponto 2 (ex: Professor)
     * @return Distância em quilômetros com precisão de 1 casa decimal (arredondada)
     */
    public double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        // Converte graus para radianos
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = RAIO_TERRA_KM * c;

        // Arredonda para 1 casa decimal para evitar precisão excessiva não autorizada (LGPD MAP-SEC-002)
        return BigDecimal.valueOf(distance)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Valida se um raio especificado está dentro do limite configurável.
     */
    public int normalizarRaio(Integer raioKm) {
        if (raioKm == null || raioKm <= 0) {
            return 10; // Raio padrão de 10 km
        }
        return Math.min(raioKm, RAIO_MAXIMO_KM);
    }

    /**
     * Verifica se o ponto de destino está dentro do raio máximo especificado a partir do ponto de origem.
     */
    public boolean estaDentroDoRaio(double lat1, double lon1, double lat2, double lon2, int raioMaximoKm) {
        double dist = calcularDistanciaHaversine(lat1, lon1, lat2, lon2);
        return dist <= normalizarRaio(raioMaximoKm);
    }

    /**
     * Gera mensagem amigável de proximidade para a interface sem revelar endereço (MAP-006).
     * Exemplo: "Aproximadamente 3.2 km da sua localização" ou "Na sua região (Boa Viagem)"
     */
    public String formatarTextoProximidade(Double distanciaKm, String regiao) {
        if (distanciaKm != null) {
            return String.format("Aprox. %.1f km", distanciaKm);
        }
        if (regiao != null && !regiao.isBlank()) {
            return "Região: " + regiao;
        }
        return "Presencial sob consulta";
    }
}
