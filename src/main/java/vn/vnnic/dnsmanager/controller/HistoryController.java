package vn.vnnic.dnsmanager.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import vn.vnnic.dnsmanager.entity.DnsRecordHistory;
import vn.vnnic.dnsmanager.service.DnsRecordHistoryService;
import vn.vnnic.dnsmanager.service.DomainService;

@Controller
public class HistoryController {

    private final DnsRecordHistoryService historyService;
    private final DomainService domainService;


    // 
    // CONSTRUCTOR
    // 

    public HistoryController(
            DnsRecordHistoryService historyService,
            DomainService domainService) {

        this.historyService = historyService;
        this.domainService = domainService;
    }


    // 
    // HISTORY
    //
    // Hỗ trợ kết hợp:
    //
    // domainId
    // recordId
    // actionType
    // fromDate
    // toDate
    // 

    @GetMapping("/history")
    public String history(

            @RequestParam(required = false)
            Long domainId,

            @RequestParam(required = false)
            Long recordId,

            @RequestParam(required = false)
            String actionType,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate toDate,

            Model model) {


       // 
        // CHUYỂN LocalDate -> LocalDateTime
       // 

        LocalDateTime startTime = null;

        LocalDateTime endTime = null;


        // Từ 00:00:00 của ngày bắt đầu
        if (fromDate != null) {

            startTime =
                    fromDate
                            .atStartOfDay();
        }


        // Đến 23:59:59.999... của ngày kết thúc
        if (toDate != null) {

            endTime =
                    toDate
                            .plusDays(1)
                            .atStartOfDay()
                            .minusNanos(1);
        }


       // 
        // KIỂM TRA KHOẢNG NGÀY
       // 

        if (fromDate != null
                && toDate != null
                && fromDate.isAfter(toDate)) {

            model.addAttribute(
                    "errorMessage",
                    "Ngày bắt đầu không được lớn hơn ngày kết thúc."
            );


            // Đổi lại thành null để không chạy filter thời gian sai
            startTime = null;
            endTime = null;
        }


       // 
        // SEARCH HISTORY
       // 

        List<DnsRecordHistory> histories =
                historyService
                        .searchHistory(
                                domainId,
                                recordId,
                                actionType,
                                startTime,
                                endTime
                        );


       // 
        // MODEL - HISTORY
       // 

        model.addAttribute(
                "histories",
                histories
        );


       // 
        // MODEL - DOMAIN DROPDOWN
       // 

        model.addAttribute(
                "domains",
                domainService.getAllDomains()
        );


       // 
        // GIỮ LẠI FILTER SAU KHI SUBMIT
       // 

        model.addAttribute(
                "domainId",
                domainId
        );


        model.addAttribute(
                "recordId",
                recordId
        );


        model.addAttribute(
                "actionType",
                actionType
        );


        model.addAttribute(
                "fromDate",
                fromDate
        );


        model.addAttribute(
                "toDate",
                toDate
        );


        return "history";
    }

}