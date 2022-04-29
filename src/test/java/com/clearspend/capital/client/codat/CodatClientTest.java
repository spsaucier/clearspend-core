package com.clearspend.capital.client.codat;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.clearspend.capital.client.codat.types.GetAccountsResponsePage;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class CodatClientTest {

  @Test
  @SneakyThrows
  public void getAccountsForBusiness_checkHateoasMapping() {
    ObjectMapper mapper =
        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    GetAccountsResponsePage page = mapper.readValue(PAGED_PAYLOAD, GetAccountsResponsePage.class);

    assertThat(page)
        .isNotNull()
        .matches(it -> page.getPageSize() == page.getResults().size())
        .matches(
            it ->
                page.getLinks()
                    .get("next")
                    .getHref()
                    .equals(
                        "/companies/197c8b68-7d82-400d-b1a9-85682e74cb1d/data/accounts?page=2&pageSize=10"));
  }

  private static final String PAGED_PAYLOAD =
      """
  {
    "results": [
        {
            "id": "84",
            "name": "Accounts Receivable (A/R)",
            "fullyQualifiedCategory": "Asset.Accounts Receivable.AccountsReceivable",
            "fullyQualifiedName": "Asset.Accounts Receivable.AccountsReceivable.Accounts Receivable (A/R)",
            "currency": "USD",
            "currentBalance": 5281.52,
            "type": "Asset",
            "status": "Active",
            "isBankAccount": false,
            "modifiedDate": "2022-03-31T17:31:06Z",
            "sourceModifiedDate": "2022-02-17T21:16:17Z",
            "validDatatypeLinks": []
        },
        {
            "id": "91",
            "nominalCode": "clearspend-credit",
            "name": "ClearSpend card",
            "fullyQualifiedCategory": "Asset.Bank.CashOnHand",
            "fullyQualifiedName": "Asset.Bank.CashOnHand.ClearSpend card",
            "currency": "USD",
            "currentBalance": 0.00,
            "type": "Asset",
            "status": "Active",
            "isBankAccount": true,
            "modifiedDate": "2022-03-31T17:31:06Z",
            "sourceModifiedDate": "2022-03-15T20:48:52Z",
            "validDatatypeLinks": [
                {
                    "property": "Id",
                    "links": [
                        "Payment.AccountRef.Id",
                        "BillPayment.AccountRef.Id",
                        "Bill.LineItems.AccountRef.Id",
                        "BillCreditNote.LineItems.AccountRef.Id",
                        "JournalEntry.JournalLines.AccountRef.Id",
                        "Item.InvoiceItem.AccountRef.Id",
                        "Item.BillItem.AccountRef.Id"
                    ]
                }
            ]
        },
        {
            "id": "92",
            "nominalCode": "4SFDVQeYqQkOoCsrJ1F0",
            "name": "PRM_UNIQUE_CARD_NAME",
            "fullyQualifiedCategory": "Asset.Bank.CashOnHand",
            "fullyQualifiedName": "Asset.Bank.CashOnHand.PRM_UNIQUE_CARD_NAME",
            "currency": "USD",
            "currentBalance": -31.64,
            "type": "Asset",
            "status": "Active",
            "isBankAccount": true,
            "modifiedDate": "2022-04-08T20:45:19Z",
            "sourceModifiedDate": "2022-04-07T14:21:20Z",
            "validDatatypeLinks": [
                {
                    "property": "Id",
                    "links": [
                        "Payment.AccountRef.Id",
                        "BillPayment.AccountRef.Id",
                        "Bill.LineItems.AccountRef.Id",
                        "BillCreditNote.LineItems.AccountRef.Id",
                        "JournalEntry.JournalLines.AccountRef.Id",
                        "Item.InvoiceItem.AccountRef.Id",
                        "Item.BillItem.AccountRef.Id"
                    ]
                }
            ]
        },
        {
            "id": "35",
            "name": "Checking",
            "fullyQualifiedCategory": "Asset.Bank.Checking",
            "fullyQualifiedName": "Asset.Bank.Checking.Checking",
            "currency": "USD",
            "currentBalance": 700.00,
            "type": "Asset",
            "status": "Active",
            "isBankAccount": true,
            "modifiedDate": "2022-04-28T20:45:31Z",
            "sourceModifiedDate": "2022-04-28T20:18:08Z",
            "validDatatypeLinks": [
                {
                    "property": "Id",
                    "links": [
                        "Payment.AccountRef.Id",
                        "BillPayment.AccountRef.Id",
                        "Bill.LineItems.AccountRef.Id",
                        "BillCreditNote.LineItems.AccountRef.Id",
                        "JournalEntry.JournalLines.AccountRef.Id",
                        "Item.InvoiceItem.AccountRef.Id",
                        "Item.BillItem.AccountRef.Id"
                    ]
                }
            ]
        },
        {
            "id": "36",
            "name": "Savings",
            "fullyQualifiedCategory": "Asset.Bank.Savings",
            "fullyQualifiedName": "Asset.Bank.Savings.Savings",
            "currency": "USD",
            "currentBalance": 800.00,
            "type": "Asset",
            "status": "Active",
            "isBankAccount": true,
            "modifiedDate": "2022-03-31T17:31:06Z",
            "sourceModifiedDate": "2022-02-17T21:00:56Z",
            "validDatatypeLinks": [
                {
                    "property": "Id",
                    "links": [
                        "Payment.AccountRef.Id",
                        "BillPayment.AccountRef.Id",
                        "Bill.LineItems.AccountRef.Id",
                        "BillCreditNote.LineItems.AccountRef.Id",
                        "JournalEntry.JournalLines.AccountRef.Id",
                        "Item.InvoiceItem.AccountRef.Id",
                        "Item.BillItem.AccountRef.Id"
                    ]
                }
            ]
        },
        {
            "id": "39",
            "name": "Depreciation",
            "fullyQualifiedCategory": "Asset.Fixed Asset.AccumulatedDepreciation.Truck",
            "fullyQualifiedName": "Asset.Fixed Asset.AccumulatedDepreciation.Truck.Depreciation",
            "currency": "USD",
            "currentBalance": 0.00,
            "type": "Asset",
            "status": "Active",
            "isBankAccount": false,
            "modifiedDate": "2022-03-31T17:31:06Z",
            "sourceModifiedDate": "2022-02-13T20:11:07Z",
            "validDatatypeLinks": [
                {
                    "property": "Id",
                    "links": [
                        "Bill.LineItems.AccountRef.Id",
                        "BillCreditNote.LineItems.AccountRef.Id",
                        "JournalEntry.JournalLines.AccountRef.Id",
                        "Item.InvoiceItem.AccountRef.Id",
                        "Item.BillItem.AccountRef.Id"
                    ]
                }
            ]
        },
        {
            "id": "37",
            "name": "Truck",
            "fullyQualifiedCategory": "Asset.Fixed Asset.Vehicles",
            "fullyQualifiedName": "Asset.Fixed Asset.Vehicles.Truck",
            "currency": "USD",
            "currentBalance": 0.00,
            "type": "Asset",
            "status": "Active",
            "isBankAccount": false,
            "modifiedDate": "2022-03-31T17:31:06Z",
            "sourceModifiedDate": "2022-02-13T20:11:07Z",
            "validDatatypeLinks": [
                {
                    "property": "Id",
                    "links": [
                        "Bill.LineItems.AccountRef.Id",
                        "BillCreditNote.LineItems.AccountRef.Id",
                        "JournalEntry.JournalLines.AccountRef.Id",
                        "Item.InvoiceItem.AccountRef.Id",
                        "Item.BillItem.AccountRef.Id"
                    ]
                }
            ]
        },
        {
            "id": "38",
            "name": "Original Cost",
            "fullyQualifiedCategory": "Asset.Fixed Asset.Vehicles.Truck",
            "fullyQualifiedName": "Asset.Fixed Asset.Vehicles.Truck.Original Cost",
            "currency": "USD",
            "currentBalance": 13495.00,
            "type": "Asset",
            "status": "Active",
            "isBankAccount": false,
            "modifiedDate": "2022-03-31T17:31:06Z",
            "sourceModifiedDate": "2022-02-13T20:11:06Z",
            "validDatatypeLinks": [
                {
                    "property": "Id",
                    "links": [
                        "Bill.LineItems.AccountRef.Id",
                        "BillCreditNote.LineItems.AccountRef.Id",
                        "JournalEntry.JournalLines.AccountRef.Id",
                        "Item.InvoiceItem.AccountRef.Id",
                        "Item.BillItem.AccountRef.Id"
                    ]
                }
            ]
        },
        {
            "id": "81",
            "name": "Inventory Asset",
            "fullyQualifiedCategory": "Asset.Other Current Asset.Inventory",
            "fullyQualifiedName": "Asset.Other Current Asset.Inventory.Inventory Asset",
            "currency": "USD",
            "currentBalance": 596.25,
            "type": "Asset",
            "status": "Active",
            "isBankAccount": false,
            "modifiedDate": "2022-03-31T17:31:06Z",
            "sourceModifiedDate": "2022-02-17T21:16:17Z",
            "validDatatypeLinks": [
                {
                    "property": "Id",
                    "links": [
                        "Payment.AccountRef.Id",
                        "Bill.LineItems.AccountRef.Id",
                        "BillCreditNote.LineItems.AccountRef.Id",
                        "JournalEntry.JournalLines.AccountRef.Id",
                        "Item.InvoiceItem.AccountRef.Id",
                        "Item.BillItem.AccountRef.Id"
                    ]
                }
            ]
        },
        {
            "id": "32",
            "name": "Uncategorized Asset",
            "fullyQualifiedCategory": "Asset.Other Current Asset.OtherCurrentAssets",
            "fullyQualifiedName": "Asset.Other Current Asset.OtherCurrentAssets.Uncategorized Asset",
            "currency": "USD",
            "currentBalance": 0.00,
            "type": "Asset",
            "status": "Active",
            "isBankAccount": false,
            "modifiedDate": "2022-03-31T17:31:06Z",
            "sourceModifiedDate": "2022-02-09T22:46:30Z",
            "validDatatypeLinks": [
                {
                    "property": "Id",
                    "links": [
                        "Payment.AccountRef.Id",
                        "Bill.LineItems.AccountRef.Id",
                        "BillCreditNote.LineItems.AccountRef.Id",
                        "JournalEntry.JournalLines.AccountRef.Id",
                        "Item.InvoiceItem.AccountRef.Id",
                        "Item.BillItem.AccountRef.Id"
                    ]
                }
            ]
        }
    ],
    "pageNumber": 1,
    "pageSize": 10,
    "totalResults": 92,
    "_links": {
        "current": {
            "href": "/companies/197c8b68-7d82-400d-b1a9-85682e74cb1d/data/accounts?page=1&pageSize=10"
        },
        "self": {
            "href": "/companies/197c8b68-7d82-400d-b1a9-85682e74cb1d/data/accounts"
        },
        "next": {
            "href": "/companies/197c8b68-7d82-400d-b1a9-85682e74cb1d/data/accounts?page=2&pageSize=10"
        }
    }
}
""";
}
