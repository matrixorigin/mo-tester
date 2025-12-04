drop database if exists d1;
create database d1;
use d1;
drop table if exists t1;
drop table if exists t4;
create table t1(c1 int primary key);
create table t4(c1 bigint primary key, c2 bigint);
insert into t1 select * from generate_series(10000) g;
insert into t4 select c1, c1 from t1;

-- @separator:table
select mo_ctl('dn', 'flush', 'd1.t4');

-- @separator:table
-- 示例1: 检查基本 EXPLAIN 输出是否包含关键节点和条件
-- @hint:Table Scan,Filter Cond,= 3,Block Filter Cond,Project
explain select * from t4 where c1 + 2 = 5;

-- @separator:table
-- 示例2: 检查 EXPLAIN ANALYZE 输出是否包含所有性能指标字段
-- 注意：我们只检查字段名是否存在，不检查具体数值
-- @hint:Table Scan,ReadSize=,|,bytes,InputSize=,OutputSize=,MemorySize=,timeConsumed=,waitTime=,inputRows=,outputRows=,inputBlocks=,= 3,Filter Cond,Block Filter Cond,Project,Analyze:
explain (analyze true) select * from t4 where c1 + 2 = 5;

-- @separator:table
-- 示例3: 检查复杂查询计划的关键节点
-- @hint:Sort,Limit:,Filter,Table Scan,% 2,% 3,Project
explain select * from (select * from t1 where c1%3=0 order by c1 desc limit 10) tmpt where c1 % 2 = 0;

-- @separator:table
-- 示例4: 检查 EXPLAIN ANALYZE 复杂查询的性能指标
-- @hint:Sort,Limit:,Filter,timeConsumed=,inputRows=,outputRows=,Analyze:
explain (analyze true) select * from (select * from t1 where c1%3=0 order by c1 desc limit 10) tmpt where c1 % 2 = 0;

drop database if exists d1;


