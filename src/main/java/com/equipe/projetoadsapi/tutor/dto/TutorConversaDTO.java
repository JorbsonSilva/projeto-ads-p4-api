package com.equipe.projetoadsapi.tutor.dto;

import java.time.Instant;
import java.util.UUID;

public record TutorConversaDTO(UUID conversaId, Instant expiraEm) {
}
