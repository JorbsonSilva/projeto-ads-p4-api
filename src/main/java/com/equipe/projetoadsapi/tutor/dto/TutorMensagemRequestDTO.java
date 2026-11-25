package com.equipe.projetoadsapi.tutor.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@JsonDeserialize(using = TutorMensagemRequestDTO.Deserializer.class)
public record TutorMensagemRequestDTO(@NotNull UUID mensagemId, String pergunta) {

    private static final Pattern ESPACOS_NAS_EXTREMIDADES =
            Pattern.compile("^\\s+|\\s+$", Pattern.UNICODE_CHARACTER_CLASS);

    public TutorMensagemRequestDTO {
        if (pergunta != null) {
            pergunta = ESPACOS_NAS_EXTREMIDADES.matcher(pergunta).replaceAll("");
        }
    }

    @JsonIgnore
    @AssertTrue(message = "A pergunta deve conter de 1 a 1.000 caracteres.")
    public boolean isPerguntaValida() {
        return pergunta != null && !pergunta.isEmpty()
                && pergunta.codePointCount(0, pergunta.length()) <= 1000;
    }

    static final class Deserializer extends JsonDeserializer<TutorMensagemRequestDTO> {
        @Override
        public TutorMensagemRequestDTO deserialize(JsonParser parser, DeserializationContext context)
                throws IOException {
            if (!parser.isExpectedStartObjectToken()) {
                return context.reportInputMismatch(TutorMensagemRequestDTO.class, "Informe um objeto JSON.");
            }
            Map<String, String> body = new HashMap<>();
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String field = parser.currentName();
                if ((!field.equals("mensagemId") && !field.equals("pergunta")) || body.containsKey(field)
                        || parser.nextToken() != JsonToken.VALUE_STRING) {
                    return context.reportInputMismatch(TutorMensagemRequestDTO.class,
                            "Informe apenas mensagemId e pergunta como texto, uma vez cada.");
                }
                body.put(field, parser.getText());
            }
            if (parser.currentToken() != JsonToken.END_OBJECT || body.size() != 2) {
                return context.reportInputMismatch(TutorMensagemRequestDTO.class,
                        "Informe apenas mensagemId e pergunta como texto.");
            }

            String rawId = body.get("mensagemId");
            UUID id;
            try {
                id = UUID.fromString(rawId);
            } catch (IllegalArgumentException exception) {
                return context.reportInputMismatch(TutorMensagemRequestDTO.class, "mensagemId deve ser um UUID.");
            }
            if (!id.toString().equalsIgnoreCase(rawId)) {
                return context.reportInputMismatch(TutorMensagemRequestDTO.class, "mensagemId deve ser um UUID.");
            }

            return new TutorMensagemRequestDTO(id, body.get("pergunta"));
        }
    }
}
