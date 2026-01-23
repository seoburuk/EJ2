package com.ej2.repository;

import com.ej2.model.User;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.List;

@Repository
public class UserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<User> findAll() {
        return entityManager.createQuery("SELECT u FROM User u", User.class).getResultList();
    }

    public User findById(Long id) {
        return entityManager.find(User.class, id);
    }

    /**
     * ユーザー名でユーザーを検索
     * @param username ユーザー名
     * @return 見つかったユーザー、存在しない場合はnull
     */
    public User findByUsername(String username) {
        try {
            return entityManager.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * メールアドレスでユーザーを検索
     * @param email メールアドレス
     * @return 見つかったユーザー、存在しない場合はnull
     */
    public User findByEmail(String email) {
        try {
            return entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * リセットトークンでユーザーを検索
     * @param resetToken リセットトークン
     * @return 見つかったユーザー、存在しない場合はnull
     */
    public User findByResetToken(String resetToken) {
        try {
            return entityManager.createQuery("SELECT u FROM User u WHERE u.resetToken = :resetToken", User.class)
                    .setParameter("resetToken", resetToken)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * ユーザー名が既に存在するか確認
     * @param username ユーザー名
     * @return 存在する場合はtrue
     */
    public boolean existsByUsername(String username) {
        Long count = entityManager.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
        return count > 0;
    }

    /**
     * メールアドレスが既に存在するか確認
     * @param email メールアドレス
     * @return 存在する場合はtrue
     */
    public boolean existsByEmail(String email) {
        Long count = entityManager.createQuery("SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        return count > 0;
    }

    public User save(User user) {
        if (user.getId() == null) {
            entityManager.persist(user);
            return user;
        } else {
            return entityManager.merge(user);
        }
    }

    public void deleteById(Long id) {
        User user = findById(id);
        if (user != null) {
            entityManager.remove(user);
        }
    }
}
