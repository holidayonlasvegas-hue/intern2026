package vn.vnnic.dnsmanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.vnnic.dnsmanager.entity.DnsRecord;

@Repository
public interface DnsRecordRepository
        extends JpaRepository<DnsRecord, Long> {

    // =====================================================
    // 1. GET ALL RECORDS BY DOMAIN
    // =====================================================

    List<DnsRecord> findByDomainId(
            Long domainId
    );

    // =====================================================
    // 2. GET RECORDS BY DOMAIN + STATUS
    // =====================================================

    List<DnsRecord> findByDomainIdAndStatus(
            Long domainId,
            String status
    );

    // =====================================================
    // 3. COUNT RECORDS BY DOMAIN + STATUS
    // =====================================================

    long countByDomainIdAndStatus(
            Long domainId,
            String status
    );

    // =====================================================
    // 4. CHECK WHETHER DOMAIN STILL HAS RECORDS
    // =====================================================

    boolean existsByDomainId(
            Long domainId
    );

    // =====================================================
    // 5. DASHBOARD
    // Count ACTIVE + INACTIVE, exclude DELETED
    // =====================================================

    long countByStatusNot(
            String status
    );

    // =====================================================
    // 6. SEARCH + FILTER
    //
    // Search by:
    // - domain
    // - hostname
    // - record value
    // - record type
    // =====================================================

    @Query("""
        SELECT r
        FROM DnsRecord r
        WHERE
            (:domainId IS NULL OR r.domain.id = :domainId)

        AND
            (
                :keyword IS NULL
                OR :keyword = ''
                OR LOWER(r.hostname)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(r.recordValue)
                    LIKE LOWER(CONCAT('%', :keyword, '%'))
            )

        AND
            (
                :recordType IS NULL
                OR :recordType = ''
                OR UPPER(r.recordType) = UPPER(:recordType)
            )

        ORDER BY r.id DESC
        """)
    List<DnsRecord> searchRecords(
            @Param("domainId")
            Long domainId,

            @Param("keyword")
            String keyword,

            @Param("recordType")
            String recordType
    );

    // =====================================================
    // 7. DUPLICATE CHECK - CREATE
    //
    // Duplicate key:
    //
    // domain_id
    // + hostname
    // + record_type
    // + record_value
    // =====================================================

    boolean existsByDomainIdAndHostnameAndRecordTypeAndRecordValue(
            Long domainId,
            String hostname,
            String recordType,
            String recordValue
    );

    // =====================================================
    // 8. DUPLICATE CHECK - UPDATE
    //
    // Same duplicate rule as CREATE,
    // but exclude the record currently being updated.
    //
    // Example:
    //
    // Record 10:
    // www | A | 203.119.10.10
    //
    // Updating record 10 without changing these values
    // must NOT be considered a duplicate of itself.
    // =====================================================

    boolean existsByDomainIdAndHostnameAndRecordTypeAndRecordValueAndIdNot(
            Long domainId,
            String hostname,
            String recordType,
            String recordValue,
            Long id
    );
}