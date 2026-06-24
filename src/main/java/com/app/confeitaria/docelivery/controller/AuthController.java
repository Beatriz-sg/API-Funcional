package com.app.confeitaria.docelivery.controller;

import com.app.confeitaria.docelivery.dto.UsuarioResponseDTO;
import com.app.confeitaria.docelivery.model.entity.*;
import com.app.confeitaria.docelivery.model.enums.TipoUsuario;
import com.app.confeitaria.docelivery.model.repository.*;
import com.app.confeitaria.docelivery.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {


    @Autowired
    private final ClienteRepository clienteRepository;


    private final UsuarioRepository usuarioRepository;
    private final ConfeiteiroRepository confeiteiroRepository;
    private final EntregadorRepository entregadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthController(UsuarioRepository usuarioRepository,
                          ClienteRepository clienteRepository,
                          ConfeiteiroRepository confeiteiroRepository,
                          EntregadorRepository entregadorRepository,
                          PasswordEncoder passwordEncoder,
                          TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.confeiteiroRepository = confeiteiroRepository;
        this.entregadorRepository = entregadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping({"/login", "/confeiteiro/login", "/cliente/login", "/entregador/login"})
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String email = loginData.get("email");
        String senha = loginData.get("senha");

        return usuarioRepository.findByEmail(email).map(user -> {
            if (Boolean.FALSE.equals(user.getCodStatus())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Esta conta está desativada."));
            }

            if (!passwordEncoder.matches(senha, user.getSenha())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "E-mail ou senha incorretos."));
            }

            String token = tokenService.generateToken(user);

            // Criamos um mapa para os dados do usuário que vão para o Front
            java.util.Map<String, Object> userData = new java.util.HashMap<>();
            userData.put("id", user.getId());
            userData.put("nome", user.getNome());
            userData.put("email", user.getEmail());
            userData.put("tipo", (user.getTipoUsuario() != null) ? user.getTipoUsuario().name() : "CLIENTE");
            userData.put("telefone", user.getTelefone()); // Para o header/perfil se precisar

            // SE FOR CONFEITEIRO, BUSCA A LOJA PELO REPOSITÓRIO JÁ INJETADO
            if (user.getTipoUsuario() != null && "CONFEITEIRO".equals(user.getTipoUsuario().name())) {
                confeiteiroRepository.findById(user.getId()).ifPresent(confeiteiro -> {
                    if (confeiteiro.getLoja() != null) {
                        userData.put("nomeFantasia", confeiteiro.getLoja().getNomeFantasia());
                        // Nota: se na sua classe Loja o campo da foto chamar getFoto(), altere abaixo
                        userData.put("fotoLoja", confeiteiro.getLoja().getFotoUrl());
                    }
                });
            }

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user", userData
            ));
        }).orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Usuário não encontrado.")));
    }

    @PostMapping("/cliente/cadastro") // Alinhado com o padrão de rota mais comum do seu front
    public ResponseEntity<?> cadastrarCliente(@RequestBody Map<String, Object> dados) {
        try {
            Cliente cliente = new Cliente();

            // Extrai e valida os dados manualmente, evitando nós cegos no Jackson
            cliente.setNome((String) dados.get("nome"));
            cliente.setCpf((String) dados.get("cpf"));
            cliente.setEmail((String) dados.get("email"));
            cliente.setTelefone((String) dados.get("telefone"));
            cliente.setApelido((String) dados.get("apelido"));
            cliente.setCep((String) dados.get("cep"));
            cliente.setEndereco((String) dados.get("endereco"));
            cliente.setBairro((String) dados.get("bairro"));
            cliente.setCidade((String) dados.get("cidade"));
            cliente.setUf((String) dados.get("uf"));

            if (dados.get("dataNascimento") != null) {
                cliente.setDataNascimento(
                        java.time.LocalDate.parse((String) dados.get("dataNascimento"))
                );
            }

            // Criptografa a senha com segurança
            cliente.setSenha(passwordEncoder.encode((String) dados.get("senha")));
            cliente.setCodStatus(true);

            System.out.println("===== DADOS RECEBIDOS =====");
            System.out.println(dados);
            System.out.println("===========================");

            // Salva e retorna o objeto persistido
            Cliente clienteSalvo = clienteRepository.save(cliente);
            return new ResponseEntity<>(clienteSalvo, HttpStatus.CREATED);

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Captura especificamente erros de banco (E-mail ou CPF duplicados)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "E-mail ou CPF já cadastrados no sistema."));
        } catch (Exception e) {
            // Crucial: Imprime o erro no console do IntelliJ para você conseguir debugar rápido
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro interno: " + e.getMessage()));
        }
    }

    // CADASTRO DE CONFEITEIRO (CORRIGIDO E IMPLEMENTADO PASSO 3)
    @PostMapping(value = "/cadastro/confeiteiro", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> cadastrarConfeiteiro(
            @RequestPart("dados") Confeiteiro confeiteiro,
            @RequestPart(value = "foto", required = false) MultipartFile foto) {
        try {
            // 1. Configurações básicas do Usuário/Confeiteiro
            confeiteiro.setCodStatus(true);
            confeiteiro.setSenha(passwordEncoder.encode(confeiteiro.getSenha()));

            // 2. Separamos a loja da cascata automática para controlar o salvamento
            Loja lojaDoFormulario = confeiteiro.getLoja();
            confeiteiro.setLoja(null); // Desvincula temporariamente para não disparar o Cascade problemático

            // 3. PRIMEIRO INSERT: Salva o Confeiteiro puro no banco
            // Isso gera o ID do usuário (ex: 10008) no SQL Server com sucesso
            Confeiteiro confeiteiroSalvo = confeiteiroRepository.save(confeiteiro);

            // 4. SEGUNDO INSERT: Se veio dados da loja, vinculamos e salvamos manualmente
            if (lojaDoFormulario != null) {
                // Amarra o confeiteiro que JÁ TEM ID dentro da loja
                lojaDoFormulario.setConfeiteiro(confeiteiroSalvo);

                // Se você tiver um LojaRepository injetado, use-o aqui.
                // Caso não tenha, vamos salvar injetando a loja de volta no confeiteiro agora que ele tem ID:
                confeiteiroSalvo.setLoja(lojaDoFormulario);
                confeiteiroSalvo = confeiteiroRepository.save(confeiteiroSalvo);
            }

            // 5. Processamento da foto (se houver)
            if (foto != null && !foto.isEmpty()) {
                // seu código de foto...
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(confeiteiroSalvo);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Erro interno no cadastro: " + e.getMessage()));
        }
    }

    // CADASTRO DE ENTREGADOR
    @PostMapping("/cadastro/entregador")
    public ResponseEntity<?> cadastrarEntregador(@RequestBody Entregador entregador) {
        try {
            entregador.setSenha(passwordEncoder.encode(entregador.getSenha()));
            entregador.setCodStatus(true);
            return new ResponseEntity<>(entregadorRepository.save(entregador), HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erro ao cadastrar entregador."));
        }
    }



    @GetMapping("/perfil")
    public ResponseEntity<UsuarioResponseDTO> obterPerfilLogado(@AuthenticationPrincipal Usuario usuarioLogado) {
        String apelido = "";

        // Se o usuário logado for um cliente, buscamos os dados dele usando o email
        if ("CLIENTE".equals(usuarioLogado.getTipoUsuario().name())) {
            apelido = clienteRepository.buscarPorEmailDoUsuario(usuarioLogado.getEmail())
                    .map(Cliente::getApelido)
                    .orElse("");
        }

        UsuarioResponseDTO resposta = new UsuarioResponseDTO(
                usuarioLogado.getId(),
                usuarioLogado.getNome(),
                usuarioLogado.getEmail(),
                apelido,
                usuarioLogado.getTipoUsuario().name(),
                usuarioLogado.getAuthorities()
        );

        return ResponseEntity.ok(resposta);
    }


}