package com.app.confeitaria.docelivery.service;

import com.app.confeitaria.docelivery.dto.KitItemRequestDTO;
import com.app.confeitaria.docelivery.dto.KitRequestDTO;
import com.app.confeitaria.docelivery.dto.ProdutoDTO;
import com.app.confeitaria.docelivery.model.entity.Categoria;
import com.app.confeitaria.docelivery.model.entity.KitItem;
import com.app.confeitaria.docelivery.model.entity.Produto;
import com.app.confeitaria.docelivery.model.entity.Usuario;
import com.app.confeitaria.docelivery.model.repository.CategoriaRepository;
import com.app.confeitaria.docelivery.model.repository.ProdutoRepository;
import com.app.confeitaria.docelivery.model.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    // 1. CRIAR PRODUTO COMUM
    @Transactional
    public Produto criarProdutoComFoto(ProdutoDTO dto, MultipartFile file) {
        Produto produto = new Produto();
        // 🟢 CORRIGIDO PARA RECORD: de dto.getNome() para dto.nome()
        produto.setNome(dto.nome());
        produto.setDescricao(dto.descricao());
        produto.setPreco(dto.preco());
        produto.setEstoque(dto.estoque());
        produto.setCodStatus(true); // Garante que nasce ativo

        produto.setCategoriaId(dto.categoriaId());

        if (file != null && !file.isEmpty()) {
            String nomeSalvo = salvarFoto(file);
            produto.setImagemUrl(nomeSalvo);
        }

        return produtoRepository.save(produto);
    }

    // 2. CADASTRAR KIT COM PRODUTOS EXISTENTES
    @Transactional
    public Produto cadastrarKit(KitRequestDTO dto) {
        if (dto.getConfeiteiroId() == null) {
            throw new IllegalArgumentException("O ID do confeiteiro não foi enviado pelo front-end.");
        }

        Usuario confeiteiro = usuarioRepository.findById(dto.getConfeiteiroId())
                .orElseThrow(() -> new RuntimeException("Confeiteiro com ID " + dto.getConfeiteiroId() + " não foi encontrado."));

        Produto kit = new Produto();
        kit.setNome(dto.getNome());
        kit.setDescricao(dto.getDescricao());
        kit.setPreco(dto.getPreco());
        kit.setEstoque(dto.getEstoque());
        kit.setConfeiteiro(confeiteiro);
        kit.setEstoque(dto.getEstoque());
        kit.setCodStatus(true);

        if (dto.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                    .orElseThrow(() -> new RuntimeException("Categoria de ID " + dto.getCategoriaId() + " não encontrada."));
            kit.setCategoria(categoria);
        } else {
            throw new IllegalArgumentException("O campo categoriaId é obrigatório e não foi enviado no JSON.");
        }

        if (dto.getItens() != null && !dto.getItens().isEmpty()) {
            for (KitItemRequestDTO itemDto : dto.getItens()) {
                if (itemDto.getProdutoId() == null) {
                    throw new IllegalArgumentException("Um dos produtos adicionados ao kit está com o ID nulo/vazio.");
                }

                Produto produtoExistente = produtoRepository.findById(itemDto.getProdutoId())
                        .orElseThrow(() -> new RuntimeException("O produto de ID " + itemDto.getProdutoId() + " não existe no catálogo."));

                KitItem kitItem = new KitItem();
                kitItem.setKit(kit);
                kitItem.setProduto(produtoExistente);
                kitItem.setQuantidade(itemDto.getQuantidade());

                kit.getItens().add(kitItem);
            }
        } else {
            throw new RuntimeException("Não é possível criar um kit sem nenhum produto associado.");
        }

        return produtoRepository.save(kit);
    }

    // 🌟 3. ALTERAR PRODUTO (Corrigido para usar a sintaxe limpa de Record)
    @Transactional
    public Produto alterarProduto(Long id, ProdutoDTO dto, MultipartFile file) {
        Produto produtoExistente = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto com ID " + id + " não encontrado."));

        // 🟢 CORRIGIDO PARA RECORD: usando nome(), descricao(), preco() e estoque()
        produtoExistente.setNome(dto.nome());
        produtoExistente.setDescricao(dto.descricao());
        produtoExistente.setPreco(dto.preco());
        produtoExistente.setEstoque(dto.estoque());

        // Atualiza a categoria de forma segura (limpa se o DTO mandar null)
        produtoExistente.setCategoriaId(dto.categoriaId());

        if (file != null && !file.isEmpty()) {
            String nomeSalvo = salvarFoto(file);
            produtoExistente.setImagemUrl(nomeSalvo);
        }

        return produtoRepository.save(produtoExistente);
    }

    // 🌟 4. DESATIVAR PRODUTO (Inativação Lógica para não quebrar pedidos antigos)
    @Transactional
    public void desativarProduto(Long id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto com ID " + id + " não encontrado."));

        produto.setCodStatus(false); // Seta como indisponível no cardápio
        produtoRepository.save(produto);
    }

    // 🌟 5. EXCLUIR PRODUTO (Deleção Física controlada com rollback se já houver vendas)
    @Transactional
    public void excluirFisicamente(Long id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto com ID " + id + " não encontrado."));

        try {
            produtoRepository.delete(produto);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Não é possível excluir este produto de forma definitiva porque ele está vinculado ao histórico de pedidos de um cliente. Sugerimos usar a opção Desativar.");
        }
    }

    // 6. SALVAR FOTO (MÉTODO EXISTENTE CORRETO)
    public String salvarFoto(MultipartFile file) {
        String nomeArquivo = System.currentTimeMillis() + "_" +
                file.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");

        try {
            Path pastaDestino = Paths.get(uploadDir).toAbsolutePath().normalize();

            if (!Files.exists(pastaDestino)) {
                Files.createDirectories(pastaDestino);
            }

            Files.copy(file.getInputStream(), pastaDestino.resolve(nomeArquivo));
            return nomeArquivo;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar a imagem no servidor", e);
        }
    }


    // 🌟 5.1 EXCLUIR KIT DESVINCULANDO PRODUTOS (Para a Opção B)
    @Transactional
    public void excluirKitFisicamente(Long id) {
        Produto kit = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kit com ID " + id + " não encontrado."));

        try {
            // 1. Se o kit tiver itens, desvincula limpando a lista
            if (kit.getItens() != null && !kit.getItens().isEmpty()) {
                kit.getItens().clear();
                // Força o Hibernate a commitar a limpeza da tabela intermediária 'kit_item' AGORA
                produtoRepository.saveAndFlush(kit);
            }

            // 2. Com as amarrações desfeitas na tabela 'kit_item', removemos apenas o cabeçalho do kit
            produtoRepository.delete(kit);

        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Não é possível excluir este kit porque ele está vinculado a pedidos antigos. Use a opção desativar.");
        }
    }

    @Transactional
    public Produto alterarKit(Long id, KitRequestDTO dto, MultipartFile imagem) {

        Produto kit = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Kit não encontrado"));

        kit.setNome(dto.getNome());
        kit.setDescricao(dto.getDescricao());
        kit.setPreco(dto.getPreco());

        // Atualiza categoria
        if (dto.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                    .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
            kit.setCategoria(categoria);
        }

        // Limpa itens antigos
        kit.getItens().clear();

        // Adiciona itens novos
        if (dto.getItens() != null) {

            for (KitItemRequestDTO itemDto : dto.getItens()) {

                Produto produto = produtoRepository.findById(itemDto.getProdutoId())
                        .orElseThrow(() ->
                                new RuntimeException("Produto não encontrado"));

                KitItem kitItem = new KitItem();
                kitItem.setKit(kit);
                kitItem.setProduto(produto);
                kitItem.setQuantidade(itemDto.getQuantidade());

                kit.getItens().add(kitItem);
            }
        }

        // Atualiza imagem
        if (imagem != null && !imagem.isEmpty()) {
            String nomeImagem = salvarFoto(imagem);
            kit.setImagemUrl(nomeImagem);
        }

        return produtoRepository.save(kit);
    }
}