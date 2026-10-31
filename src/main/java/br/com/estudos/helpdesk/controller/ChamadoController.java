package br.com.estudos.helpdesk.controller;

import br.com.estudos.helpdesk.dto.request.AlteracaoStatusRequest;
import br.com.estudos.helpdesk.dto.request.AtribuicaoRequest;
import br.com.estudos.helpdesk.dto.request.ChamadoAtualizacaoRequest;
import br.com.estudos.helpdesk.dto.request.ChamadoFiltro;
import br.com.estudos.helpdesk.dto.request.ChamadoRequest;
import br.com.estudos.helpdesk.dto.response.ChamadoResponse;
import br.com.estudos.helpdesk.dto.response.HistoricoStatusResponse;
import br.com.estudos.helpdesk.dto.response.PaginaResponse;
import br.com.estudos.helpdesk.service.ChamadoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {

    private final ChamadoService service;

    public ChamadoController(ChamadoService service) {
        this.service = service;
    }

    @GetMapping
    public PaginaResponse<ChamadoResponse> listar(
            ChamadoFiltro filtro,
            @PageableDefault(size = 20, sort = "dataAbertura", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(filtro, pageable);
    }

    @GetMapping("/{id}")
    public ChamadoResponse buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ChamadoResponse> abrir(@Valid @RequestBody ChamadoRequest request) {
        ChamadoResponse aberto = service.abrir(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(aberto.id()).toUri();
        return ResponseEntity.created(location).body(aberto);
    }

    @PutMapping("/{id}")
    public ChamadoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ChamadoAtualizacaoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        service.excluir(id);
    }

    @PatchMapping("/{id}/status")
    public ChamadoResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody AlteracaoStatusRequest request) {
        return service.alterarStatus(id, request);
    }

    @PatchMapping("/{id}/atribuicao")
    public ChamadoResponse atribuir(@PathVariable Long id, @Valid @RequestBody AtribuicaoRequest request) {
        return service.atribuir(id, request);
    }

    @GetMapping("/{id}/historico")
    public List<HistoricoStatusResponse> listarHistorico(@PathVariable Long id) {
        return service.listarHistorico(id);
    }
}
