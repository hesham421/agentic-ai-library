package com.erp.fx.entity;

@Entity
@Table(name = "FX_WIDGET",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_FX_WIDGET_CODE", columnNames = {"CODE"}),
        @UniqueConstraint(name = "UQ_FX_WIDGET_NAME", columnNames = {"NAME"})
    },
    indexes = { @Index(name = "IX_FX_WIDGET_PARENT", columnList = "PARENT_ID") }
)
public class FxWidget extends AuditableEntity {

    @Id
    @Column(name = "WIDGET_PK")
    private Long widgetPk;

    @Column(name = "CODE", length = 20, nullable = false)
    private String code;

    @Column(name = "NAME_EN", length = 150)
    private String nameEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ID")
    private FxWidget parent;

    @Column(name = "IS_PRIMARY_FL", nullable = false)
    @Convert(converter = BooleanNumberConverter.class)
    private Boolean isPrimaryFl = Boolean.FALSE;
}
