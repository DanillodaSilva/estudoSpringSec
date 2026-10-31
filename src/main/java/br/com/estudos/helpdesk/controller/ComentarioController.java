package br.com.estudos.helpdesk.controller;

import br.com.estudos.helpdesk.dto.request.ComentarioAtualizacaoRequest;
import br.com.estudos.helpdesk.dto.request.ComentarioRequest;
import br.com.estudos.helpdesk.dto.response.ComentarioResponse;
import br.com.estudos.helpdesk.dto.response.PaginaResponse;
import br.com.estudos.helpdesk.service.ComentarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/chamados/{chamadoId}/comentarios")
public class ComentarioController {

    private final ComentarioService service;

    public ComentarioController(ComentarioService service) {
        this.service = service;
    }

    @GetMapping
    public PaginaResponse<ComentarioResponse> listar(@PathVariable Long chamadoId,
                                                     @PageableDefault(size = 20, sort = "dataCriacao") Pageable pageable) {
        return service.listar(chamadoId, pageable);
    }

    @GetMapping("/{comentarioId}")
    public ComentarioResponse buscar(@PathVariable Long chamadoId, @PathVariable Long comentarioId) {
        return service.buscarPorId(chamadoId, comentarioId);
    }

    @PostMapping
    public ResponseEntity<ComentarioResponse> criar(@PathVariable Long chamadoId,
                                                    @Valid @RequestBody ComentarioRequest request) {
        ComentarioResponse criado = service.criar(chamadoId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{comentarioId}")
    public ComentarioResponse atualizar(@PathVariable Long chamadoId,
                                        @PathVariable Long comentarioId,
                                        @Valid @RequestBody ComentarioAtualizacaoRequest request) {
        return service.atualizar(chamadoId, comentarioId, request);
    }

    @DeleteMapping("/{comentarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long chamadoId, @PathVariable Long comentarioId) {
        service.excluir(chamadoId, comentarioId);
    }
}
