# VNNIC Domain & DNS Record Management System

Hệ thống quản lý tập trung tên miền và các bản ghi DNS được xây dựng trong quá trình thực tập tại **Trung tâm Internet Việt Nam (VNNIC)**.

Hệ thống tập trung vào quản lý Domain, DNS Record, kiểm tra tính hợp lệ của dữ liệu DNS và lưu lịch sử thay đổi.

> Version 1 chưa tích hợp trực tiếp với DNS Server và chưa triển khai quy trình phê duyệt DNS Change Request.

---

## 1. Technology Stack

### Backend
- Java 17+
- Spring Boot
- Spring Web / Spring MVC
- Spring Data JPA
- Hibernate

### Database
- MySQL

### Frontend
- Thymeleaf
- SB Admin 2
- Bootstrap

### Development
- Maven
- Eclipse / Spring Tools
- Git / GitHub

---

## 2. System Architecture

Hệ thống được tổ chức theo kiến trúc phân lớp:

```text
Browser
   |
   v
Controller
   |
   v
Service
   |
   +------> Validator
   |
   v
Repository
   |
   v
Spring Data JPA / Hibernate
   |
   v
MySQL
```

### Controller Layer

Tiếp nhận HTTP request, xử lý dữ liệu từ giao diện và điều hướng tới Thymeleaf template.

Các controller chính:

```text
PageController
DnsRecordController
HistoryController
```

### Service Layer

Chứa business logic của hệ thống:

```text
DomainService
DnsRecordService
DnsRecordHistoryService
```

Service chịu trách nhiệm:

- Chuẩn hóa dữ liệu
- Validation
- Duplicate checking
- Create / Update / Delete
- Soft delete / Hard delete
- Ghi lịch sử thay đổi

### Repository Layer

Sử dụng Spring Data JPA để thao tác với MySQL.

```text
DomainRepository
DnsRecordRepository
DnsRecordHistoryRepository
```

---

## 3. Core Database Model

Hệ thống Version 1 sử dụng ba bảng chính:

```text
DOMAIN
   |
   | 1
   |
   | N
   v
DNS_RECORD
   |
   v
DNS_RECORD_HISTORY
```

### DOMAIN

Lưu thông tin tên miền:

```text
id
domain_name
description
status
created_at
updated_at
deleted_at
```

Domain có các trạng thái:

```text
ACTIVE
INACTIVE
DELETED
```

Tên miền được chuẩn hóa về lowercase trước khi lưu.

Ví dụ:

```text
VNNIC.VN
```

được chuẩn hóa thành:

```text
vnnic.vn
```

---

## 4. DNS Record Management

Mỗi DNS Record bắt buộc thuộc về một Domain.

Các trường chính:

```text
id
domain_id
hostname
record_type
record_value
ttl
priority
status
description
created_at
updated_at
deleted_at
```

### Supported Record Types

Version 1 hỗ trợ:

```text
A
AAAA
CNAME
MX
NS
TXT
```

Kiến trúc cho phép mở rộng thêm:

```text
SRV
CAA
PTR
DS
DNSKEY
```

---

## 5. Relative Hostname

Hostname được lưu dưới dạng tương đối thay vì lưu toàn bộ FQDN.

Ví dụ với domain:

```text
example.vn
```

Thay vì lưu:

```text
www.example.vn
mail.example.vn
portal.example.vn
```

hệ thống lưu:

```text
www
mail
portal
```

Domain root được biểu diễn bằng:

```text
@
```

Ví dụ:

| Hostname | Type | Value | TTL | Priority |
|---|---|---|---:|---:|
| www | A | 203.119.10.10 | 3600 | |
| www6 | AAAA | 2001:dc8::10 | 3600 | |
| portal | CNAME | www.example.vn | 3600 | |
| @ | MX | mail.example.vn | 3600 | 10 |

---

## 6. DNS Validation

Dữ liệu được kiểm tra trước khi ghi xuống database.

### A Record

Value phải là IPv4 hợp lệ.

Valid:

```text
203.119.10.10
```

Invalid:

```text
999.999.999.999
```

### AAAA Record

Value phải là IPv6 hợp lệ.

Ví dụ:

```text
2001:dc8::10
```

### CNAME / NS

Value phải có định dạng hostname hợp lệ.

### MX

MX Record yêu cầu:

```text
Valid hostname
+
Priority
```

Ví dụ:

```text
Hostname : @
Type     : MX
Value    : mail.example.vn
Priority : 10
TTL      : 3600
```

### TTL

TTL phải là số nguyên dương.

Ví dụ:

```text
300
3600
86400
```

---

## 7. Duplicate Detection

Hệ thống không cho phép tạo hai DNS Record hoàn toàn giống nhau.

Duplicate được xác định theo:

```text
domain_id
+
hostname
+
record_type
+
record_value
```

Ví dụ không hợp lệ:

```text
www | A | 203.119.10.10
www | A | 203.119.10.10
```

Record thứ hai sẽ bị từ chối.

Tuy nhiên hệ thống vẫn cho phép:

```text
www | A | 203.119.10.10
www | A | 203.119.10.11
```

Điều này cho phép nhiều record cùng hostname/type nhưng có value khác nhau.

---

## 8. DNS Record Processing Flow

Ví dụ khi tạo một DNS Record:

```text
HTTP POST
   |
   v
DnsRecordController
   |
   v
DnsRecordService
   |
   +----> Find Domain
   |
   +----> Normalize Input
   |
   +----> Validate DNS Data
   |
   +----> Duplicate Check
   |
   v
DnsRecordRepository
   |
   v
MySQL
   |
   v
DNS Record History
```

Nếu validation thất bại:

```text
Invalid Input
     |
     v
Service Exception
     |
     v
Controller
     |
     v
Error Message
     |
     v
Thymeleaf Form
```

Lỗi nghiệp vụ được hiển thị trên giao diện thay vì chuyển người dùng tới Whitelabel Error Page.

---

## 9. Soft Delete & Hard Delete

Hệ thống hỗ trợ hai mức xóa.

### Soft Delete

Record không bị xóa ngay khỏi database.

Thay vào đó:

```text
status = DELETED
deleted_at = current timestamp
```

Điều này giúp dữ liệu vẫn có thể được truy vết.

### Hard Delete

Xóa vật lý record khỏi database khi thỏa mãn điều kiện nghiệp vụ.

```text
ACTIVE
   |
   v
Soft Delete
   |
   v
DELETED
   |
   v
Hard Delete
   |
   v
Removed
```

---

## 10. Change History

Mọi thay đổi DNS Record đều được ghi lại.

Các action:

```text
CREATE
UPDATE
DELETE
```

History lưu cả trạng thái trước và sau thay đổi.

Ví dụ:

### Before

```text
Hostname : www
Type     : A
Value    : 203.119.10.10
TTL      : 3600
```

### After

```text
Hostname : www
Type     : A
Value    : 203.119.10.20
TTL      : 3600
```

History:

```text
Action       : UPDATE
Before Value : 203.119.10.10
After Value  : 203.119.10.20
Changed At   : timestamp
```

Hệ thống hỗ trợ xem lịch sử:

- Toàn hệ thống
- Theo Domain
- Theo DNS Record
- Theo action type
- Theo thời gian

---

## 11. Main Features

### Domain Management

- Create Domain
- View Domain
- Update Domain
- Search Domain
- Filter by status
- Activate / Deactivate
- Soft Delete
- Hard Delete
- Count active DNS Records

### DNS Record Management

- Create DNS Record
- View DNS Record
- Update DNS Record
- Search DNS Record
- Filter by Record Type
- Soft Delete
- Hard Delete

### Validation

- IPv4 validation
- IPv6 validation
- Hostname validation
- MX Priority validation
- TTL validation
- Duplicate detection

### History

- CREATE history
- UPDATE history
- DELETE history
- Before / After snapshot
- Domain history
- Record history
- Search / Filter

---

## 12. Project Structure

```text
src/main/java/vn/vnnic/dnsmanager
|
+-- controller
|   +-- PageController
|   +-- DnsRecordController
|   +-- HistoryController
|
+-- entity
|   +-- Domain
|   +-- DnsRecord
|   +-- DnsRecordHistory
|
+-- repository
|   +-- DomainRepository
|   +-- DnsRecordRepository
|   +-- DnsRecordHistoryRepository
|
+-- service
    +-- DomainService
    +-- DnsRecordService
    +-- DnsRecordHistoryService
```

Frontend:

```text
src/main/resources/templates
```

Configuration:

```text
src/main/resources/application.properties
```

---

## 13. Running the Project

### Requirements

```text
Java 17+
MySQL
Maven
```

Create the MySQL database:

```sql
CREATE DATABASE vnnic_dns_manager;
```

Database configuration:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vnnic_dns_manager
spring.datasource.username=root
spring.datasource.password=
```

Run with Maven Wrapper:

### Windows

```bash
mvnw.cmd spring-boot:run
```

Or run the Spring Boot application directly from Eclipse / Spring Tools.

The application is available at:

```text
http://localhost:8080
```

---

## 14. Future Development

The current database and architecture are designed to support future modules:

```text
DNS_CHANGE_REQUEST
DNS_CHANGE_REQUEST_DETAIL
DNS_SYNC_JOB
DNS_SYNC_LOG
```

Expected future workflow:

```text
DNS Change Request
        |
        v
     Approval
     /      \
 Approved  Rejected
     |
     v
 DNS Sync Job
     |
     v
VNNIC DNS Hosting
     |
     v
  Sync Log
```

This will allow the system to evolve from a DNS data management application into a DNS change-management and synchronization platform.

---

## Project Status

**Version 1 – Core DNS Management**

Implemented:

- Domain Management
- DNS Record Management
- DNS Validation
- Duplicate Detection
- Soft / Hard Delete
- Change History
- Search / Filter
- Dashboard