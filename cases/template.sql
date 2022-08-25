create table t1 ( a int not null default 1, int32 int primary key);
insert into t1 (int32) values (-1),(1234567890),(2147483647);
select * from t1 order by a desc, int32 asc;
select min(int32),max(int32),max(int32)-1 from t1;
select min(int32),max(int32),max(int32)-1 from t1 group by a;
drop table t1;