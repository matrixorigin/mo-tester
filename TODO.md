功能使用：

- [x] 增加 regex 命令，支持 include/exclude 两种模式，可以多次设置，表示同时满足
  - `--@regex('(?i)partten', true)` 表示结果中必须包含 pattern, 不区分大小写
  - 实例可查看 test/distributed/cases/analyze/explain_phyplan.sql 中的变化
  - 替代 `-- @pattern` 命令

- [x] 重构行列分隔符实现，支持多行结果，并保证兼容
  - 新增 case 不再需要指定 `select mo_ctl` 等命令的 `--@separator`
  - 多行结果得到保留

- [x] 增加 matecmp 命令，可用于列类型、精度的比较
  - 实例可查看 test/distributed/cases/1_parallel/function/func_datetime_date_add.test 中的使用
    
- [x] 并发执行测试
    - [x] 实例化、简化 Executor / ConnectionManager，为并发准备
    - [x] 增加目录级别的租户隔离
        - [x] 收益问题，无 create account 目录中的测试用例最多执行 4 分钟。更新：现在提升了 10 分钟
        - [x] mo_ctl flush 等命令的权限问题，如果转回 sys 租户的表可见性问题。更新：测试 account 改造后已经支持 mo-ctl flush
        - [x] show accounts 等全局状态查询的改造。更新：完成，另外 snapshot 中 restore sys account 会影响其他租户，放到最后执行


mo-tester 开发：

- [ ] 简化代码
    - [x] 移除不需要的常量
    - [x] 移除 force 命令
    - [x] 删除 kafka 相关内容
    - [x] 删除对 Resource_Local 的使用，都用 Resource
    - [x] Tester.java 对参数设置的重复
    - [x] 移除 delimiter 设置
    - [x] 使用 lombok 简化 setter/getter

- [x] 升级单测套件，支持单测过滤
  - `mvn test -D test='RegexParsingTest#testParseSingleRegexDirective'`

- [x] 修改 run.sh 脚本，支持优先使用 target 中编译结果
  - `mvn clean compile jar:jar -DskipTests` 只编译到 target/mo-tester-*.jar 文件

- [ ] fix: ScriptParser 不 trim leading spaces, 保留多行原格式展示

- [ ] fix: error compare without trimming
    if(!this.errorMessage.trim().eEuals(stmtResult.getErrorMessage().trim()))