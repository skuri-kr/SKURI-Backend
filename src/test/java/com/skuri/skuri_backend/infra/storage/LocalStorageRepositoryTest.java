package com.skuri.skuri_backend.infra.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveRelativePath_publicBaseUrl기준으로_상대경로를복원한다() {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setBaseDir(tempDir.toString());
        properties.setPublicBaseUrl("https://cdn.skuri.app/uploads");
        properties.setUrlPrefix("/uploads");

        LocalStorageRepository repository = new LocalStorageRepository(
                properties,
                new MockEnvironment().withProperty("server.port", "8080")
        );

        assertEquals(
                "profiles/firebase-uid/2026/04/06/photo.jpg",
                repository.resolveRelativePath("https://cdn.skuri.app/uploads/profiles/firebase-uid/2026/04/06/photo.jpg").orElseThrow()
        );
        assertTrue(repository.resolveRelativePath("https://images.example.com/photo.jpg").isEmpty());
    }

    @Test
    void resolveRelativePath_publicBaseUrl이없으면_localhostUrlPrefix기준으로_상대경로를복원한다() {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setBaseDir(tempDir.toString());
        properties.setUrlPrefix("media-files");

        LocalStorageRepository repository = new LocalStorageRepository(
                properties,
                new MockEnvironment().withProperty("server.port", "9090")
        );

        assertEquals(
                "profiles/firebase-uid/2026/04/06/photo.jpg",
                repository.resolveRelativePath("http://localhost:9090/media-files/profiles/firebase-uid/2026/04/06/photo.jpg").orElseThrow()
        );
    }

    @Test
    void resolveVerifiedRelativePath_실제저장된파일만_상대경로를반환한다() throws Exception {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setBaseDir(tempDir.toString());
        properties.setPublicBaseUrl("https://cdn.skuri.app/uploads");
        LocalStorageRepository repository = new LocalStorageRepository(
                properties,
                new MockEnvironment().withProperty("server.port", "8080")
        );
        String relativePath = "chat/2026/08/image.jpg";
        Path storedPath = tempDir.resolve(relativePath);
        Files.createDirectories(storedPath.getParent());
        Files.writeString(storedPath, "image");

        assertEquals(
                relativePath,
                repository.resolveVerifiedRelativePath("https://cdn.skuri.app/uploads/" + relativePath).orElseThrow()
        );
        assertTrue(repository.resolveVerifiedRelativePath(
                "https://cdn.skuri.app/uploads/chat/2026/08/not-uploaded.jpg"
        ).isEmpty());
    }
}
