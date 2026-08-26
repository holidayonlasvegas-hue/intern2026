package vn.vnnic.dnsmanager.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.vnnic.dnsmanager.entity.Domain;
import vn.vnnic.dnsmanager.service.DnsRecordHistoryService;
import vn.vnnic.dnsmanager.service.DnsRecordService;
import vn.vnnic.dnsmanager.service.DomainService;

@Controller
public class PageController {

    private final DomainService domainService;
    private final DnsRecordService dnsRecordService;
    private final DnsRecordHistoryService historyService;


    // 
    // CONSTRUCTOR
    // 

    public PageController(
            DomainService domainService,
            DnsRecordService dnsRecordService,
            DnsRecordHistoryService historyService) {

        this.domainService = domainService;
        this.dnsRecordService = dnsRecordService;
        this.historyService = historyService;
    }


    // 
    // DASHBOARD
    // 

    @GetMapping("/")
    public String home(
            Model model) {

        model.addAttribute(
                "totalDomains",
                domainService.countManagedDomains()
        );

        model.addAttribute(
                "activeDomains",
                domainService.countActiveDomains()
        );

        model.addAttribute(
                "inactiveDomains",
                domainService.countInactiveDomains()
        );

        model.addAttribute(
                "totalRecords",
                dnsRecordService.countManagedRecords()
        );

        model.addAttribute(
                "recentChanges",
                historyService.countRecentChanges()
        );

        return "index";
    }


    // 
    // DOMAIN LIST
    // SEARCH + FILTER
    // 

    @GetMapping("/domains")
    public String domains(
            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            String status,

            Model model) {

        loadDomainsPage(
                keyword,
                status,
                model
        );

        return "domains";
    }


    // 
    // CREATE DOMAIN
    // 

    @PostMapping("/domains")
    public String createDomain(
            Domain domain,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            Domain savedDomain =
                    domainService.createDomain(domain);


            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã thêm tên miền "
                            + savedDomain.getDomainName()
                            + " thành công."
            );


            return "redirect:/domains";


        } catch (IllegalArgumentException
                | IllegalStateException e) {

            /*
             * Không sang Whitelabel.
             * Trả lại chính trang domains.html.
             */

            model.addAttribute(
                    "errorMessage",
                    e.getMessage()
            );


            /*
             * Giữ dữ liệu người dùng vừa nhập
             * để có thể fill lại form.
             */
            model.addAttribute(
                    "domainForm",
                    domain
            );


            loadDomainsPage(
                    null,
                    null,
                    model
            );


            return "domains";
        }
    }


    // 
    // DOMAIN DETAIL
    // 

    @GetMapping("/domains/{id}")
    public String domainDetail(
            @PathVariable
            Long id,

            Model model) {

        Domain domain =
                domainService.getDomainById(id);


        loadDomainDetail(
                domain,
                model
        );


        return "domain-detail";
    }


    // 
    // UPDATE DOMAIN
    // 

    @PostMapping("/domains/{id}/edit")
    public String updateDomain(
            @PathVariable
            Long id,

            Domain domain,

            Model model,

            RedirectAttributes redirectAttributes) {

        try {

            Domain updatedDomain =
                    domainService.updateDomain(
                            id,
                            domain
                    );


            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã cập nhật tên miền "
                            + updatedDomain.getDomainName()
                            + " thành công."
            );


            return "redirect:/domains/" + id;


        } catch (IllegalArgumentException
                | IllegalStateException e) {

            model.addAttribute(
                    "errorMessage",
                    e.getMessage()
            );


            Domain currentDomain =
                    domainService.getDomainById(id);


            loadDomainDetail(
                    currentDomain,
                    model
            );


            return "domain-detail";
        }
    }


    // 
    // ACTIVATE DOMAIN
    //
    // INACTIVE -> ACTIVE
    // 

    @PostMapping("/domains/{id}/activate")
    public String activateDomain(
            @PathVariable
            Long id,

            Model model,

            RedirectAttributes redirectAttributes) {

        try {

            Domain domain =
                    domainService.activateDomain(id);


            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã kích hoạt tên miền "
                            + domain.getDomainName()
                            + "."
            );


            return "redirect:/domains/" + id;


        } catch (IllegalArgumentException
                | IllegalStateException e) {

            return showDomainOperationError(
                    id,
                    e.getMessage(),
                    model
            );
        }
    }


    // 
    // DEACTIVATE DOMAIN
    //
    // ACTIVE -> INACTIVE
    // 

    @PostMapping("/domains/{id}/deactivate")
    public String deactivateDomain(
            @PathVariable
            Long id,

            Model model,

            RedirectAttributes redirectAttributes) {

        try {

            Domain domain =
                    domainService.deactivateDomain(id);


            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã ngừng quản lý tên miền "
                            + domain.getDomainName()
                            + "."
            );


            return "redirect:/domains/" + id;


        } catch (IllegalArgumentException
                | IllegalStateException e) {

            return showDomainOperationError(
                    id,
                    e.getMessage(),
                    model
            );
        }
    }


    // 
    // SOFT DELETE DOMAIN
    //
    // ACTIVE / INACTIVE -> DELETED
    // 

    @PostMapping("/domains/{id}/delete")
    public String softDeleteDomain(
            @PathVariable
            Long id,

            Model model,

            RedirectAttributes redirectAttributes) {

        try {

            Domain domain =
                    domainService.softDeleteDomain(id);


            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Tên miền "
                            + domain.getDomainName()
                            + " đã chuyển sang DELETED."
            );


            return "redirect:/domains/" + id;


        } catch (IllegalArgumentException
                | IllegalStateException e) {

            return showDomainOperationError(
                    id,
                    e.getMessage(),
                    model
            );
        }
    }


    // 
    // HARD DELETE DOMAIN
    //
    // Chỉ:
    // status = DELETED
    // và không còn DNS Record
    // 

    @PostMapping("/domains/{id}/hard-delete")
    public String hardDeleteDomain(
            @PathVariable
            Long id,

            Model model,

            RedirectAttributes redirectAttributes) {

        try {

            /*
             * Lấy tên trước khi xóa,
             * vì sau delete không còn entity nữa.
             */
            Domain domain =
                    domainService.getDomainById(id);


            String domainName =
                    domain.getDomainName();


            domainService.hardDeleteDomain(id);


            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Đã xóa vĩnh viễn tên miền "
                            + domainName
                            + "."
            );


            return "redirect:/domains";


        } catch (IllegalArgumentException
                | IllegalStateException e) {

            /*
             * Domain vẫn tồn tại nếu hard-delete thất bại,
             * ví dụ vì còn DNS Record.
             */
            return showDomainOperationError(
                    id,
                    e.getMessage(),
                    model
            );
        }
    }


    // 
    // HELPER:
    // LOAD DOMAIN LIST PAGE
    // 

    private void loadDomainsPage(
            String keyword,
            String status,
            Model model) {

        List<Domain> domains =
                domainService.filterDomains(
                        keyword,
                        status
                );


        Map<Long, Long> recordCounts =
                new HashMap<>();


        for (Domain domain : domains) {

            long count =
                    dnsRecordService
                            .countActiveRecords(
                                    domain.getId()
                            );


            recordCounts.put(
                    domain.getId(),
                    count
            );
        }


        model.addAttribute(
                "domains",
                domains
        );


        model.addAttribute(
                "recordCounts",
                recordCounts
        );


        model.addAttribute(
                "keyword",
                keyword
        );


        model.addAttribute(
                "status",
                status
        );
    }


    // 
    // HELPER:
    // LOAD DOMAIN DETAIL
    // 

    private void loadDomainDetail(
            Domain domain,
            Model model) {

        long recordCount =
                dnsRecordService
                        .countActiveRecords(
                                domain.getId()
                        );


        model.addAttribute(
                "domain",
                domain
        );


        model.addAttribute(
                "recordCount",
                recordCount
        );
    }


    // 
    // HELPER:
    // SHOW ERROR ON DOMAIN DETAIL PAGE
    // 

    private String showDomainOperationError(
            Long id,
            String message,
            Model model) {

        model.addAttribute(
                "errorMessage",
                message
        );


        Domain domain =
                domainService.getDomainById(id);


        loadDomainDetail(
                domain,
                model
        );


        return "domain-detail";
    }

}