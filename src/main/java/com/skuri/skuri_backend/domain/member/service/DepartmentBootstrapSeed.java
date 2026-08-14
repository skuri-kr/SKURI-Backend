package com.skuri.skuri_backend.domain.member.service;

import com.skuri.skuri_backend.domain.member.entity.Department;
import com.skuri.skuri_backend.domain.member.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DepartmentBootstrapSeed {

    private static final List<String> INITIAL_DEPARTMENTS = List.of(
            "신학과",
            "기독교교육상담학과",
            "문화선교학과",
            "영어영문학과",
            "중어중문학과",
            "국어국문학과",
            "사회복지학과",
            "국제개발협력학과",
            "행정학과",
            "관광학과",
            "경영학과",
            "글로벌물류학과",
            "산업경영공학과",
            "유아교육과",
            "체육교육과",
            "교직부",
            "컴퓨터공학과",
            "정보통신공학과",
            "미디어소프트웨어학과",
            "도시디자인정보공학과",
            "음악학부",
            "실용음악과",
            "공연음악예술학부",
            "연기예술학과",
            "영화영상학과",
            "연극영화학부",
            "뷰티디자인학과",
            "융합학부",
            "파이데이아학부"
    );

    private final DepartmentRepository departmentRepository;

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        int createdCount = 0;
        for (int index = 0; index < INITIAL_DEPARTMENTS.size(); index++) {
            String name = INITIAL_DEPARTMENTS.get(index);
            if (departmentRepository.existsById(name)) {
                continue;
            }
            departmentRepository.save(Department.initial(name, index + 1));
            createdCount++;
        }
        if (createdCount > 0) {
            log.info("학과 마스터 bootstrap seed 완료: {}건 생성", createdCount);
        }
    }
}
