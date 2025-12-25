# --- Giai đoạn 1: Build (Tùy chọn, nếu bạn muốn build ngay trong Docker) ---
# Nếu bạn đã build file .jar ở ngoài bằng Intellij/Maven rồi thì bỏ qua bước này cũng được
# Nhưng để chuyên nghiệp, chúng ta thường build 2 bước (Multi-stage build)

# 1. Chọn Base Image có Maven để build
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
# Lệnh này sẽ tạo ra file .jar trong thư mục target/
RUN mvn clean package -DskipTests

# --- Giai đoạn 2: Run (Chạy ứng dụng) ---
# 2. Chọn Base Image JRE siêu nhẹ (Alpine) để chạy
FROM eclipse-temurin:17-jre-alpine

# 3. CÀI ĐẶT TIMEZONE (Quan trọng nhất)
# Alpine mặc định rất "trần trụi", chưa có dữ liệu múi giờ, phải cài thêm gói tzdata
RUN apk add --no-cache tzdata
# Thiết lập biến môi trường cho OS
ENV TZ=Asia/Ho_Chi_Minh

# 4. Tạo thư mục làm việc
WORKDIR /app

# 5. Copy file .jar từ giai đoạn Build sang giai đoạn Run
# (Nếu bạn build tay ở ngoài, sửa dòng này thành: COPY target/*.jar app.jar)
COPY --from=build /app/target/*.jar app.jar

# 6. Thiết lập biến môi trường cho Java (JVM)
# Đây là chốt chặn thứ 2 để chắc chắn Java nhận đúng giờ
ENV JAVA_OPTS="-Duser.timezone=Asia/Ho_Chi_Minh"

# 7. Mở port
EXPOSE 8080

# 8. Chạy ứng dụng
# $JAVA_OPTS sẽ được chèn vào câu lệnh java
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]