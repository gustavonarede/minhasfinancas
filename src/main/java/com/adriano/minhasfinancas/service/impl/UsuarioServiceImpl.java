package com.adriano.minhasfinancas.service.impl;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adriano.minhasfinancas.exception.ErroAutenticacao;
import com.adriano.minhasfinancas.exception.RegraNegocioException;
import com.adriano.minhasfinancas.model.entity.Usuario;
import com.adriano.minhasfinancas.model.repository.UsuarioRepository;
import com.adriano.minhasfinancas.service.UsuarioService;

@Service
public class UsuarioServiceImpl implements UsuarioService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LancamentoServiceImpl.class);

	private UsuarioRepository repository;
	
	@Autowired
	public UsuarioServiceImpl(UsuarioRepository repository) {
		super();
		this.repository = repository;
	}
	
	
	@Override
	public Usuario autenticar(String email, String senha) {
		
		LOGGER.info("#### Método: UsuarioServiceImpl.autenticar(), status: INICIO");
		
		Optional<Usuario> usuario = repository.findByEmail(email);
		
		if(!usuario.isPresent()) {
			throw new ErroAutenticacao("Usuario não encontrado");
		}
		if(!usuario.get().getSenha().equals(senha)){
			throw new ErroAutenticacao("Senha inválida");
		}
		return usuario.get();
	}

	@Override
	@Transactional
	public Usuario salvarUsuario(Usuario usuario) {
		
		LOGGER.info("#### Método: UsuarioServiceImpl.salvarUsuario(), status: INICIO, nomeNovoUsuario: "+ usuario.getNome());
		
		validarEmail(usuario.getEmail());
		return repository.save(usuario);
	}

	@Override
	public void validarEmail(String email) {
		
		LOGGER.info("#### Método: UsuarioServiceImpl.validarEmail(), status: INICIO, email: "+ email);
		
		boolean existe = repository.existsByEmail(email);
		if(existe) {
			throw new RegraNegocioException("Já existe um usuario cadastrado com este email");
		}
		
	}


	@Override
	public Optional<Usuario> obterPorId(Long id) {
		
		LOGGER.info("#### Método: UsuarioServiceImpl.obterPorId(), status: INICIO, id: "+ id);
		
		return repository.findById(id);
	}


	
}
