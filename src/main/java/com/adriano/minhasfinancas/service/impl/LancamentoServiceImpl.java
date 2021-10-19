package com.adriano.minhasfinancas.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.cfg.annotations.Nullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.adriano.minhasfinancas.api.resource.LancamentoResource;
import com.adriano.minhasfinancas.exception.RegraNegocioException;
import com.adriano.minhasfinancas.model.entity.Lancamento;
import com.adriano.minhasfinancas.model.enums.StatusLancamento;
import com.adriano.minhasfinancas.model.enums.TipoLancamento;
import com.adriano.minhasfinancas.model.repository.LancamentoRepository;
import com.adriano.minhasfinancas.service.LancamentoService;

@Service
public class LancamentoServiceImpl implements LancamentoService {

	private static final Logger LOGGER = LoggerFactory.getLogger(LancamentoServiceImpl.class);
	
	private LancamentoRepository repository;
	
	public LancamentoServiceImpl(LancamentoRepository repository) {
		this.repository = repository;
	}
	@Override
	@Transactional
	public Lancamento salvar(Lancamento lancamento) {
		
		LOGGER.info("#### Método: LancamentoServiceImpl.buscar(), status: INICIO, idUsuario: "+ lancamento.getUsuario()+", valor: "+ lancamento.getValor());
		
		validar(lancamento);
		lancamento.setStatus(StatusLancamento.PENDENTE);
		return repository.save(lancamento);
	}

	@Override
	@Transactional
	public Lancamento atualizar(Lancamento lancamento) {
		
		LOGGER.info("#### Método: LancamentoServiceImpl.atualizar(), status: INICIO, id: "+ lancamento.getId());
		
		Objects.requireNonNull(lancamento.getId());
		validar(lancamento);
		//lancamento.setStatus(StatusLancamento.PENDENTE);
		return repository.save(lancamento);
	}

	@Override
	@Transactional
	public void deletar(Lancamento lancamento) {
		LOGGER.info("#### Método: LancamentoServiceImpl.deletar(), status: INICIO, id: "+ lancamento.getId());
		
		Objects.requireNonNull(lancamento.getId());
		repository.delete(lancamento);
		
	}

	@Override
	@Transactional(readOnly = true)
	public List<Lancamento> buscar(Lancamento lancamentoFiltro) {
		
		LOGGER.info("#### Método: LancamentoServiceImpl.buscar(), status: INICIO, idUsuario: "+ lancamentoFiltro.getUsuario());
		
		Example  example = Example.of(lancamentoFiltro,
				ExampleMatcher.matching()
				.withIgnoreCase()
				.withStringMatcher(StringMatcher.CONTAINING));
		
		return repository.findAll(example);
	}

	@Override
	public void atualizarStatus(Lancamento lancamento, StatusLancamento status) {
		
		LOGGER.info("#### Método: LancamentoServiceImpl.atualizarStatus(), status: INICIO, novoStatus: "+ status);
		
		lancamento.setStatus(status);
		atualizar(lancamento);
		
	}
	@Override
	public void validar(Lancamento lancamento) {
		if(lancamento.getDescricao() == null || lancamento.getDescricao().trim().equals("")) {
			throw new RegraNegocioException("Informe uma descrição válida");
		}
		
		if(lancamento.getMes() == null || lancamento.getMes( ) < 1 || lancamento.getMes() > 12) {
			throw new RegraNegocioException("Informe um mês válido");
		}
		if(lancamento.getAno() == null || lancamento.getAno().toString().length() != 4) {
			throw new RegraNegocioException("Informe um Ano válido.");
		}
		if(lancamento.getUsuario() == null || lancamento.getUsuario().getId() == null) {
			throw new RegraNegocioException("Informe um usuário.");
			
		}
		if(lancamento.getValor() == null || lancamento.getValor().compareTo(BigDecimal.ZERO) < 1) {
			throw new RegraNegocioException("Informe um valor válido.");
		}
		if(lancamento.getTipo() == null) {
			throw new RegraNegocioException("Informe um tipo de lançamento.");
		}
		
	}
	@Override
	public Optional<Lancamento> obterPorId(Long id) {
		
		LOGGER.info("#### Método: LancamentoServiceImpl.obterPorId(), status: INICIO, id: "+ id);
		
		return repository.findById(id);
	}
	
	@Override
	@Transactional(readOnly = true)
	public BigDecimal obterSaldoPorUsuario(Long id) {
		
		LOGGER.info("#### Método: LancamentoServiceImpl.obterSaldoPorUsuario(), status: INICIO, id: "+ id);
		
		BigDecimal receitas = repository.obterSaldoPorTipoLancamentoEUsuario(id, TipoLancamento.RECEITA);
		BigDecimal despesas = repository.obterSaldoPorTipoLancamentoEUsuario(id, TipoLancamento.DESPESA);
		
		if(receitas == null) {
			receitas = BigDecimal.ZERO;
		}
		if(despesas == null) {
			despesas = BigDecimal.ZERO;
		}
		return receitas.subtract(despesas);
		
	}
	
	

}
