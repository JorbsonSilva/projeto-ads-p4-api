package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.ProfessorBuscaResponseDTO;
import com.equipe.projetoadsapi.model.Aptidao;
import com.equipe.projetoadsapi.model.PapelUsuario;
import com.equipe.projetoadsapi.model.Saber;
import com.equipe.projetoadsapi.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TESTES UNITÁRIOS DO SERVIÇO DE GEOLOCALIZAÇÃO E HAVERSINE (BACK-017).
 * Rastreabilidade: Fase 7 - Bloco L (BACK-017) & Proteção LGPD
 */
public class LocalizacaoServiceTest {

    private final LocalizacaoService localizacaoService = new LocalizacaoService();

    @Test
    @DisplayName("[BACK-017] Haversine: Deve calcular com precisão a distância conhecida entre Marco Zero e Boa Viagem")
    void deveCalcularDistanciaHaversineComPrecisao() {
        // Marco Zero (Recife Antigo)
        double latMarcoZero = -8.063169;
        double lonMarcoZero = -34.871139;

        // Praia de Boa Viagem (Recife)
        double latBoaViagem = -8.118900;
        double lonBoaViagem = -34.898800;

        double distancia = localizacaoService.calcularDistanciaHaversine(
                latMarcoZero, lonMarcoZero, latBoaViagem, lonBoaViagem
        );

        // A distância geodésica em linha reta é de ~6.8 a 7.0 km
        assertTrue(distancia >= 6.5 && distancia <= 7.5,
                "A distância calculada (" + distancia + " km) deve estar em torno de 6.9 km");
    }

    @Test
    @DisplayName("[BACK-017] Haversine: Coordenadas idênticas devem retornar distância 0.0 km")
    void deveRetornarZeroParaPontosIdenticos() {
        double lat = -8.053890;
        double lon = -34.951110;

        double distancia = localizacaoService.calcularDistanciaHaversine(lat, lon, lat, lon);
        assertEquals(0.0, distancia, "Distância entre pontos idênticos deve ser 0.0 km");
    }

    @Test
    @DisplayName("[BACK-017] Normalização de Raio: Deve aceitar 2, 5, 10, 20 km e truncar valores excessivos em 50 km")
    void deveNormalizarRaioDeAtendimento() {
        assertEquals(10, localizacaoService.normalizarRaio(null), "Raio nulo deve assumir default de 10 km");
        assertEquals(10, localizacaoService.normalizarRaio(-5), "Raio negativo deve assumir default de 10 km");
        assertEquals(10, localizacaoService.normalizarRaio(0), "Raio zero deve assumir default de 10 km");
        assertEquals(2, localizacaoService.normalizarRaio(2));
        assertEquals(5, localizacaoService.normalizarRaio(5));
        assertEquals(10, localizacaoService.normalizarRaio(10));
        assertEquals(20, localizacaoService.normalizarRaio(20));
        assertEquals(50, localizacaoService.normalizarRaio(999), "Raio acima do teto deve ser truncado em 50 km");
    }

    @Test
    @DisplayName("[BACK-017] Raio de Atendimento: Deve validar corretamente se ponto alvo está dentro ou fora do raio")
    void deveValidarSePontoEstaDentroOuForaDoRaio() {
        double latOrigem = -8.063169;
        double lonOrigem = -34.871139;

        // Ponto a ~6.9 km (Boa Viagem)
        double latAlvo = -8.118900;
        double lonAlvo = -34.898800;

        // Raio de 10 km: dentro
        assertTrue(localizacaoService.estaDentroDoRaio(latOrigem, lonOrigem, latAlvo, lonAlvo, 10));

        // Raio de 5 km: fora
        assertFalse(localizacaoService.estaDentroDoRaio(latOrigem, lonOrigem, latAlvo, lonAlvo, 5));
    }

    @Test
    @DisplayName("[BACK-017] LGPD (Minimização): DTO de busca pública nunca pode expor latitude ou longitude do professor")
    void deveGarantirProtecaoLgpdSemCamposDeCoordenadasEmDtoPublico() {
        Field[] campos = ProfessorBuscaResponseDTO.class.getDeclaredFields();
        for (Field f : campos) {
            String nome = f.getName().toLowerCase();
            assertFalse(nome.contains("latitude"), "Violação LGPD: DTO público não pode conter campo de latitude! Campo: " + f.getName());
            assertFalse(nome.contains("longitude"), "Violação LGPD: DTO público não pode conter campo de longitude! Campo: " + f.getName());
        }
    }

    @Test
    @DisplayName("[BACK-017] Formatação de Proximidade: Exibe distância calculada ou bairro sem revelar coordenadas")
    void deveFormatarTextoProximidadeSemExporEndereco() {
        String textoComDistancia = localizacaoService.formatarTextoProximidade(4.5, "Manaíra");
        assertTrue(textoComDistancia.contains("4.5 km") || textoComDistancia.contains("4,5 km"));

        String textoSemDistancia = localizacaoService.formatarTextoProximidade(null, "Tambaú");
        assertEquals("Região: Tambaú", textoSemDistancia);

        String textoFallback = localizacaoService.formatarTextoProximidade(null, null);
        assertEquals("Presencial sob consulta", textoFallback);
    }

    @Test
    @DisplayName("[BACK-017] Factory fromEntity: Converte professor preservando distância calculada e flag presencial")
    void deveConverterProfessorPreservandoDistanciaCalculada() {
        Usuario prof = Usuario.builder()
                .id(20L)
                .nome("Prof. Carlos Geometria")
                .papel(PapelUsuario.ROLE_PROFESSOR)
                .oferecePresencial(true)
                .regiaoAtendimento("Cabo Branco")
                .latitudeAproximada(-7.1150)
                .longitudeAproximada(-34.8250)
                .build();

        Saber saber = new Saber(5L, "Geometria Analítica", "Matemática");
        Aptidao apt = new Aptidao();
        apt.setId(1L);
        apt.setUsuario(prof);
        apt.setSaber(saber);
        apt.setNivelDominio("Avançado");
        apt.setPrecoHora(BigDecimal.valueOf(80.0));
        prof.setAptidoes(List.of(apt));

        ProfessorBuscaResponseDTO dto = ProfessorBuscaResponseDTO.fromEntity(prof, 3.8, "Aprox. 3.8 km");

        assertNotNull(dto);
        assertEquals(20L, dto.getId());
        assertEquals("Prof. Carlos Geometria", dto.getNome());
        assertTrue(dto.getOferecePresencial());
        assertEquals("Cabo Branco", dto.getRegiaoAtendimento());
        assertEquals(3.8, dto.getDistanciaAproximadaKm());
        assertEquals(3.8, dto.getDistanciaKm());
        assertEquals("Aprox. 3.8 km", dto.getTextoProximidade());
    }
}
