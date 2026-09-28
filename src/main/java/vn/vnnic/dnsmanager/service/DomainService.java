package vn.vnnic.dnsmanager.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.vnnic.dnsmanager.entity.Domain;
import vn.vnnic.dnsmanager.repository.DnsRecordRepository;
import vn.vnnic.dnsmanager.repository.DomainRepository;
import vn.vnnic.dnsmanager.validation.DomainValidation;

@Service
public class DomainService {

    private final DomainRepository domainRepository;
    private final DnsRecordRepository dnsRecordRepository;
    private final DomainValidation domainValidation;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public DomainService(
            DomainRepository domainRepository,
            DnsRecordRepository dnsRecordRepository,
            DomainValidation domainValidation) {

        this.domainRepository =
                domainRepository;

        this.dnsRecordRepository =
                dnsRecordRepository;

        this.domainValidation =
                domainValidation;
    }

    // =====================================================
    // 1. GET ALL DOMAINS
    // =====================================================

    public List<Domain> getAllDomains() {

        return domainRepository.findAll();
    }

    // =====================================================
    // 2. GET DOMAIN BY ID
    // =====================================================

    public Domain getDomainById(
            Long id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "ID tên miền không được để trống."
            );
        }

        return domainRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tên miền có ID: "
                                        + id
                        )
                );
    }

    // =====================================================
    // 3. CREATE DOMAIN
    // =====================================================

    @Transactional
    public Domain createDomain(
            Domain domain) {

        if (domain == null) {
            throw new IllegalArgumentException(
                    "Domain không được để trống."
            );
        }

        // -------------------------------------------------
        // VALIDATE + NORMALIZE DOMAIN NAME
        //
        // Ví dụ:
        //
        // 123       -> INVALID
        // abc       -> INVALID
        // vnnic.vn  -> VALID
        // VNNIC.VN  -> vnnic.vn
        // 123.vn    -> VALID
        // -------------------------------------------------

        domainValidation.validate(
                domain
        );

        String normalizedName =
                domain.getDomainName();

        // -------------------------------------------------
        // CHECK DUPLICATE
        // -------------------------------------------------

        if (domainRepository
                .existsByDomainNameIgnoreCase(
                        normalizedName
                )) {

            throw new IllegalArgumentException(
                    "Tên miền đã tồn tại: "
                            + normalizedName
            );
        }

        // -------------------------------------------------
        // STATUS
        // -------------------------------------------------

        if (domain.getStatus() == null
                || domain.getStatus().isBlank()) {

            domain.setStatus(
                    "ACTIVE"
            );

        } else {

            domain.setStatus(
                    normalizeStatus(
                            domain.getStatus()
                    )
            );
        }

        /*
         * Không cho người dùng tạo Domain mới
         * ở trạng thái DELETED.
         */
        if ("DELETED".equals(
                domain.getStatus())) {

            throw new IllegalArgumentException(
                    "Tên miền mới không thể có trạng thái DELETED."
            );
        }

        // -------------------------------------------------
        // DESCRIPTION
        // -------------------------------------------------

        normalizeDescription(
                domain
        );

        // -------------------------------------------------
        // TIMESTAMP
        // -------------------------------------------------

        LocalDateTime now =
                LocalDateTime.now();

        /*
         * Nếu Entity của bạn đã dùng @PrePersist
         * thì việc set timestamp ở đây có thể bỏ.
         *
         * Nhưng set ở Service giúp business flow rõ ràng.
         */
        if (domain.getCreatedAt() == null) {
            domain.setCreatedAt(now);
        }

        domain.setUpdatedAt(now);
        domain.setDeletedAt(null);

        // -------------------------------------------------
        // SAVE
        // -------------------------------------------------

        return domainRepository.saveAndFlush(
                domain
        );
    }

    // =====================================================
    // 4. UPDATE DOMAIN
    // =====================================================

    @Transactional
    public Domain updateDomain(
            Long id,
            Domain newDomain) {

        if (newDomain == null) {
            throw new IllegalArgumentException(
                    "Dữ liệu cập nhật tên miền không được để trống."
            );
        }

        Domain current =
                getDomainById(id);

        // -------------------------------------------------
        // DOMAIN DELETED KHÔNG ĐƯỢC UPDATE
        // -------------------------------------------------

        if ("DELETED".equalsIgnoreCase(
                current.getStatus())) {

            throw new IllegalStateException(
                    "Không thể cập nhật tên miền đã DELETED."
            );
        }

        // -------------------------------------------------
        // VALIDATE + NORMALIZE DOMAIN NAME
        // -------------------------------------------------

        domainValidation.validate(
                newDomain
        );

        String normalizedName =
                newDomain.getDomainName();

        // -------------------------------------------------
        // DUPLICATE CHECK
        //
        // Bỏ qua chính Domain đang update.
        // -------------------------------------------------

        if (domainRepository
                .existsByDomainNameIgnoreCaseAndIdNot(
                        normalizedName,
                        id
                )) {

            throw new IllegalArgumentException(
                    "Tên miền đã tồn tại: "
                            + normalizedName
            );
        }

        // -------------------------------------------------
        // UPDATE DOMAIN NAME
        // -------------------------------------------------

        current.setDomainName(
                normalizedName
        );

        // -------------------------------------------------
        // UPDATE DESCRIPTION
        // -------------------------------------------------

        current.setDescription(
                newDomain.getDescription()
        );

        normalizeDescription(
                current
        );

        // -------------------------------------------------
        // UPDATE STATUS
        // -------------------------------------------------

        if (newDomain.getStatus() != null
                && !newDomain
                        .getStatus()
                        .isBlank()) {

            String normalizedStatus =
                    normalizeStatus(
                            newDomain.getStatus()
                    );

            /*
             * DELETED phải thông qua soft delete.
             *
             * Không cho người dùng edit form
             * và chuyển trực tiếp sang DELETED.
             */
            if ("DELETED".equals(
                    normalizedStatus)) {

                throw new IllegalArgumentException(
                        "Hãy sử dụng chức năng xóa mềm "
                                + "để chuyển tên miền sang DELETED."
                );
            }

            current.setStatus(
                    normalizedStatus
            );
        }

        // -------------------------------------------------
        // TIMESTAMP
        // -------------------------------------------------

        current.setUpdatedAt(
                LocalDateTime.now()
        );

        /*
         * Domain ACTIVE / INACTIVE
         * không phải domain đã xóa.
         */
        current.setDeletedAt(null);

        // -------------------------------------------------
        // SAVE
        // -------------------------------------------------

        return domainRepository.saveAndFlush(
                current
        );
    }

    // =====================================================
    // 5. DEACTIVATE DOMAIN
    //
    // ACTIVE -> INACTIVE
    // =====================================================

    @Transactional
    public Domain deactivateDomain(
            Long id) {

        Domain domain =
                getDomainById(id);

        if ("DELETED".equalsIgnoreCase(
                domain.getStatus())) {

            throw new IllegalStateException(
                    "Không thể ngừng quản lý "
                            + "tên miền đã DELETED."
            );
        }

        domain.setStatus(
                "INACTIVE"
        );

        domain.setUpdatedAt(
                LocalDateTime.now()
        );

        return domainRepository.saveAndFlush(
                domain
        );
    }

    // =====================================================
    // 6. ACTIVATE DOMAIN
    //
    // INACTIVE -> ACTIVE
    // =====================================================

    @Transactional
    public Domain activateDomain(
            Long id) {

        Domain domain =
                getDomainById(id);

        if ("DELETED".equalsIgnoreCase(
                domain.getStatus())) {

            throw new IllegalStateException(
                    "Không thể kích hoạt "
                            + "tên miền đã DELETED."
            );
        }

        domain.setStatus(
                "ACTIVE"
        );

        domain.setUpdatedAt(
                LocalDateTime.now()
        );

        domain.setDeletedAt(null);

        return domainRepository.saveAndFlush(
                domain
        );
    }

    // =====================================================
    // 7. SOFT DELETE DOMAIN
    //
    // ACTIVE / INACTIVE
    //        ↓
    //     DELETED
    // =====================================================

    @Transactional
    public Domain softDeleteDomain(
            Long id) {

        Domain domain =
                getDomainById(id);

        if ("DELETED".equalsIgnoreCase(
                domain.getStatus())) {

            throw new IllegalStateException(
                    "Tên miền đã ở trạng thái DELETED."
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        domain.setStatus(
                "DELETED"
        );

        domain.setDeletedAt(
                now
        );

        domain.setUpdatedAt(
                now
        );

        return domainRepository.saveAndFlush(
                domain
        );
    }

    // =====================================================
    // 8. HARD DELETE DOMAIN
    //
    // Chỉ Domain DELETED mới được xóa vật lý.
    //
    // Đồng thời Domain không được còn DNS Record.
    // =====================================================

    @Transactional
    public void hardDeleteDomain(
            Long id) {

        Domain domain =
                getDomainById(id);

        // -------------------------------------------------
        // PHẢI SOFT DELETE TRƯỚC
        // -------------------------------------------------

        if (!"DELETED".equalsIgnoreCase(
                domain.getStatus())) {

            throw new IllegalStateException(
                    "Tên miền phải ở trạng thái DELETED "
                            + "trước khi xóa vật lý."
            );
        }

        // -------------------------------------------------
        // KIỂM TRA DNS RECORD
        //
        // Kể cả DNS Record đã DELETED,
        // nếu nó vẫn tồn tại trong database
        // thì Foreign Key vẫn tham chiếu domain_id.
        // -------------------------------------------------

        if (dnsRecordRepository
                .existsByDomainId(id)) {

            throw new IllegalStateException(
                    "Không thể xóa vật lý tên miền vì "
                            + "vẫn còn DNS Record thuộc tên miền này. "
                            + "Hãy xóa vật lý các DNS Record trước."
            );
        }

        // -------------------------------------------------
        // HARD DELETE
        // -------------------------------------------------

        domainRepository.delete(
                domain
        );

        domainRepository.flush();
    }

    // =====================================================
    // 9. SEARCH + FILTER DOMAIN
    // =====================================================

    public List<Domain> filterDomains(
            String keyword,
            String status) {

        boolean hasKeyword =
                keyword != null
                        && !keyword.isBlank();

        boolean hasStatus =
                status != null
                        && !status.isBlank();

        // -------------------------------------------------
        // KEYWORD + STATUS
        // -------------------------------------------------

        if (hasKeyword
                && hasStatus) {

            return domainRepository
                    .findByDomainNameContainingIgnoreCaseAndStatus(
                            keyword.trim(),
                            normalizeStatus(status)
                    );
        }

        // -------------------------------------------------
        // KEYWORD ONLY
        // -------------------------------------------------

        if (hasKeyword) {

            return domainRepository
                    .findByDomainNameContainingIgnoreCase(
                            keyword.trim()
                    );
        }

        // -------------------------------------------------
        // STATUS ONLY
        // -------------------------------------------------

        if (hasStatus) {

            return domainRepository
                    .findByStatus(
                            normalizeStatus(status)
                    );
        }

        // -------------------------------------------------
        // NO FILTER
        // -------------------------------------------------

        return domainRepository.findAll();
    }

    // =====================================================
    // 10. DASHBOARD - MANAGED DOMAINS
    //
    // ACTIVE + INACTIVE
    // =====================================================

    public long countManagedDomains() {

        return domainRepository
                .countByStatusNot(
                        "DELETED"
                );
    }

    // =====================================================
    // 11. DASHBOARD - ACTIVE
    // =====================================================

    public long countActiveDomains() {

        return domainRepository
                .countByStatus(
                        "ACTIVE"
                );
    }

    // =====================================================
    // 12. DASHBOARD - INACTIVE
    // =====================================================

    public long countInactiveDomains() {

        return domainRepository
                .countByStatus(
                        "INACTIVE"
                );
    }

    // =====================================================
    // 13. NORMALIZE STATUS
    // =====================================================

    private String normalizeStatus(
            String status) {

        if (status == null
                || status.isBlank()) {

            throw new IllegalArgumentException(
                    "Trạng thái tên miền không được để trống."
            );
        }

        String normalized =
                status
                        .trim()
                        .toUpperCase();

        if (!normalized.equals("ACTIVE")
                && !normalized.equals("INACTIVE")
                && !normalized.equals("DELETED")) {

            throw new IllegalArgumentException(
                    "Trạng thái tên miền không hợp lệ: "
                            + status
            );
        }

        return normalized;
    }

    // =====================================================
    // 14. NORMALIZE DESCRIPTION
    // =====================================================

    private void normalizeDescription(
            Domain domain) {

        if (domain == null
                || domain.getDescription() == null) {

            return;
        }

        String description =
                domain
                        .getDescription()
                        .trim();

        if (description.isBlank()) {

            domain.setDescription(
                    null
            );

        } else {

            domain.setDescription(
                    description
            );
        }
    }
}