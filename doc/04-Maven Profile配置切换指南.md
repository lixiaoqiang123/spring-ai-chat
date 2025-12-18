# Maven Profile 配置切换指南

## 📋 概述

项目已配置 Maven Profiles，可以轻松切换不同的 AI 模型配置（智谱AI、Gemini等）。

## 🎯 可用的 Profiles

| Profile ID | 说明 | 配置文件 | 默认 |
|-----------|------|---------|------|
| `zhipu` | 智谱AI配置 | application-zhipu.yaml | ✅ |
| `gemini` | Gemini配置 | application-gemini.yaml | ❌ |

## 🚀 使用方法

### 方法1: Maven 命令行切换

#### 使用智谱AI（默认）
```bash
mvn clean package
# 或显式指定
mvn clean package -Pzhipu
```

#### 使用 Gemini
```bash
mvn clean package -Pgemini
```

#### 运行应用
```bash
# 使用智谱AI
mvn spring-boot:run

# 使用 Gemini
mvn spring-boot:run -Pgemini
```

### 方法2: IDEA 配置

#### 在 Maven 面板中切换
1. 打开 IDEA 右侧的 Maven 面板
2. 展开 `Profiles` 节点
3. 勾选想要激活的 profile（zhipu 或 gemini）
4. 点击刷新按钮

#### 在 Run Configuration 中配置
1. 打开 `Run` -> `Edit Configurations`
2. 选择你的 Spring Boot 配置
3. 在 `Active profiles` 中填入：`zhipu` 或 `gemini`
4. 点击 `Apply` 和 `OK`

### 方法3: 命令行运行 JAR

```bash
# 构建时指定 profile
mvn clean package -Pgemini

# 运行时也可以覆盖
java -jar target/spring-api-chat-0.0.1-SNAPSHOT.jar --spring.profiles.active=gemini
```

### 方法4: 环境变量

```bash
# Windows
set SPRING_PROFILES_ACTIVE=gemini
mvn spring-boot:run

# Linux/Mac
export SPRING_PROFILES_ACTIVE=gemini
mvn spring-boot:run
```

## 🔧 工作原理

### 1. Maven Profiles 定义
在 `pom.xml` 中定义了多个 profile：

```xml
<profiles>
    <profile>
        <id>zhipu</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <properties>
            <spring.profiles.active>zhipu</spring.profiles.active>
        </properties>
    </profile>

    <profile>
        <id>gemini</id>
        <properties>
            <spring.profiles.active>gemini</spring.profiles.active>
        </properties>
    </profile>
</profiles>
```

### 2. 资源过滤
Maven 会在构建时替换 `application.yaml` 中的占位符：

```yaml
spring:
  profiles:
    active: @spring.profiles.active@
```

构建后会变成：
```yaml
spring:
  profiles:
    active: zhipu  # 或 gemini
```

### 3. Spring Boot 加载配置
Spring Boot 会根据激活的 profile 加载对应的配置文件：
- `application.yaml` (主配置)
- `application-{profile}.yaml` (profile 特定配置)

## 📝 添加新的 Profile

### 步骤1: 创建配置文件
在 `src/main/resources/` 下创建新的配置文件，例如 `application-openai.yaml`：

```yaml
spring:
  application:
    name: spring-api-chat
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4
```

### 步骤2: 在 pom.xml 中添加 profile

```xml
<profile>
    <id>openai</id>
    <properties>
        <spring.profiles.active>openai</spring.profiles.active>
    </properties>
</profile>
```

### 步骤3: 使用新 profile
```bash
mvn spring-boot:run -Popenai
```

## ⚠️ 注意事项

1. **资源过滤限制**：只有 `application.yaml` 会被过滤，profile 特定的配置文件（如 `application-zhipu.yaml`）不会被过滤，以避免破坏特殊字符。

2. **构建时确定**：Profile 在 Maven 构建时确定，运行时可以通过 `--spring.profiles.active` 覆盖。

3. **默认 Profile**：如果不指定 profile，默认使用 `zhipu`。

4. **多 Profile 激活**：可以同时激活多个 profile：
   ```bash
   mvn spring-boot:run -Pzhipu,dev
   ```

5. **IDE 缓存**：修改 pom.xml 后，记得在 IDEA 中刷新 Maven 项目。

## 🐛 故障排查

### 问题1: Profile 没有生效
**解决方案**：
```bash
# 清理并重新构建
mvn clean package -Pgemini

# 检查生成的 application.yaml
cat target/classes/application.yaml
```

### 问题2: IDEA 中 profile 不生效
**解决方案**：
1. 打开 Maven 面板
2. 点击刷新按钮（循环箭头图标）
3. 重新运行应用

### 问题3: 配置文件中的占位符没有被替换
**解决方案**：
- 确保使用 `@property@` 语法（不是 `${property}`）
- 确保文件在资源过滤的 `<includes>` 列表中
- 重新构建项目

## 📚 参考资料

- [Maven Profiles 官方文档](https://maven.apache.org/guides/introduction/introduction-to-profiles.html)
- [Spring Boot Profiles 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.profiles)
- [Maven 资源过滤](https://maven.apache.org/plugins/maven-resources-plugin/examples/filter.html)

---

**最后更新**: 2025-12-16
