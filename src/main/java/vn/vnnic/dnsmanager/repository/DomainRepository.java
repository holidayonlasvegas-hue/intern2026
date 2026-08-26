package vn.vnnic.dnsmanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.vnnic.dnsmanager.entity.Domain;

@Repository
public interface DomainRepository
        extends JpaRepository<Domain, Long> {


    // 
    // SEARCH DOMAIN
    // 

    List<Domain> findByDomainNameContainingIgnoreCase(
            String keyword
    );


    // 
    // FILTER STATUS
    // 

    List<Domain> findByStatus(
            String status
    );


    // 
    // SEARCH + FILTER
    // 

    List<Domain> findByDomainNameContainingIgnoreCaseAndStatus(
            String keyword,
            String status
    );


    // 
    // CHECK DOMAIN DUPLICATE KHI CREATE
    // 

    boolean existsByDomainNameIgnoreCase(
            String domainName
    );


    // 
    // CHECK DOMAIN DUPLICATE KHI UPDATE
    //
    // Kiểm tra tên đã tồn tại nhưng bỏ qua domain hiện tại.
    // 

    boolean existsByDomainNameIgnoreCaseAndIdNot(
            String domainName,
            Long id
    );


    // 
    // DASHBOARD
    // 

    long countByStatus(
            String status
    );


    long countByStatusNot(
            String status
    );

}