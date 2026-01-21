package com.ej2.repository;

import com.ej2.model.TimetableCourse;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Repository
public class TimetableCourseRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<TimetableCourse> findAll() {
        return entityManager.createQuery("SELECT tc FROM TimetableCourse tc", TimetableCourse.class)
                .getResultList();
    }

    public TimetableCourse findById(Long id) {
        return entityManager.find(TimetableCourse.class, id);
    }

    public List<TimetableCourse> findByTimetableId(Long timetableId) {
        TypedQuery<TimetableCourse> query = entityManager.createQuery(
                "SELECT tc FROM TimetableCourse tc WHERE tc.timetable.timetableId = :timetableId " +
                "ORDER BY tc.dayOfWeek, tc.periodStart",
                TimetableCourse.class
        );
        query.setParameter("timetableId", timetableId);
        return query.getResultList();
    }

    public TimetableCourse save(TimetableCourse course) {
        if (course.getCourseId() == null) {
            entityManager.persist(course);
            return course;
        } else {
            return entityManager.merge(course);
        }
    }

    public void deleteById(Long id) {
        TimetableCourse course = findById(id);
        if (course != null) {
            entityManager.remove(course);
        }
    }

    public boolean hasTimeConflict(Long timetableId, Integer dayOfWeek, Integer periodStart,
                                   Integer periodEnd, Long excludeCourseId) {
        String jpql = "SELECT COUNT(tc) FROM TimetableCourse tc " +
                     "WHERE tc.timetable.timetableId = :timetableId " +
                     "AND tc.dayOfWeek = :dayOfWeek " +
                     "AND ((tc.periodStart <= :periodEnd AND tc.periodEnd >= :periodStart))";

        if (excludeCourseId != null) {
            jpql += " AND tc.courseId != :excludeCourseId";
        }

        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        query.setParameter("timetableId", timetableId);
        query.setParameter("dayOfWeek", dayOfWeek);
        query.setParameter("periodStart", periodStart);
        query.setParameter("periodEnd", periodEnd);

        if (excludeCourseId != null) {
            query.setParameter("excludeCourseId", excludeCourseId);
        }

        return query.getSingleResult() > 0;
    }
}
