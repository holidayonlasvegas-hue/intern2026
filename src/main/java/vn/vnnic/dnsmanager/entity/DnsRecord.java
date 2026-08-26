package vn.vnnic.dnsmanager.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "dns_record",

        indexes = {

                @Index(
                        name = "idx_dns_record_domain",
                        columnList = "domain_id"
                ),

                @Index(
                        name = "idx_dns_record_hostname",
                        columnList = "hostname"
                ),

                @Index(
                        name = "idx_dns_record_type",
                        columnList = "record_type"
                ),

                @Index(
                        name = "idx_dns_record_value",
                        columnList = "record_value"
                ),

                @Index(
                        name = "idx_dns_record_status",
                        columnList = "status"
                )
        }
)
public class DnsRecord {

    // 
    // ID
    // 

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;


    // 
    // DOMAIN
    //
    // DOMAIN 1 - N DNS_RECORD
    // 

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "domain_id",
            nullable = false
    )
    private Domain domain;


    // 
    // HOSTNAME TƯƠNG ĐỐI
    //
    // @
    // www
    // mail
    // api
    // portal
    // 

    @Column(
            name = "hostname",
            nullable = false,
            length = 255
    )
    private String hostname;


    // 
    // RECORD TYPE
    //
    // VERSION 1:
    //
    // A
    // AAAA
    // CNAME
    // MX
    // NS
    // TXT
    // 

    @Column(
            name = "record_type",
            nullable = false,
            length = 20
    )
    private String recordType;


    // 
    // RECORD VALUE
    // 

    @Column(
            name = "record_value",
            nullable = false,
            length = 2000
    )
    private String recordValue;


    // 
    // TTL
    // 

    @Column(
            name = "ttl",
            nullable = false
    )
    private Integer ttl = 3600;


    // 
    // PRIORITY
    //
    // Dùng cho MX
    // 

    @Column(
            name = "priority"
    )
    private Integer priority;


    // 
    // STATUS
    //
    // ACTIVE
    // INACTIVE
    // DELETED
    // 

    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private String status = "ACTIVE";


    // 
    // DESCRIPTION
    // 

    @Column(
            name = "description",
            length = 1000
    )
    private String description;


    // 
    // CREATED AT
    // 

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;


    // 
    // UPDATED AT
    // 

    @Column(
            name = "updated_at"
    )
    private LocalDateTime updatedAt;


    // 
    // DELETED AT
    // 

    @Column(
            name = "deleted_at"
    )
    private LocalDateTime deletedAt;


    // 
    // CONSTRUCTOR
    // 

    public DnsRecord() {
    }


    // 
    // BEFORE INSERT
    // 

    @PrePersist
    public void prePersist() {

        createdAt =
                LocalDateTime.now();


        normalize();
    }


    // 
    // BEFORE UPDATE
    // 

    @PreUpdate
    public void preUpdate() {

        updatedAt =
                LocalDateTime.now();


        normalize();
    }


    // 
    // NORMALIZE
    // 

    private void normalize() {


        // HOSTNAME

        if (hostname == null
                || hostname.isBlank()
                || "@".equals(
                        hostname.trim()
                )) {

            hostname = "@";

        } else {

            hostname =
                    hostname
                            .trim()
                            .toLowerCase();
        }


        // RECORD TYPE

        if (recordType != null) {

            recordType =
                    recordType
                            .trim()
                            .toUpperCase();
        }


        // VALUE

        if (recordValue != null) {

            recordValue =
                    recordValue.trim();
        }


        // TTL

        if (ttl == null) {

            ttl = 3600;
        }


        // STATUS

        if (status == null
                || status.isBlank()) {

            status = "ACTIVE";

        } else {

            status =
                    status
                            .trim()
                            .toUpperCase();
        }


        // DESCRIPTION

        if (description != null) {

            description =
                    description.trim();


            if (description.isBlank()) {

                description = null;
            }
        }


        // Chỉ MX dùng priority

        if (!"MX".equals(recordType)) {

            priority = null;
        }
    }


    // 
    // GETTERS / SETTERS
    // 

    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public Domain getDomain() {
        return domain;
    }


    public void setDomain(
            Domain domain) {

        this.domain = domain;
    }


    public String getHostname() {
        return hostname;
    }


    public void setHostname(
            String hostname) {

        this.hostname = hostname;
    }


    public String getRecordType() {
        return recordType;
    }


    public void setRecordType(
            String recordType) {

        this.recordType = recordType;
    }


    public String getRecordValue() {
        return recordValue;
    }


    public void setRecordValue(
            String recordValue) {

        this.recordValue = recordValue;
    }


    public Integer getTtl() {
        return ttl;
    }


    public void setTtl(
            Integer ttl) {

        this.ttl = ttl;
    }


    public Integer getPriority() {
        return priority;
    }


    public void setPriority(
            Integer priority) {

        this.priority = priority;
    }


    public String getStatus() {
        return status;
    }


    public void setStatus(
            String status) {

        this.status = status;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(
            String description) {

        this.description = description;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    public void setCreatedAt(
            LocalDateTime createdAt) {

        this.createdAt = createdAt;
    }


    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }


    public void setUpdatedAt(
            LocalDateTime updatedAt) {

        this.updatedAt = updatedAt;
    }


    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }


    public void setDeletedAt(
            LocalDateTime deletedAt) {

        this.deletedAt = deletedAt;
    }

}