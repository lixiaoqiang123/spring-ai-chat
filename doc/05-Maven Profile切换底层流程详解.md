# 🔍 Maven Profile 配置切换底层流程详解

## 📊 整体流程图

```text
用户执行命令
    ↓
Maven 解析 pom.xml
    ↓
激活指定的 Profile
    ↓
设置 Maven 属性
    ↓
资源过滤处理
    ↓
替换占位符
    ↓
生成最终配置文件
    ↓
Spring Boot 加载配置
    ↓
应用启动
```

## 1️⃣ Maven Profile 激活阶段

当你执行 `mvn spring-boot:run -Pgemini` 时：

```xml
<!-- pom.xml 中的 profiles 定义 -->
<profiles>
    <profile>
        <id>zhipu</id>
        <activation>
            <activeByDefault>true</activeByDefault>  <!-- 默认激活 -->
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

### Maven 的处理逻辑

1. **解析命令行参数** `-Pgemini`
   - Maven 识别出需要激活 `id=gemini` 的 profile

2. **Profile 激活优先级**（从高到低）：
   ```text
   命令行 -P 参数 > 环境变量 > settings.xml > activeByDefault
   ```

3. **设置 Maven 属性**：
   - 激活 gemini profile 后，Maven 会设置：
     ```text
     spring.profiles.active = "gemini"
     ```
   - 这个属性存储在 Maven 的属性上下文中

## 2️⃣ 资源过滤阶段

Maven 在 `process-resources` 生命周期阶段处理资源文件：

```xml
<build>
    <resources>
        <!-- 第一个 resource：启用过滤 只过滤里边配置的文件，防止替换其他文件里边的字符-->
        <resource>
            <directory>src/main/resources</directory>
            <filtering>true</filtering>  <!-- 关键：启用过滤 -->
            <includes>
                <include>application.yaml</include>
            </includes>
        </resource>

        <!-- 第二个 resource：禁用过滤，除了这几个，其他的都不需要过滤 -->
        <resource>
            <directory>src/main/resources</directory>
            <filtering>false</filtering>
            <excludes>
                <exclude>application.yaml</exclude>
            </excludes>
        </resource>
    </resources>
</build>
```

### Maven Resources Plugin 的工作流程

```java
// Maven Resources Plugin 伪代码
for (Resource resource : project.getResources()) {
    if (resource.isFiltering()) {
        // 对每个文件进行过滤处理
        for (File file : resource.getFiles()) {
            String content = readFile(file);

            // 替换占位符
            content = replacePlaceholders(content, mavenProperties);

            // 写入 target/classes
            writeFile(targetDir + file.getName(), content);
        }
    } else {
        // 直接复制，不处理
        copyFiles(resource.getFiles(), targetDir);
    }
}
```

## 3️⃣ 占位符替换阶段

### 源文件

`src/main/resources/application.yaml`：

```yaml
spring:
  profiles:
    active: @spring.profiles.active@  # 占位符
```

### Maven 的占位符语法

- Spring Boot 推荐使用 `@property@` 语法
- 传统 Maven 使用 `${property}` 语法
- Spring Boot 的 `spring-boot-starter-parent` 配置了使用 `@` 作为分隔符

### 替换过程

```java
// Maven 资源过滤伪代码
String content = "spring:\n  profiles:\n    active: @spring.profiles.active@";

// 查找所有 @...@ 模式
Pattern pattern = Pattern.compile("@([^@]+)@");
Matcher matcher = pattern.matcher(content);

while (matcher.find()) {
    String propertyName = matcher.group(1);  // "spring.profiles.active"
    String propertyValue = mavenProperties.get(propertyName);  // "gemini"

    // 替换占位符
    content = content.replace("@" + propertyName + "@", propertyValue);
}

// 结果：
// spring:
//   profiles:
//     active: gemini
```

### 生成的文件

`target/classes/application.yaml`：

```yaml
spring:
  profiles:
    active: gemini  # 已替换
```

## 4️⃣ Spring Boot 配置加载阶段

当应用启动时，Spring Boot 的配置加载流程：

```java
// Spring Boot 配置加载伪代码
public class ConfigFileApplicationListener {

    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        // 1. 加载 application.yaml
        Properties mainConfig = loadYaml("classpath:application.yaml");
        String activeProfile = mainConfig.get("spring.profiles.active");  // "gemini"

        // 2. 根据 active profile 加载对应的配置文件
        String profileConfigFile = "application-" + activeProfile + ".yaml";
        Properties profileConfig = loadYaml("classpath:" + profileConfigFile);

        // 3. 合并配置（profile 配置优先级更高）
        Properties finalConfig = merge(mainConfig, profileConfig);

        // 4. 设置到 Spring Environment
        environment.getPropertySources().addLast(
            new PropertiesPropertySource("applicationConfig", finalConfig)
        );
    }
}
```

### 配置文件加载顺序

1. `application.yaml` (基础配置)
2. `application-{profile}.yaml` (profile 特定配置，优先级更高)

## 5️⃣ 完整的数据流

```text
┌─────────────────────────────────────────────────────────────┐
│ 1. 用户执行命令                                              │
│    mvn spring-boot:run -Pgemini                             │
└────────────────────┬────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Maven 解析 pom.xml                                       │
│    - 读取 <profiles> 配置                                   │
│    - 识别 -Pgemini 参数                                     │
└────────────────────┬────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. 激活 gemini profile                                      │
│    Maven Properties:                                        │
│    {                                                        │
│      "spring.profiles.active": "gemini"                    │
│    }                                                        │
└────────────────────┬────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. 执行 process-resources 阶段                              │
│    maven-resources-plugin:3.3.1:resources                   │
└────────────────────┬────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. 读取源文件                                                │
│    src/main/resources/application.yaml                      │
│    ┌──────────────────────────────────────┐                │
│    │ spring:                              │                │
│    │   profiles:                          │                │
│    │     active: @spring.profiles.active@ │                │
│    └──────────────────────────────────────┘                │
└────────────────────┬────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. 占位符替换                                                │
│    查找: @spring.profiles.active@                           │
│    替换为: gemini                                           │
└────────────────────┬────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────┐
│ 7. 写入目标文件                                              │
│    target/classes/application.yaml                          │
│    ┌──────────────────────────────────────┐                │
│    │ spring:                              │                │
│    │   profiles:                          │                │
│    │     active: gemini                   │                │
│    └──────────────────────────────────────┘                │
└────────────────────┬────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────┐
│ 8. 复制其他资源文件（不过滤）                                │
│    - application-zhipu.yaml  → target/classes/              │
│    - application-gemini.yaml → target/classes/              │
│    - static/**               → target/classes/static/       │
│    - templates/**            → target/classes/templates/    │
└────────────────────┬────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────┐
│ 9. Spring Boot 启动                                         │
│    SpringApplication.run()                                  │
└────────────────────┬────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────┐
│ 10. 加载配置文件                                             │
│     a) 读取 application.yaml                                │
│        发现: spring.profiles.active=gemini                  │
│     b) 加载 application-gemini.yaml                         │
│     c) 合并配置                                             │
└────────────────────┬────────────────────────────────────────┘
                     ↓
┌─────────────────────────────────────────────────────────────┐
│ 11. 应用运行                                                 │
│     使用 Gemini 配置的 AI 模型                              │
└─────────────────────────────────────────────────────────────┘
```

## 6️⃣ 关键技术点深入解析

### A. 为什么使用 `@property@` 而不是 `${property}`？

```xml
<!-- spring-boot-starter-parent 中的配置 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <configuration>
        <!-- 使用 @ 作为分隔符，避免与 Spring 的 ${} 冲突 -->
        <delimiters>
            <delimiter>@</delimiter>
        </delimiters>
        <useDefaultDelimiters>false</useDefaultDelimiters>
    </configuration>
</plugin>
```

**原因**：

- Spring 配置文件中也使用 `${property}` 语法引用环境变量
- 如果 Maven 也使用 `${}`，会导致冲突
- 例如：`${ZHIPUAI_API_KEY}` 应该在运行时解析，而不是构建时

### B. 资源过滤的性能考虑

```xml
<!-- 为什么要分两个 resource 配置？ -->
<resources>
    <!-- 只过滤需要的文件 -->
    <resource>
        <filtering>true</filtering>
        <includes>
            <include>application.yaml</include>
        </includes>
    </resource>

    <!-- 其他文件不过滤，提高性能 -->
    <resource>
        <filtering>false</filtering>
        <excludes>
            <exclude>application.yaml</exclude>
        </excludes>
    </resource>
</resources>
```

**原因**：

1. **性能优化** - 过滤需要逐字符扫描，很慢
2. **避免破坏二进制文件** - 图片、字体等文件可能被破坏
3. **保护特殊字符** - 某些配置文件可能包含 `@` 字符

### C. Maven 属性的作用域

```xml
<properties>
    <!-- 全局属性 -->
    <spring.profiles.active>zhipu</spring.profiles.active>
</properties>

<profiles>
    <profile>
        <id>gemini</id>
        <properties>
            <!-- Profile 属性会覆盖全局属性 -->
            <spring.profiles.active>gemini</spring.profiles.active>
        </properties>
    </profile>
</profiles>
```

**属性解析优先级**：

1. 命令行 `-Dproperty=value`
2. 激活的 Profile 中的 `<properties>`
3. pom.xml 中的 `<properties>`
4. settings.xml 中的 `<properties>`
5. 系统环境变量

## 7️⃣ 调试和验证

### 查看激活的 Profile

```bash
mvn help:active-profiles
```

**输出**：

```text
Active Profiles for Project 'com.lxq:spring-api-chat:jar:0.0.1-SNAPSHOT':

The following profiles are active:
 - gemini (source: com.lxq:spring-api-chat:0.0.1-SNAPSHOT)
```

### 查看有效的 POM

```bash
mvn help:effective-pom -Pgemini
```

### 查看属性值

```bash
mvn help:evaluate -Dexpression=spring.profiles.active -Pgemini
```

**输出**：

```text
gemini
```

### 查看资源过滤结果

```bash
# 编译后查看生成的文件
mvn clean compile -Pgemini
cat target/classes/application.yaml
```

## 8️⃣ 常见问题和陷阱

### 问题1：占位符没有被替换

**原因**：

- 忘记设置 `<filtering>true</filtering>`
- 使用了错误的分隔符（`${}` vs `@@`）
- 文件不在 `<includes>` 列表中

**解决**：

```xml
<resource>
    <directory>src/main/resources</directory>
    <filtering>true</filtering>  <!-- 必须启用 -->
    <includes>
        <include>application.yaml</include>  <!-- 必须包含 -->
    </includes>
</resource>
```

### 问题2：二进制文件被破坏

**原因**：

- 对所有文件启用了过滤
- 二进制文件中的字节序列被误认为是占位符

**解决**：

```xml
<!-- 明确排除二进制文件 -->
<resource>
    <filtering>false</filtering>
    <includes>
        <include>**/*.png</include>
        <include>**/*.jpg</include>
        <include>**/*.ttf</include>
    </includes>
</resource>
```

### 问题3：Spring 的 `${}` 被 Maven 替换

**原因**：

- 使用了默认的 `${}` 分隔符
- Maven 在构建时就替换了，导致运行时无法解析

**解决**：

```xml
<!-- 使用 @ 分隔符 -->
<plugin>
    <artifactId>maven-resources-plugin</artifactId>
    <configuration>
        <delimiters>
            <delimiter>@</delimiter>
        </delimiters>
        <useDefaultDelimiters>false</useDefaultDelimiters>
    </configuration>
</plugin>
```

## 9️⃣ 扩展：多环境配置

你可以组合使用多个 profile：

```xml
<profiles>
    <!-- AI 模型 profiles -->
    <profile>
        <id>zhipu</id>
        <properties>
            <ai.model>zhipu</ai.model>
        </properties>
    </profile>
    <profile>
        <id>gemini</id>
        <properties>
            <ai.model>gemini</ai.model>
        </properties>
    </profile>

    <!-- 环境 profiles -->
    <profile>
        <id>dev</id>
        <properties>
            <env>dev</env>
        </properties>
    </profile>
    <profile>
        <id>prod</id>
        <properties>
            <env>prod</env>
        </properties>
    </profile>
</profiles>
```

### 使用

```bash
# 同时激活多个 profile
mvn spring-boot:run -Pgemini,prod
```

### 配置文件

```yaml
spring:
  profiles:
    active: @ai.model@,@env@
```

### 生成结果

```yaml
spring:
  profiles:
    active: gemini,prod
```

Spring Boot 会加载：

- `application.yaml`
- `application-gemini.yaml`
- `application-prod.yaml`

---

## 📝 总结

### Maven Profile 配置切换的核心流程

1. **Profile 激活** - Maven 根据命令行参数激活对应的 profile
2. **属性设置** - 激活的 profile 中的属性被加载到 Maven 上下文
3. **资源过滤** - Maven Resources Plugin 处理资源文件
4. **占位符替换** - 将 `@property@` 替换为实际的属性值
5. **文件生成** - 生成最终的配置文件到 `target/classes`
6. **Spring 加载** - Spring Boot 根据 `spring.profiles.active` 加载对应的配置

### 这个机制的优势

- ✅ **构建时确定** - 配置在构建时就确定，避免运行时错误
- ✅ **类型安全** - Maven 会验证属性是否存在
- ✅ **灵活性** - 支持多种激活方式和组合
- ✅ **可追溯** - 可以通过 JAR 包中的配置文件看到使用的配置
