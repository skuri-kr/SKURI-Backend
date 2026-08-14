package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.domain.member.entity.Department;
import com.skuri.skuri_backend.domain.member.repository.DepartmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void seed_이벤트리스너메서드에최우선순서를지정한다() throws NoSuchMethodException {
        Order order = DepartmentBootstrapSeed.class
                .getDeclaredMethod("seed")
                .getAnnotation(Order.class);

        assertNotNull(order);
        assertEquals(Ordered.HIGHEST_PRECEDENCE, order.value());
    }

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
