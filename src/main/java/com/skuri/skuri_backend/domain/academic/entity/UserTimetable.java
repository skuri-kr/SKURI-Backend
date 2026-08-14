package com.skuri.skuri_backend.domain.academic.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "user_timetables",
        indexes = {
                @Index(name = "idx_user_timetables_user", columnList = "user_id"),
                @Index(name = "idx_user_timetables_semester", columnList = "semester")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "idx_user_timetables_user_semester",
                columnNames = {"user_id", "semester"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTimetable extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 10)
    private String semester;

    @OneToMany(mappedBy = "timetable", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<UserTimetableCourse> courseMappings = new ArrayList<>();

    @OneToMany(mappedBy = "timetable", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<UserTimetableManualCourse> manualCourses = new ArrayList<>();

    private UserTimetable(String userId, String semester) {
        this.userId = userId;
        this.semester = semester;
    }

    public static UserTimetable create(String userId, String semester) {
        return new UserTimetable(userId, semester);
    }

    public boolean containsCourse(String courseId) {
        return courseMappings.stream().anyMatch(mapping -> mapping.getCourseId().equals(courseId));
    }

    public void addCourse(Course course) {
        this.courseMappings.add(UserTimetableCourse.create(this, course));
    }

    public void addManualCourse(
            String name,
            String professor,
            String department,
            Integer credits,
            boolean isOnline,
            String location,
            Integer dayOfWeek,
            Integer startPeriod,
            Integer endPeriod
    ) {
        this.manualCourses.add(UserTimetableManualCourse.create(
                this,
                name,
                professor,
                department,
                credits,
                isOnline,
                location,
                dayOfWeek,
                startPeriod,
                endPeriod
        ));
    }

    public boolean removeCourse(String courseId) {
        return courseMappings.removeIf(mapping -> mapping.getCourseId().equals(courseId));
    }

    public boolean removeManualCourse(String courseId) {
        return manualCourses.removeIf(course -> course.getId().equals(courseId));
    }
}
