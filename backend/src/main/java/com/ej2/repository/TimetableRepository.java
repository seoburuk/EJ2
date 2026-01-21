package com.ej2.repository;

import com.ej2.model.Timetable;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Repository
public class TimetableRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Timetable> findAll() {
        return entityManager.createQuery("SELECT t FROM Timetable t", Timetable.class)
                .getResultList();
    }

    public Timetable findById(Long id) {
        return entityManager.find(Timetable.class, id);
    }

    public Timetable findByUserIdAndYearAndSemester(Long userId, Integer year, String semester) {
        TypedQuery<Timetable> query = entityManager.createQuery(
                "SELECT t FROM Timetable t WHERE t.userId = :userId AND t.year = :year AND t.semester = :semester",
                Timetable.class
        );
        query.setParameter("userId", userId);
        query.setParameter("year", year);
        query.setParameter("semester", semester);

        List<Timetable> results = query.getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public List<Timetable> findByUserId(Long userId) {
        TypedQuery<Timetable> query = entityManager.createQuery(
                "SELECT t FROM Timetable t WHERE t.userId = :userId ORDER BY t.year DESC, t.semester DESC",
                Timetable.class
        );
        query.setParameter("userId", userId);
        return query.getResultList();
    }

    public Timetable save(Timetable timetable) {
        if (timetable.getTimetableId() == null) {
            entityManager.persist(timetable);
            return timetable;
        } else {
            return entityManager.merge(timetable);
        }
    }

    public void deleteById(Long id) {
        Timetable timetable = findById(id);
        if (timetable != null) {
            entityManager.remove(timetable);
        }
    }
}
