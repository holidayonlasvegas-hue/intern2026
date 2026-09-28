package vn.vnnic.dnsmanager.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import vn.vnnic.dnsmanager.entity.DnsRecord;
import vn.vnnic.dnsmanager.entity.DnsRecordHistory;
import vn.vnnic.dnsmanager.repository.DnsRecordHistoryRepository;
//tạo và quản lí
@Service
public class DnsRecordHistoryService {

    private final DnsRecordHistoryRepository historyRepository;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public DnsRecordHistoryService(
            DnsRecordHistoryRepository historyRepository) {

        this.historyRepository = historyRepository;
    }

    // =====================================================
    // 1. SEARCH HISTORY
    // =====================================================

    public List<DnsRecordHistory> searchHistory(
            Long domainId,
            Long recordId,
            String actionType,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        String normalizedAction = normalizeActionType(actionType);

        return historyRepository.searchHistory(
                domainId,
                recordId,
                normalizedAction,
                startTime,
                endTime
        );
    }

    // =====================================================
    // 2. LOG CREATE
    //
    // CREATE:
    // before = null
    // after  = record mới
    // =====================================================

    public void logCreate(DnsRecord record) {

        validateRecord(record);

        DnsRecordHistory history =
                baseHistory(
                        record,
                        "CREATE",
                        "Tạo DNS Record mới"
                );

        setAfter(
                history,
                record
        );

        historyRepository.save(history);
    }

    // =====================================================
    // 3. LOG UPDATE
    //
    // UPDATE:
    // before = trạng thái cũ
    // after  = trạng thái mới
    // =====================================================

    public void logUpdate(
            DnsRecord before,
            DnsRecord after) {

        validateRecord(before);
        validateRecord(after);

        DnsRecordHistory history =
                baseHistory(
                        after,
                        "UPDATE",
                        "Cập nhật DNS Record"
                );

        setBefore(
                history,
                before
        );

        setAfter(
                history,
                after
        );

        historyRepository.save(history);
    }

    // =====================================================
    // 4. LOG DELETE
    //
    // DELETE:
    // before = trạng thái trước khi xóa mềm
    // after  = trạng thái DELETED
    // =====================================================

    public void logDelete(
            DnsRecord before,
            DnsRecord after) {

        validateRecord(before);
        validateRecord(after);

        DnsRecordHistory history =
                baseHistory(
                        after,
                        "DELETE",
                        "Xóa mềm DNS Record"
                );

        setBefore(
                history,
                before
        );

        setAfter(
                history,
                after
        );

        historyRepository.save(history);
    }

    // =====================================================
    // 5. BASE HISTORY
    //
    // Các field dùng chung cho CREATE / UPDATE / DELETE
    // =====================================================

    private DnsRecordHistory baseHistory(
            DnsRecord record,
            String actionType,
            String description) {

        validateRecord(record);

        if (record.getDomain() == null
                || record.getDomain().getId() == null) {

            throw new IllegalArgumentException(
                    "DNS Record History bắt buộc phải có Domain."
            );
        }

        DnsRecordHistory history =
                new DnsRecordHistory();

        history.setDnsRecordId(
                record.getId()
        );

        history.setDomainId(
                record.getDomain().getId()
        );

        history.setActionType(
                actionType
        );

        history.setDescription(
                description
        );

        /*
         * Nếu entity DnsRecordHistory của bạn
         * có @PrePersist để tự set changedAt
         * thì có thể bỏ dòng này.
         *
         * Nếu không có thì giữ lại.
         */
        history.setChangedAt(
                LocalDateTime.now()
        );

        return history;
    }

    // =====================================================
    // 6. SET BEFORE SNAPSHOT
    // =====================================================

    private void setBefore(
            DnsRecordHistory history,
            DnsRecord record) {

        if (history == null
                || record == null) {

            return;
        }

        history.setBeforeHostname(
                record.getHostname()
        );

        history.setBeforeRecordType(
                record.getRecordType()
        );

        history.setBeforeValue(
                record.getRecordValue()
        );

        history.setBeforeTtl(
                record.getTtl()
        );

        history.setBeforePriority(
                record.getPriority()
        );
    }

    // =====================================================
    // 7. SET AFTER SNAPSHOT
    // =====================================================

    private void setAfter(
            DnsRecordHistory history,
            DnsRecord record) {

        if (history == null
                || record == null) {

            return;
        }

        history.setAfterHostname(
                record.getHostname()
        );

        history.setAfterRecordType(
                record.getRecordType()
        );

        history.setAfterValue(
                record.getRecordValue()
        );

        history.setAfterTtl(
                record.getTtl()
        );

        history.setAfterPriority(
                record.getPriority()
        );
    }

    // =====================================================
    // 8. COUNT RECENT CHANGES
    //
    // Dashboard:
    // số thay đổi trong 7 ngày gần nhất
    // =====================================================

    public long countRecentChanges() {

        LocalDateTime sevenDaysAgo =
                LocalDateTime.now()
                        .minusDays(7);

        return historyRepository
                .countByChangedAtAfter(
                        sevenDaysAgo
                );
    }

    // =====================================================
    // 9. NORMALIZE ACTION TYPE
    // =====================================================

    private String normalizeActionType(
            String actionType) {

        if (actionType == null
                || actionType.isBlank()) {

            return null;
        }

        return actionType
                .trim()
                .toUpperCase();
    }

    // =====================================================
    // 10. VALIDATE RECORD
    // =====================================================

    private void validateRecord(
            DnsRecord record) {

        if (record == null) {

            throw new IllegalArgumentException(
                    "DNS Record không được null khi ghi History."
            );
        }
    }
}