package com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity;

import com.qbitspark.buildwisebackend.accounting_service.documentflow.invoice.entity.InvoiceDocEntity;
import jakarta.persistence.*;
import lombok.*;
import org.javamoney.moneta.Money;

import javax.money.MonetaryAmount;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "invoice_line_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private InvoiceDocEntity invoice;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "quantity", precision = 10, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 19, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 19, scale = 4)
    private BigDecimal lineTotal;

    @Column(name = "unit_of_measure")
    private String unitOfMeasure;

    @Column(name = "line_order")
    private Integer lineOrder;

    @Column(name = "taxable")
    @Builder.Default
    private Boolean taxable = true;

    public void calculateLineTotal() {
        this.lineTotal = quantity.multiply(unitPrice);
    }

    // JAVA MONEY VERSION FOR PRECISION
    public void calculateLineTotalWithMoney() {
        MonetaryAmount quantityMoney = Money.of(quantity, "TZS");
        MonetaryAmount unitPriceMoney = Money.of(unitPrice, "TZS");
        MonetaryAmount lineTotalMoney = quantityMoney.multiply(unitPriceMoney.getNumber());

        this.lineTotal = lineTotalMoney.getNumber().numberValue(BigDecimal.class);
    }
}