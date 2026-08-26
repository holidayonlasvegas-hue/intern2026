package vn.vnnic.dnsmanager.validation;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import vn.vnnic.dnsmanager.entity.DnsRecord;

@Component
public class DnsRecordValidator {

    // 
    // CÁC RECORD TYPE ĐƯỢC HỖ TRỢ TRONG VERSION 1
    // 

    private static final Set<String> SUPPORTED_TYPES =
            Set.of(
                    "A",
                    "AAAA",
                    "CNAME",
                    "MX",
                    "NS",
                    "TXT"
            );


    // 
    // HOSTNAME / DOMAIN NAME
    //
    // Ví dụ hợp lệ:
    //
    // www.example.vn
    // mail.example.vn
    // ns1.example.vn
    //
    // Mỗi label:
    // - chữ cái
    // - số
    // - dấu -
    // - không bắt đầu/kết thúc bằng -
    // 

    private static final Pattern HOSTNAME_PATTERN =
            Pattern.compile(
                    "^(?=.{1,253}\\.?$)"
                    + "(?:"
                    + "[a-zA-Z0-9]"
                    + "(?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?"
                    + "\\."
                    + ")*"
                    + "[a-zA-Z0-9]"
                    + "(?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?"
                    + "\\.?$"
            );


    // 
    // VALIDATE
    // 

    public void validate(
            DnsRecord record) {


        if (record == null) {

            throw new IllegalArgumentException(
                    "DNS Record không được null."
            );
        }


       // 
        // DOMAIN BẮT BUỘC
       // 

        if (record.getDomain() == null
                || record.getDomain().getId() == null) {

            throw new IllegalArgumentException(
                    "DNS Record bắt buộc phải thuộc về một tên miền."
            );
        }


       // 
        // HOSTNAME BẮT BUỘC
        //
        // @ được phép vì là tên miền gốc.
       // 

        if (record.getHostname() == null
                || record.getHostname().isBlank()) {

            throw new IllegalArgumentException(
                    "Hostname không được để trống."
            );
        }


        validateRelativeHostname(
                record.getHostname()
        );


       // 
        // RECORD TYPE BẮT BUỘC
       // 

        if (record.getRecordType() == null
                || record.getRecordType().isBlank()) {

            throw new IllegalArgumentException(
                    "Record Type không được để trống."
            );
        }


        String type =
                record
                        .getRecordType()
                        .trim()
                        .toUpperCase();


       // 
        // VERSION 1
       // 

        if (!SUPPORTED_TYPES.contains(type)) {

            throw new IllegalArgumentException(
                    "Record Type "
                            + type
                            + " chưa được hỗ trợ trong phiên bản 1. "
                            + "Các loại hỗ trợ: "
                            + "A, AAAA, CNAME, MX, NS, TXT."
            );
        }


       // 
        // VALUE BẮT BUỘC
       // 

        if (record.getRecordValue() == null
                || record.getRecordValue().isBlank()) {

            throw new IllegalArgumentException(
                    "Record Value không được để trống."
            );
        }


       // 
        // TTL
       // 

        if (record.getTtl() == null) {

            throw new IllegalArgumentException(
                    "TTL không được để trống."
            );
        }


        if (record.getTtl() <= 0) {

            throw new IllegalArgumentException(
                    "TTL phải là số nguyên dương."
            );
        }


       // 
        // VALIDATE THEO TYPE
       // 

        switch (type) {

            case "A" ->
                    validateIPv4(
                            record.getRecordValue()
                    );


            case "AAAA" ->
                    validateIPv6(
                            record.getRecordValue()
                    );


            case "CNAME" ->
                    validateHostnameValue(
                            record.getRecordValue(),
                            "CNAME"
                    );


            case "MX" ->
                    validateMx(record);


            case "NS" ->
                    validateHostnameValue(
                            record.getRecordValue(),
                            "NS"
                    );


            case "TXT" ->
                    validateTxt(
                            record.getRecordValue()
                    );


            default ->
                    throw new IllegalArgumentException(
                            "Record Type không hợp lệ."
                    );
        }
    }


    // 
    // RELATIVE HOSTNAME
    //
    // Đề yêu cầu lưu:
    //
    // @
    // www
    // mail
    // api
    // portal
    //
    // Không lưu www.example.vn
    // 

    private void validateRelativeHostname(
            String hostname) {

        String value =
                hostname.trim();


        if ("@".equals(value)) {

            return;
        }


        /*
         * Vì database lưu hostname tương đối,
         * không cho dấu chấm.
         *
         * Ví dụ:
         *
         * www       OK
         * mail      OK
         * api       OK
         *
         * www.example.vn   KHÔNG OK
         */
        if (value.contains(".")) {

            throw new IllegalArgumentException(
                    "Hostname phải được lưu ở dạng tương đối. "
                            + "Ví dụ dùng 'www' thay vì "
                            + "'www.example.vn'."
            );
        }


        if (value.length() > 63) {

            throw new IllegalArgumentException(
                    "Hostname tương đối không được vượt quá 63 ký tự."
            );
        }


        if (!value.matches(
                "^[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$"
        )) {

            throw new IllegalArgumentException(
                    "Hostname không hợp lệ: "
                            + hostname
            );
        }
    }


    // 
    // IPv4
    //
    // Không dùng regex kiểu:
    //
    // \d+\.\d+\.\d+\.\d+
    //
    // vì 999.999.999.999 cũng match.
    // 

    private void validateIPv4(
            String value) {

        String ip =
                value.trim();


        String[] parts =
                ip.split(
                        "\\.",
                        -1
                );


        if (parts.length != 4) {

            throw new IllegalArgumentException(
                    "Bản ghi A phải chứa IPv4 hợp lệ."
            );
        }


        for (String part : parts) {

            if (part.isEmpty()) {

                throw new IllegalArgumentException(
                        "Bản ghi A phải chứa IPv4 hợp lệ."
                );
            }


            /*
             * Chỉ cho chữ số.
             */
            for (char c : part.toCharArray()) {

                if (!Character.isDigit(c)) {

                    throw new IllegalArgumentException(
                            "Bản ghi A phải chứa IPv4 hợp lệ."
                    );
                }
            }


            /*
             * Tránh dạng quá dài.
             */
            if (part.length() > 3) {

                throw new IllegalArgumentException(
                        "Bản ghi A phải chứa IPv4 hợp lệ."
                );
            }


            int number;


            try {

                number =
                        Integer.parseInt(part);

            } catch (NumberFormatException e) {

                throw new IllegalArgumentException(
                        "Bản ghi A phải chứa IPv4 hợp lệ."
                );
            }


            if (number < 0
                    || number > 255) {

                throw new IllegalArgumentException(
                        "IPv4 không hợp lệ. "
                                + "Mỗi octet phải nằm trong khoảng 0-255."
                );
            }
        }
    }


    // 
    // IPv6
    // 

    private void validateIPv6(
            String value) {

        String ip =
                value.trim();


        /*
         * IPv6 phải có dấu :
         *
         * Điều này ngăn InetAddress coi IPv4
         * là địa chỉ hợp lệ trong validator AAAA.
         */
        if (!ip.contains(":")) {

            throw new IllegalArgumentException(
                    "Bản ghi AAAA phải chứa IPv6 hợp lệ."
            );
        }


        try {

            InetAddress address =
                    InetAddress.getByName(ip);


            if (!(address instanceof Inet6Address)) {

                throw new IllegalArgumentException(
                        "Bản ghi AAAA phải chứa IPv6 hợp lệ."
                );
            }

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "IPv6 không hợp lệ: "
                            + value
            );
        }
    }


    // 
    // CNAME / NS VALUE
    // 

    private void validateHostnameValue(
            String value,
            String type) {

        String hostname =
                normalizeHostnameValue(
                        value
                );


        if (!HOSTNAME_PATTERN
                .matcher(hostname)
                .matches()) {

            throw new IllegalArgumentException(
                    "Giá trị của bản ghi "
                            + type
                            + " phải là hostname hợp lệ."
            );
        }
    }


    // 
    // MX
    // 

    private void validateMx(
            DnsRecord record) {


        // Priority bắt buộc

        if (record.getPriority() == null) {

            throw new IllegalArgumentException(
                    "Bản ghi MX bắt buộc phải có Priority."
            );
        }


        if (record.getPriority() < 0
                || record.getPriority() > 65535) {

            throw new IllegalArgumentException(
                    "Priority của MX phải nằm trong khoảng 0-65535."
            );
        }


        // Value phải là mail server hostname

        validateHostnameValue(
                record.getRecordValue(),
                "MX"
        );
    }


    // 
    // TXT
    // 

    private void validateTxt(
            String value) {

        if (value == null
                || value.isBlank()) {

            throw new IllegalArgumentException(
                    "Giá trị TXT không được để trống."
            );
        }


        /*
         * Entity cho phép record_value tối đa 2000.
         */
        if (value.length() > 2000) {

            throw new IllegalArgumentException(
                    "Giá trị TXT không được vượt quá 2000 ký tự."
            );
        }
    }


    // 
    // NORMALIZE HOSTNAME VALUE
    //
    // Cho phép người dùng nhập FQDN có dấu chấm cuối:
    //
    // mail.example.vn.
    //
    // cũng như:
    //
    // mail.example.vn
    // 

    private String normalizeHostnameValue(
            String value) {

        if (value == null) {

            return "";
        }


        return value.trim();
    }

}