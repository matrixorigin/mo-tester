create table t1 ( a int not null default 1, int32 int primary key);
insert into t1 (int32) values (-1),(1234567890),(2147483647);
select * from t1 order by a desc, int32 asc;
select min(int32),max(int32),max(int32)-1 from t1;
select min(int32),max(int32),max(int32)-1 from t1 group by a;
drop table t1;
CREATE TABLE NATION  ( 
N_NATIONKEY  INTEGER NOT NULL,
N_NAME       VARCHAR(25) NOT NULL,
N_REGIONKEY  INTEGER NOT NULL,
N_COMMENT    VARCHAR(152),
PRIMARY KEY (N_NATIONKEY)
);
load data infile '$resources/data/nation.tbl' into table nation FIELDS TERMINATED BY '|' LINES TERMINATED BY '\n';
select * from nation;