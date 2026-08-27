package vn.vnnic.dnsmanager.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.vnnic.dnsmanager.entity.DnsRecord;
import vn.vnnic.dnsmanager.entity.Domain;
import vn.vnnic.dnsmanager.service.DnsRecordService;
import vn.vnnic.dnsmanager.service.DomainService;

@Controller
public class DnsRecordController {

    private final DnsRecordService dnsRecordService;
    private final DomainService domainService;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public DnsRecordController(
            DnsRecordService dnsRecordService,
            DomainService domainService) {

        this.dnsRecordService = dnsRecordService;
        this.domainService = domainService;
    }

    // =====================================================
    // 1. DANH SÁCH DNS RECORD
    //
    // Hỗ trợ:
    // - chọn Domain
    // - tìm hostname / value
    // - lọc theo record type
    // =====================================================

    @GetMapping("/records")
    public String records(
            @RequestParam(required = false) Long domainId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String recordType,
            Model model) {

        loadRecordsPage(
                domainId,
                keyword,
                recordType,
                model
        );

        return "records";
    }

    // =====================================================
    // 2. CREATE DNS RECORD
    // =====================================================

    @PostMapping("/records")
    public String createRecord(
            @RequestParam Long domainId,
            DnsRecord record,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            DnsRecord savedRecord =
                    dnsRecordService.createRecord(
                            domainId,
                            record
                    );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã thêm DNS Record "
                            + buildDisplayName(savedRecord)
                            + " thành công."
            );

            return "redirect:/records?domainId="
                    + domainId;

        } catch (IllegalArgumentException
                | IllegalStateException e) {

            /*
             * Hiển thị lỗi ngay trên records.html.
             */
            model.addAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            /*
             * Giữ dữ liệu người dùng vừa nhập
             * để form có thể hiển thị lại.
             */
            model.addAttribute(
                    "recordForm",
                    record
            );

            loadRecordsPage(
                    domainId,
                    null,
                    null,
                    model
            );

            return "records";
        }
    }

    // =====================================================
    // 3. XEM CHI TIẾT DNS RECORD
    // =====================================================

    @GetMapping("/records/{id}")
    public String recordDetail(
            @PathVariable Long id,
            Model model) {

        DnsRecord record =
                dnsRecordService.getRecordById(id);

        model.addAttribute(
                "record",
                record
        );

        return "record-detail";
    }

    // =====================================================
    // 4. UPDATE DNS RECORD
    // =====================================================

    @PostMapping("/records/{id}/edit")
    public String updateRecord(
            @PathVariable Long id,
            DnsRecord newRecord,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            DnsRecord updatedRecord =
                    dnsRecordService.updateRecord(
                            id,
                            newRecord
                    );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã cập nhật DNS Record "
                            + buildDisplayName(updatedRecord)
                            + " thành công."
            );

            return "redirect:/records/"
                    + updatedRecord.getId();

        } catch (IllegalArgumentException
                | IllegalStateException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            /*
             * Vì update fail nên dữ liệu trong DB
             * vẫn là dữ liệu cũ.
             */
            DnsRecord currentRecord =
                    dnsRecordService.getRecordById(id);

            model.addAttribute(
                    "record",
                    currentRecord
            );

            return "record-detail";
        }
    }

    // =====================================================
    // 5. SOFT DELETE DNS RECORD
    //
    // ACTIVE / INACTIVE
    //        ↓
    //     DELETED
    //
    // DnsRecordService hiện dùng:
    // deleteRecord(Long id)
    // =====================================================

    @PostMapping("/records/{id}/delete")
    public String softDeleteRecord(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        /*
         * Lấy record trước để giữ domainId
         * và tên record cho redirect/message.
         */
        DnsRecord currentRecord =
                dnsRecordService.getRecordById(id);

        Long domainId =
                currentRecord
                        .getDomain()
                        .getId();

        String recordName =
                buildDisplayName(
                        currentRecord
                );

        try {

            /*
             * Đây là soft delete thực tế.
             */
            dnsRecordService.deleteRecord(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "DNS Record "
                            + recordName
                            + " đã chuyển sang DELETED."
            );

            return "redirect:/records?domainId="
                    + domainId;

        } catch (IllegalArgumentException
                | IllegalStateException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            loadRecordsPage(
                    domainId,
                    null,
                    null,
                    model
            );

            return "records";
        }
    }

    // =====================================================
    // 6. HARD DELETE DNS RECORD
    //
    // Chỉ record DELETED mới được xóa vật lý.
    // =====================================================

    @PostMapping("/records/{id}/hard-delete")
    public String hardDeleteRecord(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        /*
         * Phải lấy trước thông tin record
         * vì sau hard delete record sẽ biến mất khỏi DB.
         */
        DnsRecord currentRecord =
                dnsRecordService.getRecordById(id);

        Long domainId =
                currentRecord
                        .getDomain()
                        .getId();

        String recordName =
                buildDisplayName(
                        currentRecord
                );

        try {

            dnsRecordService.hardDeleteRecord(id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã xóa vĩnh viễn DNS Record "
                            + recordName
                            + "."
            );

            return "redirect:/records?domainId="
                    + domainId;

        } catch (IllegalArgumentException
                | IllegalStateException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            loadRecordsPage(
                    domainId,
                    null,
                    null,
                    model
            );

            return "records";
        }
    }

    // =====================================================
    // 7. HELPER - LOAD records.html
    // =====================================================

    private void loadRecordsPage(
            Long domainId,
            String keyword,
            String recordType,
            Model model) {

        // -------------------------------------------------
        // DNS RECORD LIST
        // -------------------------------------------------

        List<DnsRecord> records;

        /*
         * Nếu có domainId:
         * lấy record thuộc domain đó.
         *
         * Nếu chưa chọn domain:
         * hiện danh sách rỗng.
         */
        if (domainId != null) {

            records =
                    dnsRecordService.searchRecords(
                            domainId,
                            keyword,
                            recordType
                    );

        } else {

            records = List.of();
        }

        model.addAttribute(
                "records",
                records
        );

        // -------------------------------------------------
        // DOMAIN LIST
        // -------------------------------------------------

        List<Domain> domains =
                domainService.getAllDomains();

        model.addAttribute(
                "domains",
                domains
        );

        // -------------------------------------------------
        // FILTER VALUES
        // -------------------------------------------------

        model.addAttribute(
                "domainId",
                domainId
        );

        model.addAttribute(
                "keyword",
                keyword
        );

        model.addAttribute(
                "recordType",
                recordType
        );

        // -------------------------------------------------
        // CURRENT DOMAIN
        // -------------------------------------------------

        if (domainId != null) {

            Domain domain =
                    domainService.getDomainById(
                            domainId
                    );

            model.addAttribute(
                    "domain",
                    domain
            );
        }

        // -------------------------------------------------
        // FORM OBJECT
        // -------------------------------------------------

        /*
         * Khi create validation fail,
         * recordForm đã được Controller thêm vào Model.
         *
         * Không ghi đè dữ liệu đó.
         */
        if (!model.containsAttribute("recordForm")) {

            model.addAttribute(
                    "recordForm",
                    new DnsRecord()
            );
        }
    }

    // =====================================================
    // 8. HELPER - BUILD FQDN DISPLAY
    //
    // @ + example.vn
    // -> example.vn
    //
    // www + example.vn
    // -> www.example.vn
    // =====================================================

    private String buildDisplayName(
            DnsRecord record) {

        if (record == null
                || record.getDomain() == null) {

            return "";
        }

        String domainName =
                record
                        .getDomain()
                        .getDomainName();

        String hostname =
                record.getHostname();

        if (hostname == null
                || hostname.isBlank()
                || "@".equals(hostname)) {

            return domainName;
        }

        return hostname
                + "."
                + domainName;
    }
}