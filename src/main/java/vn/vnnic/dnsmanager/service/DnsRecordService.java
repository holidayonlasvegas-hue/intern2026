package vn.vnnic.dnsmanager.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.vnnic.dnsmanager.entity.DnsRecord;
import vn.vnnic.dnsmanager.entity.Domain;
import vn.vnnic.dnsmanager.repository.DnsRecordRepository;
import vn.vnnic.dnsmanager.validation.DnsRecordValidator;

@Service
public class DnsRecordService {

    private static final Logger logger =
            LoggerFactory.getLogger(DnsRecordService.class);

    private final DnsRecordRepository dnsRecordRepository;
    private final DomainService domainService;
    private final DnsRecordValidator dnsRecordValidator;
    private final DnsRecordHistoryService historyService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public DnsRecordService(
            DnsRecordRepository dnsRecordRepository,
            DomainService domainService,
            DnsRecordValidator dnsRecordValidator,
            DnsRecordHistoryService historyService) {

        this.dnsRecordRepository = dnsRecordRepository;
        this.domainService = domainService;
        this.dnsRecordValidator = dnsRecordValidator;
        this.historyService = historyService;
    }

    // =====================================================
    // 1. GET ALL DNS RECORDS
    // =====================================================

    public List<DnsRecord> getAllRecords() {

        logger.debug("Loading all DNS records");

        return dnsRecordRepository.findAll();
    }

    // =====================================================
    // 2. GET RECORDS BY DOMAIN
    // =====================================================

    public List<DnsRecord> getRecordsByDomain(
            Long domainId) {

        logger.debug(
                "Loading DNS records for domainId={}",
                domainId
        );

        return dnsRecordRepository
                .findByDomainId(domainId);
    }

    // =====================================================
    // 3. GET RECORD DETAIL
    // =====================================================

    public DnsRecord getRecordById(
            Long id) {

        return dnsRecordRepository
                .findById(id)
                .orElseThrow(() -> {

                    logger.warn(
                            "DNS record not found: id={}",
                            id
                    );

                    return new IllegalArgumentException(
                            "Không tìm thấy DNS Record có ID: " + id
                    );
                });
    }

    // =====================================================
    // 4. COUNT ACTIVE RECORDS BY DOMAIN
    // =====================================================

    public long countActiveRecords(
            Long domainId) {

        return dnsRecordRepository
                .countByDomainIdAndStatus(
                        domainId,
                        "ACTIVE"
                );
    }

    // =====================================================
    // 5. DASHBOARD
    // COUNT RECORDS EXCEPT DELETED
    // =====================================================

    public long countManagedRecords() {

        return dnsRecordRepository
                .countByStatusNot("DELETED");
    }

    // =====================================================
    // 6. CREATE DNS RECORD
    // =====================================================

    @Transactional
    public DnsRecord createRecord(
            Long domainId,
            DnsRecord record) {

        logger.info(
                "Creating DNS record: domainId={}, hostname={}, type={}, value={}",
                domainId,
                record != null ? record.getHostname() : null,
                record != null ? record.getRecordType() : null,
                record != null ? record.getRecordValue() : null
        );

        if (record == null) {

            logger.warn(
                    "DNS record creation rejected: record is null"
            );

            throw new IllegalArgumentException(
                    "DNS Record không được để trống."
            );
        }

        // -------------------------------------------------
        // 1. DOMAIN PHẢI TỒN TẠI
        // -------------------------------------------------

        Domain domain =
                domainService.getDomainById(domainId);

        // -------------------------------------------------
        // 2. KHÔNG CHO THÊM RECORD VÀO DOMAIN DELETED
        // -------------------------------------------------

        if ("DELETED".equalsIgnoreCase(
                domain.getStatus())) {

            logger.warn(
                    "DNS record creation rejected: domainId={} is DELETED",
                    domainId
            );

            throw new IllegalStateException(
                    "Không thể thêm DNS Record cho tên miền đã DELETED."
            );
        }

        /*
         * QUAN TRỌNG:
         *
         * Phải gắn Domain vào DNS Record
         * TRƯỚC khi gọi Validator.
         *
         * Validator có rule:
         * record.getDomain() != null
         */
        record.setDomain(domain);

        logger.debug(
                "Domain assigned to DNS record: domainId={}",
                record.getDomain() != null
                        ? record.getDomain().getId()
                        : null
        );

        // -------------------------------------------------
        // 3. NORMALIZE
        // -------------------------------------------------

        normalizeRecord(record);

        // -------------------------------------------------
        // 4. VALIDATE
        // -------------------------------------------------

        try {

            dnsRecordValidator.validate(record);

        } catch (IllegalArgumentException
                | IllegalStateException ex) {

            logger.warn(
                    "DNS record validation failed: domainId={}, hostname={}, type={}, reason={}",
                    domainId,
                    record.getHostname(),
                    record.getRecordType(),
                    ex.getMessage()
            );

            throw ex;
        }

        // -------------------------------------------------
        // 5. DUPLICATE CHECK
        // -------------------------------------------------

        if (isDuplicate(record)) {

            logger.warn(
                    "Duplicate DNS record rejected: domainId={}, hostname={}, type={}, value={}",
                    domainId,
                    record.getHostname(),
                    record.getRecordType(),
                    record.getRecordValue()
            );

            throw new IllegalArgumentException(
                    "DNS Record đã tồn tại."
            );
        }

        // -------------------------------------------------
        // 6. SAVE
        // -------------------------------------------------

        DnsRecord savedRecord =
                dnsRecordRepository
                        .saveAndFlush(record);

        // -------------------------------------------------
        // 7. HISTORY CREATE
        // -------------------------------------------------

        historyService.logCreate(
                savedRecord
        );

        logger.info(
                "DNS record created successfully: id={}, domainId={}, hostname={}, type={}, value={}",
                savedRecord.getId(),
                domainId,
                savedRecord.getHostname(),
                savedRecord.getRecordType(),
                savedRecord.getRecordValue()
        );

        return savedRecord;
    }

    // =====================================================
    // 7. UPDATE DNS RECORD
    // =====================================================

    @Transactional
    public DnsRecord updateRecord(
            Long id,
            DnsRecord newRecord) {

        logger.info(
                "Updating DNS record: id={}",
                id
        );

        if (newRecord == null) {

            throw new IllegalArgumentException(
                    "Dữ liệu DNS Record không được để trống."
            );
        }

        DnsRecord current =
                getRecordById(id);

        // -------------------------------------------------
        // KHÔNG CHO SỬA RECORD DELETED
        // -------------------------------------------------

        if ("DELETED".equalsIgnoreCase(
                current.getStatus())) {

            logger.warn(
                    "DNS record update rejected: id={} is DELETED",
                    id
            );

            throw new IllegalStateException(
                    "Không thể cập nhật DNS Record đã DELETED."
            );
        }

        /*
         * Snapshot BEFORE.
         *
         * Không được:
         * DnsRecord before = current;
         *
         * vì hai biến sẽ cùng trỏ một object.
         */
        DnsRecord before =
                copyRecord(current);

        // -------------------------------------------------
        // UPDATE FIELDS
        // -------------------------------------------------

        current.setHostname(
                newRecord.getHostname()
        );

        current.setRecordType(
                newRecord.getRecordType()
        );

        current.setRecordValue(
                newRecord.getRecordValue()
        );

        current.setTtl(
                newRecord.getTtl()
        );

        current.setPriority(
                newRecord.getPriority()
        );

        current.setDescription(
                newRecord.getDescription()
        );

        /*
         * Nếu form không gửi status,
         * giữ nguyên status hiện tại.
         */
        if (newRecord.getStatus() != null
                && !newRecord.getStatus().isBlank()) {

            current.setStatus(
                    newRecord.getStatus()
            );
        }

        // -------------------------------------------------
        // NORMALIZE
        // -------------------------------------------------

        normalizeRecord(current);

        // -------------------------------------------------
        // VALIDATE
        // -------------------------------------------------

        try {

            dnsRecordValidator.validate(
                    current
            );

        } catch (IllegalArgumentException
                | IllegalStateException ex) {

            logger.warn(
                    "DNS record update validation failed: id={}, reason={}",
                    id,
                    ex.getMessage()
            );

            throw ex;
        }

        // -------------------------------------------------
        // DUPLICATE CHECK EXCLUDING SELF
        // -------------------------------------------------

        if (isDuplicateExcludingSelf(current)) {

            logger.warn(
                    "DNS record update rejected because of duplicate: id={}, hostname={}, type={}, value={}",
                    id,
                    current.getHostname(),
                    current.getRecordType(),
                    current.getRecordValue()
            );

            throw new IllegalArgumentException(
                    "DNS Record trùng với một DNS Record khác."
            );
        }

        // -------------------------------------------------
        // SAVE
        // -------------------------------------------------

        DnsRecord savedRecord =
                dnsRecordRepository
                        .saveAndFlush(current);

        // -------------------------------------------------
        // HISTORY UPDATE
        // -------------------------------------------------

        historyService.logUpdate(
                before,
                savedRecord
        );

        logger.info(
                "DNS record updated successfully: id={}, hostname={}, type={}, value={}",
                savedRecord.getId(),
                savedRecord.getHostname(),
                savedRecord.getRecordType(),
                savedRecord.getRecordValue()
        );

        return savedRecord;
    }

    // =====================================================
    // 8. SOFT DELETE DNS RECORD
    // =====================================================

    @Transactional
    public DnsRecord deleteRecord(
            Long id) {

        logger.info(
                "Soft deleting DNS record: id={}",
                id
        );

        DnsRecord record =
                getRecordById(id);

        if ("DELETED".equalsIgnoreCase(
                record.getStatus())) {

            logger.warn(
                    "Soft delete rejected: DNS record id={} already DELETED",
                    id
            );

            throw new IllegalStateException(
                    "DNS Record đã ở trạng thái DELETED."
            );
        }

        // Snapshot BEFORE
        DnsRecord before =
                copyRecord(record);

        // -------------------------------------------------
        // SOFT DELETE
        // -------------------------------------------------

        record.setStatus(
                "DELETED"
        );

        record.setDeletedAt(
                LocalDateTime.now()
        );

        DnsRecord savedRecord =
                dnsRecordRepository
                        .saveAndFlush(record);

        // -------------------------------------------------
        // HISTORY DELETE
        // -------------------------------------------------

        historyService.logDelete(
                before,
                savedRecord
        );

        logger.info(
                "DNS record soft deleted successfully: id={}, hostname={}, type={}",
                savedRecord.getId(),
                savedRecord.getHostname(),
                savedRecord.getRecordType()
        );

        return savedRecord;
    }

    // =====================================================
    // 9. HARD DELETE DNS RECORD
    // =====================================================

    @Transactional
    public void hardDeleteRecord(
            Long id) {

        logger.info(
                "Hard deleting DNS record: id={}",
                id
        );

        DnsRecord record =
                getRecordById(id);

        /*
         * Chỉ record đã soft delete
         * mới được hard delete.
         */
        if (!"DELETED".equalsIgnoreCase(
                record.getStatus())) {

            logger.warn(
                    "Hard delete rejected: DNS record id={} status={}",
                    id,
                    record.getStatus()
            );

            throw new IllegalStateException(
                    "Chỉ có thể xóa vật lý DNS Record đã DELETED."
            );
        }

        /*
         * History đã giữ dnsRecordId,
         * vì vậy record có thể bị hard delete
         * mà history vẫn tồn tại.
         */
        dnsRecordRepository.delete(record);
        dnsRecordRepository.flush();

        logger.info(
                "DNS record hard deleted successfully: id={}",
                id
        );
    }

    // =====================================================
    // 10. SEARCH + FILTER
    // =====================================================

    public List<DnsRecord> searchRecords(
            Long domainId,
            String keyword,
            String recordType) {

        String normalizedKeyword =
                normalizeNullableText(keyword);

        String normalizedType =
                normalizeNullableType(recordType);

        logger.debug(
                "Searching DNS records: domainId={}, keyword={}, type={}",
                domainId,
                normalizedKeyword,
                normalizedType
        );

        return dnsRecordRepository
                .searchRecords(
                        domainId,
                        normalizedKeyword,
                        normalizedType
                );
    }

    // =====================================================
    // 11. NORMALIZE DNS RECORD
    // =====================================================

    private void normalizeRecord(
            DnsRecord record) {

        if (record == null) {

            throw new IllegalArgumentException(
                    "DNS Record không được null."
            );
        }

        // -------------------------------------------------
        // HOSTNAME
        // -------------------------------------------------

        record.setHostname(
                normalizeHostname(
                        record.getHostname()
                )
        );

        // -------------------------------------------------
        // TYPE
        // -------------------------------------------------

        if (record.getRecordType() != null) {

            record.setRecordType(
                    record
                            .getRecordType()
                            .trim()
                            .toUpperCase()
            );
        }

        // -------------------------------------------------
        // VALUE
        // -------------------------------------------------

        if (record.getRecordValue() != null) {

            record.setRecordValue(
                    record
                            .getRecordValue()
                            .trim()
            );
        }

        // -------------------------------------------------
        // TTL DEFAULT
        // -------------------------------------------------

        if (record.getTtl() == null) {

            record.setTtl(
                    3600
            );
        }

        // -------------------------------------------------
        // STATUS DEFAULT
        // -------------------------------------------------

        if (record.getStatus() == null
                || record.getStatus().isBlank()) {

            record.setStatus(
                    "ACTIVE"
            );

        } else {

            record.setStatus(
                    record
                            .getStatus()
                            .trim()
                            .toUpperCase()
            );
        }

        // -------------------------------------------------
        // DESCRIPTION
        // -------------------------------------------------

        if (record.getDescription() != null) {

            String description =
                    record
                            .getDescription()
                            .trim();

            if (description.isBlank()) {

                record.setDescription(null);

            } else {

                record.setDescription(
                        description
                );
            }
        }

        // -------------------------------------------------
        // PRIORITY
        //
        // Chỉ MX dùng priority.
        // -------------------------------------------------

        if (!"MX".equals(
                record.getRecordType())) {

            record.setPriority(null);
        }
    }

    // =====================================================
    // 12. NORMALIZE HOSTNAME
    // =====================================================

    private String normalizeHostname(
            String hostname) {

        if (hostname == null
                || hostname.isBlank()
                || "@".equals(
                        hostname.trim()
                )) {

            return "@";
        }

        return hostname
                .trim()
                .toLowerCase();
    }

    // =====================================================
    // 13. NORMALIZE SEARCH KEYWORD
    // =====================================================

    private String normalizeNullableText(
            String value) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return value.trim();
    }

    // =====================================================
    // 14. NORMALIZE RECORD TYPE
    // =====================================================

    private String normalizeNullableType(
            String recordType) {

        if (recordType == null
                || recordType.isBlank()) {

            return null;
        }

        return recordType
                .trim()
                .toUpperCase();
    }

    // =====================================================
    // 15. DUPLICATE CHECK WHEN CREATE
    // =====================================================

    private boolean isDuplicate(
            DnsRecord record) {

        return dnsRecordRepository
                .existsByDomainIdAndHostnameAndRecordTypeAndRecordValue(
                        record
                                .getDomain()
                                .getId(),
                        record.getHostname(),
                        record.getRecordType(),
                        record.getRecordValue()
                );
    }

    // =====================================================
    // 16. DUPLICATE CHECK WHEN UPDATE
    // =====================================================

    private boolean isDuplicateExcludingSelf(
            DnsRecord record) {

        List<DnsRecord> domainRecords =
                dnsRecordRepository
                        .findByDomainId(
                                record
                                        .getDomain()
                                        .getId()
                        );

        return domainRecords
                .stream()

                // Bỏ qua chính record đang update
                .filter(other ->
                        other.getId() != null
                                &&
                        !other.getId()
                                .equals(
                                        record.getId()
                                )
                )

                .anyMatch(other ->
                        equalsIgnoreCase(
                                other.getHostname(),
                                record.getHostname()
                        )
                        &&
                        equalsIgnoreCase(
                                other.getRecordType(),
                                record.getRecordType()
                        )
                        &&
                        equalsText(
                                other.getRecordValue(),
                                record.getRecordValue()
                        )
                );
    }

    // =====================================================
    // 17. COPY RECORD FOR HISTORY BEFORE
    // =====================================================

    private DnsRecord copyRecord(
            DnsRecord source) {

        DnsRecord copy =
                new DnsRecord();

        copy.setId(
                source.getId()
        );

        copy.setDomain(
                source.getDomain()
        );

        copy.setHostname(
                source.getHostname()
        );

        copy.setRecordType(
                source.getRecordType()
        );

        copy.setRecordValue(
                source.getRecordValue()
        );

        copy.setTtl(
                source.getTtl()
        );

        copy.setPriority(
                source.getPriority()
        );

        copy.setStatus(
                source.getStatus()
        );

        copy.setDescription(
                source.getDescription()
        );

        copy.setCreatedAt(
                source.getCreatedAt()
        );

        copy.setUpdatedAt(
                source.getUpdatedAt()
        );

        copy.setDeletedAt(
                source.getDeletedAt()
        );

        return copy;
    }

    // =====================================================
    // 18. STRING EQUALS IGNORE CASE
    // =====================================================

    private boolean equalsIgnoreCase(
            String first,
            String second) {

        if (first == null
                || second == null) {

            return first == null
                    && second == null;
        }

        return first.equalsIgnoreCase(
                second
        );
    }

    // =====================================================
    // 19. STRING EQUALS
    // =====================================================

    private boolean equalsText(
            String first,
            String second) {

        if (first == null
                || second == null) {

            return first == null
                    && second == null;
        }

        return first.equals(
                second
        );
    }
}