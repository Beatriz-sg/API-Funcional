package com.app.confeitaria.docelivery.controller;

import com.app.confeitaria.docelivery.dto.KitRequestDTO;
import com.app.confeitaria.docelivery.dto.ProdutoDTO;
import com.app.confeitaria.docelivery.model.entity.*;
import com.app.confeitaria.docelivery.model.repository.ProdutoRepository;
import com.app.confeitaria.docelivery.model.repository.UsuarioRepository;
import com.app.confeitaria.docelivery.model.repository.CategoriaRepository;
import com.app.confeitaria.docelivery.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

    @RestController
    @RequestMapping("/api/produtos")
    public class ProdutoController {

        @Autowired
        private ProdutoRepository produtoRepository;

        @Autowired
        private UsuarioRepository usuarioRepository;

        @Autowired
        private CategoriaRepository categoriaRepository;

        @Autowired
        private ProdutoService produtoService;

        // 1. CRIAR PRODUTO NORMAL
        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<?> create(
                @RequestPart("produto") ProdutoDTO dto,
                @RequestPart(value = "imagem", required = false) MultipartFile imagem,
                @RequestParam("confeiteiroId") Long confeiteiroId) {
            try {
                Usuario confeiteiro = usuarioRepository.findById(confeiteiroId)
                        .orElseThrow(() -> new RuntimeException("Confeiteiro não encontrado"));

                Produto produto = new Produto();
                // 🟢 SINTAXE CORRETA PARA RECORD: nome(), descricao(), preco(), estoque()
                produto.setNome(dto.nome());
                produto.setDescricao(dto.descricao());
                produto.setPreco(dto.preco());
                produto.setEstoque(dto.estoque());
                produto.setConfeiteiro(confeiteiro);
                produto.setCodStatus(true); // Nasce ativo por padrão

                System.out.println("===== DEBUG PRODUTO =====");
                System.out.println("Categoria ID recebida: " + dto.categoriaId());
                System.out.println("Nome: " + dto.nome());
                System.out.println("=========================");

                if (dto.categoriaId() != null) {
                    Categoria categoria = categoriaRepository.findById(dto.categoriaId())
                            .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));
                    produto.setCategoria(categoria);
                }

                if (imagem != null && !imagem.isEmpty()) {
                    produto.setImagemUrl(produtoService.salvarFoto(imagem));
                }

                return ResponseEntity.status(HttpStatus.CREATED).body(produtoRepository.save(produto));
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.badRequest().body("Erro ao salvar produto: " + e.getMessage());
            }
        }

        // 2. ALTERAR PRODUTO NORMAL
        @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<?> update(
                @PathVariable Long id,
                @RequestPart("produto") ProdutoDTO dto,
                @RequestPart(value = "imagem", required = false) MultipartFile imagem) {
            try {
                Produto produtoAtualizado = produtoService.alterarProduto(id, dto, imagem);
                return ResponseEntity.ok(produtoAtualizado);
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Erro ao atualizar: " + e.getMessage()));
            }
        }

        // 3. BUSCAR POR LOJA / CONFEITEIRO (Mude para chamar o método de produtos comuns)
        @GetMapping("/store/{id}")
        public ResponseEntity<?> getByStore(@PathVariable Long id) {
            List<Produto> produtos = produtoRepository.findProdutosComunsByConfeiteiroId(id);

            for (Produto p : produtos) {
                System.out.println("================================");
                System.out.println("Produto: " + p.getNome());

                if (p.getCategoria() != null) {
                    System.out.println("Categoria ID: " + p.getCategoria().getId());
                    System.out.println("Categoria Desc: " + p.getCategoria().getDescricao());
                } else {
                    System.out.println("CATEGORIA NULA");
                }
            }

            return ResponseEntity.ok(produtos);
        }

        // 4. DESATIVAR PRODUTO
        @PutMapping("/{id}/desativar")
        public ResponseEntity<?> desativar(@PathVariable Long id) {
            try {
                produtoService.desativarProduto(id);
                return ResponseEntity.noContent().build();
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            }
        }

        // 5. EXCLUIR PRODUTO NORMAL
        @DeleteMapping("/{id}")
        public ResponseEntity<?> delete(@PathVariable Long id) {
            try {
                produtoService.excluirFisicamente(id);
                return ResponseEntity.noContent().build();
            } catch (IllegalStateException e) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
            }
        }

        // 5.1 EXCLUIR PRODUTO KIT (Rota nova que o front-end chama)
        @DeleteMapping("/kit/{id}")
        public ResponseEntity<?> deleteKit(@PathVariable Long id) {
            try {
                produtoService.excluirKitFisicamente(id);
                return ResponseEntity.noContent().build();
            } catch (IllegalStateException e) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
            }
        }

        // 6. LISTAR KITS DO CONFEITEIRO (Mude para chamar o método de kits)
        @GetMapping("/kit/confeiteiro/{id}")
        public ResponseEntity<?> getKitsByConfeiteiro(@PathVariable Long id) {
            try {
                List<Produto> kits = produtoRepository.findKitsByConfeiteiroId(id);
                return ResponseEntity.ok(kits);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Erro ao buscar kits: " + e.getMessage());
            }
        }

        // 7. CADASTRAR KIT
        @PostMapping(value = "/kit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<?> criarKit(
                @RequestPart("kit") KitRequestDTO kitDTO,
                @RequestPart(value = "imagem", required = false) MultipartFile imagem) {

            try {
                Produto kit = produtoService.cadastrarKit(kitDTO);

                if (imagem != null && !imagem.isEmpty()) {
                    String nomeImagem = produtoService.salvarFoto(imagem);
                    kit.setImagemUrl(nomeImagem);
                    kit = produtoRepository.save(kit);
                }

                return ResponseEntity.status(HttpStatus.CREATED).body(kit);

            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Erro interno ao processar o kit: " + e.getMessage()));
            }
        }



// 👇 COLE O NOVO MÉTODO AQUI

        @PutMapping(value = "/kit/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<?> alterarKit(
                @PathVariable Long id,
                @RequestPart("kit") KitRequestDTO kitDTO,
                @RequestPart(value = "imagem", required = false) MultipartFile imagem) {

            try {

                Produto kitAtualizado =
                        produtoService.alterarKit(id, kitDTO, imagem);

                return ResponseEntity.ok(kitAtualizado);

            } catch (Exception e) {

                e.printStackTrace();

                return ResponseEntity.badRequest()
                        .body(Map.of("error", e.getMessage()));
            }
        }

        // 8. VITRINE DO CLIENTE
        @GetMapping
        public ResponseEntity<List<Produto>> listarVitrineGeralCliente() {

        }
    }

    // 8. VITRINE DO CLIENTE
    @GetMapping
    public ResponseEntity<List<Produto>> listarVitrineGeralCliente() {
        List<Produto> produtos = produtoRepository.findAll();
        return ResponseEntity.ok(produtos);
    }


    @PutMapping(value = "/kit/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> alterarKit(
            @PathVariable Long id,
            @RequestPart("kit") KitRequestDTO kitDTO,
            @RequestPart(value = "imagem", required = false) MultipartFile imagem) {

        try {

            Produto kitAtualizado =
                    produtoService.alterarKit(id, kitDTO, imagem);

            return ResponseEntity.ok(kitAtualizado);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

}