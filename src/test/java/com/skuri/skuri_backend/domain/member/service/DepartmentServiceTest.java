package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.domain.member.entity.Department;
import com.skuri.skuri_backend.domain.member.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    void getActiveDepartments_활성학과를저장순서대로응답한다() {
        when(departmentRepository.findAllByActiveTrueOrderByDisplayOrderAscNameAsc()).thenReturn(List.of(
                Department.initial("컴퓨터공학과", 1),
                Department.initial("정보통신공학과", 2)
        ));

        assertEquals(
                List.of("컴퓨터공학과", "정보통신공학과"),
                departmentService.getActiveDepartments().stream().map(response -> response.name()).toList()
        );
    }

    @Test
    void normalizeSupported_legacyAlias를DB의canonical학과로정규화한다() {
        when(departmentRepository.existsByNameAndActiveTrue("미디어소프트웨어학과")).thenReturn(true);

        assertEquals("미디어소프트웨어학과", departmentService.normalizeSupported(" 소프트웨어학과 "));
    }

    @Test
    void normalizeSupported_비활성또는없는학과는거부한다() {
        when(departmentRepository.existsByNameAndActiveTrue("없는학과")).thenReturn(false);

        assertNull(departmentService.normalizeSupported("없는학과"));
    }
}
