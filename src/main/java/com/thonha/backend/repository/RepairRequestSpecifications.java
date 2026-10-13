package com.thonha.backend.repository;

import com.thonha.backend.entity.RepairRequest;
import com.thonha.backend.enums.RepairStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class RepairRequestSpecifications {
    private RepairRequestSpecifications() {
    }


    public static Specification<RepairRequest> forCustomer(Long customerId,
                                                           Collection<RepairStatus> statuses,
                                                           String keyword) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("customer").get("id"), customerId));
            if (statuses != null && !statuses.isEmpty()) {
                ps.add(root.get("status").in(statuses));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + escapeLike(keyword.trim().toLowerCase()) + "%";
                ps.add(cb.like(cb.lower(root.get("requestCode")), like, '\\'));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }


    public static Specification<RepairRequest> forWorker(Long workerId,
                                                         Collection<RepairStatus> statuses,
                                                         String keyword) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("worker").get("id"), workerId));
            if (statuses != null && !statuses.isEmpty()) {
                ps.add(root.get("status").in(statuses));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + escapeLike(keyword.trim().toLowerCase()) + "%";
                ps.add(cb.like(cb.lower(root.get("requestCode")), like, '\\'));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
