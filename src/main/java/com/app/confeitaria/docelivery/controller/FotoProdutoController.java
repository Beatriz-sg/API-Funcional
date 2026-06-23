package com.app.confeitaria.docelivery.controller;

import com.app.confeitaria.docelivery.model.entity.FotoProduto;
import com.app.confeitaria.docelivery.model.repository.FotoProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/fotos")
@CrossOrigin("*") // Permite que seu React acesse o backend sem erro de CORS
public class FotoProdutoController {

    @Autowired
    private FotoProdutoRepository repository;

    // 1. Endpoint para UPLOAD da imagem
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFoto(@RequestParam("file") MultipartFile file) {
        try {
            FotoProduto foto = new FotoProduto();
            foto.setNomeArquivo(file.getOriginalFilename());
            foto.setContentType(file.getContentType());
            foto.setDadosBinarios(file.getBytes()); // Converte o arquivo para byte[]

            repository.save(foto);
            return ResponseEntity.ok("Foto salva com sucesso! ID: " + foto.getId());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Erro ao processar arquivo");
        }
    }

    // 2. Endpoint para BUSCAR/VISUALIZAR a imagem
    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getFoto(@PathVariable int id) {
        return repository.findById(id)
                .map(foto -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(foto.getContentType()))
                        .body(foto.getDadosBinarios()))
                .orElse(ResponseEntity.notFound().build());
    }
}