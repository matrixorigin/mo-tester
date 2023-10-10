select sleep(10);
create table t1 ( a int not null default 1, int32 int primary key);
insert into t1 (int32) values (-1),(1234567890),(2147483647);
-- @pattern
insert into t1 (int32) values (-1),(1234567890),(2147483647);
select * from t1 order by a desc, int32 asc;
select min(int32),max(int32),max(int32)-1 from t1;
-- @bvt:issue#1234
select min(int32),max(int32),max(int32)-1 from t1 group by a;
-- @bvt:issue

drop table t1;
CREATE TABLE NATION  ( 
N_NATIONKEY  INTEGER NOT NULL,
N_NAME       VARCHAR(25) NOT NULL,
N_REGIONKEY  INTEGER NOT NULL,
N_COMMENT    VARCHAR(152),
PRIMARY KEY (N_NATIONKEY)
);
-- @sleep:3
load data infile '$resources/data/nation.tbl' into table nation FIELDS TERMINATED BY '|' LINES TERMINATED BY '\n';
select * from nation;

-- @delimiter $
create table t1 ( a int not null default 1, int32 int primary key);
$
insert into t1 (int32) values (-1),(1234567890),(2147483647);
$

-- @delimiter ;
select * from t1 order by a desc, int32 asc;

-- @system pwd
-- @system ls
-- @system abcd ddaa

-- @session:id=1
use template;
select * from nation;
select * from nation limit 1;
-- @session

-- @session:id=2&user=root&password=111
use template;
select * from nation;
select * from nation limit 2;
-- @session
