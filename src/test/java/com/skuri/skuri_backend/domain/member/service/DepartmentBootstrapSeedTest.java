package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.domain.member.entity.Department;
import com.skuri.skuri_backend.domain.member.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentBootstrapSeedTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentBootstrapSeed bootstrapSeed;

    @Test
    void seed_빈DB에는초기29개학과를생성한다() {
        bootstrapSeed.seed();

        verify(departmentRepository, times(29)).save(any(Department.class));
    }

    @Test
    void seed_이미존재하는학과는덮어쓰지않는다() {
        when(departmentRepository.existsById(any())).thenReturn(true);

        bootstrapSeed.seed();

        verify(departmentRepository, never()).save(any(Department.class));
    }
}
