# lab-edu core

lab-edu 的后端服务，基于 Spring Boot。

## 仓库用途

- 提供后端 API 能力
- 提供 OpenAPI 与 Swagger UI 文档

## 启动 Spring Boot

接入 PostgreSQL 时，只需要把数据源连接信息通过环境变量传入应用。

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://<postgres-host>:<postgres-port>/<postgres-db> \
SPRING_DATASOURCE_USERNAME=<postgres-username> \
SPRING_DATASOURCE_PASSWORD=<postgres-password> \
SERVER_PORT=8080 \
./mvnw spring-boot:run
```
