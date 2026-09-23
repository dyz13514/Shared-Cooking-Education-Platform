package com.example.teachingplatform.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class StorageService {

    private final Path rootDir;

    public StorageService(@Value("${app.upload.dir:./uploads}") String rootDir) {
        this.rootDir = Paths.get(rootDir).toAbsolutePath().normalize();
    }

    public StoredFile store(String subDir, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String ext = "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot >= 0 && dot < originalFilename.length() - 1) {
            ext = originalFilename.substring(dot + 1).toLowerCase();
        }

        String date = LocalDate.now().toString();
        String filename = UUID.randomUUID().toString().replace("-", "") + (ext.isEmpty() ? "" : "." + ext);

        Path dir = rootDir.resolve(Paths.get(subDir, date)).normalize();
        if (!dir.startsWith(rootDir)) {
            throw new IllegalArgumentException("非法存储路径");
        }
        Files.createDirectories(dir);

        Path target = dir.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String storedPath = rootDir.relativize(target).toString().replace('\\', '/');
        return new StoredFile(storedPath, originalFilename, file.getContentType() == null ? "application/octet-stream" : file.getContentType(), file.getSize());
    }

    public Path resolve(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new IllegalArgumentException("文件路径为空");
        }
        Path p = rootDir.resolve(storedPath.replace('/', java.io.File.separatorChar)).normalize();
        if (!p.startsWith(rootDir)) {
            throw new IllegalArgumentException("非法存储路径");
        }
        return p;
    }

    public boolean deleteIfExists(String storedPath) throws IOException {
        Path p = resolve(storedPath);
        return Files.deleteIfExists(p);
    }

    public record StoredFile(
            String storedPath,
            String originalFilename,
            String contentType,
            long sizeBytes
    ) {
    }
}
