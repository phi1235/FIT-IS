package com.example.user.repository;

import com.example.user.dto.UserProfileProjection;
import com.example.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    @Query(value = "SELECT d.name AS departmentName, uo.position " +
                   "FROM usr.user_organization uo " +
                   "JOIN usr.department d ON d.id = uo.department_id " +
                   "JOIN auth.auth_user u ON u.id = uo.user_id " +
                   "WHERE u.username = :username AND uo.is_primary = true " +
                   "LIMIT 1",
           nativeQuery = true)
    List<UserProfileProjection> findProfileByUsername(@Param("username") String username);
}
