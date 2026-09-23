package com.example.teachingplatform.resource.web;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.web.SessionKeys;
import com.example.teachingplatform.resource.model.TeachingResource;
import com.example.teachingplatform.resource.service.ResourceService;
import com.example.teachingplatform.storage.StorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

@Controller
public class ResourceFileController {

    private final ResourceService resourceService;
    private final StorageService storageService;

    public ResourceFileController(ResourceService resourceService, StorageService storageService) {
        this.resourceService = resourceService;
        this.storageService = storageService;
    }

    @GetMapping("/api/files/resources/{id}")
    public ResponseEntity<Resource> get(@PathVariable Long id,
                                        @RequestParam(name = "download", defaultValue = "0") int download,
                                        HttpSession session) {
        Role role = (Role) session.getAttribute(SessionKeys.ROLE);
        TeachingResource r = resourceService.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (role == Role.STUDENT && r.getVisibility() == TeachingResource.Visibility.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Path path = storageService.resolve(r.getStoredPath());
        FileSystemResource fs = new FileSystemResource(path);
        if (!fs.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        MediaType mt;
        try {
            mt = MediaType.parseMediaType(r.getContentType());
        } catch (Exception e) {
            mt = MediaType.APPLICATION_OCTET_STREAM;
        }

        ContentDisposition disposition = download == 1
                ? ContentDisposition.attachment().filename(r.getOriginalFilename()).build()
                : ContentDisposition.inline().filename(r.getOriginalFilename()).build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mt);
        headers.setContentDisposition(disposition);
        headers.setContentLength(r.getSizeBytes());
        return new ResponseEntity<>(fs, headers, HttpStatus.OK);
    }
}
