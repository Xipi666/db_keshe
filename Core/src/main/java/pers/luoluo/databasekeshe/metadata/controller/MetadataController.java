package pers.luoluo.databasekeshe.metadata.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pers.luoluo.databasekeshe.metadata.dto.TransformerOptionResponse;
import pers.luoluo.databasekeshe.metadata.service.MetadataService;
import pers.luoluo.databasekeshe.security.AccessGuard;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private final MetadataService metadataService;
    private final AccessGuard accessGuard;

    public MetadataController(MetadataService metadataService, AccessGuard accessGuard) {
        this.metadataService = metadataService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/transformers")
    public List<TransformerOptionResponse> transformers(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Role-Code") String roleCode
    ) {
        accessGuard.requireUser(userId, roleCode);
        return metadataService.listTransformers();
    }
}
