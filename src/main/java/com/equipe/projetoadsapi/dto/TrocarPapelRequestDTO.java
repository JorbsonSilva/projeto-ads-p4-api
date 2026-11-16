package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.PapelUsuario;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrocarPapelRequestDTO {

    @NotNull(message = "Informe o perfil que deseja ativar.")
    private PapelUsuario papel;
}
