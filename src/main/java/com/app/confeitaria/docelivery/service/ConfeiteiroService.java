package com.app.confeitaria.docelivery.service;

import com.app.confeitaria.docelivery.dto.ConfeiteiroDTO;
import com.app.confeitaria.docelivery.dto.LojaDTO;
import com.app.confeitaria.docelivery.model.entity.Confeiteiro;
import com.app.confeitaria.docelivery.model.entity.Loja;
import com.app.confeitaria.docelivery.model.entity.Usuario;
import com.app.confeitaria.docelivery.model.entity.Produto; // Import da sua entidade Produto
import com.app.confeitaria.docelivery.model.repository.ConfeiteiroRepository;
import com.app.confeitaria.docelivery.model.repository.LojaRepository;
import com.app.confeitaria.docelivery.model.repository.ProdutoRepository; // Import do seu Repositório de Produtos
import com.app.confeitaria.docelivery.model.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List; // Import necessário para reconhecer coleções do tipo List

@Service
public class ConfeiteiroService {

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private ConfeiteiroRepository repository;

    @Autowired
    private LojaRepository lojaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * MÉTODO ADICIONADO PARA O CONTROLLER FUNCIONAR DIREITO:
     * Ele busca a entidade real com o cache limpo e converte para o DTO esperado pelo front-end.
     */
    @Transactional(readOnly = true)
    public ConfeiteiroDTO buscarConfeiteiroPorId(Long id) {
        // Aproveita a busca do buscarPorIdReal que limpa o cache do Hibernate
        Confeiteiro confeiteiro = buscarPorIdReal(id);

        // Converte a entidade Confeiteiro encontrada para o ConfeiteiroDTO
        return converterParaDTO(confeiteiro);
    }

    /**
     * 🟢 NOVO MÉTODO CRÍTICO: Usado pelo seu Controller para buscar os dados reais por ID
     * Força a limpeza do cache de sessão para garantir que o nó da loja não venha nulo
     */
    @Transactional(readOnly = true)
    public Confeiteiro buscarPorIdReal(Long id) {
        // Limpa o cache local do Hibernate para forçar o SQL com LEFT JOIN FETCH
        entityManager.clear();

        return repository.buscarComLojaPorId(id)
                .orElseThrow(() -> new RuntimeException("Confeiteiro não encontrado com o ID: " + id));
    }

    @Transactional
    public Confeiteiro atualizarConfeiteiro(Long id, Confeiteiro dadosAtualizados) {
        System.out.println("Recebido no Service para atualizar: " + dadosAtualizados.getNome() + " - CEP: " + dadosAtualizados.getCep());

        Confeiteiro confeiteiro = repository.buscarComLojaPorId(id)
                .orElseThrow(() -> new RuntimeException("Confeiteiro não encontrado com o ID: " + id));

        if (dadosAtualizados.getNome() != null) confeiteiro.setNome(dadosAtualizados.getNome());
        if (dadosAtualizados.getTelefone() != null) confeiteiro.setTelefone(dadosAtualizados.getTelefone());
        if (dadosAtualizados.getCpf() != null) confeiteiro.setCpf(dadosAtualizados.getCpf());
        if (dadosAtualizados.getCep() != null) confeiteiro.setCep(dadosAtualizados.getCep());
        if (dadosAtualizados.getEndereco() != null) confeiteiro.setEndereco(dadosAtualizados.getEndereco());
        if (dadosAtualizados.getBairro() != null) confeiteiro.setBairro(dadosAtualizados.getBairro());
        if (dadosAtualizados.getCidade() != null) confeiteiro.setCidade(dadosAtualizados.getCidade());
        if (dadosAtualizados.getUf() != null) confeiteiro.setUf(dadosAtualizados.getUf());
        if (dadosAtualizados.getDataNascimento() != null) confeiteiro.setDataNascimento(dadosAtualizados.getDataNascimento());

        if (dadosAtualizados.getLoja() != null && confeiteiro.getLoja() != null) {
            confeiteiro.getLoja().setNomeFantasia(dadosAtualizados.getLoja().getNomeFantasia());
            if (dadosAtualizados.getLoja().getDescricao() != null) confeiteiro.getLoja().setDescricao(dadosAtualizados.getLoja().getDescricao());
            if (dadosAtualizados.getLoja().getFotoUrl() != null) confeiteiro.getLoja().setFotoUrl(dadosAtualizados.getLoja().getFotoUrl());
        }

        return repository.save(confeiteiro);
    }

    @Transactional
    public Confeiteiro atualizarPerfilLoja(Long idLoja, LojaDTO dto, MultipartFile imagem) {
        // 1. Busca primeiro a loja pelo ID dela (que é o número 4 que o front está a enviar)
        Loja loja = lojaRepository.findById(idLoja)
                .orElseThrow(() -> new RuntimeException("Loja não encontrada com o ID: " + idLoja));

        // 2. Pega o confeiteiro dono dessa loja
        Confeiteiro confeiteiro = loja.getConfeiteiro();
        if (confeiteiro == null) {
            throw new RuntimeException("Nenhum confeiteiro vinculado a esta loja.");
        }

        // 3. Atualiza os campos vindos do DTO na loja
        if (dto.getNomeFantasia() != null && !dto.getNomeFantasia().trim().isEmpty()) {
            loja.setNomeFantasia(dto.getNomeFantasia());
        }

        if (dto.getCnpj() != null && !dto.getCnpj().trim().isEmpty()) {
            loja.setCnpj(dto.getCnpj().replaceAll("[^0-9]", ""));
        }

        if (dto.getTelefone() != null && !dto.getTelefone().trim().isEmpty()) {
            loja.setTelefone(dto.getTelefone());
        }

        if (dto.getEndereco() != null && !dto.getEndereco().trim().isEmpty()) {
            loja.setEndereco(dto.getEndereco());
        }

        loja.setDescricao(dto.getDescricao());
        if (imagem != null && !imagem.isEmpty()) {

            String nomeArquivo = produtoService.salvarFoto(imagem);

            loja.setFotoUrl(nomeArquivo);

            System.out.println("Imagem salva: " + nomeArquivo);
        }

        loja.setStatus("ATIVO");

        // Se você inverteu a relação e tirou a coluna confeiteiro_id da loja:
        Loja lojaSalva = lojaRepository.save(loja);
        confeiteiro.setLoja(lojaSalva); // O Confeiteiro recebe a loja salva e atualiza seu loja_id
        return repository.save(confeiteiro);
    }

    @Transactional(readOnly = true)
    public Object obterPerfilPorEmail(String email) {
        entityManager.clear(); // Garante dados novos na busca por email
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (usuario instanceof Confeiteiro) {
            Confeiteiro confeiteiro = repository.buscarComLojaPorId(usuario.getId())
                    .orElse((Confeiteiro) usuario);

            if (confeiteiro.getLoja() == null) {
                Loja lojaProvisoria = new Loja();
                lojaProvisoria.setNomeFantasia("Preencha o nome da sua Confeitaria");
                lojaProvisoria.setDescricao("Clique em editar para adicionar uma descrição.");
                lojaProvisoria.setStatus("PENDENTE");
                confeiteiro.setLoja(lojaProvisoria);
            }
            return confeiteiro;
        }
        return usuario;
    }

    @Transactional
    public void atualizarDadosGerais(Long idLoja, String novoNome) {
        Loja loja = lojaRepository.findById(idLoja)
                .orElseThrow(() -> new RuntimeException("Loja não encontrada com o ID: " + idLoja));

        loja.setNomeFantasia(novoNome);
        lojaRepository.save(loja);
    }

    // 🟢 CORRIGIDO: Alterado de Usuario para Confeiteiro para o Java achar o .getLoja() e injetado a busca de produtos com o método correto do repositório
    public ConfeiteiroDTO converterParaDTO(Confeiteiro confeiteiroEntity) {
        ConfeiteiroDTO dto = new ConfeiteiroDTO();
        dto.setId(confeiteiroEntity.getId());
        dto.setNome(confeiteiroEntity.getNome());
        dto.setEmail(confeiteiroEntity.getEmail());
        dto.setCpf(confeiteiroEntity.getCpf());
        dto.setTelefone(confeiteiroEntity.getTelefone());
        dto.setDataNascimento(confeiteiroEntity.getDataNascimento());
        dto.setTipoUsuario(confeiteiroEntity.getTipoUsuario());
        dto.setCodStatus(confeiteiroEntity.getCodStatus());
        dto.setCep(confeiteiroEntity.getCep());
        dto.setEndereco(confeiteiroEntity.getEndereco());
        dto.setBairro(confeiteiroEntity.getBairro());
        dto.setCidade(confeiteiroEntity.getCidade());
        dto.setUf(confeiteiroEntity.getUf());

        // Agora o método getLoja() compila perfeitamente porque a tipagem é Confeiteiro
        if (confeiteiroEntity.getLoja() != null) {
            LojaDTO lojaDTO = new LojaDTO();
            lojaDTO.setId(confeiteiroEntity.getLoja().getId());
            lojaDTO.setNomeFantasia(confeiteiroEntity.getLoja().getNomeFantasia());
            lojaDTO.setCnpj(confeiteiroEntity.getLoja().getCnpj());
            lojaDTO.setTelefone(confeiteiroEntity.getLoja().getTelefone());
            lojaDTO.setDescricao(confeiteiroEntity.getLoja().getDescricao());
            lojaDTO.setEndereco(confeiteiroEntity.getLoja().getEndereco());
            lojaDTO.setFotoUrl(confeiteiroEntity.getLoja().getFotoUrl());

            // 🟢 AJUSTADO: Usando o método real do seu repositório (findByConfeiteiroId)
            // Passamos o ID do Confeiteiro (confeiteiroEntity.getId()) que está vinculado à loja
            List<Produto> listaProdutos = produtoRepository.findKitsByConfeiteiroId(confeiteiroEntity.getId());
            lojaDTO.setProdutos(listaProdutos);

            dto.setLoja(lojaDTO);
        } else {
            dto.setLoja(null);
        }

        return dto;
    }
}