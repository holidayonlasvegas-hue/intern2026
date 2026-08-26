package vn.vnnic.dnsmanager.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.vnnic.dnsmanager.entity.Domain;
import vn.vnnic.dnsmanager.repository.DnsRecordRepository;
import vn.vnnic.dnsmanager.repository.DomainRepository;

@Service
public class DomainService {

    private final DomainRepository domainRepository;
    private final DnsRecordRepository dnsRecordRepository;


    // 
    // CONSTRUCTOR
    // 

    public DomainService(
            DomainRepository domainRepository,
            DnsRecordRepository dnsRecordRepository) {

        this.domainRepository =
                domainRepository;

        this.dnsRecordRepository =
                dnsRecordRepository;
    }


    // 
    // GET ALL
    // 

    public List<Domain> getAllDomains() {

        return domainRepository.findAll();
    }


    // 
    // GET BY ID
    // 

    public Domain getDomainById(
            Long id) {

        return domainRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Không tìm thấy tên miền có ID: "
                                        + id
                        )
                );
    }


    // 
    // CREATE DOMAIN
    // 

    @Transactional
    public Domain createDomain(
            Domain domain) {


        // ---------------------------------------------
        // DOMAIN NAME BẮT BUỘC
        // ---------------------------------------------

        if (domain.getDomainName() == null
                || domain.getDomainName().isBlank()) {

            throw new IllegalArgumentException(
                    "Tên miền không được để trống."
            );
        }


        // ---------------------------------------------
        // NORMALIZE
        // ---------------------------------------------

        String normalizedName =
                normalizeDomainName(
                        domain.getDomainName()
                );


        // ---------------------------------------------
        // CHECK DUPLICATE
        // ---------------------------------------------

        if (domainRepository
                .existsByDomainNameIgnoreCase(
                        normalizedName
                )) {

            throw new IllegalArgumentException(
                    "Tên miền đã tồn tại: "
                            + normalizedName
            );
        }


        domain.setDomainName(
                normalizedName
        );


        // ---------------------------------------------
        // STATUS
        // ---------------------------------------------

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


        // Không cho tạo mới thẳng ở trạng thái DELETED

        if ("DELETED".equals(
                domain.getStatus())) {

            throw new IllegalArgumentException(
                    "Tên miền mới không thể có trạng thái DELETED."
            );
        }


        normalizeDescription(
                domain
        );


        return domainRepository.save(
                domain
        );
    }


    // 
    // UPDATE DOMAIN
    // 

    @Transactional
    public Domain updateDomain(
            Long id,
            Domain newDomain) {

        Domain current =
                getDomainById(id);


        if ("DELETED".equals(
                current.getStatus())) {

            throw new IllegalStateException(
                    "Không thể cập nhật tên miền đã DELETED."
            );
        }


        // ---------------------------------------------
        // DOMAIN NAME
        // ---------------------------------------------

        if (newDomain.getDomainName() == null
                || newDomain
                        .getDomainName()
                        .isBlank()) {

            throw new IllegalArgumentException(
                    "Tên miền không được để trống."
            );
        }


        String normalizedName =
                normalizeDomainName(
                        newDomain.getDomainName()
                );


        // ---------------------------------------------
        // UNIQUE - BỎ QUA CHÍNH DOMAIN HIỆN TẠI
        // ---------------------------------------------

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


        current.setDomainName(
                normalizedName
        );


        current.setDescription(
                newDomain.getDescription()
        );


        // ---------------------------------------------
        // STATUS
        // ---------------------------------------------

        if (newDomain.getStatus() != null
                && !newDomain
                        .getStatus()
                        .isBlank()) {

            String status =
                    normalizeStatus(
                            newDomain.getStatus()
                    );


            /*
             * DELETED phải đi qua chức năng soft delete.
             * Không cho người dùng sửa form rồi chọn DELETED.
             */
            if ("DELETED".equals(status)) {

                throw new IllegalArgumentException(
                        "Hãy sử dụng chức năng xóa mềm "
                                + "để chuyển tên miền sang DELETED."
                );
            }


            current.setStatus(
                    status
            );
        }


        normalizeDescription(
                current
        );


        return domainRepository.save(
                current
        );
    }


    // 
    // DEACTIVATE DOMAIN
    // 

    @Transactional
    public Domain deactivateDomain(
            Long id) {

        Domain domain =
                getDomainById(id);


        if ("DELETED".equals(
                domain.getStatus())) {

            throw new IllegalStateException(
                    "Không thể ngừng quản lý "
                            + "tên miền đã DELETED."
            );
        }


        domain.setStatus(
                "INACTIVE"
        );


        return domainRepository.save(
                domain
        );
    }


    // 
    // ACTIVATE DOMAIN
    // 

    @Transactional
    public Domain activateDomain(
            Long id) {

        Domain domain =
                getDomainById(id);


        if ("DELETED".equals(
                domain.getStatus())) {

            throw new IllegalStateException(
                    "Không thể kích hoạt "
                            + "tên miền đã DELETED."
            );
        }


        domain.setStatus(
                "ACTIVE"
        );


        return domainRepository.save(
                domain
        );
    }


    // 
    // SOFT DELETE DOMAIN
    // 

    @Transactional
    public Domain softDeleteDomain(
            Long id) {

        Domain domain =
                getDomainById(id);


        if ("DELETED".equals(
                domain.getStatus())) {

            throw new IllegalStateException(
                    "Tên miền đã ở trạng thái DELETED."
            );
        }


        domain.setStatus(
                "DELETED"
        );


        domain.setDeletedAt(
                LocalDateTime.now()
        );


        return domainRepository.save(
                domain
        );
    }


    // 
    // HARD DELETE DOMAIN
    // 

    @Transactional
    public void hardDeleteDomain(
            Long id) {

        Domain domain =
                getDomainById(id);


        // ---------------------------------------------
        // BẮT BUỘC SOFT DELETE TRƯỚC
        // ---------------------------------------------

        if (!"DELETED".equals(
                domain.getStatus())) {

            throw new IllegalStateException(
                    "Tên miền phải ở trạng thái DELETED "
                            + "trước khi xóa vật lý."
            );
        }


        // ---------------------------------------------
        // KIỂM TRA DNS RECORD
        //
        // Kể cả record DELETED vẫn còn trong database,
        // nên vẫn đang tham chiếu domain_id.
        // ---------------------------------------------

        if (dnsRecordRepository
                .existsByDomainId(id)) {

            throw new IllegalStateException(
                    "Không thể xóa vật lý tên miền vì "
                            + "vẫn còn DNS Record thuộc tên miền này. "
                            + "Hãy xóa vật lý các DNS Record trước."
            );
        }


        domainRepository.delete(
                domain
        );


        domainRepository.flush();
    }


    // 
    // SEARCH + FILTER
    // 

    public List<Domain> filterDomains(
            String keyword,
            String status) {

        boolean hasKeyword =
                keyword != null
                        && !keyword.isBlank();


        boolean hasStatus =
                status != null
                        && !status.isBlank();


        if (hasKeyword
                && hasStatus) {

            return domainRepository
                    .findByDomainNameContainingIgnoreCaseAndStatus(
                            keyword.trim(),
                            normalizeStatus(status)
                    );
        }


        if (hasKeyword) {

            return domainRepository
                    .findByDomainNameContainingIgnoreCase(
                            keyword.trim()
                    );
        }


        if (hasStatus) {

            return domainRepository
                    .findByStatus(
                            normalizeStatus(status)
                    );
        }


        return domainRepository.findAll();
    }


    // 
    // DASHBOARD
    // 

    public long countManagedDomains() {

        return domainRepository
                .countByStatusNot(
                        "DELETED"
                );
    }


    public long countActiveDomains() {

        return domainRepository
                .countByStatus(
                        "ACTIVE"
                );
    }


    public long countInactiveDomains() {

        return domainRepository
                .countByStatus(
                        "INACTIVE"
                );
    }


    // 
    // NORMALIZE DOMAIN NAME
    // 

    private String normalizeDomainName(
            String domainName) {

        return domainName
                .trim()
                .toLowerCase();
    }


    // 
    // NORMALIZE STATUS
    // 

    private String normalizeStatus(
            String status) {

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


    // 
    // NORMALIZE DESCRIPTION
    // 

    private void normalizeDescription(
            Domain domain) {

        if (domain.getDescription() == null) {

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