package vn.vnnic.dnsmanager.validation;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import vn.vnnic.dnsmanager.entity.Domain;

@Component
public class DomainValidation {

    /*
     * Domain hợp lệ:
     *
     * vnnic.vn
     * example.com
     * dns01.vnnic.vn
     * 123.vn
     * my-domain.vn
     *
     * Domain không hợp lệ:
     *
     * 123
     * example
     * -abc.vn
     * abc-.vn
     * abc..vn
     * abc vn
     */
    private static final Pattern DOMAIN_PATTERN =
            Pattern.compile(
                    "^(?=.{1,253}$)"
                    + "(?:[a-z0-9]"
                    + "(?:[a-z0-9-]{0,61}[a-z0-9])?"
                    + "\\.)+"
                    + "[a-z]{2,63}$",
                    Pattern.CASE_INSENSITIVE
            );

    public void validate(Domain domain) {

        if (domain == null) {
            throw new IllegalArgumentException(
                    "Domain không được để trống."
            );
        }

        String domainName =
                domain.getDomainName();

        if (domainName == null
                || domainName.isBlank()) {

            throw new IllegalArgumentException(
                    "Tên miền không được để trống."
            );
        }

        /*
         * Chuẩn hóa:
         *
         * "  VNNIC.VN  "
         *      ↓
         * "vnnic.vn"
         */
        domainName =
                domainName
                        .trim()
                        .toLowerCase();

        if (domainName.length() > 253) {

            throw new IllegalArgumentException(
                    "Tên miền không được vượt quá 253 ký tự."
            );
        }

        /*
         * Không cho:
         *
         * 123
         * abc
         * abc..vn
         * -abc.vn
         * abc-.vn
         */
        if (!DOMAIN_PATTERN
                .matcher(domainName)
                .matches()) {

            throw new IllegalArgumentException(
                    "Tên miền không hợp lệ. "
                    + "Ví dụ hợp lệ: vnnic.vn, example.com."
            );
        }

        /*
         * Gán lại tên miền đã chuẩn hóa.
         */
        domain.setDomainName(domainName);
    }
}