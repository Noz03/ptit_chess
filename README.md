# PTIT CHESS

Ứng dụng chơi cờ vua trực tuyến theo thời gian thực, xây dựng với **Spring Boot**, **WebSocket (STOMP)**, **MySQL** và **Thymeleaf**.

---

## Mục lục

- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Cách chạy: Không dùng Docker](#cách-chạy-không-dùng-docker)
- [Cách chạy: Dùng Docker (chỉ DB)](#cách-chạy-dùng-docker-chỉ-db)
- [Tạo tài khoản Admin](#tạo-tài-khoản-admin)
- [Tính năng theo Role](#tính-năng-theo-role)

---

## Yêu cầu hệ thống

| Công cụ | Phiên bản tối thiểu |
|---|---|
| Java (JDK) | 17+ |
| Maven | 3.8+ (hoặc dùng `mvnw` đi kèm) |
| MySQL | 8.0+ |
| Docker & Docker Compose | Tùy chọn (chỉ để chạy DB) |

---

## Cách chạy: Không dùng Docker

### Bước 1 — Chuẩn bị MySQL

Đảm bảo MySQL đang chạy trên máy của bạn. Tạo database:

```sql
CREATE DATABASE chess_db;
```

> Mặc định ứng dụng sẽ kết nối với `root / root` tại `localhost:3306`.
> Nếu bạn dùng thông tin khác, sửa trong `src/main/resources/application.yml`:

```yaml
datasource:
  username: your_username
  password: your_password
```

### Bước 2 — Clone và build

```bash
git clone https://github.com/Noz03/ptit_chess.git
cd ptit_chess
```

### Bước 3 — Chạy ứng dụng

**Windows (PowerShell):**
```powershell
./mvnw spring-boot:run
```

**Linux / macOS:**
```bash
chmod +x mvnw
./mvnw spring-boot:run
```

### Bước 4 — Truy cập

Mở trình duyệt và vào: **http://localhost:8080**

---

## Cách chạy: Dùng Docker (chỉ DB)

Dự án chỉ Docker hóa phần **database MySQL** — Spring Boot vẫn chạy trực tiếp trên máy bạn.

### Bước 1 — Khởi động MySQL bằng Docker

```bash
docker compose up -d
```

Lệnh này sẽ:
- Tạo container `chess_mysql` chạy MySQL 8.0
- Database `chess_db` được tạo tự động
- Dữ liệu được lưu bền vững qua Docker Volume `db_data`
- Cổng kết nối: `localhost:3306`

Kiểm tra container đang chạy:
```bash
docker compose ps
```

### Bước 2 — Chạy Spring Boot

```powershell
# Windows
./mvnw spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

### Bước 3 — Truy cập

Mở trình duyệt và vào: **http://localhost:8080**

### Dừng và xóa DB

```bash
# Dừng container (giữ dữ liệu)
docker compose stop

# Dừng và xóa container + volume (xóa toàn bộ dữ liệu)
docker compose down -v
```

---

## Tạo tài khoản Admin

1. Đăng ký tài khoản bình thường tại `/register`
2. Kết nối vào MySQL và chạy lệnh SQL sau:

```sql
UPDATE account SET role = 'ADMIN' WHERE username = 'your_username';
```

3. Đăng xuất và đăng nhập lại. Hệ thống sẽ tự động chuyển bạn vào trang Admin Dashboard.

---

## Tính năng theo Role

### Người chơi (PLAYER)

**Xác thực:**
- Đăng ký tài khoản mới (username, display name, password)
- Đăng nhập / Đăng xuất

**Sảnh chờ (Lobby):**
- Xem danh sách phòng đang mở (bao gồm thông tin thời gian đấu)
- **Tạo phòng** — chọn loại phòng (Công khai / Kín) và thời gian đấu (1, 3, 5, 10, 30, 60 phút)
- **Tham gia phòng công khai** — bấm nút Join trực tiếp từ danh sách
- **Tham gia theo mã phòng** — nhập mã code riêng của phòng kín
- **Tìm kiếm phòng** — lọc phòng theo tên/mã
- **Gửi lời mời** — mời người chơi đang online vào phòng của mình
- **Nhận lời mời** — chấp nhận / từ chối lời mời từ người khác
- Tự động chuyển hướng vào ván đấu khi phòng đủ người

**Ván đấu (Match):**
- Chơi cờ vua thời gian thực qua WebSocket
- Bàn cờ tự động xác định màu quân (Trắng / Đen) ngẫu nhiên
- Đồng hồ đếm ngược cho mỗi bên (bắt đầu từ khi Trắng đi nước đầu tiên)
- Thông báo lỗi khi đi sai luật ("Lỗi sai luật / Illegal move!")
- Cảnh báo riêng khi đang bị chiếu mà đi sai
- **Đề nghị hòa** — gửi lời đề nghị hòa; đối thủ có thể chấp nhận hoặc từ chối
- **Đầu hàng** — kết thúc ván ngay lập tức, đối thủ thắng
- **Hết giờ** — ván tự động kết thúc khi một bên cạn thời gian
- **Chiếu bí** — hệ thống tự động phát hiện và kết thúc ván
- **Rematch** — sau khi ván kết thúc, đề nghị chơi lại ngay trong cùng phòng mà không cần tạo phòng mới
- Quay về Lobby

**Hồ sơ cá nhân (Profile):**
- Xem thông tin cá nhân: Display Name, ELO, Tỉ lệ Thắng/Hòa/Thua
- **Cập nhật Display Name**
- **Upload ảnh đại diện** từ máy tính (lưu vào server)
- Ảnh đại diện hiển thị trên thanh điều hướng ở tất cả các trang
- Xem **Bảng xếp hạng ELO** (Top 50 người chơi)

**Hệ thống ELO:**
- Điểm ELO tự động cập nhật sau mỗi ván đấu kết thúc
- Công thức tính theo chuẩn K-factor = 32

---

### Quản trị viên (ADMIN)

Sử dụng chung form đăng nhập. Sau khi đăng nhập bằng tài khoản Admin, hệ thống tự động chuyển sang **Admin Dashboard** (`/admin`). Nút **"Dashboard"** luôn hiển thị trên thanh điều hướng của mọi trang.

**Tổng quan hệ thống:**
- Xem 5 chỉ số thống kê tổng hợp:
  - Tổng số tài khoản
  - Số tài khoản đang bị khóa
  - Số người chơi đang online
  - Số phòng đang mở
  - Tổng số trận đấu đã diễn ra

**Quản lý tài khoản:**
- Xem danh sách toàn bộ tài khoản (ID, Username, Display Name, ELO, Role, Trạng thái)
- **Khóa tài khoản** — ngăn người dùng đăng nhập
- **Mở khóa tài khoản** — khôi phục quyền truy cập
- **Đổi Role** — nâng cấp Player → Admin hoặc hạ cấp Admin → Player

**Theo dõi vận hành:**
- Xem danh sách **người chơi đang online** theo thời gian thực (kèm ảnh đại diện, ELO, trạng thái)
- Xem danh sách **phòng đang hoạt động** (tên phòng, loại phòng, trạng thái, thời gian đấu)

**Quản lý trận đấu:**
- Xem **lịch sử toàn bộ trận đấu** theo thứ tự mới nhất
- **Tra cứu / lọc** trận đấu theo:
  - Kết quả: Trắng thắng / Đen thắng / Hòa
  - Lý do kết thúc: Chiếu bí / Đầu hàng / Hết giờ / Hòa thỏa thuận
- **Xem chi tiết PGN** của từng ván đấu (lịch sử nước đi theo chuẩn PGN)

---

## Cấu trúc công nghệ

```
Backend:  Spring Boot 3 · Spring Security · Spring WebSocket (STOMP) · JPA/Hibernate
Database: MySQL 8.0
Frontend: Thymeleaf · Tailwind CSS · chess.js · chessboard.js · SockJS · STOMP.js
Auth:     JWT (JSON Web Token)
```
