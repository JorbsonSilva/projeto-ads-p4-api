package com.equipe.projetoadsapi.tutor.dto;

import com.equipe.projetoadsapi.dto.ApiResponse;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TutorDtosTest {

    private static final UUID ID = UUID.randomUUID();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void normalizaEspacosEValidaLimitesEmCaracteresUnicode() throws Exception {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var mensagem = ler("   " + "😀".repeat(1000) + "  ");
            assertEquals(ID, mensagem.mensagemId());
            assertEquals("😀".repeat(1000), mensagem.pergunta());
            assertTrue(validator.validate(mensagem).isEmpty());
            assertTrue(validator.validate(ler(" a ")).isEmpty());
            assertFalse(validator.validate(ler("   ")).isEmpty());
            assertFalse(validator.validate(ler("a".repeat(1001))).isEmpty());
            assertFalse(validator.validate(new TutorMensagemRequestDTO(null, "a")).isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"usuarioId", "mode", "modo", "provider", "provedor", "desconhecido", "pergunta"})
    void rejeitaCamposExtrasOuDuplicadosMesmoComJacksonPermissivo(String campo) {
        String json = """
                {"mensagemId":"%s","pergunta":"Olá","%s":"outro valor"}
                """.formatted(ID, campo);
        assertThrows(JsonMappingException.class, () -> mapper.readValue(json, TutorMensagemRequestDTO.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"não é UUID", "1-1-1-1-1", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
    void rejeitaUuidInvalidoOuAbreviado(String id) {
        String json = """
                {"mensagemId":"%s","pergunta":"Olá"}
                """.formatted(id);
        assertThrows(JsonMappingException.class, () -> mapper.readValue(json, TutorMensagemRequestDTO.class));
    }

    @Test
    void rejeitaCoercaoDeTiposEExigeOsDoisCampos() {
        assertThrows(JsonMappingException.class, () -> mapper.readValue(
                "{\"mensagemId\":\"" + ID + "\",\"pergunta\":42}", TutorMensagemRequestDTO.class));
        assertThrows(JsonMappingException.class, () -> mapper.readValue(
                "{\"mensagemId\":\"" + ID + "\"}", TutorMensagemRequestDTO.class));
    }

    @Test
    void respostaUsaEnvelopeExistente() throws Exception {
        var resposta = new TutorRespostaDTO(ID, ID, TutorRespostaDTO.Resultado.RESPONDIDA,
                "Orientação", TutorRespostaDTO.Origem.SIMULADO, List.of(new TutorEvidenciaDTO("M07", 1)));
        var tree = mapper.valueToTree(ApiResponse.ok(resposta));
        assertTrue(tree.path("success").asBoolean());
        assertEquals(200, tree.path("status").asInt());
        assertEquals(ID.toString(), tree.path("data").path("conversaId").asText());
        assertEquals("SIMULADO", tree.path("data").path("origem").asText());
        assertEquals("M07", tree.path("data").path("evidencias").get(0).path("id").asText());
    }

    private TutorMensagemRequestDTO ler(String texto) throws Exception {
        return mapper.readValue(mapper.writeValueAsBytes(java.util.Map.of("mensagemId", ID, "pergunta", texto)),
                TutorMensagemRequestDTO.class);
    }
}
