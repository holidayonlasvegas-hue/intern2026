package vn.vnnic.dnsmanager.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 
    // 400 - DỮ LIỆU / NGHIỆP VỤ KHÔNG HỢP LỆ
    // 

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgumentException(
            IllegalArgumentException exception,
            HttpServletRequest request,
            Model model) {

        prepareModel(
                model,
                request,
                "Dữ liệu không hợp lệ",
                exception.getMessage()
        );

        return "error";
    }


    // 
    // 409 - TRẠNG THÁI KHÔNG CHO PHÉP THAO TÁC
    //
    // Ví dụ:
    // - sửa record DELETED
    // - hard delete domain còn record
    // - activate domain DELETED
    // 

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalStateException(
            IllegalStateException exception,
            HttpServletRequest request,
            Model model) {

        prepareModel(
                model,
                request,
                "Không thể thực hiện thao tác",
                exception.getMessage()
        );

        return "error";
    }


    // 
    // DATABASE CONSTRAINT
    //
    // Ví dụ:
    // - domain_name UNIQUE
    // - foreign key
    // 

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request,
            Model model) {

        prepareModel(
                model,
                request,
                "Xung đột dữ liệu",
                "Không thể thực hiện thao tác vì dữ liệu đang "
                        + "được sử dụng hoặc đã tồn tại."
        );

        return "error";
    }


    // 
    // FALLBACK
    //
    // Bắt exception còn lại để tránh Whitelabel.
    // Không hiển thị stack trace ra giao diện.
    // 

    @ExceptionHandler(Exception.class)
    public String handleException(
            Exception exception,
            HttpServletRequest request,
            Model model) {

        prepareModel(
                model,
                request,
                "Đã xảy ra lỗi",
                "Hệ thống không thể hoàn thành yêu cầu. "
                        + "Vui lòng kiểm tra dữ liệu và thử lại."
        );

        return "error";
    }


    // 
    // COMMON MODEL
    // 

    private void prepareModel(
            Model model,
            HttpServletRequest request,
            String title,
            String message) {

        model.addAttribute(
                "errorTitle",
                title
        );

        model.addAttribute(
                "errorMessage",
                message != null
                        ? message
                        : "Đã xảy ra lỗi."
        );

        model.addAttribute(
                "requestPath",
                request.getRequestURI()
        );
    }
}