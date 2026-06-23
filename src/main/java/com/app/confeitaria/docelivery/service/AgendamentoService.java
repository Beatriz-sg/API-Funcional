package com.app.confeitaria.docelivery.service;

import com.app.confeitaria.docelivery.dto.PedidoDTO;
import com.app.confeitaria.docelivery.model.entity.Pedido;
import com.app.confeitaria.docelivery.model.enums.StatusPedido;
import com.app.confeitaria.docelivery.model.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgendamentoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Se o seu método de conversão no PedidoService for privado, você pode injetar o PedidoService
    // ou fazer uma conversão manual aqui. Assumindo que você possa usar o service de pedidos:
    @Autowired
    private PedidoService pedidoService;

    /**
     * Este método roda automaticamente.
     * fixedRate = 3600000 significa que ele roda a cada 1 hora (3.600.000 milissegundos).
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void verificarEAtivarPedidosAgendados() {
        // DEFINIÇÃO DA ANTECEDÊNCIA: Mudamos para o painel do confeiteiro 3 dias antes da entrega
        int diasDeAntecedencia = 3;
        LocalDateTime dataLimite = LocalDateTime.now().plusDays(diasDeAntecedencia);

        // 1. Busca todos os pedidos que estão AGENDADOS e que a data de entrega está dentro da janela de 3 dias
        List<Pedido> pedidosProntosParaProducao = pedidoRepository
                .findByStatusAndDataEntregaAgendadaBefore(StatusPedido.AGENDADO, dataLimite);

        for (Pedido pedido : pedidosProntosParaProducao) {
            // 2. Transforma o status de AGENDADO para NOVO (Entra na fila de trabalho)
            pedido.setStatus(StatusPedido.NOVO);
            Pedido pedidoAtualizado = pedidoRepository.save(pedido);

            // 3. Notifica o Confeiteiro em tempo real pelo WebSocket para atualizar a tela dele no React
            if (pedidoAtualizado.getConfeiteiro() != null) {
                // Aqui usamos o método de envio seguro que estruturamos ontem
                // Certifique-se de que o método 'converterParaDTO' no seu PedidoService esteja como 'public'
                PedidoDTO dto = pedidoService.converterParaDTO(pedidoAtualizado);

                String destino = "/topico/confeiteiro/" + pedidoAtualizado.getConfeiteiro().getId() + "/pedidos";
                messagingTemplate.convertAndSend(destino, dto);
            }
        }
    }
}