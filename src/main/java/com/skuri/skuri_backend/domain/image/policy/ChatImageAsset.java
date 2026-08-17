package com.skuri.skuri_backend.domain.image.policy;

import java.util.List;

public record ChatImageAsset(
        String familyKey,
        List<String> cleanupPaths,
        boolean thumbnailReference
) {
}
