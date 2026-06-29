package com.app.confeitaria.docelivery.controller;

import com.app.confeitaria.docelivery.dto.CadastroEntregadorDTO;
import com.app.confeitaria.docelivery.model.entity.Entregador;
import com.app.confeitaria.docelivery.model.repository.EntregadorRepository;
import com.app.confeitaria.docelivery.model.repository.UsuarioRepository;
import com.app.confeitaria.docelivery.service.ProdutoService;
import com.app.confeitaria.docelivery.service.TokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.app.confeitaria.docelivery.dto.PedidoDTO;
import com.app.confeitaria.docelivery.dto.ItemPedidoDTO;
import com.app.confeitaria.docelivery.model.enums.StatusPedido;
import com.app.confeitaria.docelivery.model.repository.PedidoRepository;
import com.app.confeitaria.docelivery.service.PedidoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoints exclusivos do Mobile Delivery App.
 *
 * POST /api/entregadores/login           — login (permitAll)
 * POST /api/entregadores                 — cadastro (permitAll)
 * GET  /api/entregadores/perfil          — perfil autenticado
 * POST /api/entregadores/perfil/foto     — upload de foto de perfil
 */
@RestController
@RequestMapping("/api/entregadores")
@CrossOrigin(origins = "*")
public class EntregadorAuthController {

    private final UsuarioRepository    usuarioRepository;
    private final EntregadorRepository entregadorRepository;
    private final PasswordEncoder      passwordEncoder;
    private final TokenService         tokenService;
    private final ProdutoService       produtoService;
    private final com.app.confeitaria.docelivery.model.repository.PedidoRepository pedidoRepository;
    private final com.app.confeitaria.docelivery.service.PedidoService             pedidoService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public EntregadorAuthController(
            UsuarioRepository    usuarioRepository,
            EntregadorRepository entregadorRepository,
            PasswordEncoder      passwordEncoder,
            TokenService         tokenService,
            ProdutoService       produtoService,
            com.app.confeitaria.docelivery.model.repository.PedidoRepository pedidoRepository,
            com.app.confeitaria.docelivery.service.PedidoService             pedidoService) {
        this.usuarioRepository    = usuarioRepository;
        this.entregadorRepository = entregadorRepository;
        this.passwordEncoder      = passwordEncoder;
        this.tokenService         = tokenService;
        this.produtoService       = produtoService;
        this.pedidoRepository     = pedidoRepository;
        this.pedidoService        = pedidoService;
    }

    // -----------------------------------------------------------------------
    // POST /api/entregadores/login
    // -----------------------------------------------------------------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String senha = body.get("senha");

        if (email == null || senha == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "E-mail e senha sao obrigatorios."));
        }

        return usuarioRepository.findByEmail(email).map(user -> {

            if (Boolean.FALSE.equals(user.getCodStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Esta conta esta desativada."));
            }

            if (!passwordEncoder.matches(senha, user.getSenha())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "E-mail ou senha incorretos."));
            }

            String token = tokenService.generateToken(user);

            Map<String, Object> resp = new HashMap<>();
            resp.put("id",         user.getId());
            resp.put("nome",       user.getNome());
            resp.put("email",      user.getEmail());
            resp.put("cpf",        user.getCpf());
            resp.put("telefone",   user.getTelefone());
            resp.put("fotoPerfil", user.getFotoPerfil());
            resp.put("perfil",     user.getTipoUsuario() != null
                                       ? user.getTipoUsuario().name()
                                       : "ENTREGADOR");
            resp.put("token",      token);

            return ResponseEntity.ok(resp);

        }).orElse(
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Usuario nao encontrado."))
        );
    }

    // -----------------------------------------------------------------------
    // POST /api/entregadores
    // -----------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody CadastroEntregadorDTO dto) {
        try {
            if (usuarioRepository.findByEmail(dto.email()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "E-mail ja cadastrado."));
            }

            Entregador e = new Entregador();
            e.setNome(dto.nome());
            e.setEmail(dto.email());
            e.setSenha(passwordEncoder.encode(dto.senha()));
            e.setCpf(dto.cpf());
            e.setTelefone(dto.telefone());
            e.setCep(dto.cep());

            String enderecoCompleto = dto.logradouro() != null ? dto.logradouro() : "";
            if (dto.numero() != null && !dto.numero().isBlank())
                enderecoCompleto += ", " + dto.numero();
            if (dto.complemento() != null && !dto.complemento().isBlank())
                enderecoCompleto += " - " + dto.complemento();
            e.setEndereco(enderecoCompleto);

            e.setBairro(dto.bairro());
            e.setCidade(dto.cidade());
            e.setUf(dto.estado());

            if (dto.dataNascimento() != null && !dto.dataNascimento().isBlank())
                e.setDataNascimento(LocalDate.parse(dto.dataNascimento()));

            e.setCodStatus(true);

            String veiculo = dto.tipoVeiculo() != null ? dto.tipoVeiculo() : "";
            if (dto.modeloVeiculo() != null && !dto.modeloVeiculo().isBlank())
                veiculo = veiculo.isBlank() ? dto.modeloVeiculo() : veiculo + " - " + dto.modeloVeiculo();
            e.setVeiculo(veiculo);
            e.setPlacaVeiculo(dto.placaVeiculo());

            String cnh = dto.numeroCnh() != null ? dto.numeroCnh() : "";
            if (dto.categoriaCnh() != null && !dto.categoriaCnh().isBlank())
                cnh += " / Cat." + dto.categoriaCnh();
            if (dto.validadeCnh() != null && !dto.validadeCnh().isBlank())
                cnh += " / Val." + dto.validadeCnh();
            e.setCnh(cnh);

            Entregador salvo = entregadorRepository.save(e);

            Map<String, Object> resp = new HashMap<>();
            resp.put("id",    salvo.getId());
            resp.put("nome",  salvo.getNome());
            resp.put("email", salvo.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);

        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "CPF ou e-mail ja cadastrado."));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erro ao cadastrar entregador: " + ex.getMessage()));
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/entregadores/perfil   — requer Bearer token
    // -----------------------------------------------------------------------
    @GetMapping("/perfil")
    public ResponseEntity<?> perfil(
            @AuthenticationPrincipal com.app.confeitaria.docelivery.model.entity.Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Token invalido ou ausente."));
        }

        return entregadorRepository.findById(usuario.getId()).map(entregador -> {
            Map<String, Object> resp = new HashMap<>();
            resp.put("id",          entregador.getId());
            resp.put("nome",        entregador.getNome());
            resp.put("email",       entregador.getEmail());
            resp.put("cpf",         entregador.getCpf());
            resp.put("telefone",    entregador.getTelefone());
            resp.put("cep",         entregador.getCep());
            resp.put("endereco",    entregador.getEndereco());
            resp.put("bairro",      entregador.getBairro());
            resp.put("cidade",      entregador.getCidade());
            resp.put("uf",          entregador.getUf());
            resp.put("veiculo",     entregador.getVeiculo());
            resp.put("placaVeiculo",entregador.getPlacaVeiculo());
            resp.put("cnh",         entregador.getCnh());
            resp.put("perfil",      entregador.getTipoUsuario() != null
                                        ? entregador.getTipoUsuario().name()
                                        : "ENTREGADOR");
            // fotoPerfil: path relativo, ex: "/uploads/usuarios/foto.jpg"
            // null quando nao há foto cadastrada
            resp.put("fotoPerfil",  entregador.getFotoPerfil());
            return ResponseEntity.ok(resp);
        }).orElse(
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Entregador nao encontrado."))
        );
    }

    // -----------------------------------------------------------------------
    // POST /api/entregadores/perfil/foto   — requer Bearer token
    //
    // Salva a imagem em C:/docelivery-storage/usuarios/<timestamp>_<nome>
    // Persiste o path relativo "/uploads/usuarios/<filename>" em Usuario.fotoPerfil
    // Retorna: { "fotoPerfil": "/uploads/usuarios/<filename>" }
    // -----------------------------------------------------------------------
    @PostMapping(value = "/perfil/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFoto(
            @AuthenticationPrincipal com.app.confeitaria.docelivery.model.entity.Usuario usuario,
            @RequestParam("foto") MultipartFile foto) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Token invalido ou ausente."));
        }
        if (foto == null || foto.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Nenhum arquivo enviado."));
        }

        try {
            // Salva o arquivo na subpasta "usuarios/" dentro do uploadDir
            // Reutiliza o mesmo mecanismo de ProdutoService
            String subfolder = "usuarios";
            String nomeArquivo = System.currentTimeMillis() + "_perfil_" +
                    foto.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");

            java.nio.file.Path destDir = java.nio.file.Paths.get(uploadDir, subfolder)
                    .toAbsolutePath().normalize();
            if (!java.nio.file.Files.exists(destDir)) {
                java.nio.file.Files.createDirectories(destDir);
            }
            java.nio.file.Files.copy(foto.getInputStream(), destDir.resolve(nomeArquivo),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Path relativo que o Mobile usara para compor a URL completa
            String fotoPerfilPath = "/uploads/usuarios/" + nomeArquivo;

            // Persiste no banco
            usuario.setFotoPerfil(fotoPerfilPath);
            usuarioRepository.save(usuario);

            return ResponseEntity.ok(Map.of("fotoPerfil", fotoPerfilPath));

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erro ao salvar foto: " + ex.getMessage()));
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/entregas/disponiveis
    //
    // Retorna todos os pedidos com status SAIU_PARA_ENTREGA.
    // Requer Bearer token valido (ROLE_ENTREGADOR ou ROLE_MASTER).
    // Rota declarada em SecurityConfig como authenticated().
    // -----------------------------------------------------------------------
    @GetMapping("/disponiveis")
    public ResponseEntity<?> entregasDisponiveis() {
        try {
            List<com.app.confeitaria.docelivery.model.entity.Pedido> pedidos =
                    pedidoRepository.findByStatusWithItens(StatusPedido.SAIU_PARA_ENTREGA);

            List<PedidoDTO> dtos = pedidos.stream()
                    .map(p -> pedidoService.converterParaDTO(p))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erro ao buscar entregas: " + ex.getMessage()));
        }
    }

    // -----------------------------------------------------------------------
    // POST /api/entregadores/pedidos/{pedidoId}/aceitar
    //
    // O entregador autenticado aceita um pedido disponivel.
    //   1. Busca o pedido pelo ID
    //   2. Associa o entregador autenticado (identidade via JWT)
    //   3. Muda o status para ENTREGUE (pedido saiu para entrega e foi aceito)
    //   4. Persiste e retorna PedidoDTO via PedidoService.atualizarStatus()
    //      (reutiliza logica existente: financeiro + WebSocket)
    // -----------------------------------------------------------------------
    @org.springframework.web.bind.annotation.PostMapping("/pedidos/{pedidoId}/aceitar")
    public ResponseEntity<?> aceitarEntrega(
            @org.springframework.web.bind.annotation.PathVariable Long pedidoId,
            @AuthenticationPrincipal com.app.confeitaria.docelivery.model.entity.Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Token invalido ou ausente."));
        }

        try {
            // Busca o pedido
            com.app.confeitaria.docelivery.model.entity.Pedido pedido =
                    pedidoRepository.findById(pedidoId)
                            .orElseThrow(() -> new RuntimeException("Pedido nao encontrado."));

            // Associa o entregador autenticado ao pedido
            com.app.confeitaria.docelivery.model.entity.Entregador entregador =
                    entregadorRepository.findById(usuario.getId())
                            .orElseThrow(() -> new RuntimeException("Entregador nao encontrado."));

            // Associa o entregador — status permanece SAIU_PARA_ENTREGA
            pedido.setEntregador(entregador);
            com.app.confeitaria.docelivery.model.entity.Pedido pedidoSalvo = pedidoRepository.save(pedido);

            // Retorna o DTO sem alterar o status
            com.app.confeitaria.docelivery.dto.PedidoDTO dto = pedidoService.converterParaDTO(pedidoSalvo);
            return ResponseEntity.ok(dto);

        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erro ao aceitar entrega: " + ex.getMessage()));
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/entregadores/pedidos/minhas
    //
    // Retorna os pedidos aceitos pelo entregador autenticado
    // com status SAIU_PARA_ENTREGA (em rota, ainda nao finalizados).
    // -----------------------------------------------------------------------
    @org.springframework.web.bind.annotation.GetMapping("/pedidos/minhas")
    public ResponseEntity<?> minhasEntregas(
            @AuthenticationPrincipal com.app.confeitaria.docelivery.model.entity.Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Token invalido ou ausente."));
        }

        try {
            List<com.app.confeitaria.docelivery.model.entity.Pedido> pedidos =
                    pedidoRepository.findByEntregadorIdAndStatusWithItens(
                            usuario.getId(), StatusPedido.SAIU_PARA_ENTREGA);

            List<com.app.confeitaria.docelivery.dto.PedidoDTO> dtos = pedidos.stream()
                    .map(p -> pedidoService.converterParaDTO(p))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erro ao buscar suas entregas: " + ex.getMessage()));
        }
    }

    // -----------------------------------------------------------------------
    // POST /api/entregadores/pedidos/{pedidoId}/finalizar
    //
    // Entregador finaliza a entrega:
    //   - Muda status para ENTREGUE
    //   - Reutiliza PedidoService.atualizarStatus() (financeiro + WebSocket)
    //   - Notifica Cliente e Confeiteiro via WebSocket
    // -----------------------------------------------------------------------
    @org.springframework.web.bind.annotation.PostMapping("/pedidos/{pedidoId}/finalizar")
    public ResponseEntity<?> finalizarEntrega(
            @org.springframework.web.bind.annotation.PathVariable Long pedidoId,
            @AuthenticationPrincipal com.app.confeitaria.docelivery.model.entity.Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Token invalido ou ausente."));
        }

        try {
            com.app.confeitaria.docelivery.dto.PedidoDTO dto =
                    pedidoService.atualizarStatus(pedidoId, StatusPedido.ENTREGUE);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erro ao finalizar entrega: " + ex.getMessage()));
        }
    }
}