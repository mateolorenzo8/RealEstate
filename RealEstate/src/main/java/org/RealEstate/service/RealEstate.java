package org.RealEstate.service;

import jakarta.persistence.criteria.*;
import org.RealEstate.enums.PropertyType;
import org.RealEstate.enums.Status;
import org.RealEstate.models.*;
import org.RealEstate.dto.*;
import org.RealEstate.utils.HibernateUtil;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
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
        List<Object[]> queryResult = new ArrayList<>();

        try (Session session = HibernateUtil.getSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
            Root<Contract> root = cq.from(Contract.class);

            cq.multiselect(
                    root.get("propertyType"),
                    cb.count(root),
                    root.get("monthlyRent"),
                    root.get("startDate"),
                    root.get("endDate")
            ).where(
                    cb.equal(root.get("status"), Status.COMPLETED),
                    cb.between(root.get("startDate"), dto.getFromDate(), dto.getToDate())
            ).groupBy(
                    root.get("id")
            );

            queryResult = session.createQuery(cq).getResultList();
        }

        List<FinishedContractSummaryDTO> result = new ArrayList<>();

        for (Object[] object : queryResult) {
            BigDecimal monthlyRent = (BigDecimal) object[2];
            LocalDate from = (LocalDate) object[3];
            LocalDate to = (LocalDate) object[4];

            BigDecimal months = new BigDecimal(ChronoUnit.MONTHS.between(from, to));

            result.add(new FinishedContractSummaryDTO(
                    (PropertyType) object[0],
                    (long) object[1],
                    monthlyRent.multiply(months)
            ));
        }

        return result;
    }

    public List<UnfinishedContractSummaryDTO> getUnfinishedContractSummary() {
        List<Object[]> query = new ArrayList<>();

        try (Session session = HibernateUtil.getSession()) {
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);
            Root<Payment> root = cq.from(Payment.class);

            cq.multiselect(
                    root.get("contract").get("id"),
                    root.get("contract").get("monthlyRent"),
                    root.get("contract").get("startDate"),
                    root.get("contract").get("endDate"),
                    cb.sum(root.get("amount"))
            ).where(
                    cb.notEqual(root.get("contract").get("status"), Status.COMPLETED)
            ).groupBy(
                    root.get("contract").get("id")
            );

            query = session.createQuery(cq).getResultList();
        }

        List<UnfinishedContractSummaryDTO> dto = new ArrayList<>();

        for (Object[] obj : query) {
            LocalDate from = (LocalDate) obj[2];
            LocalDate to = (LocalDate) obj[3];
            BigDecimal monthlyRent = (BigDecimal) obj[1];
            BigDecimal totalMonths = new BigDecimal(ChronoUnit.MONTHS.between(from, to));

            dto.add(new  UnfinishedContractSummaryDTO(
                    (long) obj[0],
                    monthlyRent.multiply(totalMonths),
                    (BigDecimal) obj[4]
            ));
        }

        return dto;
    }
}
