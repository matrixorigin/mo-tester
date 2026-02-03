# Getting Started with Wait-Expect


### 1. Wait-Expect？

智能等待，替代固定的 `sleep`，自动轮询直到条件满足。

**对比**：
```sql
# 老方法：总是等 20 秒 ❌
-- @sleep:20
SELECT * FROM table WHERE status = 'done';

# 新方法：通常 2 秒完成 ✅
-- @wait_expect(1, 20)
SELECT * FROM table WHERE status = 'done';
```

### 2. 基本语法

```sql
-- @wait_expect(检查间隔, 超时时间)
SELECT 你的查询;
```

**参数说明**：
- **检查间隔**：多久检查一次（秒）
- **超时时间**：最多等待多久（秒）

### 3. 第一个例子

创建文件 `cases/my_first_wait_expect.sql`：

```sql
# 创建测试表
CREATE TABLE test_status (id INT, status VARCHAR(20));
INSERT INTO test_status VALUES (1, 'pending');

# 使用 wait_expect 等待状态变化
-- @wait_expect(1, 10)
SELECT status FROM test_status WHERE id = 1;

# 更新状态（模拟异步操作）
UPDATE test_status SET status = 'completed' WHERE id = 1;

# 清理
DROP TABLE test_status;
```

### 4. 生成结果文件

```bash
./run.sh -n -g -p cases/my_first_wait_expect.sql -m genrs
```

### 5. 运行测试

```bash
./run.sh -n -g -p cases/my_first_wait_expect.sql -m run
```

## 常见场景

### 场景 1：等待异步任务完成

```sql
# 启动异步任务
INSERT INTO jobs (id, status) VALUES (123, 'running');

# 等待任务完成
-- @wait_expect(2, 60)
SELECT status FROM jobs WHERE id = 123 AND status = 'completed';
```

### 场景 2：等待数据同步

```sql
# 在主库插入数据
INSERT INTO users (id, name) VALUES (1, 'Alice');

# 等待从库同步
-- @wait_expect(1, 30)
SELECT COUNT(*) FROM users WHERE id = 1;
```

### 场景 3：等待缓存更新

```sql
# 更新数据
UPDATE config SET value = 'new_value' WHERE key = 'setting';

# 等待缓存刷新
-- @wait_expect(1, 20)
SELECT value FROM config WHERE key = 'setting';
```

### 场景 4：等待索引构建

```sql
# 创建索引
CREATE INDEX idx_name ON large_table(name);

# 等待索引可用
-- @wait_expect(5, 120)
SELECT COUNT(*) FROM large_table WHERE name = 'test';
```

## 参数选择指南

### 检查间隔（interval）

| 操作速度 | 推荐间隔 | 说明 |
|---------|---------|------|
| 很快（< 5秒） | 1 秒 | 快速响应 |
| 中等（5-30秒） | 2-3 秒 | 平衡效率 |
| 较慢（> 30秒） | 5 秒 | 减少负载 |

### 超时时间（timeout）

**公式**：`timeout = 平均完成时间 × 2 或 3`

**示例**：
- 平均 5 秒完成 → timeout = 10-15 秒
- 平均 10 秒完成 → timeout = 20-30 秒
- 平均 30 秒完成 → timeout = 60-90 秒

## 实际案例

### 案例 1：替换 template.sql 中的 sleep

**原代码**：
```sql
-- @sleep:20
SELECT TIMEDIFF('2000:01:01 00:00:00', '2000:01:01 00:00:00.000001');
```

**优化后**：
```sql
-- @wait_expect(1, 20)
SELECT TIMEDIFF('2000:01:01 00:00:00', '2000:01:01 00:00:00.000001');
```

**效果**：
- 之前：总是等 20 秒
- 之后：立即完成（< 1 秒）
- 节省：19 秒

### 案例 2：批量数据处理

```sql
# 插入大量数据
INSERT INTO batch_data SELECT * FROM source_table;

# 等待处理完成
-- @wait_expect(5, 300)
SELECT COUNT(*) FROM batch_data WHERE processed = true;
```

### 案例 3：分布式事务

```sql
# 开始分布式事务
BEGIN;
INSERT INTO orders VALUES (1, 'pending');
COMMIT;

# 等待所有节点确认
-- @wait_expect(2, 30)
SELECT status FROM orders WHERE id = 1 AND status = 'confirmed';
```

## 调试技巧

### 1. 查看日志

运行测试后，检查日志：

```
[test.sql][row:10] Executing with wait_expect: interval=1s, timeout=20s
[test.sql][row:10] wait_expect succeeded after 3 attempts (2.15s)
```

**信息解读**：
- 尝试了 3 次
- 总共用时 2.15 秒
- 成功匹配

### 2. 超时调试

如果看到超时：

```
[test.sql][row:15] wait_expect timeout after 10 attempts (20.02s)
```

**可能原因**：
1. 操作确实很慢 → 增加 timeout
2. 预期结果不对 → 检查 .result 文件
3. 条件永远不满足 → 检查 SQL 逻辑

### 3. 性能分析

如果总是很快成功：
```
wait_expect succeeded after 1 attempts (0.05s)
```

**优化**：可以减小 timeout，比如从 20 改为 10。

如果经常超时：
```
wait_expect timeout after 20 attempts (40.02s)
```

**优化**：增加 timeout 或检查系统性能。

## 常见错误

### 错误 1：格式错误

```
Invalid wait_expect flag format: -- @wait_expect(1)
```

**解决**：必须提供两个参数
```sql
-- @wait_expect(1, 20)  ✅
-- @wait_expect(1)      ❌
```

### 错误 2：参数为 0

```
Invalid wait_expect interval: 0. Interval must be positive.
```

**解决**：参数必须 > 0
```sql
-- @wait_expect(1, 20)  ✅
-- @wait_expect(0, 20)  ❌
```

### 错误 3：interval > timeout

```
Invalid wait_expect parameters: interval(30) > timeout(20)
```

**解决**：interval 必须 <= timeout
```sql
-- @wait_expect(10, 20)  ✅
-- @wait_expect(30, 20)  ❌
```

## 性能对比

### 测试场景

10 个测试用例，每个等待异步操作完成。

#### 使用 @sleep:20

```
测试 1: 20 秒
测试 2: 20 秒
...
测试 10: 20 秒
总计: 200 秒
```

#### 使用 @wait_expect(1, 20)

```
测试 1: 2 秒 ✅
测试 2: 3 秒 ✅
测试 3: 1 秒 ✅
...
测试 10: 2 秒 ✅
总计: 25 秒
```

**节省：175 秒（87.5%）**

## 下一步

### 学习更多

- 📖 **完整文档**：`WAIT_EXPECT_FEATURE.md`
- 📋 **快速参考**：`WAIT_EXPECT_QUICK_REFERENCE.md`
- 🔧 **实现细节**：`WAIT_EXPECT_IMPLEMENTATION_SUMMARY.md`

### 查看示例

- 📝 **完整演示**：`cases/wait_expect_demo.sql`
- 📝 **简单示例**：`cases/wait_expect_simple.sql`

### 验证安装

```bash
./test_wait_expect_syntax.sh
```

## 最佳实践总结

1. ✅ **用于异步操作**：后台任务、数据同步、缓存更新
2. ✅ **合理设置参数**：interval 1-5 秒，timeout 是平均时间的 2-3 倍
3. ✅ **逐步迁移**：先测试几个用例，验证效果后批量替换
4. ✅ **监控日志**：关注尝试次数和耗时，优化参数
5. ❌ **不用于同步操作**：同步操作直接执行即可
6. ❌ **不用于确定性延迟**：如等待锁释放，用 @sleep

## 快速命令

```bash
# 检查语法
./test_wait_expect_syntax.sh

# 生成结果
./run.sh -n -g -p cases/your_test.sql -m genrs

# 运行测试
./run.sh -n -g -p cases/your_test.sql -m run

# 查找所有 sleep
grep -r "-- @sleep:" cases/

# 批量替换（示例）
# 手动编辑文件，将 @sleep:20 改为 @wait_expect(1, 20)
```

## 获取帮助

如果遇到问题：

1. 查看日志输出
2. 检查参数是否正确
3. 验证 SQL 语法
4. 查看完整文档
5. 运行示例测试

---

**恭喜！你已经掌握了 wait_expect 的基本用法。开始优化你的测试吧！** 🚀
