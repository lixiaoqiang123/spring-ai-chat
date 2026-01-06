# Java 21 新特性

Java 21 是 LTS（长期支持）版本，引入了多项重要特性。

## Virtual Threads

Virtual Threads 是轻量级线程，可以创建数百万个线程而不会耗尽系统资源。

优势：
- 更高的并发能力
- 更简单的代码编写
- 更好的性能表现

使用方式：
```java
Thread.ofVirtual().start(() -> {
    // 任务逻辑
});
```

## Record Patterns

Record Patterns 简化了 Record 类型的模式匹配。

示例：
```java
record Point(int x, int y) {}

if (obj instanceof Point(int x, int y)) {
    System.out.println("x=" + x + ", y=" + y);
}
```

## Pattern Matching for switch

switch 表达式现在支持模式匹配。

示例：
```java
return switch (obj) {
    case String s -> s.length();
    case Integer i -> i;
    case null -> 0;
    default -> -1;
};
```

## Sequenced Collections

新增有序集合接口，提供统一的访问方式。

方法：
- getFirst() - 获取第一个元素
- getLast() - 获取最后一个元素
- reversed() - 反转集合

## 性能改进

Java 21 在 GC、JIT 编译等方面都有显著的性能提升。

推荐使用 G1GC 或 ZGC 作为垃圾收集器。
