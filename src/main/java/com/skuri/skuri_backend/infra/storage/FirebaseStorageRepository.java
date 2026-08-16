package com.skuri.skuri_backend.infra.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FirebaseStorageRepository implements StorageRepository {

    // Firebase download URL은 object metadata의 token을 기준으로 안정적인 공유 URL을 만든다.
    static final String DOWNLOAD_TOKEN_METADATA_KEY = "firebaseStorageDownloadTokens";

    private final Bucket bucket;

    public FirebaseStorageRepository(Bucket bucket) {
        this.bucket = bucket;
    }

    @Override
    public StoredObject store(String relativePath, byte[] data, String contentType) {
        String downloadToken = UUID.randomUUID().toString();
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket.getName(), relativePath))
                .setContentType(contentType)
                .setMetadata(Map.of(DOWNLOAD_TOKEN_METADATA_KEY, downloadToken))
                .build();
        bucket.getStorage().create(blobInfo, data);
        return new StoredObject(relativePath, buildDownloadUrl(relativePath, downloadToken));
    }

    @Override
    public void delete(String relativePath) {
        Blob blob = bucket.get(relativePath);
        if (blob != null) {
            blob.delete();
        }
    }

    @Override
    public Optional<String> resolveRelativePath(String publicUrl) {
        return parseDownloadUrl(publicUrl).map(FirebaseDownloadUrl::relativePath);
    }

    @Override
    public Optional<String> resolveVerifiedRelativePath(String publicUrl) {
        return parseDownloadUrl(publicUrl)
                .filter(FirebaseDownloadUrl::hasDownloadToken)
                .filter(this::matchesStoredDownloadToken)
                .map(FirebaseDownloadUrl::relativePath);
    }

    private Optional<FirebaseDownloadUrl> parseDownloadUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return Optional.empty();
        }

        try {
            URI uri = URI.create(publicUrl.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !"firebasestorage.googleapis.com".equalsIgnoreCase(uri.getHost())) {
                return Optional.empty();
            }
            String path = uri.getPath();
            String prefix = "/v0/b/" + bucket.getName() + "/o/";
            if (path == null || !path.startsWith(prefix)) {
                return Optional.empty();
            }
            String encodedRelativePath = path.substring(prefix.length());
            if (encodedRelativePath.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new FirebaseDownloadUrl(
                    URLDecoder.decode(encodedRelativePath, StandardCharsets.UTF_8),
                    parseQueryParameters(uri.getRawQuery())
            ));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private boolean matchesStoredDownloadToken(FirebaseDownloadUrl downloadUrl) {
        String token = downloadUrl.queryParameters().get("token");
        Blob blob = bucket.get(downloadUrl.relativePath());
        if (blob == null || blob.getMetadata() == null) {
            return false;
        }
        String configuredTokens = blob.getMetadata().get(DOWNLOAD_TOKEN_METADATA_KEY);
        return configuredTokens != null
                && Arrays.stream(configuredTokens.split(","))
                .map(String::trim)
                .anyMatch(token::equals);
    }

    private Map<String, String> parseQueryParameters(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }
        return Arrays.stream(rawQuery.split("&"))
                .map(parameter -> parameter.split("=", 2))
                .filter(parameter -> parameter.length == 2)
                .collect(java.util.stream.Collectors.toMap(
                        parameter -> URLDecoder.decode(parameter[0], StandardCharsets.UTF_8),
                        parameter -> URLDecoder.decode(parameter[1], StandardCharsets.UTF_8),
                        (first, ignored) -> first
                ));
    }

    private String buildDownloadUrl(String relativePath, String downloadToken) {
        String encodedPath = URLEncoder.encode(relativePath, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media&token=%s"
                .formatted(bucket.getName(), encodedPath, downloadToken);
    }

    private record FirebaseDownloadUrl(String relativePath, Map<String, String> queryParameters) {

        private boolean hasDownloadToken() {
            String token = queryParameters.get("token");
            return "media".equals(queryParameters.get("alt")) && token != null && !token.isBlank();
        }
    }
}
