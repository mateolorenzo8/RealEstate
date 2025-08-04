package org.RealEstate.service;

import org.RealEstate.dto.*;
import org.RealEstate.enums.PropertyType;
import org.RealEstate.enums.Status;
import org.RealEstate.models.Contract;
import org.RealEstate.models.Payment;
import org.RealEstate.utils.HibernateUtil;
import org.hibernate.Session;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class RealEstateTest {
    Session session;
    RealEstate service;
    Contract contractCompleted;
    Contract contractActive;
    Contract contractOverdue;
    Payment p1;
    Payment p2;

    @BeforeEach
    void setUp() {
        session = HibernateUtil.getSession();
        service = RealEstate.getInstance();

        contractCompleted = new Contract(
                "Mateo",
                PropertyType.HOUSE,
                new BigDecimal(700),
                LocalDate.now().minusYears(2),
                LocalDate.now(),
                Status.COMPLETED
        );

        contractActive = new Contract(
                "Mateo",
                PropertyType.HOUSE,
                new BigDecimal(500),
                LocalDate.now().minusYears(2),
                LocalDate.now().plusYears(1),
                Status.ACTIVE
        );

        contractOverdue = new Contract(
                "Mateo",
                PropertyType.HOUSE,
                new BigDecimal(500),
                LocalDate.now().minusYears(2),
                LocalDate.now().minusYears(1),
                Status.ACTIVE
        );

        p1 = new Payment(
                contractActive,
                LocalDate.now(),
                new BigDecimal("10")
        );

        p2 = new Payment(
                contractOverdue,
                LocalDate.now(),
                new BigDecimal("10")
        );

        session.beginTransaction();
        session.persist(contractCompleted);
        session.persist(contractActive);
        session.persist(contractOverdue);
        session.persist(p1);
        session.persist(p2);
        session.getTransaction().commit();
    }

    @AfterEach
    void tearDown() {
        if (session != null && session.isOpen()) {
            session.beginTransaction();
            session.createQuery("delete from Payment").executeUpdate();
            session.createQuery("delete from Contract").executeUpdate();
            session.getTransaction().commit();
            session.close();
        }
    }

    @Test
    void testMakePaymentUnexistingContract() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.makePayment(new MakePaymentDTO(
                    10000L,
                    new BigDecimal(100)
            ));
        });
    }

    @Test
    void testMakePaymentCompletedContract() {
        assertThrows(RuntimeException.class, () -> {
            service.makePayment(new MakePaymentDTO(
                    contractCompleted.getId(),
                    new BigDecimal(100)
            ));
        });
    }

    @Test
    void testMakePaymentFully() {
        Payment res = service.makePayment(new MakePaymentDTO(
                contractActive.getId(),
                new BigDecimal(20000)
        ));

        assertNotNull(res);
        assertEquals(contractActive.getId(), res.getContract().getId());
        assertEquals(Status.COMPLETED, res.getContract().getStatus());
    }

    @Test
    void testMakePaymentInOverdue() {
        Payment res = service.makePayment(new MakePaymentDTO(
                contractOverdue.getId(),
                new BigDecimal(100)
        ));

        assertNotNull(res);
        assertEquals(contractOverdue.getId(), res.getContract().getId());
        assertEquals(Status.OVERDUE, res.getContract().getStatus());
    }

    @Test
    void testSearchContractsWithAllFilters() {
        FilterDTO filterDTO = new FilterDTO("M");
        filterDTO.setPropertyType(PropertyType.HOUSE);
        filterDTO.setFromAmount(new BigDecimal(100));
        filterDTO.setToAmount(new BigDecimal(2000));
        filterDTO.setFromDate(LocalDate.now().minusYears(3));
        filterDTO.setToDate(LocalDate.now().plusYears(3));

        List<Contract> res = service.searchContractsWithFilters(filterDTO);

        assertNotNull(res);
        assertEquals(3, res.size());
        assertEquals(contractCompleted.getId(), res.get(0).getId());
        assertEquals(contractActive.getId(), res.get(1).getId());
        assertEquals(contractOverdue.getId(), res.get(2).getId());
    }

    @Test
    void testSearchContractsWithFromFilters() {
        FilterDTO filterDTO = new FilterDTO("M");
        filterDTO.setFromAmount(new BigDecimal(100));
        filterDTO.setFromDate(LocalDate.now().minusYears(3));

        List<Contract> res = service.searchContractsWithFilters(filterDTO);

        assertNotNull(res);
        assertEquals(3, res.size());
        assertEquals(contractCompleted.getId(), res.get(0).getId());
        assertEquals(contractActive.getId(), res.get(1).getId());
        assertEquals(contractOverdue.getId(), res.get(2).getId());
    }

    @Test
    void testGetFinishedContractSummary() {
        List<FinishedContractSummaryDTO> res = service.getFinishedContractSummary(new DateRangeDTO(
                LocalDate.now().minusYears(3),
                LocalDate.now().plusYears(3)
        ));

        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals(PropertyType.HOUSE, res.get(0).getPropertyType());
        assertEquals(0, contractCompleted.getTotal().compareTo(res.get(0).getTotal()));
        assertEquals(1, res.get(0).getQuantity());
    }

    @Test
    void testUnfinishedContractSummary() {
        List<UnfinishedContractSummaryDTO> res = service.getUnfinishedContractSummary();

        assertNotNull(res);
        assertEquals(2, res.size());
        assertEquals(contractActive.getId(), res.get(0).getContractId());
        assertEquals(0, contractActive.getTotal().compareTo(res.get(0).getExpected()));
        assertEquals(0, res.get(0).getActual().compareTo(new BigDecimal(10)));
        assertEquals(contractOverdue.getId(), res.get(1).getContractId());
        assertEquals(0, contractOverdue.getTotal().compareTo(res.get(1).getExpected()));
        assertEquals(0, res.get(1).getActual().compareTo(new BigDecimal(10)));
    }
}