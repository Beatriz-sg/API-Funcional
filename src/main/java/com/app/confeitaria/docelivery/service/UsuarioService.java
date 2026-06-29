package com.app.confeitaria.docelivery.service;

import com.app.confeitaria.docelivery.model.entity.Confeiteiro;
import com.app.confeitaria.docelivery.model.entity.Loja;
import com.app.confeitaria.docelivery.model.entity.Usuario;
import com.app.confeitaria.docelivery.model.repository.ConfeiteiroRepository;
import com.app.confeitaria.docelivery.model.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder; // Adicione isso!
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ConfeiteiroRepository confeiteiroRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Essencial para comparar senhas criptografadas

    // CORREÇÃO DEFINITIVA:
    public Optional<Usuario> realizarLogin(String email, String senha) {
        // Como o repository já retorna Optional<Usuario>, chamamos direto:
        return usuarioRepository.findByEmail(email)
                .filter(user -> passwordEncoder.matches(senha, user.getSenha()));
    }

    @Transactional
    public Confeiteiro salvarConfeiteiro(Confeiteiro confeiteiro) {
        if (!com.app.confeitaria.docelivery.util.CpfValidator.isValid(confeiteiro.getCpf())) {
            throw new IllegalArgumentException("CPF inválido.");
        }
        if (usuarioRepository.existsByCpf(confeiteiro.getCpf())) {
            throw new RuntimeException("CPF já cadastrado no sistema!");
        }

        // Criptografa a senha antes de salvar
        confeiteiro.setSenha(passwordEncoder.encode(confeiteiro.getSenha()));

        // 🟢 ADICIONE ESTE BLOCO AQUI:
        // Garante que o objeto Loja conheça o Confeiteiro pai antes de ir para o banco
        if (confeiteiro.getLoja() != null) {
            Loja loja = confeiteiro.getLoja();
            loja.setConfeiteiro(confeiteiro); // Vincula o Confeiteiro dentro da Loja em memória
        }

        // Agora, ao salvar o Confeiteiro, o CascadeType.ALL vai propagar para a loja
        // contendo o ID do confeiteiro preenchido perfeitamente!
        return confeiteiroRepository.save(confeiteiro);
    }

    public List<Confeiteiro> listarLojasPorCidade(String cidade) {
        return confeiteiroRepository.findByCidadeIgnoreCase(cidade);
    }
}