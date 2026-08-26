package vn.vnnic.dnsmanager.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.vnnic.dnsmanager.entity.DnsRecord;
import vn.vnnic.dnsmanager.entity.Domain;
import vn.vnnic.dnsmanager.repository.DnsRecordRepository;
import vn.vnnic.dnsmanager.validation.DnsRecordValidator;

@Service
public class DnsRecordService {

    private final DnsRecordRepository dnsRecordRepository;
    private final DomainService domainService;
    private final DnsRecordValidator dnsRecordValidator;
    private final DnsRecordHistoryService historyService;


    // 
    // CONSTRUCTOR
    // 

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


    // 
    // 1. XEM TOÀN BỘ DNS RECORD
    // 

    public List<DnsRecord> getAllRecords() {

        return dnsRecordRepository.findAll();
    }


    // 
    // 2. XEM DNS RECORD THEO DOMAIN
    // 

    public List<DnsRecord> getRecordsByDomain(
            Long domainId) {

        return dnsRecordRepository
                .findByDomainId(domainId);
    }


    // 
    // 3. XEM CHI TIẾT DNS RECORD
    // 

    public DnsRecord getRecordById(
            Long id) {

        return dnsRecordRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy DNS Record có ID: "
                                        + id
                        )
                );
    }


    // 
    // 4. ĐẾM RECORD ACTIVE THEO DOMAIN
    // 

    public long countActiveRecords(
            Long domainId) {

        return dnsRecordRepository
                .countByDomainIdAndStatus(
                        domainId,
                        "ACTIVE"
                );
    }


    // 
    // 5. DASHBOARD
    // KHÔNG TÍNH RECORD DELETED
    // 

    public long countManagedRecords() {

        return dnsRecordRepository
                .countByStatusNot(
                        "DELETED"
                );
    }


    // 
    // 6. CREATE DNS RECORD
    // 

    @Transactional
    public DnsRecord createRecord(
            Long domainId,
            DnsRecord record) {

        // Domain phải tồn tại
        Domain domain =
                domainService
                        .getDomainById(domainId);


        // Không cho thêm record vào domain DELETED
        if ("DELETED".equals(
                domain.getStatus())) {

            throw new IllegalStateException(
                    "Không thể thêm DNS Record "
                            + "cho tên miền đã DELETED."
            );
        }


        // Mỗi DNS Record bắt buộc thuộc một Domain
        record.setDomain(domain);


        // Chuẩn hóa dữ liệu
        normalizeRecord(record);


        // Validate A, AAAA, CNAME, MX, TTL...
        dnsRecordValidator.validate(
                record
        );


        // Không cho tạo record giống hoàn toàn
        if (isDuplicate(record)) {

            throw new IllegalArgumentException(
                    "DNS Record đã tồn tại."
            );
        }


        /*
         * Flush để database sinh ID trước khi
         * tạo bản ghi History.
         */
        DnsRecord savedRecord =
                dnsRecordRepository
                        .saveAndFlush(record);


        // HISTORY CREATE
        historyService.logCreate(
                savedRecord
        );


        return savedRecord;
    }


    // 
    // 7. UPDATE DNS RECORD
    // 

    @Transactional
    public DnsRecord updateRecord(
            Long id,
            DnsRecord newRecord) {

        DnsRecord current =
                getRecordById(id);


        // Không cho sửa record DELETED
        if ("DELETED".equals(
                current.getStatus())) {

            throw new IllegalStateException(
                    "Không thể cập nhật DNS Record đã DELETED."
            );
        }


        /*
         * QUAN TRỌNG:
         *
         * Copy dữ liệu hiện tại TRƯỚC khi sửa.
         *
         * Không được viết:
         *
         * DnsRecord before = current;
         *
         * vì cả hai sẽ trỏ vào cùng một object.
         */
        DnsRecord before =
                copyRecord(current);


       // 
        // CẬP NHẬT
       // 

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

        current.setStatus(
                newRecord.getStatus()
        );


        // Chuẩn hóa
        normalizeRecord(current);


        // Validate dữ liệu mới
        dnsRecordValidator.validate(
                current
        );


        // Check duplicate nhưng bỏ qua chính record này
        if (isDuplicateExcludingSelf(
                current)) {

            throw new IllegalArgumentException(
                    "DNS Record trùng với một DNS Record khác."
            );
        }


        DnsRecord savedRecord =
                dnsRecordRepository
                        .saveAndFlush(current);


       // 
        // HISTORY UPDATE
        //
        // before = dữ liệu cũ
        // savedRecord = dữ liệu mới
       // 

        historyService.logUpdate(
                before,
                savedRecord
        );


        return savedRecord;
    }


    // 
    // 8. SOFT DELETE DNS RECORD
    // 

    @Transactional
    public DnsRecord deleteRecord(
            Long id) {

        DnsRecord record =
                getRecordById(id);


        if ("DELETED".equals(
                record.getStatus())) {

            throw new IllegalStateException(
                    "DNS Record đã ở trạng thái DELETED."
            );
        }


        // Lưu trạng thái BEFORE
        DnsRecord before =
                copyRecord(record);


       // 
        // SOFT DELETE
       // 

        record.setStatus(
                "DELETED"
        );

        record.setDeletedAt(
                LocalDateTime.now()
        );


        DnsRecord savedRecord =
                dnsRecordRepository
                        .saveAndFlush(record);


       // 
        // HISTORY DELETE
       // 

        historyService.logDelete(
                before,
                savedRecord
        );


        return savedRecord;
    }


    // 
    // 9. HARD DELETE DNS RECORD
    // 

    @Transactional
    public void hardDeleteRecord(
            Long id) {

        DnsRecord record =
                getRecordById(id);


        /*
         * Chỉ record đã soft-delete mới được
         * xóa vật lý.
         */
        if (!"DELETED".equals(
                record.getStatus())) {

            throw new IllegalStateException(
                    "Chỉ có thể xóa vật lý "
                            + "DNS Record đã DELETED."
            );
        }


        /*
         * History không có foreign key object
         * trực tiếp tới DnsRecord.
         *
         * History chỉ giữ dnsRecordId,
         * nên record bị hard-delete thì
         * lịch sử vẫn được giữ lại.
         */
        dnsRecordRepository.delete(
                record
        );


        dnsRecordRepository.flush();
    }


    // 
    // 10. SEARCH + FILTER
    // 

    public List<DnsRecord> searchRecords(
            Long domainId,
            String keyword,
            String recordType) {

        String normalizedKeyword =
                normalizeNullableText(
                        keyword
                );


        String normalizedType =
                normalizeNullableType(
                        recordType
                );


        return dnsRecordRepository
                .searchRecords(
                        domainId,
                        normalizedKeyword,
                        normalizedType
                );
    }


    // 
    // 11. NORMALIZE DNS RECORD
    // 

    private void normalizeRecord(
            DnsRecord record) {


       // 
        // HOSTNAME
       // 

        record.setHostname(
                normalizeHostname(
                        record.getHostname()
                )
        );


       // 
        // TYPE
       // 

        if (record.getRecordType() != null) {

            record.setRecordType(
                    record
                            .getRecordType()
                            .trim()
                            .toUpperCase()
            );
        }


       // 
        // VALUE
       // 

        if (record.getRecordValue() != null) {

            record.setRecordValue(
                    record
                            .getRecordValue()
                            .trim()
            );
        }


       // 
        // TTL DEFAULT = 3600
       // 

        if (record.getTtl() == null) {

            record.setTtl(
                    3600
            );
        }


       // 
        // STATUS DEFAULT = ACTIVE
       // 

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


       // 
        // DESCRIPTION
       // 

        if (record.getDescription() != null) {

            String description =
                    record
                            .getDescription()
                            .trim();


            if (description.isBlank()) {

                record.setDescription(
                        null
                );

            } else {

                record.setDescription(
                        description
                );
            }
        }


       // 
        // PRIORITY
        //
        // Chỉ MX sử dụng Priority.
       // 

        if (!"MX".equals(
                record.getRecordType())) {

            record.setPriority(
                    null
            );
        }
    }


    // 
    // 12. NORMALIZE HOSTNAME
    //
    // null / blank / @ -> @
    //
    // WWW -> www
    // Mail -> mail
    // 

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


    // 
    // 13. NORMALIZE SEARCH KEYWORD
    // 

    private String normalizeNullableText(
            String value) {

        if (value == null
                || value.isBlank()) {

            return null;
        }


        return value.trim();
    }


    // 
    // 14. NORMALIZE RECORD TYPE
    // 

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


    // 
    // 15. CHECK DUPLICATE KHI CREATE
    //
    // domain_id
    // hostname
    // record_type
    // record_value
    // 

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


    // 
    // 16. CHECK DUPLICATE KHI UPDATE
    //
    // Cho phép:
    //
    // www | A | 203.119.10.10
    // www | A | 203.119.10.11
    //
    // Không cho:
    //
    // www | A | 203.119.10.10
    // www | A | 203.119.10.10
    // 

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


    // 
    // 17. COPY RECORD
    //
    // Dùng để lưu BEFORE cho History.
    // 

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


        return copy;
    }


    // 
    // 18. STRING EQUALS IGNORE CASE
    // 

    private boolean equalsIgnoreCase(
            String first,
            String second) {

        if (first == null
                || second == null) {

            return first == null
                    && second == null;
        }


        return first
                .equalsIgnoreCase(
                        second
                );
    }


    // 
    // 19. STRING EQUALS
    // 

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