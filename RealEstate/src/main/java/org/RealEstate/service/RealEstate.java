package org.RealEstate.service;

import jakarta.persistence.criteria.*;
import org.RealEstate.enums.Status;
import org.RealEstate.models.*;
import org.RealEstate.dto.*;
import org.RealEstate.utils.HibernateUtil;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class RealEstate {
    private static volatile RealEstate instance;

    private RealEstate() {}

    public static RealEstate getInstance() {
        if (instance == null) {
            synchronized (RealEstate.class) {
                if (instance == null) {
                    instance = new RealEstate();
                }
            }
        }
        return instance;
    }

    public Payment makePayment(MakePaymentDTO dto) {
        Contract contract;

        try (Session session = HibernateUtil.getSession()) {
            contract = session.get(Contract.class, dto.getContractId());

            if (contract == null) throw new IllegalArgumentException("Not found contract with id: " + dto.getContractId());
        }

        if (contract.getStatus() == Status.COMPLETED) throw new RuntimeException("Contract paid fully");

        Payment payment = new Payment(
                contract,
                LocalDate.now(),
                dto.getAmount()
        );

        try (Session session = HibernateUtil.getSession()) {
            session.beginTransaction();
            session.persist(payment);
            session.getTransaction().commit();
        }

        BigDecimal paid;

        try (Session session = HibernateUtil.getSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<BigDecimal> cq = cb.createQuery(BigDecimal.class);
            Root<Payment> root = cq.from(Payment.class);

            cq.select(
                    cb.sum(root.get("amount"))
            ).where(
                    cb.equal(root.get("contract").get("id"), dto.getContractId())
            ).groupBy(
                    root.get("contract").get("id")
            );

            paid = session.createQuery(cq).getSingleResult();
        }

        if (paid.compareTo(contract.getTotal()) >= 0) {
            contract.setStatus(Status.COMPLETED);
        }
        else if (contract.getEndDate().isBefore(LocalDate.now())) {
            contract.setStatus(Status.OVERDUE);
        }

        try (Session session = HibernateUtil.getSession()) {
            session.beginTransaction();
            session.merge(payment);
            session.getTransaction().commit();
        }

        return payment;
    }

    public List<Contract> searchContractsWithFilters(FilterDTO dto) {
        try (Session session = HibernateUtil.getSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Contract> cq = cb.createQuery(Contract.class);
            Root<Contract> root = cq.from(Contract.class);

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.like(root.get("tenantName"), "%" + dto.getClientName() + "%"));

            if (dto.getPropertyType() != null) predicates.add(cb.equal(root.get("propertyType"), dto.getPropertyType()));

            if (dto.getFromDate() != null) {
                if (dto.getToDate() != null) {
                    predicates.add(cb.between(root.get("startDate"), dto.getFromDate(), dto.getToDate()));
                }
                else {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), dto.getFromDate()));
                }
            }

            if (dto.getFromAmount() != null) {
                if (dto.getToAmount() != null) {
                    predicates.add(cb.between(root.get("monthlyRent"), dto.getFromAmount(), dto.getToAmount()));
                }
                else {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("monthlyRent"), dto.getFromAmount()));
                }
            }

            cq.where(predicates.toArray(new Predicate[predicates.size()]));
            cq.orderBy(cb.desc(root.get("startDate")));

            return session.createQuery(cq).getResultList();
        }
    }

    public List<FinishedContractSummaryDTO> getFinishedContractSummary(DateRangeDTO dto) {
        try (Session session = HibernateUtil.getSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<FinishedContractSummaryDTO> cq = cb.createQuery(FinishedContractSummaryDTO.class);
            Root<Contract> root = cq.from(Contract.class);

            Expression<Integer> years = cb.diff(
                    cb.function("YEAR", Integer.class, root.get("endDate")),
                    cb.function("YEAR", Integer.class, root.get("startDate"))
            );

            Expression<Integer> months = cb.abs(cb.diff(
                    cb.function("MONTH", Integer.class, root.get("endDate")),
                    cb.function("MONTH", Integer.class, root.get("startDate"))
            ));

            Expression<Integer> totalMonths = cb.sum(
                    cb.prod(years, cb.literal(12)),
                    months
            );

            cq.multiselect(
                    root.get("propertyType"),
                    cb.count(root),
                    cb.prod(root.get("monthlyRent"), totalMonths)
            ).where(
                    cb.equal(root.get("status"), Status.COMPLETED),
                    cb.between(root.get("startDate"), dto.getFromDate(), dto.getToDate())
            ).groupBy(
                    root.get("id")
            );

            return session.createQuery(cq).getResultList();
        }
    }

    public List<UnfinishedContractSummaryDTO> getUnfinishedContractSummary() {
        try (Session session = HibernateUtil.getSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<UnfinishedContractSummaryDTO> cq = cb.createQuery(UnfinishedContractSummaryDTO.class);
            Root<Payment> root = cq.from(Payment.class);

            Expression<Integer> years = cb.diff(
                    cb.function("YEAR", Integer.class, root.get("contract").get("endDate")),
                    cb.function("YEAR", Integer.class, root.get("contract").get("startDate"))
            );

            Expression<Integer> months = cb.abs(cb.diff(
                    cb.function("MONTH", Integer.class, root.get("contract").get("endDate")),
                    cb.function("MONTH", Integer.class, root.get("contract").get("startDate"))
            ));

            Expression<Integer> totalMonths = cb.sum(
                    cb.prod(years, cb.literal(12)),
                    months
            );

            cq.multiselect(
                    root.get("contract").get("id"),
                    cb.prod(root.get("contract").get("monthlyRent"), totalMonths),
                    cb.sum(root.get("amount"))
            ).where(
                    cb.notEqual(root.get("contract").get("status"), Status.COMPLETED)
            ).groupBy(
                    root.get("contract").get("id")
            );

            return session.createQuery(cq).getResultList();
        }
    }
}
