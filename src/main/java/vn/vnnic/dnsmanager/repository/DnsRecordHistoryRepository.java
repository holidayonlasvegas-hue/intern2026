package vn.vnnic.dnsmanager.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.vnnic.dnsmanager.entity.DnsRecordHistory;

@Repository
public interface DnsRecordHistoryRepository
        extends JpaRepository<DnsRecordHistory, Long> {


    // 
    // SEARCH + FILTER HISTORY
    // 

    @Query("""
        SELECT h
        FROM DnsRecordHistory h

        WHERE
            (:domainId IS NULL
                OR h.domainId = :domainId)

        AND
            (:recordId IS NULL
                OR h.dnsRecordId = :recordId)

        AND (
            :actionType IS NULL
            OR :actionType = ''
            OR h.actionType = :actionType
        )

        AND (
            :startTime IS NULL
            OR h.changedAt >= :startTime
        )

        AND (
            :endTime IS NULL
            OR h.changedAt <= :endTime
        )

        ORDER BY h.changedAt DESC
        """)
    List<DnsRecordHistory> searchHistory(

            @Param("domainId")
            Long domainId,

            @Param("recordId")
            Long recordId,

            @Param("actionType")
            String actionType,

            @Param("startTime")
            LocalDateTime startTime,

            @Param("endTime")
            LocalDateTime endTime
    );


    // 
    // TOÀN BỘ HISTORY
    // 

    List<DnsRecordHistory>
            findAllByOrderByChangedAtDesc();


    // 
    // DASHBOARD
    // SỐ THAY ĐỔI SAU MỘT THỜI ĐIỂM
    // 

    long countByChangedAtAfter(
            LocalDateTime time
    );

}