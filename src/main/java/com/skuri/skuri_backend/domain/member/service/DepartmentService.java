package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.domain.member.constant.DepartmentAliasNormalizer;
import com.skuri.skuri_backend.domain.member.dto.response.DepartmentResponse;
import com.skuri.skuri_backend.domain.member.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getActiveDepartments() {
        return departmentRepository.findAllByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(department -> new DepartmentResponse(department.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getActiveDepartmentNames() {
        return departmentRepository.findAllByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(department -> department.getName())
                .toList();
    }

    @Transactional(readOnly = true)
    public String normalizeSupported(String department) {
        String candidate = DepartmentAliasNormalizer.normalizeCandidate(department);
        if (candidate == null || !departmentRepository.existsByNameAndActiveTrue(candidate)) {
            return null;
        }
        return candidate;
    }

    @Transactional(readOnly = true)
    public String normalizeOrOriginal(String department) {
        String candidate = DepartmentAliasNormalizer.normalizeCandidate(department);
        if (candidate == null) {
            return null;
        }
        return departmentRepository.existsByNameAndActiveTrue(candidate)
                ? candidate
                : department.trim();
    }
}
