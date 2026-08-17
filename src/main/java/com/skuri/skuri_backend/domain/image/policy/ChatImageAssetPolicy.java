package com.skuri.skuri_backend.domain.image.policy;

import org.springframework.util.StringUtils;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ChatImageAssetPolicy {

    public static final String NO_MANAGED_ASSET_KEY = "__no_managed_chat_image__";

    private static final String CHAT_STORAGE_DIRECTORY_PREFIX = "chat/";
    private static final String THUMBNAIL_SUFFIX = "_thumb";
    private static final List<String> IMAGE_EXTENSIONS = List.of(".jpg", ".png", ".webp");

    private ChatImageAssetPolicy() {
    }

    public static Optional<ChatImageAsset> resolve(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return Optional.empty();
        }

        try {
            Path normalizedPath = Path.of(relativePath.replace('\\', '/')).normalize();
            if (normalizedPath.isAbsolute()) {
                return Optional.empty();
            }

            String normalized = normalizedPath.toString().replace('\\', '/');
            if (!normalized.startsWith(CHAT_STORAGE_DIRECTORY_PREFIX)) {
                return Optional.empty();
            }

            int extensionIndex = normalized.lastIndexOf('.');
            if (extensionIndex <= normalized.lastIndexOf('/')) {
                return Optional.empty();
            }

            String pathWithoutExtension = normalized.substring(0, extensionIndex);
            boolean thumbnailReference = pathWithoutExtension.endsWith(THUMBNAIL_SUFFIX);
            String familyKey = thumbnailReference
                    ? pathWithoutExtension.substring(0, pathWithoutExtension.length() - THUMBNAIL_SUFFIX.length())
                    : pathWithoutExtension;
            if (!StringUtils.hasText(familyKey) || !familyKey.startsWith(CHAT_STORAGE_DIRECTORY_PREFIX)) {
                return Optional.empty();
            }

            return Optional.of(new ChatImageAsset(
                    familyKey,
                    cleanupPathsForFamilyKey(familyKey, normalized),
                    thumbnailReference
            ));
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    public static List<String> cleanupPathsForFamilyKey(String familyKey) {
        if (!StringUtils.hasText(familyKey)) {
            return List.of();
        }
        try {
            Path normalizedPath = Path.of(familyKey.replace('\\', '/')).normalize();
            if (normalizedPath.isAbsolute()) {
                return List.of();
            }
            String normalizedFamilyKey = normalizedPath.toString().replace('\\', '/');
            if (!normalizedFamilyKey.startsWith(CHAT_STORAGE_DIRECTORY_PREFIX)) {
                return List.of();
            }
            return cleanupPathsForFamilyKey(normalizedFamilyKey, null);
        } catch (InvalidPathException e) {
            return List.of();
        }
    }

    private static List<String> cleanupPathsForFamilyKey(String familyKey, String additionalPath) {
        Set<String> cleanupPaths = new LinkedHashSet<>();
        IMAGE_EXTENSIONS.forEach(extension -> cleanupPaths.add(familyKey + extension));
        IMAGE_EXTENSIONS.forEach(extension -> cleanupPaths.add(familyKey + THUMBNAIL_SUFFIX + extension));
        if (StringUtils.hasText(additionalPath)) {
            cleanupPaths.add(additionalPath);
        }
        return cleanupPaths.stream().sorted().toList();
    }
}
