# Hệ thống quản lý tên miền và bản ghi DNS VNNIC

Ứng dụng quản lý tên miền và bản ghi DNS được xây dựng trong quá trình thực tập tại Trung tâm Internet Việt Nam (VNNIC). Hệ thống tập trung vào quản lý tên miền, DNS Record, kiểm tra tính hợp lệ của dữ liệu DNS và lưu lịch sử thay đổi. Phiên bản hiện tại tập trung vào quản lý dữ liệu trên hệ thống, chưa đồng bộ trực tiếp với DNS Server và chưa có quy trình phê duyệt yêu cầu thay đổi DNS.

## Công nghệ sử dụng

**Backend:** Java 17, Spring Boot, Spring MVC, Spring Data JPA, Hibernate  
**Frontend:** Thymeleaf, Bootstrap, SB Admin 2  
**Cơ sở dữ liệu:** MySQL  
**Công cụ phát triển:** Maven, Eclipse / Spring Tools, Git / GitHub

## Chức năng chính

Hệ thống hỗ trợ quản lý tên miền với các chức năng thêm, sửa, tìm kiếm, lọc theo trạng thái, kích hoạt/vô hiệu hóa, xóa mềm và xóa hoàn toàn.

DNS Record hiện hỗ trợ các loại `A`, `AAAA`, `CNAME`, `MX`, `NS`, `TXT`. Người dùng có thể thêm, sửa, tìm kiếm, lọc và xóa bản ghi theo từng tên miền.

Hostname được lưu ở dạng tương đối. Ví dụ với tên miền `example.vn`, các hostname có thể là `www`, `mail`, `portal`; ký hiệu `@` được sử dụng cho tên miền gốc.

Dữ liệu được kiểm tra trước khi lưu: bản ghi A kiểm tra địa chỉ IPv4, AAAA kiểm tra IPv6, CNAME/MX/NS kiểm tra hostname, MX yêu cầu priority và TTL phải là số nguyên dương. Hệ thống cũng kiểm tra bản ghi trùng dựa trên:

```text
domain_id + hostname + record_type + record_value
```

Ví dụ `www | A | 203.119.10.10` không thể được tạo hai lần, nhưng `www | A | 203.119.10.10` và `www | A | 203.119.10.11` vẫn được chấp nhận.

## Kiến trúc hệ thống

Hệ thống được chia thành các tầng:

```text
Browser → Controller → Service → Validation → Repository → MySQL
```

- **Controller:** nhận HTTP request và xử lý dữ liệu từ giao diện.
- **Service:** xử lý logic nghiệp vụ, chuẩn hóa dữ liệu, validation, kiểm tra trùng lặp và các thao tác CRUD.
- **Repository:** làm việc với MySQL thông qua Spring Data JPA.
- **Entity:** ánh xạ các đối tượng Java với dữ liệu trong cơ sở dữ liệu.
- **Thymeleaf:** hiển thị dữ liệu từ backend lên giao diện.

Các package chính:

```text
controller  → PageController, DnsRecordController, HistoryController
entity      → Domain, DnsRecord, DnsRecordHistory
repository  → DomainRepository, DnsRecordRepository, DnsRecordHistoryRepository
service     → DomainService, DnsRecordService, DnsRecordHistoryService
validation  → DomainValidation, DnsRecordValidator
```

## Lịch sử thay đổi và xóa dữ liệu

Các thao tác `CREATE`, `UPDATE` và `DELETE` của DNS Record được lưu vào lịch sử. Khi cập nhật, hệ thống lưu dữ liệu trước và sau thay đổi để hỗ trợ việc truy vết. Lịch sử có thể được xem theo tên miền, DNS Record, loại thao tác và thời gian.

Hệ thống sử dụng xóa mềm trước khi xóa hoàn toàn. Khi xóa mềm, dữ liệu vẫn được giữ trong cơ sở dữ liệu nhưng bản ghi chuyển sang trạng thái `DELETED` và lưu thời gian xóa tại `deleted_at`. Xóa hoàn toàn sẽ loại bỏ vật lý bản ghi khỏi cơ sở dữ liệu.

## Chạy hệ thống

Yêu cầu: **Java 17+, MySQL và Maven**.

Tạo cơ sở dữ liệu:

```sql
CREATE DATABASE vnnic_dns_manager;
```

Cấu hình kết nối MySQL trong `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vnnic_dns_manager
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

Chạy bằng Maven:

```bash
mvnw.cmd spring-boot:run
```

Hoặc chạy trực tiếp ứng dụng Spring Boot từ Eclipse / Spring Tools. Sau khi khởi động, hệ thống có thể được truy cập tại:

```text
http://localhost:8080
```

## Phạm vi phiên bản hiện tại

Phiên bản hiện tại đã có các chức năng quản lý tên miền, quản lý DNS Record, kiểm tra dữ liệu DNS, kiểm tra bản ghi trùng, tìm kiếm/lọc, xóa mềm/xóa hoàn toàn, lịch sử thay đổi và Dashboard. Đồng bộ trực tiếp với DNS Server và quy trình phê duyệt yêu cầu thay đổi DNS chưa nằm trong phạm vi của phiên bản này.
