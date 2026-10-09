package com.sistemaportocabral.backend.controller;

import com.sistemaportocabral.backend.dto.AtivacaoLicencaDTO;
import com.sistemaportocabral.backend.dto.LicencaStatusDTO;
import com.sistemaportocabral.backend.service.LicencaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Únicos endpoints liberados enquanto o sistema não está ativado (ver LicencaInterceptor). */
@RestController
@RequestMapping("/licenca")
public class LicencaController {

    @Autowired
    private LicencaService service;

    @GetMapping("/status")
    public LicencaStatusDTO status() {
        return LicencaStatusDTO.de(service);
    }

    @PostMapping("/ativar")
    public ResponseEntity<?> ativar(@RequestBody AtivacaoLicencaDTO dto) {
        try {
            service.ativar(dto.getChave());
            return ResponseEntity.ok(LicencaStatusDTO.de(service));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
