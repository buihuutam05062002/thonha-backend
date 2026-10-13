package com.thonha.backend.repository;

import com.thonha.backend.dto.request.UserSearchRequest;
import com.thonha.backend.entity.Role;
import com.thonha.backend.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecifications {
    private UserSpecifications() {
    }

    public static Specification<User> search(UserSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (req == null) {
                return cb.conjunction();
            }

            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String like = "%" + escapeLike(req.getKeyword().trim().toLowerCase()) + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), like, '\\'),
                        cb.like(cb.lower(root.get("email")), like, '\\'),
                        cb.like(cb.lower(root.get("phoneNumber")), like, '\\'),
                        cb.like(cb.lower(root.get("username")), like, '\\')));
            }
            if (req.getUserStatus() != null) {
                ps.add(cb.equal(root.get("status"), req.getUserStatus()));
            }
            if (req.getRole() != null && !req.getRole().isBlank()) {
                Join<User, Role> roles = root.join("roles");
                ps.add(cb.equal(roles.get("name"), req.getRole().trim().toUpperCase()));
                query.distinct(true);
            }
            if (req.getCreatedFrom() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), req.getCreatedFrom()));
            }
            if (req.getCreatedTo() != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("createdAt"), req.getCreatedTo()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
