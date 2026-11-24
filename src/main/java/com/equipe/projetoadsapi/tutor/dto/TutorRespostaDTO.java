package com.equipe.projetoadsapi.tutor.dto;

import java.util.List;
import java.util.UUID;

public record TutorRespostaDTO(
        UUID conversaId,
        UUID mensagemId,
        Resultado resultado,
        String resposta,
        Origem origem,
        List<TutorEvidenciaDTO> evidencias
) {
    public enum Resultado {
        RESPONDIDA,
        SEM_EVIDENCIA
    }

    public enum Origem {
        RAG,
        DETERMINISTICO,
        SIMULADO,
        DEMONSTRATIVO
    }
}
