# BÁO CÁO PHÂN TÍCH LỖ HỔNG BẢO MẬT & LỖI LOGIC
## DỰ ÁN: RIKKEI INTELLIGENT BANKING & ASSISTANT SUITE (RikkeiPay)
**Phân hệ:** Trợ lý giao dịch thông minh RikkeiPay (Langfuse SDK Integration)  
**Bài tập:** Bài 2 - Dò Lỗi & Tối Ưu Code Tích Hợp SDK Tracing (Phần Phân Tích)

---

## 1. PHÂN TÍCH LỖ HỔNG BẢO MẬT (SECURITY VULNERABILITIES)

### 1.1. Hardcode Khóa Bí Mật & API Key Trực Tiếp Trong Mã Nguồn (Hardcoded Secret/API Keys)
- **Vị trí lỗi:** Class `LangfuseConfig.java`
  ```java
  return new LangfuseClient(
      "pk-lf-1234567890abcdef", 
      "sk-lf-0987654321fedcba", 
      "https://cloud.langfuse.com"
  );
  ```
- **Phân tích rủi ro:**
  - **Rò rỉ thông tin xác thực (Credential Leakage):** Việc đặt trực tiếp `Secret Key (sk-lf-...)` và `Public Key (pk-lf-...)` vào code sẽ khiến các khóa này bị đẩy lên hệ thống quản lý phiên bản (Git/GitHub/GitLab). Bất kỳ ai có quyền truy cập repository (kể cả đối tác hoặc thành viên nội bộ) đều có thể chiếm quyền đọc/ghi toàn bộ hệ thống telemetry Langfuse.
  - **Vi phạm nguyên tắc Twelve-Factor App:** Cấu hình môi trường (Config) bị gắn chặt với mã nguồn ứng dụng, gây khó khăn và rủi ro lớn khi chuyển đổi giữa các môi trường (Dev, Staging, UAT, Production).
  - **Khó khăn trong việc thu hồi/xoay vòng khóa (Key Rotation):** Khi API Key bị lộ hoặc cần thay thế định kỳ theo chuẩn bảo mật ngân hàng, lập trình viên buộc phải sửa code, build và deploy lại toàn bộ ứng dụng thay vì cập nhật biến môi trường / Secret Manager.

---

### 1.2. Rò Rỉ Dữ Liệu Định Danh Cá Nhân & Thông Tin Tài Chính Nhạy Cảm (PII Data Exposure)
- **Vị trí lỗi:** Class `TransferService.java`
  ```java
  .input("User " + user + " chuyển tiền cho " + toAccount + " số tiền " + amount)
  ...
  trace.output("Thành công chuyển khoản " + amount + " từ " + user + " sang " + toAccount);
  ```
- **Phân tích rủi ro:**
  - **Rò rỉ dữ liệu tài chính dạng Plain-text:** Toàn bộ thông tin nhạy cảm bao gồm **Tên/ID khách hàng (`user`)**, **Số tài khoản người nhận (`toAccount`)**, và **Số tiền giao dịch (`amount`)** được gửi trực tiếp lên Langfuse Server mà không qua bất kỳ cơ chế che dấu (Data Masking/Sanitization/Hashing) nào.
  - **Vi phạm nghiêm trọng tiêu chuẩn bảo mật Ngân hàng & Luật bảo vệ dữ liệu:**
    - Vi phạm quy định bảo vệ dữ liệu cá nhân (GDPR, Nghị định 13/2023/NĐ-CP của Việt Nam) và các tiêu chuẩn bảo mật thanh toán tài chính (PCI-DSS, ISO 27001).
    - Các log/trace telemetry thường được tiếp cận bởi nhiều kỹ sư phát triển, DevOps, QA hoặc bên thứ 3 phân tích dữ liệu, dẫn đến nguy cơ lộ dữ liệu giao dịch tài chính nội bộ của khách hàng ngân hàng.

---

## 2. PHÂN TÍCH LỖI LOGIC & QUẢN TRỊ LLMOps (LOGICAL & LLMOps ERRORS)

### 2.1. Thiếu Định Danh Tập Trung: `userId` và `sessionId`
- **Vị trí lỗi:** Class `TransferService.java`
  ```java
  Trace trace = langfuseClient.trace(new Trace()
      .name("bank-transfer")
      .input(...));
  ```
- **Phân tích lỗi:**
  - **Mất dấu vết ngữ cảnh phiên giao dịch (Session Context):** Không truyền `sessionId` khiến Langfuse không thể nhóm các lượt gọi (turns/interactions) thuộc cùng một phiên trò chuyện hoặc luồng giao dịch đa bước (Multi-step Transaction / AI Agent Flow).
  - **Không theo dõi được hành vi và chi phí theo người dùng (User-level Tracking):** Việc không truyền `userId` làm mất khả năng lọc, truy vết lịch sử giao dịch của một tài khoản cụ thể trên Dashboard của Langfuse, đồng thời không thể phân bổ chi phí token tiêu tốn theo từng khách hàng.

---

### 2.2. Xử Lý Bất Đồng Bộ & Xử Lý Ngoại Lệ Thiếu An Toàn (Exception Handling & Execution Flow)
- **Vị trí lỗi:** Class `TransferService.java`
- **Phân tích lỗi:**
  - **Giả định giao dịch luôn thành công:** Dòng `trace.output(...)` được gọi trực tiếp ngay sau lệnh in console mà không có cơ chế `try-catch-finally`. Nếu quá trình `processTransfer` xảy ra ngoại lệ (ví dụ: số dư không đủ, lỗi kết nối Core Banking, timeout), Trace trên Langfuse vẫn bị ghi nhận là thành công hoặc bị treo trạng thái dở dang (unhandled trace).
  - **Thiếu cấu trúc Span / Generation:** Giao dịch ngân hàng bằng AI thường bao gồm nhiều công đoạn (Intent Recognition, Function Calling, API Core Banking, LLM Generation). Đoạn code chỉ tạo 1 Trace đơn lẻ mà không chia thành các `span` chi tiết, làm giảm hiệu quả giám sát độ trễ (Latency Breakdown) và xác định đúng điểm nghẽn gây lỗi.
  - **Nguy cơ gây nghẽn luồng chính (Main Thread Blocking):** Việc gọi trực tiếp SDK telemetry mà không đảm bảo cơ chế flush/asynchronous phù hợp có thể ảnh hưởng trực tiếp đến thời gian phản hồi giao dịch của người dùng cuối.

---

## 3. TỔNG HỢP & ĐÁNH GIÁ MỨC ĐỘ NGUY HIỂM

| STT | Lỗ hổng / Lỗi phát hiện | Vị trí | Mức độ nghiêm trọng | Hậu quả chính |
| :---: | :--- | :--- | :---: | :--- |
| 1 | **Hardcode API Keys / Secret Key** | `LangfuseConfig.java` | 🔴 **Critical (Tối khẩn)** | Lộ toàn bộ quyền kiểm soát hệ thống telemetry và LLMOps khi commit code. |
| 2 | **Rò rỉ dữ liệu nhạy cảm (PII / Plain-text Transaction)** | `TransferService.java` | 🔴 **Critical (Tối khẩn)** | Vi phạm bảo mật ngân hàng, rò rỉ số tài khoản và thông tin tài chính khách hàng. |
| 3 | **Thiếu `userId` và `sessionId`** | `TransferService.java` | 🟡 **High (Cao)** | Không thể truy vết hội thoại, phân tích luồng Agent hoặc đo lường chi phí theo user. |
| 4 | **Thiếu xử lý lỗi & cấu trúc Span phân rã** | `TransferService.java` | 🟡 **Medium (Trung bình)** | Trace sai lệch kết quả thực tế khi có exception; không đo lường được latency từng bước. |