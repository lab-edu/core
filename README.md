# lab-edu core

lab-edu 的后端服务，基于 Spring Boot。

## 仓库用途

- 提供后端 API 能力
- 提供 OpenAPI 与 Swagger UI 文档

## 当前能力

- JWT 登录态与 HttpOnly Cookie 写入
- 用户注册、登录和当前用户查询
- 教师创建课程、学生加入课程、课程成员查看
- 教师发布实验、学生查看实验列表与详情
- 学生提交实验文件，支持多次提交并保留历史

## 启动 Spring Boot

接入 PostgreSQL 时，只需要把数据源连接信息通过环境变量传入应用。

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://<postgres-host>:<postgres-port>/<postgres-db> \
SPRING_DATASOURCE_USERNAME=<postgres-username> \
SPRING_DATASOURCE_PASSWORD=<postgres-password> \
SERVER_PORT=8080 \
./mvnw spring-boot:run
```

默认还需要设置 JWT 密钥与文件存储目录，分别通过 `LAB_SECURITY_JWT_SECRET` 和 `LAB_STORAGE_BASE_PATH` 覆盖。
