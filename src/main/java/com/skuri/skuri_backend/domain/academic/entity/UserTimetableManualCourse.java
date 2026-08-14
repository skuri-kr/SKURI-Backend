package com.skuri.skuri_backend.domain.academic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "user_timetable_manual_courses",
        indexes = {
                @Index(name = "idx_user_timetable_manual_courses_timetable", columnList = "timetable_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTimetableManualCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false)
    private UserTimetable timetable;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String professor;

    @Column(length = 50)
    private String department;

    @Column(nullable = false)
    private Integer credits;

    @Column(name = "is_online", nullable = false)
    private boolean online;

    @Column(length = 100)
    private String location;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(name = "start_period")
    private Integer startPeriod;

    @Column(name = "end_period")
    private Integer endPeriod;

    private UserTimetableManualCourse(
            UserTimetable timetable,
            String name,
            String professor,
            String department,
            Integer credits,
            boolean online,
            String location,
            Integer dayOfWeek,
            Integer startPeriod,
            Integer endPeriod
    ) {
        this.timetable = timetable;
        this.name = name;
        this.professor = professor;
        this.department = department;
        this.credits = credits;
        this.online = online;
        this.location = location;
        this.dayOfWeek = dayOfWeek;
        this.startPeriod = startPeriod;
        this.endPeriod = endPeriod;
    }

    public static UserTimetableManualCourse create(
            UserTimetable timetable,
            String name,
            String professor,
            String department,
            Integer credits,
            boolean online,
            String location,
            Integer dayOfWeek,
            Integer startPeriod,
            Integer endPeriod
    ) {
        return new UserTimetableManualCourse(
                timetable,
                name,
                professor,
                department,
                credits,
                online,
                location,
                dayOfWeek,
                startPeriod,
                endPeriod
        );
    }

    public boolean hasSchedule() {
        return !online && dayOfWeek != null && startPeriod != null && endPeriod != null;
    }

    public boolean conflictsWith(Course other) {
        if (!hasSchedule()) {
            return false;
        }
        for (CourseSchedule schedule : other.getSchedules()) {
            if (overlaps(schedule.getDayOfWeek(), schedule.getStartPeriod(), schedule.getEndPeriod())) {
                return true;
            }
        }
        return false;
    }

    public boolean overlaps(Integer otherDayOfWeek, Integer otherStartPeriod, Integer otherEndPeriod) {
        if (!hasSchedule() || otherDayOfWeek == null || otherStartPeriod == null || otherEndPeriod == null) {
            return false;
        }
        if (!dayOfWeek.equals(otherDayOfWeek)) {
            return false;
        }
        return startPeriod <= otherEndPeriod && otherStartPeriod <= endPeriod;
    }
}
