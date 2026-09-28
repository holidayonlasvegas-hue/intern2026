package vn.vnnic.dnsmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication //annotation
public class VnnicDnsManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(VnnicDnsManagerApplication.class, args);
	}

}
//public: cho phép JVM truy cập method từ bên ngoài class.
//static: có thể gọi main() mà không cần tạo object của class.
//void: method không trả về giá trị.
//main: tên method đặc biệt mà JVM nhận biết là điểm bắt đầu chương trình.
//String[] args: mảng chứa các tham số được truyền vào khi khởi động chương trình.
//SpringApplication: class của Spring Boot hỗ trợ khởi động ứng dụng.
//run(): method thực hiện quá trình khởi động.
//VnnicDnsManagerApplication.class: chỉ cho Spring biết class cấu hình chính của ứng dụng.
//args: chuyển các tham số khởi động từ main() cho Spring Boot.