package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Usuario findByEmail(String email);

    Usuario findByCpf(String cpf);

    // 🟢 Busca unificada por e-mail (ignorando maiúsculas) OU por CPF (limpo ou formatado)
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:identificador) OR u.cpf = :cpfLimpo OR u.cpf = :identificador")
    Usuario findByEmailOrCpf(@Param("identificador") String identificador, @Param("cpfLimpo") String cpfLimpo);

    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.aptidoes a WHERE a.saber.id IN :saberIds ORDER BY u.notaMedia DESC")
    List<Usuario> findProfessoresPorMateriasOrdenadosPorNota(@Param("saberIds") List<Long> saberIds);
}