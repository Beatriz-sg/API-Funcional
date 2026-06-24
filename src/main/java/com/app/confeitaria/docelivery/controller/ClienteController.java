package com.app.confeitaria.docelivery.controller;

import com.app.confeitaria.docelivery.model.entity.Cliente;
import com.app.confeitaria.docelivery.model.entity.Usuario;
import com.app.confeitaria.docelivery.model.repository.ClienteRepository;
import com.app.confeitaria.docelivery.service.ProdutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoService produtoService;

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Converte lista Java → string CSV para persistir na coluna VARCHAR */
    private String listaParaCsv(List<String> lista) {
        if (lista == null || lista.isEmpty()) return null;
        return lista.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }

    /** Converte string CSV → lista Java para retornar ao mobile */
    private List<String> csvParaLista(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /** Monta o DTO de resposta — usado no GET e no PUT */
    private Map<String, Object> toDto(Cliente cliente) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id",             cliente.getId());
        dto.put("nome",           cliente.getNome());
        dto.put("apelido",        cliente.getApelido());
        dto.put("cpf",            cliente.getCpf());
        dto.put("dataNascimento", cliente.getDataNascimento() != null
                ? cliente.getDataNascimento().toString() : null);
        dto.put("email",          cliente.getEmail());
        dto.put("telefone",       cliente.getTelefone());
        dto.put("cep",            cliente.getCep());
        dto.put("logradouro",     cliente.getEndereco());
        dto.put("numero",         cliente.getNumero());
        dto.put("complemento",    cliente.getComplemento());
        dto.put("bairro",         cliente.getBairro());
        dto.put("cidade",         cliente.getCidade());
        dto.put("estado",         cliente.getUf());
        dto.put("fotoPerfil",     cliente.getFotoPerfil());
        dto.put("preferencias",   csvParaLista(cliente.getPreferencias()));
        dto.put("restricoes",     csvParaLista(cliente.getRestricoes()));
        dto.put("role",           "ROLE_CLIENTE");
        dto.put("tipo",           "CLIENTE");
        return dto;
    }

    // ── GET /api/cliente/perfil ───────────────────────────────────────────────

    @GetMapping("/cliente/perfil")
    public ResponseEntity<?> getPerfil(@AuthenticationPrincipal Object principal) {
        if (!(principal instanceof Usuario usuario)) {
            return ResponseEntity.status(401).body(Map.of("error", "Não autenticado."));
        }
        return clienteRepository.findById(usuario.getId())
                .map(c -> ResponseEntity.ok(toDto(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ── PUT /api/cliente/perfil ───────────────────────────────────────────────

    @PutMapping("/cliente/perfil")
    @Transactional
    public ResponseEntity<?> atualizarPerfil(
            @AuthenticationPrincipal Object principal,
            @RequestBody Map<String, Object> dados) {

        if (!(principal instanceof Usuario usuario)) {
            return ResponseEntity.status(401).body(Map.of("error", "Não autenticado."));
        }

        return clienteRepository.findById(usuario.getId()).map(cliente -> {
            if (dados.get("nome") != null)        cliente.setNome((String) dados.get("nome"));
            if (dados.get("apelido") != null)     cliente.setApelido((String) dados.get("apelido"));
            if (dados.get("email") != null)       cliente.setEmail((String) dados.get("email"));
            if (dados.get("telefone") != null)    cliente.setTelefone((String) dados.get("telefone"));
            if (dados.get("cep") != null)         cliente.setCep((String) dados.get("cep"));
            if (dados.get("logradouro") != null)  cliente.setEndereco((String) dados.get("logradouro"));
            if (dados.get("numero") != null)      cliente.setNumero((String) dados.get("numero"));
            if (dados.get("complemento") != null) cliente.setComplemento((String) dados.get("complemento"));
            if (dados.get("bairro") != null)      cliente.setBairro((String) dados.get("bairro"));
            if (dados.get("cidade") != null)      cliente.setCidade((String) dados.get("cidade"));
            if (dados.get("estado") != null)      cliente.setUf((String) dados.get("estado"));
            if (dados.get("dataNascimento") != null) {
                try {
                    cliente.setDataNascimento(
                            java.time.LocalDate.parse((String) dados.get("dataNascimento")));
                } catch (Exception ignored) {}
            }
            if (dados.get("preferencias") != null) {
                @SuppressWarnings("unchecked")
                List<String> lista = (List<String>) dados.get("preferencias");
                cliente.setPreferencias(listaParaCsv(lista));
            }
            if (dados.get("restricoes") != null) {
                @SuppressWarnings("unchecked")
                List<String> lista = (List<String>) dados.get("restricoes");
                cliente.setRestricoes(listaParaCsv(lista));
            }
            // CPF não é atualizado — intencional
            clienteRepository.save(cliente);
            return ResponseEntity.ok(toDto(cliente));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── POST /api/cliente/foto ────────────────────────────────────────────────

    @PostMapping(value = "/cliente/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> uploadFoto(
            @AuthenticationPrincipal Object principal,
            @RequestPart("foto") MultipartFile foto) {

        if (!(principal instanceof Usuario usuario)) {
            return ResponseEntity.status(401).body(Map.of("error", "Não autenticado."));
        }

        return clienteRepository.findById(usuario.getId()).map(cliente -> {
            try {
                String nomeArquivo = produtoService.salvarFoto(foto);
                cliente.setFotoPerfil(nomeArquivo);
                clienteRepository.save(cliente);
                return ResponseEntity.ok(Map.of(
                        "fotoPerfil", nomeArquivo,
                        "fotoUrl",    "/uploads/" + nomeArquivo
                ));
            } catch (Exception e) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Erro ao salvar foto: " + e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    // ── PUT /api/atualizar/{id} — mantido para compatibilidade Web ────────────

    @PutMapping("/atualizar/{id}")
    @Transactional
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Cliente dados) {
        return clienteRepository.findById(id).map(c -> {
            if (dados.getNome() != null)      c.setNome(dados.getNome());
            if (dados.getTelefone() != null)  c.setTelefone(dados.getTelefone());
            if (dados.getEmail() != null)     c.setEmail(dados.getEmail());
            if (dados.getCpf() != null)       c.setCpf(dados.getCpf());
            if (dados.getCep() != null)       c.setCep(dados.getCep());
            if (dados.getEndereco() != null)  c.setEndereco(dados.getEndereco());
            if (dados.getBairro() != null)    c.setBairro(dados.getBairro());
            if (dados.getCidade() != null)    c.setCidade(dados.getCidade());
            if (dados.getUf() != null)        c.setUf(dados.getUf());
            if (dados.getApelido() != null)   c.setApelido(dados.getApelido());
            return ResponseEntity.ok(clienteRepository.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }
}
