package com.app.confeitaria.docelivery.controller;

import com.app.confeitaria.docelivery.dto.ConfeiteiroDTO; // Import do seu novo ConfeiteiroDTO
import com.app.confeitaria.docelivery.dto.LojaDTO; // Import do seu novo DTO
import com.app.confeitaria.docelivery.model.entity.Confeiteiro;
import com.app.confeitaria.docelivery.model.repository.ConfeiteiroRepository;
import com.app.confeitaria.docelivery.model.repository.UsuarioRepository;
import com.app.confeitaria.docelivery.service.ConfeiteiroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/confeiteiro")
@CrossOrigin("*")
public class ConfeiteiroController {

    @Autowired
    private ConfeiteiroRepository repository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ConfeiteiroService service;

    @Autowired
    private ConfeiteiroService confeiteiroService; // Injete o service caso ainda não esteja injetado

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id) {
        try {
            // AJUSTADO: Agora chama o método do Service que retorna o DTO estruturado com a Loja interna
            ConfeiteiroDTO confeiteiroDTO = confeiteiroService.buscarConfeiteiroPorId(id);
            return ResponseEntity.ok(confeiteiroDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }


    @PutMapping(value = "/atualizar/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> atualizarConfeiteiro(@PathVariable Long id, @ModelAttribute Confeiteiro dadosAtualizados) {
        try {
            Confeiteiro salvo = service.atualizarConfeiteiro(id, dadosAtualizados);
            return ResponseEntity.ok(salvo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao atualizar perfil: " + e.getMessage());
        }
    }

    /**
     * NOVO ENDPOINT ATUALIZADO (PASSO 2)
     * Mapeia exatamente a URL: http://localhost:8080/api/confeiteiro/loja/atualizar/{id}
     * que o seu front-end do React está chamando.
     */


    @PutMapping(
            value = "/loja/atualizar/{idLoja}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> atualizarPerfilLoja(
            @PathVariable Long idLoja,
            @RequestPart("dados") LojaDTO lojaDTO,
            @RequestPart(value = "imagem", required = false) MultipartFile imagem) {

        try {
            System.out.println("Recebendo requisição PUT em /loja/atualizar/" + idLoja);
            System.out.println("Payload recebido: Nome Fantasia -> " + lojaDTO.getNomeFantasia());
            if (imagem != null) {
                System.out.println("Imagem recebida: " + imagem.getOriginalFilename());
            }

            // AJUSTADO: Passamos o idLoja para o service tratar a busca corretamente pela Loja
            com.app.confeitaria.docelivery.model.entity.Confeiteiro confeiteiroAtualizado =
                    service.atualizarPerfilLoja(
                            idLoja,
                            lojaDTO,
                            imagem
                    );

            // Retorna o objeto atualizado completo para o React
            return ResponseEntity.ok(confeiteiroAtualizado);

        } catch (RuntimeException e) {
            System.err.println("Erro de regra de negócio: " + e.getMessage());
            // Retorna o JSON estruturado para o Axios capturar no front-end
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\": \"Erro interno no servidor ao salvar no banco: " + e.getMessage() + "\"}");
        }
    }


    @GetMapping("/profile")
    public ResponseEntity<?> obterPerfilPorEmail(@RequestParam String email) {
        try {
            Object perfil = service.obterPerfilPorEmail(email);
            return ResponseEntity.ok(perfil);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/public/lojas")
    public ResponseEntity<List<Confeiteiro>> listarLojas() {
        return ResponseEntity.ok(repository.findAll());
    }
}