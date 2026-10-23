package com.equipe.projetoadsapi.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
