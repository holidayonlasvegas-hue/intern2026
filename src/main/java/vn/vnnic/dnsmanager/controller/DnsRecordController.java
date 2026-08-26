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
    // CONSTRUCTOR

    public DnsRecordController(
            DnsRecordService dnsRecordService,
            DomainService domainService) {

        this.dnsRecordService = dnsRecordService;
        this.domainService = domainService;
    }


    // 1. DANH SÁCH DNS RECORD
    //
    // Hỗ trợ:
    // - theo Domain
    // - tìm kiếm hostname / value
    // - lọc theo record type
    // 

    @GetMapping("/records")
    public String records(

            @RequestParam(required = false)
            Long domainId,

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String recordType,

            Model model) {


        loadRecordsPage(
                domainId,
                keyword,
                recordType,
                model
        );


        return "records";
    }

    // 2. THÊM DNS RECORD

    @PostMapping("/records")
    public String createRecord(

            @RequestParam
            Long domainId,

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
             * Hiển thị lỗi trên records.html.
             * Không cho rơi vào Whitelabel.
             */

            model.addAttribute(
                    "errorMessage",
                    e.getMessage()
            );


            /*
             * Giữ dữ liệu người dùng vừa nhập.
             * records.html dùng recordForm
             * để điền lại modal Add Record.
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


    // 
    // 3. XEM CHI TIẾT DNS RECORD
    // 

    @GetMapping("/records/{id}")
    public String recordDetail(

            @PathVariable
            Long id,

            Model model) {


        DnsRecord record =
                dnsRecordService.getRecordById(id);


        model.addAttribute(
                "record",
                record
        );


        return "record-detail";
    }


    // 
    // 4. CẬP NHẬT DNS RECORD
    //
    // DnsRecordService sẽ:
    // - copy BEFORE
    // - normalize
    // - validate
    // - duplicate check
    // - save
    // - ghi History UPDATE
    // 

    @PostMapping("/records/{id}/edit")
    public String updateRecord(

            @PathVariable
            Long id,

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
             * Lấy lại dữ liệu thật trong DB.
             * Nếu validation thất bại thì transaction
             * không lưu dữ liệu mới.
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


    // 
    // 5. XÓA MỀM DNS RECORD
    //
    // ACTIVE / INACTIVE
    //          ↓
    //       DELETED
    //
    // Service đồng thời ghi History DELETE.
    // 

    @PostMapping("/records/{id}/delete")
    public String softDeleteRecord(

            @PathVariable
            Long id,

            Model model,

            RedirectAttributes redirectAttributes) {


        /*
         * Lấy Domain ID trước khi thao tác.
         */
        DnsRecord currentRecord =
                dnsRecordService.getRecordById(id);


        Long domainId =
                currentRecord
                        .getDomain()
                        .getId();


        try {

            DnsRecord deletedRecord =
                    dnsRecordService.deleteRecord(id);


            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "DNS Record "
                            + buildDisplayName(deletedRecord)
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


    // 
    // 6. HARD DELETE DNS RECORD
    //
    // Chỉ record DELETED mới được xóa vật lý.
    //
    // History vẫn tồn tại vì History chỉ lưu:
    // dnsRecordId
    // domainId
    // BEFORE / AFTER
    // 

    @PostMapping("/records/{id}/hard-delete")
    public String hardDeleteRecord(

            @PathVariable
            Long id,

            Model model,

            RedirectAttributes redirectAttributes) {


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

            dnsRecordService.hardDeleteRecord(
                    id
            );


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


    // 
    // HELPER:
    // LOAD records.html
    // 

    private void loadRecordsPage(

            Long domainId,
            String keyword,
            String recordType,
            Model model) {


        // 
        // RECORD LIST
        // 

        List<DnsRecord> records =
                dnsRecordService.searchRecords(
                        domainId,
                        keyword,
                        recordType
                );


        model.addAttribute(
                "records",
                records
        );


        // 
        // DOMAIN LIST
        //
        // Dùng cho dropdown Add Record.
        // 

        List<Domain> domains =
                domainService.getAllDomains();


        model.addAttribute(
                "domains",
                domains
        );


        // 
        // GIỮ FILTER
        // 

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


        // 
        // DOMAIN HIỆN TẠI
        // 

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
    }


    // 
    // HELPER:
    // HIỂN THỊ FQDN
    //
    // @ + example.vn
    //      ↓
    // example.vn
    //
    // www + example.vn
    //      ↓
    // www.example.vn
    // 

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