package com.skuri.skuri_backend.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "departments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {

    @Id
    @Column(length = 50)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private Department(String name, boolean active, int displayOrder) {
        this.name = name;
        this.active = active;
        this.displayOrder = displayOrder;
    }

    public static Department initial(String name, int displayOrder) {
        return new Department(name, true, displayOrder);
    }
}
