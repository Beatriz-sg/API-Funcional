package com.app.confeitaria.docelivery.controller;

import com.app.confeitaria.docelivery.dto.PedidoDTO;
import com.app.confeitaria.docelivery.model.entity.Pedido;
import com.app.confeitaria.docelivery.service.PedidoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5175"})
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /**
     * CLIENTE: Realiza um novo pedido.
     * Retorna o Pedido criado.
     */
    @PostMapping
    public ResponseEntity<Pedido> create(@RequestBody Pedido pedido) {
        Pedido novoPedido = pedidoService.realizarPedido(pedido);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoPedido);
    }

    /**
     * CONFEITEIRO: Lista pedidos ativos (Fila de Trabalho).
     * Inclui NOVO, AGENDADO, PREPARANDO e SAIU_PARA_ENTREGA.
     */
    @GetMapping("/confeiteiro/{id}/fila")
    public ResponseEntity<List<PedidoDTO>> getFilaTrabalho(@PathVariable Long id) {
        List<String> statusAtivos = Arrays.asList("NOVO", "AGENDADO", "PREPARANDO", "SAIU_PARA_ENTREGA");
        List<Pedido> pedidos = pedidoService.buscarFilaConfeiteiro(id, statusAtivos);

        List<PedidoDTO> dtos = pedidos.stream()
                .map(pedido -> pedidoService.converterParaDTO(pedido))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Detalhes de um pedido específico por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Pedido> getById(@PathVariable Long id) {
        Pedido pedido = pedidoService.buscarPorId(id);
        return ResponseEntity.ok(pedido);
    }

    /**
     * CONFEITEIRO: Atualiza o status do pedido (Ex: NOVO -> PREPARANDO).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Pedido> atualizarStatus(
            @PathVariable Long id,
            @RequestParam String novoStatus) {

        Pedido atualizado = pedidoService.atualizarStatusViaString(id, novoStatus);
        return ResponseEntity.ok(atualizado);
    }

    /**
     * CONFEITEIRO: Cadastra manualmente um pedido/encomenda recebido por fora (Ex: WhatsApp, Balcão).
     */
    @PostMapping("/confeiteiro/{confeiteiroId}")
    public ResponseEntity<Pedido> criarPedidoManualmente(
            @PathVariable Long confeiteiroId,
            @RequestBody Pedido pedido) {

        com.app.confeitaria.docelivery.model.entity.Confeiteiro confeiteiroDono = new com.app.confeitaria.docelivery.model.entity.Confeiteiro();
        confeiteiroDono.setId(confeiteiroId);

        pedido.setConfeiteiro(confeiteiroDono);

        Pedido novoPedido = pedidoService.realizarPedido(pedido);

        return ResponseEntity.status(HttpStatus.CREATED).body(novoPedido);
    }

    /**
     * CONFEITEIRO: Histórico completo de todos os pedidos (todos os status).
     * Usado pelo painel web e pelo orderService.getTodosPedidos().
     */
    @GetMapping("/confeiteiro/{id}/historico")
    public ResponseEntity<List<PedidoDTO>> getHistoricoConfeiteiro(@PathVariable Long id) {
        List<Pedido> pedidos = pedidoService.buscarTodosPedidosConfeiteiro(id);
        List<PedidoDTO> dtos = pedidos.stream()
                .map(pedido -> pedidoService.converterParaDTO(pedido))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * CLIENTE: Lista o histórico de pedidos que aquele cliente específico fez.
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<PedidoDTO>> getHistoricoCliente(@PathVariable Long clienteId) {        List<Pedido> pedidos = pedidoService.buscarPedidosPorCliente(clienteId);

        List<PedidoDTO> dtos = pedidos.stream()
                .map(pedido -> {
                    try {
                        return pedidoService.converterParaDTO(pedido);
                    } catch (org.hibernate.LazyInitializationException lie) {
                        System.err.println("=== LazyInitializationException em converterParaDTO ===");
                        System.err.println("Pedido ID : " + pedido.getId());
                        System.err.println("Mensagem  : " + lie.getMessage());
                        // Imprime o stack trace completo — a linha exata dentro de converterParaDTO aparece aqui
                        lie.printStackTrace(System.err);
                        System.err.println("======================================================");
                        throw lie; // relança para não suprimir o erro
                    }
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}