
- [ ] 简化代码
    - [x] 移除不需要的常量
    - [x] 移除 force 命令
    - [x] 删除 kafka 相关内容
    - [x] 删除对 Resource_Local 的使用，都用 Resource
    - [x] Tester.java 对参数设置的重复
    - [x] 移除 delimiter 设置

- [x] 升级单测套件，支持单测过滤
  - `mvn test -D test='RegexParsingTest#testParseSingleRegexDirective'`

- [x] 修改 run.sh 脚本，支持优先使用 target 中编译结果
  - `mvn clean compile jar:jar -DskipTests` 只编译到 target/mo-tester-*.jar 文件

- [x] 增加 regex 命令，支持 include/exclude 两种模式

- [x] 重构行列分隔符，支持多行结果，并保证兼容
    
- [ ] 并发执行测试
    - [x] 实例化、简化 Executor / ConnectionManager，为并发准备
    - [ ] 避免测试用用例使用全局参数，比如相同的 db 名、database 等
        - [ ] 增加目录级别的租户隔离(效果不好，依然有)

- [ ] fix: ScriptParser 不 trim leading spaces, 保留多行原格式展示

- [ ] fix: error compare without trimming
    if(!this.errorMessage.trim().eEuals(stmtResult.getErrorMessage().trim()))