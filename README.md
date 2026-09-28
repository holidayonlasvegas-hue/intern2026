# VNNIC Domain & DNS Record Management System

Ứng dụng quản lý tên miền và DNS Record được xây dựng trong quá trình thực tập tại Trung tâm Internet Việt Nam (VNNIC). Project tập trung vào quản lý Domain, DNS Record, kiểm tra dữ liệu DNS và lưu lịch sử thay đổi. Phiên bản hiện tại quản lý dữ liệu trên hệ thống, chưa đồng bộ trực tiếp với DNS Server và chưa có quy trình phê duyệt DNS Change Request.

## Technologies

**Backend:** Java 17, Spring Boot, Spring MVC, Spring Data JPA, Hibernate  
**Frontend:** Thymeleaf, Bootstrap, SB Admin 2  
**Database:** MySQL  
**Tools:** Maven, Eclipse / Spring Tools, Git / GitHub

## Main Features

Hệ thống hỗ trợ quản lý Domain với các chức năng thêm, sửa, tìm kiếm, lọc theo trạng thái, activate/deactivate, soft delete và hard delete.

DNS Record hiện hỗ trợ các loại `A`, `AAAA`, `CNAME`, `MX`, `NS`, `TXT`. Người dùng có thể thêm, sửa, tìm kiếm, lọc và xóa record theo từng Domain.

Hostname được lưu ở dạng tương đối. Ví dụ với domain `example.vn`, các hostname có thể là `www`, `mail`, `portal`; ký hiệu `@` được sử dụng cho domain gốc.

Dữ liệu được validation trước khi lưu: A record kiểm tra IPv4, AAAA kiểm tra IPv6, CNAME/MX/NS kiểm tra hostname, MX yêu cầu priority và TTL phải là số nguyên dương. Hệ thống cũng kiểm tra duplicate dựa trên:

```text
domain_id + hostname + record_type + record_value
```

Ví dụ `www | A | 203.119.10.10` không thể được tạo hai lần, nhưng `www | A | 203.119.10.10` và `www | A | 203.119.10.11` vẫn được chấp nhận.

## Architecture

Project được chia thành các layer:

```text
Browser → Controller → Service → Validation → Repository → MySQL
```

- **Controller:** nhận HTTP request và xử lý dữ liệu từ giao diện.
- **Service:** xử lý business logic, normalization, validation, duplicate checking và các thao tác CRUD.
- **Repository:** làm việc với MySQL thông qua Spring Data JPA.
- **Entity:** ánh xạ các đối tượng Java với dữ liệu trong database.
- **Thymeleaf:** hiển thị dữ liệu từ backend lên giao diện.

Các package chính:

```text
controller  → PageController, DnsRecordController, HistoryController
entity      → Domain, DnsRecord, DnsRecordHistory
repository  → DomainRepository, DnsRecordRepository, DnsRecordHistoryRepository
service     → DomainService, DnsRecordService, DnsRecordHistoryService
validation  → DomainValidation, DnsRecordValidator
```

## History and Delete

Các thao tác `CREATE`, `UPDATE` và `DELETE` của DNS Record được lưu vào history. Khi update, hệ thống lưu dữ liệu trước và sau thay đổi để hỗ trợ truy vết. History có thể được xem theo Domain, DNS Record, action type và thời gian.

Hệ thống sử dụng soft delete trước khi hard delete. Khi soft delete, dữ liệu vẫn còn trong database nhưng record chuyển sang trạng thái `DELETED` và lưu `deleted_at`. Hard delete thực hiện xóa vật lý record khỏi database.

## Running the Project

Yêu cầu: **Java 17+, MySQL và Maven**.

Tạo database:

```sql
CREATE DATABASE vnnic_dns_manager;
```

Cấu hình MySQL trong `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vnnic_dns_manager
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

Chạy bằng Maven:

```bash
mvnw.cmd spring-boot:run
```

Hoặc chạy trực tiếp Spring Boot application từ Eclipse / Spring Tools. Sau khi khởi động, ứng dụng chạy tại:

```text
http://localhost:8080
```

## Current Scope

Version hiện tại đã có Domain Management, DNS Record Management, DNS Validation, Duplicate Detection, Search/Filter, Soft/Hard Delete, Change History và Dashboard. Đồng bộ trực tiếp với DNS Server và quy trình DNS Change Request chưa nằm trong phạm vi của version này.
