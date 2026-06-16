# Open Report - 企业级报表平台

## 项目简介

Open Report 是一款面向企业级应用的开源类帆软报表平台，基于 Spring Boot + MyBatis Plus + React + TypeScript 技术栈构建，提供了报表设计、数据源管理、数据集配置、报表预览、导出打印、嵌入式集成等完整的报表解决方案。

### 核心能力

- **可视化报表设计器**：基于类 Excel 的操作体验，支持拖拽式布局、公式计算、条件格式、图表配置
- **多数据源支持**：支持 MySQL、PostgreSQL、Oracle、SQL Server、达梦等主流关系型数据库
- **强大的数据引擎**：基于 Apache Calcite 实现动态 SQL 解析与执行，支持跨数据源联邦查询
- **多格式导出**：支持 Excel、PDF、HTML 三种格式的报表导出
- **嵌入式集成**：提供 iframe 嵌入、URL 链接、RESTful API 等多种集成方式
- **定时调度**：集成 XXL-JOB 实现报表定时执行与邮件推送
- **权限管理**：基于 RBAC 模型的用户-角色-菜单权限管理体系

---

## 功能特性

### 报表管理
- 报表模板创建、编辑、删除、复制
- 报表发布/下线状态管理
- 报表参数动态配置
- 数据集绑定与字段映射

### 报表设计器
- 类 Excel 表格布局编辑
- 单元格表达式与公式计算
- 数据行/列扩展设置
- 条件格式配置（字体、背景、边框）
- 图表配置（柱状图、折线图、饼图、仪表盘等）
- 参数面板设计

### 数据源管理
- 多数据源连接配置
- 连接池参数调优
- 数据源连通性测试
- 支持动态数据源切换

### 数据集管理
- SQL 可视化编辑器
- 动态参数绑定
- 数据预览与分页
- 字段类型自动识别

### 报表预览与导出
- 实时数据渲染预览
- 参数化查询
- Excel 导出（支持模板和原生两种模式）
- PDF 导出（中文字体支持）
- HTML 导出（内嵌样式，离线可用）
- 批量导出

### 嵌入式集成
- Token 安全访问机制
- iframe 嵌入方式
- URL 链接方式
- RESTful API 调用方式
- 临时访问 Token 生成

### 调度管理
- Cron 表达式配置
- 任务启用/禁用
- 执行日志查询
- 任务失败重试

### 系统管理
- 用户管理（增删改查、启用禁用）
- 角色管理（权限分配）
- 菜单管理（动态菜单配置）

---

## 技术架构

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.7.18 | 核心应用框架 |
| MyBatis Plus | 3.5.3.2 | ORM 持久层框架 |
| MySQL | 8.0+ | 主数据库（可选达梦） |
| Druid | 1.2.20 | 数据库连接池 |
| Apache Calcite | 1.36.0 | 动态 SQL 解析与执行引擎 |
| JJWT | 0.11.5 | JWT Token 生成与验证 |
| Knife4j | 4.3.0 | Swagger API 文档增强 |
| Hutool | 5.8.22 | Java 工具类库 |
| Apache POI / JXLS | 5.x / 2.13.0 | Excel 导出 |
| iText / OpenPDF | - | PDF 导出 |
| XXL-JOB | 2.4.0 | 分布式任务调度 |
| Lombok | - | 代码简化 |
| FastJSON | - | JSON 序列化 |

### 前端技术栈

| 技术 | 说明 |
|------|------|
| React 18 | 前端框架 |
| TypeScript | 类型系统 |
| Vite | 构建工具 |
| Ant Design 5.x | UI 组件库 |
| Zustand | 状态管理 |
| React Router v6 | 路由管理 |
| Axios | HTTP 客户端 |
| Luckysheet | 类 Excel 电子表格（设计器核心） |
| ECharts | 图表渲染 |

### 系统架构图

```
┌──────────────────────────────────────────────────────────────────┐
│                          前端 (open-report-web)                   │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐    │
│  │  报表设计器  │  │  报表预览器  │  │  系统管理/数据源管理    │    │
│  └──────────────┘  └──────────────┘  └───────────────────────┘    │
└────────────────────────────┬─────────────────────────────────────┘
                             │ HTTPS / REST API
┌────────────────────────────▼─────────────────────────────────────┐
│                       后端网关 (open-report-admin)                 │
│  ┌────────────────────────────────────────────────────────────┐   │
│  │  Controller Layer (Auth / Report / DataSource / Embed ...) │   │
│  └────────────────────────────┬───────────────────────────────┘   │
│                               │                                   │
│  ┌────────────────────────────▼───────────────────────────────┐   │
│  │                 Service Layer (业务逻辑层)                  │   │
│  └────────────────────────────┬───────────────────────────────┘   │
│                               │                                   │
│  ┌────────────────────────────▼───────────────────────────────┐   │
│  │  open-report-engine (报表引擎)  │  open-report-scheduler    │   │
│  │  - 模板解析                    │  - XXL-JOB 调度            │   │
│  │  - Calcite SQL 执行            │  - 任务执行日志            │   │
│  │  - 数据渲染                    │  - 报表清理任务            │   │
│  │  - Excel/PDF/HTML 导出         │                            │   │
│  └────────────────────────────┬───────────────────────────────┘   │
└───────────────────────────────┼───────────────────────────────────┘
                                │
            ┌───────────────────┼───────────────────┐
            ▼                   ▼                   ▼
    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
    │   MySQL/达梦  │    │  业务数据源   │    │  XXL-JOB     │
    │  (系统库)     │    │ (多数据源)    │    │  (调度中心)   │
    └──────────────┘    └──────────────┘    └──────────────┘
```

---

## 目录结构

```
open-report/
├── open-report-common/              # 公共模块（工具类、常量、异常、统一返回等）
│   ├── src/main/java/com/openreport/common/
│   │   ├── config/                  # 通用配置（Jackson、MyBatis Plus、WebMvc）
│   │   ├── constants/               # 常量定义
│   │   ├── entity/                  # 基础实体类
│   │   ├── enums/                   # 枚举定义
│   │   ├── exception/               # 全局异常处理
│   │   ├── result/                  # 统一返回 Result
│   │   └── utils/                   # 工具类（JWT、密码、Servlet 等）
│   └── pom.xml
│
├── open-report-admin/               # 后台管理模块（Web 入口）
│   ├── src/main/java/com/openreport/admin/
│   │   ├── config/                  # 配置类（Auth、JWT 拦截器）
│   │   ├── controller/              # 控制器层
│   │   │   ├── LoginController.java
│   │   │   ├── ReportTemplateController.java
│   │   │   ├── ReportExecuteController.java
│   │   │   ├── EmbedReportController.java       # 嵌入式集成 API
│   │   │   ├── ReportExportController.java      # 导出打印 API
│   │   │   ├── DataSourceConfigController.java
│   │   │   ├── DataSetController.java
│   │   │   ├── SysUserController.java
│   │   │   ├── SysRoleController.java
│   │   │   └── SysMenuController.java
│   │   ├── entity/                  # 实体类
│   │   ├── mapper/                  # MyBatis Mapper
│   │   └── service/                 # 业务逻辑层
│   ├── src/main/resources/
│   │   ├── mapper/                  # MyBatis XML 映射
│   │   ├── application.yml          # 主配置文件
│   │   └── application-dev.yml      # 开发环境配置
│   └── pom.xml
│
├── open-report-engine/              # 报表引擎模块
│   ├── src/main/java/com/openreport/engine/
│   │   ├── calcite/                 # Calcite SQL 执行器
│   │   ├── datasource/              # 动态数据源管理
│   │   ├── export/                  # 导出组件（Excel、PDF）
│   │   ├── model/                   # 报表模型（单元格、参数、模板等）
│   │   ├── parser/                  # 表达式解析器
│   │   ├── renderer/                # 渲染器（HTML 表格、图表选项）
│   │   └── service/                 # 报表引擎服务
│   └── pom.xml
│
├── open-report-scheduler/           # 调度任务模块
│   ├── src/main/java/com/openreport/scheduler/
│   │   ├── config/                  # Kafka、XXL-JOB 配置
│   │   ├── controller/              # 调度与日志控制器
│   │   ├── entity/                  # 调度任务、执行日志实体
│   │   ├── job/                     # 定时任务实现
│   │   ├── listener/                # Kafka 监听器
│   │   ├── mapper/                  # MyBatis Mapper
│   │   └── service/                 # 调度服务
│   └── pom.xml
│
├── open-report-web/                 # 前端工程
│   ├── src/
│   │   ├── api/                     # API 接口封装
│   │   │   ├── report.ts
│   │   │   ├── embed.ts             # 嵌入式集成 API
│   │   │   ├── datasource.ts
│   │   │   ├── dataset.ts
│   │   │   ├── schedule.ts
│   │   │   ├── system.ts
│   │   │   └── user.ts
│   │   ├── components/              # 通用组件
│   │   ├── layouts/                 # 布局组件
│   │   ├── pages/                   # 页面
│   │   │   ├── login/               # 登录页
│   │   │   ├── dashboard/           # 仪表盘
│   │   │   ├── designer/            # 报表设计器
│   │   │   ├── preview/             # 报表预览
│   │   │   ├── viewer/              # 报表查看
│   │   │   ├── print/               # 打印专用页
│   │   │   ├── embed/               # 嵌入式集成示例
│   │   │   ├── report/              # 报表管理
│   │   │   ├── datasource/          # 数据源管理
│   │   │   ├── dataset/             # 数据集管理
│   │   │   ├── schedule/            # 调度管理
│   │   │   └── system/              # 系统管理（用户、角色、菜单）
│   │   ├── router/                  # 路由配置
│   │   ├── store/                   # 状态管理（Zustand）
│   │   ├── types/                   # TypeScript 类型定义
│   │   ├── utils/                   # 工具函数（request、storage）
│   │   ├── App.tsx
│   │   ├── main.tsx
│   │   └── index.css
│   ├── index.html
│   ├── package.json
│   ├── tsconfig.json
│   └── vite.config.ts
│
├── sql/                             # 数据库脚本
│   ├── schema-mysql.sql             # MySQL 建表脚本
│   ├── schema-dm.sql                # 达梦建表脚本
│   └── data-mysql.sql               # 初始化数据
│
├── pom.xml                          # 父 POM
└── README.md
```

---

## 快速开始

### 环境要求

| 软件 | 版本要求 |
|------|----------|
| JDK | 1.8+ |
| Node.js | 16+ |
| MySQL | 5.7+ 或 8.0+ |
| Maven | 3.6+ |

### 1. 数据库初始化

```bash
# 登录 MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE open_report DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

# 执行建表脚本
USE open_report;
SOURCE sql/schema-mysql.sql;

# 执行初始化数据（可选，包含默认管理员账号）
SOURCE sql/data-mysql.sql;
```

默认管理员账号：
- 用户名：`admin`
- 密码：`123456`

### 2. 后端启动

#### 修改配置文件

编辑 `open-report-admin/src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/open_report?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

#### 编译并启动

```bash
# 在项目根目录执行 Maven 编译
mvn clean install -DskipTests

# 启动后台服务
cd open-report-admin
mvn spring-boot:run
```

服务启动后访问：
- API 文档：http://localhost:8080/doc.html
- 健康检查：http://localhost:8080/actuator/health

### 3. 前端启动

```bash
cd open-report-web

# 安装依赖
npm install
# 或使用 yarn
yarn install

# 启动开发服务器
npm run dev
```

前端启动后访问：http://localhost:5173

---

## 部署说明

### 后端部署（Jar 包方式）

```bash
# 打包
mvn clean package -DskipTests

# 运行 Jar
java -jar open-report-admin/target/open-report-admin-1.0.0.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

### 后端部署（Docker 方式）

创建 `Dockerfile`：

```dockerfile
FROM openjdk:8-jre-alpine
VOLUME /tmp
COPY open-report-admin/target/open-report-admin-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar"]
```

```bash
# 构建镜像
docker build -t open-report:1.0.0 .

# 运行容器
docker run -d \
  --name open-report \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql-host:3306/open_report \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  open-report:1.0.0
```

### 前端部署

```bash
# 构建生产包
npm run build

# 将 dist 目录部署到 Nginx
cp -r dist/* /usr/share/nginx/html/
```

Nginx 配置示例：

```nginx
server {
    listen 80;
    server_name report.yourdomain.com;

    root /usr/share/nginx/html;
    index index.html;

    # 前端路由（History 模式）
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## API 接口说明

### 认证接口

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| POST | /auth/login | 用户登录 | 否 |
| POST | /auth/logout | 用户登出 | 是 |

### 报表管理接口

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | /report/list | 分页查询报表列表 | 是 |
| GET | /report/all | 获取全部报表 | 是 |
| GET | /report/{id} | 获取报表详情 | 是 |
| POST | /report | 创建报表 | 是 |
| PUT | /report | 更新报表 | 是 |
| DELETE | /report/{id} | 删除报表 | 是 |
| POST | /report/batch-delete | 批量删除报表 | 是 |
| POST | /report/copy/{id} | 复制报表 | 是 |
| POST | /report/publish/{id} | 发布报表 | 是 |
| POST | /report/unpublish/{id} | 下线报表 | 是 |
| POST | /report/import | 导入报表 | 是 |
| GET | /report/export/{id} | 导出报表配置 | 是 |

### 报表执行接口

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | /report-execute/params/{templateId} | 获取报表参数配置 | 是 |
| POST | /report-execute/preview/{templateId} | 预览报表数据 | 是 |
| POST | /report-execute/export/{templateId} | 导出报表数据 | 是 |
| POST | /report-execute/dataset-preview/{dataSetId} | 预览单个数据集 | 是 |

### 嵌入式集成接口（公开）

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | /api/embed/report/{id} | 获取公开报表配置 | Token 校验 |
| GET | /api/embed/report/{id}/data | 获取报表数据 | Token 校验 |
| POST | /api/embed/report/{id}/export | 导出报表 | Token 校验 |
| GET | /api/embed/report/{id}/token | 生成临时访问 Token | 是（需登录） |

### 导出打印接口

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | /api/export/report/{id}/excel | 导出 Excel | Token 校验 |
| GET | /api/export/report/{id}/pdf | 导出 PDF | Token 校验 |
| GET | /api/export/report/{id}/html | 导出 HTML | Token 校验 |
| POST | /api/export/report/batch | 批量导出 | 是 |

### 数据源管理接口

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | /datasource/list | 分页查询数据源 | 是 |
| GET | /datasource/all | 获取全部数据源 | 是 |
| GET | /datasource/{id} | 获取数据源详情 | 是 |
| POST | /datasource | 创建数据源 | 是 |
| PUT | /datasource | 更新数据源 | 是 |
| DELETE | /datasource/{id} | 删除数据源 | 是 |
| POST | /datasource/test | 测试连接 | 是 |

### 数据集管理接口

| 方法 | 路径 | 说明 | 鉴权 |
|------|------|------|------|
| GET | /dataset/list | 分页查询数据集 | 是 |
| GET | /dataset/{id} | 获取数据集详情 | 是 |
| POST | /dataset | 创建数据集 | 是 |
| PUT | /dataset | 更新数据集 | 是 |
| DELETE | /dataset/{id} | 删除数据集 | 是 |
| POST | /dataset/parse-sql | 解析 SQL | 是 |
| POST | /dataset/preview/{id} | 预览数据 | 是 |

---

## 嵌入式集成指南

### 1. 生成临时访问 Token

在管理后台登录后，调用生成 Token 接口：

```http
GET /api/embed/report/1/token?expireSeconds=3600
Authorization: Bearer {your_login_token}
```

响应：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "reportId": 1,
    "reportName": "销售月报",
    "expireSeconds": 3600,
    "createTime": 1717238400000,
    "expireTime": 1717242000000
  }
}
```

### 2. iframe 嵌入方式

```html
<iframe
  src="https://report.yourdomain.com/report/viewer?id=1&token=eyJhbGciOiJIUzI1NiJ9..."
  width="100%"
  height="600"
  frameborder="0"
  allowfullscreen>
</iframe>
```

### 3. URL 链接方式

```
https://report.yourdomain.com/report/viewer?id=1&token=eyJhbGciOiJIUzI1NiJ9...&toolbar=1&header=1&theme=light
```

URL 参数说明：

| 参数 | 必填 | 说明 |
|------|------|------|
| id | 是 | 报表 ID |
| token | 是 | 嵌入式访问 Token |
| toolbar | 否 | 是否显示工具栏（1=显示，0=隐藏），默认 1 |
| header | 否 | 是否显示页头（1=显示，0=隐藏），默认 1 |
| theme | 否 | 主题（light/dark），默认 light |
| lang | 否 | 语言（zh-CN/en-US），默认 zh-CN |

### 4. RESTful API 方式

```javascript
// 使用 Fetch API 获取报表数据
const token = 'eyJhbGciOiJIUzI1NiJ9...';

fetch('/api/embed/report/1?token=' + token)
  .then(res => res.json())
  .then(res => {
    console.log('报表配置:', res.data);
  });

// 获取报表数据
fetch('/api/embed/report/1/data?token=' + token + '&startDate=2024-01-01&endDate=2024-01-31')
  .then(res => res.json())
  .then(res => {
    console.log('报表数据:', res.data);
  });
```

---

## 常见问题

### Q1: 数据库连接失败怎么办？

请检查以下几点：
1. 确认数据库服务已启动
2. 检查 `application-dev.yml` 中的数据库地址、端口、用户名、密码是否正确
3. 确认数据库用户有对应数据库的访问权限
4. 如果是远程数据库，检查防火墙是否开放端口

### Q2: 前端启动后无法访问后端 API？

1. 检查 `open-report-web/.env.development` 中的 `VITE_APP_API_BASE_URL` 配置是否正确
2. 确认后端服务是否正常启动，默认端口为 8080
3. 检查浏览器控制台是否有 CORS 跨域错误（已通过 WebMvcConfig 配置跨域）

### Q3: 报表导出 Excel 打开乱码？

请使用 Excel 2016 及以上版本打开。如果使用 WPS，一般不会有乱码问题。旧版 Excel 可能需要手动转换编码。

### Q4: PDF 导出中文显示为方框？

这是字体问题。系统已内置 STSong-Light 字体支持，如果仍有问题，请检查服务器是否安装了中文字体，或在 PdfExporter 中配置自定义字体路径。

### Q5: 嵌入式 Token 过期时间如何设置？

调用 `/api/embed/report/{id}/token` 接口时，通过 `expireSeconds` 参数指定有效期（单位：秒），默认 3600 秒（1 小时）。最长有效期受 JWT 配置限制。

### Q6: 如何修改默认管理员密码？

1. 登录系统后，进入「系统管理 → 用户管理」
2. 找到 admin 用户，点击「编辑」
3. 修改密码后保存

### Q7: 如何接入新的数据库类型？

1. 在 `open-report-common/src/main/java/com/openreport/common/enums/DataSourceTypeEnum.java` 中添加新类型
2. 在 `open-report-engine/src/main/java/com/openreport/engine/datasource/DynamicDataSourceManager.java` 中添加对应驱动
3. 前端 `open-report-web/src/types/index.ts` 中同步更新 `DataSourceType` 类型
4. 在数据源管理页面添加对应选项

### Q8: 定时任务不执行？

1. 确认 XXL-JOB 调度中心已正常启动
2. 检查 `open-report-scheduler/src/main/resources/application.yml` 中的 XXL-JOB 配置
3. 确认调度中心是否已添加对应执行器和任务
4. 查看调度日志排查具体错误

---

## License

本项目采用 MIT License 开源。

---

## 联系方式

如有问题或建议，欢迎提交 Issue。
