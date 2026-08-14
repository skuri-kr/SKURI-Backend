package com.skuri.skuri_backend.domain.member.repository;

import com.skuri.skuri_backend.domain.member.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, String> {

    boolean existsByNameAndActiveTrue(String name);

    List<Department> findAllByActiveTrueOrderByDisplayOrderAscNameAsc();
}
