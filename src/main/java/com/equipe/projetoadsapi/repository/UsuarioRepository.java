package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * REPOSITÓRIO DE ACESSO A DADOS: USUÁRIO
 *
 * Roteiro de Defesa para a Banca Examinadora:
 * "Esta interface estende o JpaRepository do Spring Data JPA, que gera automaticamente
 * as operações CRUD e consultas otimizadas para a tabela 'tb_usuarios' do PostgreSQL/Supabase.
 * Centraliza as regras de busca tanto para o fluxo de Autenticação (Login por E-mail ou CPF)
 * quanto para o fluxo de Cadastro (verificação de unicidade cadastral) e busca por aptidões."
 *
 * Arquivo compartilhado entre: Tela de Login e Tela de Cadastro.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca usuário pelo e-mail exato.
     * Utilizado na validação de unicidade de e-mail no Cadastro e no envio do código de 6 dígitos.
     *
     * @param email E-mail do usuário normalizado em minúsculas
     * @return Entidade Usuario ou null se não encontrado
     */
    Usuario findByEmail(String email);

    /**
     * Busca usuário pelo CPF (11 dígitos numéricos limpos ou com formatação).
     * Utilizado na validação de unicidade de CPF no Cadastro para impedir duplicidade no banco.
     *
     * @param cpf Número do CPF sem pontuação
     * @return Entidade Usuario ou null se não encontrado
     */
    Usuario findByCpf(String cpf);

    /**
     * Verifica rapidamente se já existe um usuário com o e-mail informado (retorna boolean sem carregar a entidade).
     * Otimização de performance para consultas de pré-validação.
     */
    boolean existsByEmail(String email);

    /**
     * Verifica se já existe um usuário com o CPF informado.
     * Garante conformidade com a restrição UNIQUE da tabela tb_usuarios.
     */
    boolean existsByCpf(String cpf);

    /**
     * BUSCA UNIFICADA POR IDENTIFICADOR (E-MAIL OU CPF):
     * Permite que o campo unificado 'E-mail ou CPF' da Tela de Login localize o usuário
     * independentemente de ele ter digitado o e-mail (em maiúsculas/minúsculas) ou o CPF
     * (com ou sem pontuação de máscara '000.000.000-00').
     *
     * @param identificador E-mail ou CPF digitado pelo usuário
     * @param cpfLimpo      CPF apenas com os números limpos (sem pontos ou traço)
     * @return Usuário localizado
     */
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:identificador) OR u.cpf = :cpfLimpo OR u.cpf = :identificador")
    Usuario findByEmailOrCpf(@Param("identificador") String identificador, @Param("cpfLimpo") String cpfLimpo);

    /**
     * Método legado mantido para compatibilidade histórica do sistema.
     * @deprecated A autenticação moderna deve utilizar AuthService com BCrypt e findByEmailOrCpf.
     */
    @Deprecated
    Usuario findBySenha(String senha);

    /**
     * BUSCA PROFESSORES FILTRADOS PELAS MATÉRIAS DE INTERESSE E ORDENA PELA MAIOR NOTA:
     * Utilizado na recomendação de mentores acadêmicos para a tela inicial do aluno.
     *
     * @param saberIds Lista de IDs das matérias de interesse do aluno
     * @return Lista de professores com melhor avaliação
     */
    @Query("SELECT DISTINCT u FROM Usuario u JOIN u.aptidoes a WHERE a.saber.id IN :saberIds ORDER BY u.notaMedia DESC")
    List<Usuario> findProfessoresPorMateriasOrdenadosPorNota(@Param("saberIds") List<Long> saberIds);
}
