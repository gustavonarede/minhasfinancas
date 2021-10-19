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
	public ResponseEntity<Object> atualizar(@PathVariable("id") Long id, @RequestBody LancamentoDTO dto) {
		
		LOGGER.info("#### Método: LancamentoResource.atualizar(), status: INICIO, idLancamento: "+ id);
		
		try {
			
			Lancamento atualLancamento = service.obterPorId(id).orElseThrow(() -> new RegraNegocioException("Lancamento não encontrado na base de Dados"));
			
			Lancamento novoLancamento = converter(dto);
			novoLancamento.setId(atualLancamento.getId());
			
			novoLancamento = service.atualizar(novoLancamento);
			
			LOGGER.info("#### Método: LancamentoResource.atualizar(), status: SUCESSO, idLancamentoAtualizado: "+ id);
			
			return ResponseEntity.ok(novoLancamento);
		} catch (RegraNegocioException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PutMapping("{id}/atualiza-status")
	public ResponseEntity atualizarStatus(@PathVariable("id") Long id, @RequestBody AtualizaStatusDTO dto) {
		
		LOGGER.info("#### Método: LancamentoResource.atualizarStatus(), status: INICIO, idLancamento: "+ id);
		
		try {
			
			StatusLancamento statusSelecionado = StatusLancamento.valueOf(dto.getStatus());
			
			if (statusSelecionado == null) throw new RegraNegocioException("Não foi possível atualiza o status do lancamento, envie um status válido");
			
			Lancamento lancamento = service.obterPorId(id).orElseThrow(() -> new RegraNegocioException("Lancamento não encontrado na base de Dados"));
			
			lancamento.setStatus(statusSelecionado);
			
			service.atualizar(lancamento);
			
			LOGGER.info("#### Método: LancamentoResource.atualizarStatus(), status: SUCESSO, novoStatus: "+ statusSelecionado);
			
			return ResponseEntity.ok(lancamento);
		} catch (RegraNegocioException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}

	}

	@DeleteMapping("{id}")
	public ResponseEntity deletar(@PathVariable("id") Long id) {
		
		LOGGER.info("#### Método: LancamentoResource.deletar(), status: INICIO, idLancamento: "+ id);
		
		try {
			
			Lancamento lancamento = service.obterPorId(id).orElseThrow(() -> new RegraNegocioException("Lancamento não encontrado na base de Dados"));
			
			service.deletar(lancamento);
			
			LOGGER.info("#### Método: LancamentoResource.deletar(), status: SUCESSO");
			
			return ResponseEntity.noContent().build();
		} catch (RegraNegocioException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
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
