package com.skuri.skuri_backend.domain.academic.repository;

import com.skuri.skuri_backend.domain.academic.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, String> {

    @Query("""
            select c
            from Course c
            where (:semester is null or c.semester = :semester)
              and (:department is null or c.department = :department)
              and (:category is null or c.category = :category)
              and (:professor is null or lower(coalesce(c.professor, '')) like lower(concat('%', :professor, '%')))
              and (:grade is null or c.grade = :grade)
              and (:search is null
                    or lower(c.name) like lower(concat('%', :search, '%'))
                    or lower(c.code) like lower(concat('%', :search, '%'))
                    or lower(coalesce(c.category, '')) like lower(concat('%', :search, '%'))
                    or lower(coalesce(c.professor, '')) like lower(concat('%', :search, '%'))
                    or lower(coalesce(c.location, '')) like lower(concat('%', :search, '%'))
                    or lower(coalesce(c.note, '')) like lower(concat('%', :search, '%')))
              and (:dayOfWeek is null or exists (
                    select 1
                    from CourseSchedule cs
                    where cs.course = c
                      and cs.dayOfWeek = :dayOfWeek
              ))
            """)
    Page<Course> search(
            @Param("semester") String semester,
            @Param("department") String department,
            @Param("category") String category,
            @Param("professor") String professor,
            @Param("search") String search,
            @Param("dayOfWeek") Integer dayOfWeek,
            @Param("grade") Integer grade,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"schedules"})
    @Query("""
            select distinct c
            from Course c
            left join c.schedules s
            where c.id in :courseIds
            """)
    List<Course> findAllWithSchedulesByIdIn(@Param("courseIds") Collection<String> courseIds);

    @EntityGraph(attributePaths = {"schedules"})
    @Query("""
            select distinct c
            from Course c
            left join c.schedules s
            where c.id = :courseId
            """)
    Optional<Course> findDetailById(@Param("courseId") String courseId);

    @EntityGraph(attributePaths = {"schedules"})
    @Query("""
            select distinct c
            from Course c
            left join c.schedules s
            where c.semester = :semester
            """)
    List<Course> findAllBySemesterWithSchedules(@Param("semester") String semester);

    @Query("""
            select c.id
            from Course c
            where c.semester = :semester
            """)
    List<String> findIdsBySemester(@Param("semester") String semester);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from Course c
            where c.semester = :semester
            """)
    int deleteBySemester(@Param("semester") String semester);

    @Query("""
            select distinct c.semester
            from Course c
            where c.semester is not null
            """)
    List<String> findDistinctSemesters();

    @Query("""
            select distinct c.department
            from Course c
            where c.semester = :semester
              and c.department is not null
              and trim(c.department) <> ''
            order by c.department
            """)
    List<String> findDistinctDepartmentsBySemester(@Param("semester") String semester);

    @Query("""
            select distinct c.grade
            from Course c
            where c.semester = :semester
              and c.grade is not null
            order by c.grade
            """)
    List<Integer> findDistinctGradesBySemester(@Param("semester") String semester);

    @Query("""
            select distinct c.category
            from Course c
            where c.semester = :semester
              and c.category is not null
              and trim(c.category) <> ''
            order by c.category
            """)
    List<String> findDistinctCategoriesBySemester(@Param("semester") String semester);
}
