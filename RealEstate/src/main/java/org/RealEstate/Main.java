package org.RealEstate;


import org.RealEstate.dto.*;
import org.RealEstate.enums.PropertyType;
import org.RealEstate.models.Contract;
import org.RealEstate.service.RealEstate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Scanner;

public class Main {
    private static RealEstate service = RealEstate.getInstance();
    private static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        boolean exit = false;

        while (!exit) {
            showMenu();
            int option = sc.nextInt();

            switch (option) {
                case 1 -> makePayment();
                case 2 -> searchContracts();
                case 3 -> getFinishedContractSummary();
                case 4 -> getUnfinishedContractSummary();
                case 5 -> exit = true;
                default -> System.out.println("Non valid");
            }
        }
    }

    private static void showMenu() {
        System.out.println("Menu");
        System.out.println("1. Make payment");
        System.out.println("2. Search contracts with filters");
        System.out.println("3. Get finished contracts summary");
        System.out.println("4. Get unfinished contracts summary");
        System.out.println("5. Exit");
    }

    private static void makePayment() {
        sc.nextLine();

        System.out.print("Enter contract ID: ");
        long contractId = sc.nextLong();

        System.out.print("Enter amount: ");
        BigDecimal amount = new BigDecimal(sc.next());

        try {
            service.makePayment(new MakePaymentDTO(
                    contractId,
                    amount
            ));

            System.out.println("Payment successful");
        }
        catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void searchContracts() {
        sc.nextLine();

        System.out.print("Enter tenant name: ");
        FilterDTO dto = new FilterDTO(sc.nextLine());

        System.out.print("Filter by property type? Y/N: ");
        String option = sc.nextLine().toUpperCase();
        if (option.equals("Y")) {
            System.out.print("Enter property type (HOUSE/APARTMENT/OFFICE): ");
            dto.setPropertyType(PropertyType.valueOf(sc.next().toUpperCase()));
        }

        sc.nextLine();

        System.out.print("Filter by dates? Y/N: ");
        option = sc.nextLine().toUpperCase();
        if (option.equals("Y")) {
            System.out.print("From (YYYY-MM-DD): ");
            dto.setFromDate(LocalDate.parse(sc.nextLine()));

            System.out.print("To? Y/N: ");
            option = sc.nextLine().toUpperCase();
            if (option.equals("Y")) {
                System.out.print("From (YYYY-MM-DD): ");
                dto.setToDate(LocalDate.parse(sc.nextLine()));
            }
        }

        System.out.print("Filter by amount? Y/N: ");
        option = sc.nextLine().toUpperCase();
        if (option.equals("Y")) {
            System.out.print("From: ");
            dto.setFromAmount(BigDecimal.valueOf(sc.nextDouble()));

            sc.nextLine();

            System.out.print("To? Y/N: ");
            option = sc.nextLine().toUpperCase();
            if (option.equals("Y")) {
                System.out.print("To: ");
                dto.setToAmount(BigDecimal.valueOf(sc.nextDouble()));
            }
        }

        for (Contract contract : service.searchContractsWithFilters(dto)) {
            System.out.println("Contract ID: " + contract.getId());
            System.out.println("Status: " + contract.getStatus());
            System.out.println("Total: " + contract.getTotal());
            System.out.println("Start: " + contract.getStartDate());
            System.out.println("End: " + contract.getEndDate());
            System.out.println("");
        }
    }

    private static void getFinishedContractSummary() {
        sc.nextLine();

        System.out.print("Enter from date (YYYY-MM-DD): ");
        LocalDate fromDate = LocalDate.parse(sc.nextLine());

        System.out.print("Enter to date (YYYY-MM-DD): ");
        LocalDate toDate = LocalDate.parse(sc.nextLine());

        for (FinishedContractSummaryDTO dto : service.getFinishedContractSummary(new DateRangeDTO(fromDate, toDate))) {
            System.out.println("Type: " + dto.getPropertyType());
            System.out.println("Total: " + dto.getTotal());
            System.out.println("Quantity: " + dto.getQuantity());
        }
    }

    private static void getUnfinishedContractSummary() {
        for (UnfinishedContractSummaryDTO dto : service.getUnfinishedContractSummary()) {
            System.out.println("Id: " + dto.getContractId());
            System.out.println("Expected: " + dto.getExpected());
            System.out.println("Actual: " + dto.getActual());
        }
    }
}