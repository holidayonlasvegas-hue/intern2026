package vn.vnnic.dnsmanager.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import vn.vnnic.dnsmanager.entity.DnsRecord;
import vn.vnnic.dnsmanager.entity.DnsRecordHistory;
import vn.vnnic.dnsmanager.repository.DnsRecordHistoryRepository;

@Service
public class DnsRecordHistoryService {

    private final DnsRecordHistoryRepository historyRepository;


    public DnsRecordHistoryService(
            DnsRecordHistoryRepository historyRepository) {

        this.historyRepository = historyRepository;
    }


    // 
    // SEARCH
    // 

    public List<DnsRecordHistory> searchHistory(
            Long domainId,
            Long recordId,
            String actionType,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        String normalizedAction = null;

        if (actionType != null
                && !actionType.isBlank()) {

            normalizedAction =
                    actionType
                            .trim()
                            .toUpperCase();
        }


        return historyRepository.searchHistory(
                domainId,
                recordId,
                normalizedAction,
                startTime,
                endTime
        );
    }


    // 
    // CREATE
    // 

    public void logCreate(
            DnsRecord record) {

        DnsRecordHistory history =
                baseHistory(
                        record,
                        "CREATE",
                        "Tạo DNS Record mới"
                );


        // CREATE không có BEFORE

        setAfter(
                history,
                record
        );


        historyRepository.save(
                history
        );
    }


    // 
    // UPDATE
    // 

    public void logUpdate(
            DnsRecord before,
            DnsRecord after) {

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


        historyRepository.save(
                history
        );
    }


    // 
    // DELETE
    // 

    public void logDelete(
            DnsRecord before,
            DnsRecord after) {

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


        historyRepository.save(
                history
        );
    }


    // 
    // BASE
    // 

    private DnsRecordHistory baseHistory(
            DnsRecord record,
            String action,
            String description) {

        DnsRecordHistory history =
                new DnsRecordHistory();


        history.setDnsRecordId(
                record.getId()
        );


        history.setDomainId(
                record.getDomain().getId()
        );


        history.setActionType(
                action
        );


        history.setDescription(
                description
        );


        return history;
    }


    // 
    // BEFORE
    // 

    private void setBefore(
            DnsRecordHistory history,
            DnsRecord record) {

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


    // 
    // AFTER
    // 

    private void setAfter(
            DnsRecordHistory history,
            DnsRecord record) {

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


    // 
    // RECENT CHANGES
    // 

    public long countRecentChanges() {

        return historyRepository
                .countByChangedAtAfter(
                        LocalDateTime.now()
                                .minusDays(7)
                );
    }
}