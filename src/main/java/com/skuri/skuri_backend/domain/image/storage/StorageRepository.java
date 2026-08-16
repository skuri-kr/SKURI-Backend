package com.skuri.skuri_backend.domain.image.storage;

import java.util.Optional;

public interface StorageRepository {

    StoredObject store(String relativePath, byte[] data, String contentType);

    void delete(String relativePath);

    Optional<String> resolveRelativePath(String publicUrl);

    /**
     * 공개 URL이 실제 저장된 객체를 가리키는지 확인한 뒤 상대 경로를 반환한다.
     *
     * <p>경로 형태만 해석하는 {@link #resolveRelativePath(String)}와 달리, 구현체는
     * provider별 공개 URL 자격 증명과 객체 존재 여부를 검증해야 한다.</p>
     */
    Optional<String> resolveVerifiedRelativePath(String publicUrl);

    record StoredObject(String relativePath, String publicUrl) {
    }
}
