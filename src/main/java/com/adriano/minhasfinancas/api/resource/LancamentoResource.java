package com.adriano.minhasfinancas.api.resource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.adriano.minhasfinancas.dto.AtualizaStatusDTO;
import com.adriano.minhasfinancas.exception.RegraNegocioException;
import com.adriano.minhasfinancas.model.entity.Lancamento;
import com.adriano.minhasfinancas.model.entity.LancamentoDTO;
import com.adriano.minhasfinancas.model.entity.Usuario;
import com.adriano.minhasfinancas.model.enums.StatusLancamento;
import com.adriano.minhasfinancas.model.enums.TipoLancamento;
import com.adriano.minhasfinancas.service.LancamentoService;
import com.adriano.minhasfinancas.service.UsuarioService;

@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoResource {

	private final LancamentoService service;

	private final UsuarioService usuarioService;

	private static final Logger LOGGER = LoggerFactory.getLogger(LancamentoResource.class);

	@GetMapping
	public ResponseEntity buscar(

			@RequestParam(value = "descricao", required = false) String descricao,
			@RequestParam(value = "mes", required = false) Integer mes,
			@RequestParam(value = "ano", required = false) Integer ano, 
			@RequestParam("usuario") Long idUsuario

	) {
		LOGGER.info("#### Método: LancamentoResource.buscar(), status: INICIO, idUsuario: "+ idUsuario + ", mes: "+ mes + ", ano: "+ ano + ", descricao: "+ descricao);
		
		Lancamento lancamentoFiltro = new Lancamento();
		lancamentoFiltro.setDescricao(descricao);
		lancamentoFiltro.setMes(mes);
		lancamentoFiltro.setAno(ano);

		Optional<Usuario> usuario = usuarioService.obterPorId(idUsuario);
		if (!usuario.isPresent()) {
			return ResponseEntity.badRequest().body("Usuario não encontrado para o Id informado");
		} else {
			lancamentoFiltro.setUsuario(usuario.get());
		}
		
		List<Lancamento> lancamentos = service.buscar(lancamentoFiltro);
		
		LOGGER.info("#### Método: LancamentoResource.buscar(), status: SUCESSO, quantidadeLancamentos: "+ lancamentos.size());
		
		return ResponseEntity.ok(lancamentos);
	}

	@GetMapping("{id}/saldo")
	public ResponseEntity obterSaldo(@PathVariable("id") Long id) {
		
		LOGGER.info("#### Método: LancamentoResource.obterSaldo(), status: INICIO, idUsuario: "+ id);

		BigDecimal saldo = service.obterSaldoPorUsuario(id);
		
		LOGGER.info("#### Método: LancamentoResource.obterSaldo(), status: SUCESSO, saldoAtual: "+ saldo);
		
		return ResponseEntity.ok().body(saldo);
	}

	@PostMapping
	public ResponseEntity salvar(@RequestBody LancamentoDTO dto) {
		
		LOGGER.info("#### Método: LancamentoResource.salvar(), status: INICIO, idUsuario: "+ dto.getUsuario() + ", valor: "+ dto.getValor()+ ", tipo: "+ dto.getTipo()+ ", status: "+ dto.getStatus());
		
		try {
			Lancamento entidade = converter(dto);
			entidade = service.salvar(entidade);
			
			LOGGER.info("#### Método: LancamentoResource.salvar(), status: SUCESSO, idNovoLancamento: "+ entidade.getId());
			
			return new ResponseEntity(entidade, HttpStatus.CREATED);
		} catch (RegraNegocioException e) {
			
			LOGGER.info("#### Método: LancamentoResource.salvar(), status: ERROR, mensagemError: "+ e.getMessage());
			
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PutMapping("{id}")
	public ResponseEntity atualizar(@PathVariable("id") Long id, @RequestBody LancamentoDTO dto) {
		return service.obterPorId(id).map(entity -> {
			try {
				Lancamento lancamento = converter(dto);
				lancamento.setId(entity.getId());
				service.atualizar(lancamento);
				return ResponseEntity.ok(lancamento);
			} catch (RegraNegocioException e) {
				return ResponseEntity.badRequest().body(e.getMessage());
			}
		}).orElseGet(() -> new ResponseEntity("Lancamento não encontrado na base de Dados", HttpStatus.BAD_REQUEST));
	}

	@PutMapping("{id}/atualiza-status")
	public ResponseEntity atualizarStatus(@PathVariable("id") Long id, @RequestBody AtualizaStatusDTO dto) {
		return service.obterPorId(id).map(entity -> {
			StatusLancamento statusSelecionado = StatusLancamento.valueOf(dto.getStatus());
			if (statusSelecionado == null) {
				return ResponseEntity.badRequest()
						.body("Não foi possível atualiza o status do lancamento, envie um status válido");
			}
			try {
				entity.setStatus(statusSelecionado);
				service.atualizar(entity);
				return ResponseEntity.ok(entity);
			} catch (RegraNegocioException e) {
				return ResponseEntity.badRequest().body(e.getMessage());
			}
		}).orElseGet(() -> new ResponseEntity("Lançamento não encontrado na base de dados", HttpStatus.BAD_REQUEST));

	}

	@DeleteMapping("{id}")
	public ResponseEntity deletar(@PathVariable("id") Long id) {
		return service.obterPorId(id).map(entidade -> {
			service.deletar(entidade);
			return new ResponseEntity(HttpStatus.NO_CONTENT);
		}).orElseGet(() -> new ResponseEntity("Lancamento não encontrado na base de Dados", HttpStatus.BAD_REQUEST));

	}

	private LancamentoDTO converter(Lancamento lancamento) {
		return LancamentoDTO.builder().id(lancamento.getId()).descricao(lancamento.getDescricao())
				.valor(lancamento.getValor()).mes(lancamento.getMes()).ano(lancamento.getAno())
				.status(lancamento.getStatus().name()).tipo(lancamento.getTipo().name())
				.usuario(lancamento.getUsuario().getId()).build();
	}

	private Lancamento converter(LancamentoDTO dto) {
		Lancamento lancamento = new Lancamento();

		lancamento.setId(dto.getId());
		lancamento.setDescricao(dto.getDescricao());
		lancamento.setAno(dto.getAno());
		lancamento.setMes(dto.getMes());
		lancamento.setValor(dto.getValor());

		Usuario usuario = usuarioService.obterPorId(dto.getUsuario())
				.orElseThrow(() -> new RegraNegocioException("Usuario não encontrado para id informado."));

		lancamento.setUsuario(usuario);
		if (dto.getTipo() != null) {
			lancamento.setTipo(TipoLancamento.valueOf(dto.getTipo().toUpperCase()));
		}
		if (dto.getStatus() != null) {
			lancamento.setStatus(StatusLancamento.valueOf(dto.getStatus().toUpperCase()));
		}
		return lancamento;
	}

	public LancamentoResource(LancamentoService service, UsuarioService usuarioService) {
		super();
		this.service = service;
		this.usuarioService = usuarioService;
	}

}
