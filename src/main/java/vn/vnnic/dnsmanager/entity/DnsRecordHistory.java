package vn.vnnic.dnsmanager.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "dns_record_history")
public class DnsRecordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "dns_record_id")
    private Long dnsRecordId;


    @Column(
            name = "domain_id",
            nullable = false
    )
    private Long domainId;


    @Column(
            name = "action_type",
            nullable = false,
            length = 20
    )
    private String actionType;


    // 
    // BEFORE
    // 

    @Column(name = "before_hostname")
    private String beforeHostname;


    @Column(name = "before_record_type")
    private String beforeRecordType;


    @Column(
            name = "before_value",
            length = 2000
    )
    private String beforeValue;


    @Column(name = "before_ttl")
    private Integer beforeTtl;


    @Column(name = "before_priority")
    private Integer beforePriority;


    // 
    // AFTER
    // 

    @Column(name = "after_hostname")
    private String afterHostname;


    @Column(name = "after_record_type")
    private String afterRecordType;


    @Column(
            name = "after_value",
            length = 2000
    )
    private String afterValue;


    @Column(name = "after_ttl")
    private Integer afterTtl;


    @Column(name = "after_priority")
    private Integer afterPriority;


    // 
    // TIME
    // 

    @Column(
            name = "changed_at",
            nullable = false
    )
    private LocalDateTime changedAt;


    @Column(
            name = "description",
            length = 1000
    )
    private String description;


    public DnsRecordHistory() {
    }


    @PrePersist
    public void prePersist() {

        if (changedAt == null) {
            changedAt = LocalDateTime.now();
        }

        if (actionType != null) {
            actionType =
                    actionType
                            .trim()
                            .toUpperCase();
        }
    }


    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public Long getDnsRecordId() {
        return dnsRecordId;
    }


    public void setDnsRecordId(Long dnsRecordId) {
        this.dnsRecordId = dnsRecordId;
    }


    public Long getDomainId() {
        return domainId;
    }


    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }


    public String getActionType() {
        return actionType;
    }


    public void setActionType(String actionType) {
        this.actionType = actionType;
    }


    public String getBeforeHostname() {
        return beforeHostname;
    }


    public void setBeforeHostname(String beforeHostname) {
        this.beforeHostname = beforeHostname;
    }


    public String getBeforeRecordType() {
        return beforeRecordType;
    }


    public void setBeforeRecordType(String beforeRecordType) {
        this.beforeRecordType = beforeRecordType;
    }


    public String getBeforeValue() {
        return beforeValue;
    }


    public void setBeforeValue(String beforeValue) {
        this.beforeValue = beforeValue;
    }


    public Integer getBeforeTtl() {
        return beforeTtl;
    }


    public void setBeforeTtl(Integer beforeTtl) {
        this.beforeTtl = beforeTtl;
    }


    public Integer getBeforePriority() {
        return beforePriority;
    }


    public void setBeforePriority(Integer beforePriority) {
        this.beforePriority = beforePriority;
    }


    public String getAfterHostname() {
        return afterHostname;
    }


    public void setAfterHostname(String afterHostname) {
        this.afterHostname = afterHostname;
    }


    public String getAfterRecordType() {
        return afterRecordType;
    }


    public void setAfterRecordType(String afterRecordType) {
        this.afterRecordType = afterRecordType;
    }


    public String getAfterValue() {
        return afterValue;
    }


    public void setAfterValue(String afterValue) {
        this.afterValue = afterValue;
    }


    public Integer getAfterTtl() {
        return afterTtl;
    }


    public void setAfterTtl(Integer afterTtl) {
        this.afterTtl = afterTtl;
    }


    public Integer getAfterPriority() {
        return afterPriority;
    }


    public void setAfterPriority(Integer afterPriority) {
        this.afterPriority = afterPriority;
    }


    public LocalDateTime getChangedAt() {
        return changedAt;
    }


    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }
}