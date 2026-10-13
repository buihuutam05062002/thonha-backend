package com.thonha.backend.repository;

import com.thonha.backend.entity.Complaint;
import com.thonha.backend.enums.ComplaintReason;
import com.thonha.backend.enums.ComplaintStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ComplaintSpecifications {
    private ComplaintSpecifications() {
    }

    public static Specification<Complaint> search(ComplaintStatus status, ComplaintReason reason, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (reason != null) {
                ps.add(cb.equal(root.get("reason"), reason));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + escapeLike(keyword.trim().toLowerCase()) + "%";
                var request = root.join("request");
                var complainant = root.join("complainant");
                ps.add(cb.or(
                        cb.like(cb.lower(request.get("requestCode")), like, '\\'),
                        cb.like(cb.lower(complainant.get("fullName")), like, '\\'),
                        cb.like(complainant.get("phoneNumber"), like, '\\')));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
