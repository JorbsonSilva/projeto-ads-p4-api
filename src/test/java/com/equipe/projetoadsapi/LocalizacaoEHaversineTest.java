package com.equipe.projetoadsapi;

import com.equipe.projetoadsapi.dto.ProfessorBuscaResponseDTO;
import com.equipe.projetoadsapi.model.Aptidao;
import com.equipe.projetoadsapi.model.PapelUsuario;
import com.equipe.projetoadsapi.model.Saber;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.service.LocalizacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TESTES UNITÁRIOS DA FASE 8: GEOLOCALIZAÇÃO E PRIVACIDADE (MAP-001 a MAP-006, LGPD)
 * Rastreabilidade: Tarefa f8-t6
 */
public class LocalizacaoEHaversineTest {

    private final LocalizacaoService localizacaoService = new LocalizacaoService();

    @Test
    @DisplayName("Cálculo Haversine: Distância conhecida entre Marco Zero e Boa Viagem (Recife)")
    void testCalculoHaversineDistanciaConhecida() {
        // Marco Zero (Recife Antigo)
        double latMarcoZero = -8.063169;
        double lonMarcoZero = -34.871139;

        // Praia de Boa Viagem (Recife)
        double latBoaViagem = -8.118900;
        double lonBoaViagem = -34.898800;

        double distancia = localizacaoService.calcularDistanciaHaversine(
                latMarcoZero, lonMarcoZero, latBoaViagem, lonBoaViagem
        );

        // A distância real em linha reta é de aproximadamente 6.8 km a 7.0 km
        assertTrue(distancia >= 6.5 && distancia <= 7.5,
                "A distância calculada (" + distancia + " km) deve estar em torno de 6.9 km");
    }

    @Test
    @DisplayName("Cálculo Haversine: Pontos idênticos devem retornar distância zero")
    void testCalculoHaversinePontosIdenticos() {
        double lat = -8.053890;
        double lon = -34.951110;

        double distancia = localizacaoService.calcularDistanciaHaversine(lat, lon, lat, lon);
        assertEquals(0.0, distancia, "Distância entre o mesmo ponto deve ser exatamente 0.0 km");
    }

    @Test
    @DisplayName("Normalização de Raio: Deve aceitar 2, 5, 10, 20 e limitar a 50 km (Decisão 17)")
    void testNormalizacaoRaio() {
        assertEquals(10, localizacaoService.normalizarRaio(null), "Raio nulo deve assumir padrão 10 km");
        assertEquals(10, localizacaoService.normalizarRaio(-5), "Raio negativo deve assumir padrão 10 km");
        assertEquals(2, localizacaoService.normalizarRaio(2));
        assertEquals(5, localizacaoService.normalizarRaio(5));
        assertEquals(20, localizacaoService.normalizarRaio(20));
        assertEquals(50, localizacaoService.normalizarRaio(999), "Raio excessivo deve ser truncado em 50 km");
    }

    @Test
    @DisplayName("LGPD e Minimização (MAP-SEC-002): DTO público nunca pode conter campos de latitude/longitude")
    void testPrivacidadeDtoPublicoSemCoordenadas() {
        // Valida que ProfessorBuscaResponseDTO não possui campos expostos de coordenadas
        Field[] campos = ProfessorBuscaResponseDTO.class.getDeclaredFields();
        for (Field f : campos) {
            String nome = f.getName().toLowerCase();
            assertFalse(nome.contains("latitude"), "O DTO público não pode possuir campo de latitude! Campo violador: " + f.getName());
            assertFalse(nome.contains("longitude"), "O DTO público não pode possuir campo de longitude! Campo violador: " + f.getName());
        }
    }

    @Test
    @DisplayName("Formatação de Proximidade: Deve exibir texto aproximado sem expor endereço residencial")
    void testFormatacaoProximidadeAmigavel() {
        String textoComDistancia = localizacaoService.formatarTextoProximidade(3.4, "Boa Viagem");
        assertTrue(textoComDistancia.contains("3.4 km") || textoComDistancia.contains("3,4 km"));

        String textoSemDistancia = localizacaoService.formatarTextoProximidade(null, "Espinheiro");
        assertEquals("Região: Espinheiro", textoSemDistancia);

        String textoFallback = localizacaoService.formatarTextoProximidade(null, null);
        assertEquals("Presencial sob consulta", textoFallback);
    }

    @Test
    @DisplayName("DTO Factory: Conversão de entidade para DTO preserva distância calculada")
    void testFromEntityComDistancia() {
        Usuario prof = Usuario.builder()
                .id(10L)
                .nome("Prof. Roberto")
                .papel(PapelUsuario.ROLE_PROFESSOR)
                .oferecePresencial(true)
                .regiaoAtendimento("Várzea")
                .latitudeAproximada(-8.053890)
                .longitudeAproximada(-34.951110)
                .build();

        Saber saber = new Saber(1L, "Cálculo I", "Matemática");
        Aptidao apt = new Aptidao(1L, prof, saber, 90.0, "Avançado");
        prof.setAptidoes(List.of(apt));

        ProfessorBuscaResponseDTO dto = ProfessorBuscaResponseDTO.fromEntity(prof, 4.2, "Aprox. 4.2 km");

        assertEquals(10L, dto.getId());
        assertEquals("Prof. Roberto", dto.getNome());
        assertTrue(dto.getOferecePresencial());
        assertEquals("Várzea", dto.getRegiaoAtendimento());
        assertEquals(4.2, dto.getDistanciaAproximadaKm());
        assertEquals("Aprox. 4.2 km", dto.getTextoProximidade());
        assertEquals(1, dto.getAptidoes().size());
        assertEquals("Cálculo I", dto.getAptidoes().get(0).getSaberNome());
    }
}
