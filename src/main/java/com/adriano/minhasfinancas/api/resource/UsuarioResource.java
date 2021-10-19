package com.adriano.minhasfinancas.api.resource;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.adriano.minhasfinancas.dto.UsuarioDTO;
import com.adriano.minhasfinancas.exception.ErroAutenticacao;
import com.adriano.minhasfinancas.exception.RegraNegocioException;
import com.adriano.minhasfinancas.model.entity.Usuario;
import com.adriano.minhasfinancas.service.LancamentoService;
import com.adriano.minhasfinancas.service.UsuarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(UsuarioResource.class);
	
	private final UsuarioService service;
	private final LancamentoService lancamentoService;

	@PostMapping("/autenticar")
	public ResponseEntity autenticar(@RequestBody UsuarioDTO dto) {
		
		LOGGER.info("#### Método: UsuarioResource.autenticar(), status: INICIO, nomeUsuario: "+ dto.getNome());
		
		try {
			Usuario usuarioAutenticado = service.autenticar(dto.getEmail(), dto.getSenha());
			
			LOGGER.info("#### Método: UsuarioResource.autenticar(), status: SUCESSO, nomeUsuarioAutenticado: "+ usuarioAutenticado.getNome());
			
			return ResponseEntity.ok(usuarioAutenticado);
		} catch (ErroAutenticacao e) {
			
			LOGGER.info("#### Método: UsuarioResource.autenticar(), status: ERROR, mensagemError: "+ e.getMessage());
			
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PostMapping
	public ResponseEntity salvar(@RequestBody UsuarioDTO dto) {
		
		LOGGER.info("#### Método: UsuarioResource.salvar(), status: INICIO, nomeUsuario: "+ dto.getNome());
		
		Usuario usuario = Usuario.builder().nome(dto.getNome()).email(dto.getEmail()).senha(dto.getSenha()).build();
		try {
			Usuario usuarioSalvo = service.salvarUsuario(usuario);
			
			LOGGER.info("#### Método: UsuarioResource.salvar(), status: SUCESSO, nomeUsuarioSalvo: "+ usuarioSalvo.getNome());
			
			return new ResponseEntity(usuarioSalvo, HttpStatus.CREATED);
		} catch (RegraNegocioException e) {
			
			LOGGER.info("#### Método: UsuarioResource.salvar(), status: ERROR, mensagemError: "+ e.getMessage());
			
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping("{id}/saldo")
	public ResponseEntity obterSaldo(@PathVariable("id") Long id) {
		
		LOGGER.info("#### Método: UsuarioResource.obterSaldo(), status: INICIO, idUsuario: "+ id);
		
		Optional<Usuario> usuario = service.obterPorId(id);

		if (!usuario.isPresent()) {
			
			LOGGER.info("#### Método: UsuarioResource.obterSaldo(), status: WARNING, mensagemWarning: usuário não encontrado para o id "+id);
			return new ResponseEntity(HttpStatus.NOT_FOUND);
		}

		BigDecimal saldo = lancamentoService.obterSaldoPorUsuario(id);
		
		LOGGER.info("#### Método: UsuarioResource.obterSaldo(), status: SUCESSO, saldo: "+ saldo);
		
		return ResponseEntity.ok(saldo);
	}

}
