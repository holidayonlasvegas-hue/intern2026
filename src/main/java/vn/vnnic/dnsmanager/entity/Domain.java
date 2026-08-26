package vn.vnnic.dnsmanager.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "domain",

        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_domain_name",
                        columnNames = "domain_name"
                )
        },

        indexes = {
                @Index(
                        name = "idx_domain_name",
                        columnList = "domain_name"
                ),

                @Index(
                        name = "idx_domain_status",
                        columnList = "status"
                )
        }
)
public class Domain {

    // 
    // ID
    // 

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;


    // 
    // DOMAIN NAME
    // 

    @Column(
            name = "domain_name",
            nullable = false,
            unique = true,
            length = 255
    )
    private String domainName;


    // 
    // DESCRIPTION
    // 

    @Column(
            name = "description",
            length = 1000
    )
    private String description;


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

    public Domain() {
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

        if (domainName != null) {

            domainName =
                    domainName
                            .trim()
                            .toLowerCase();
        }


        if (status == null
                || status.isBlank()) {

            status = "ACTIVE";

        } else {

            status =
                    status
                            .trim()
                            .toUpperCase();
        }


        if (description != null) {

            description =
                    description.trim();


            if (description.isBlank()) {

                description = null;
            }
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


    public String getDomainName() {
        return domainName;
    }


    public void setDomainName(
            String domainName) {

        this.domainName = domainName;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(
            String description) {

        this.description = description;
    }


    public String getStatus() {
        return status;
    }


    public void setStatus(
            String status) {

        this.status = status;
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