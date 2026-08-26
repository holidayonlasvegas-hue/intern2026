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


    // 
    // 1. LẤY TẤT CẢ DNS RECORD THEO DOMAIN
    // 

    List<DnsRecord> findByDomainId(
            Long domainId
    );


    // 
    // 2. LẤY DNS RECORD THEO DOMAIN + STATUS
    // 

    List<DnsRecord> findByDomainIdAndStatus(
            Long domainId,
            String status
    );


    // 
    // 3. ĐẾM RECORD THEO DOMAIN + STATUS
    //
    // Ví dụ:
    // count ACTIVE record của example.vn
    // 

    long countByDomainIdAndStatus(
            Long domainId,
            String status
    );


    // 
    // 4. KIỂM TRA DOMAIN CÓ CÒN DNS RECORD KHÔNG
    //
    // DomainService dùng method này trước khi hard delete.
    //
    // Nếu domain vẫn còn bất kỳ DNS Record nào
    // trong database thì trả về true.
    // 

    boolean existsByDomainId(
            Long domainId
    );


    // 
    // 5. DASHBOARD
    //
    // Đếm tất cả record có status khác DELETED.
    //
    // ACTIVE   -> tính
    // INACTIVE -> tính
    // DELETED  -> không tính
    // 

    long countByStatusNot(
            String status
    );


    // 
    // 6. SEARCH + FILTER
    //
    // Có thể kết hợp:
    //
    // domainId
    // keyword
    // recordType
    //
    // keyword tìm trong:
    //
    // hostname
    // recordValue
    // 

    @Query("""
        SELECT r
        FROM DnsRecord r

        WHERE
            (
                :domainId IS NULL
                OR r.domain.id = :domainId
            )

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
                OR r.recordType = :recordType
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


    // 
    // 7. CHECK DUPLICATE DNS RECORD
    //
    // Duplicate nếu giống cả:
    //
    // domain_id
    // hostname
    // record_type
    // record_value
    //
    // Ví dụ không hợp lệ:
    //
    // www | A | 203.119.10.10
    // www | A | 203.119.10.10
    //
    // Nhưng vẫn cho phép:
    //
    // www | A | 203.119.10.10
    // www | A | 203.119.10.11
    // 

    boolean existsByDomainIdAndHostnameAndRecordTypeAndRecordValue(
            Long domainId,
            String hostname,
            String recordType,
            String recordValue
    );

}