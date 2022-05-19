create database if not exists decimal_test;
use decimal_test;
drop table if exists numtable;
-- test for min length of decimal type
create table numtable(id int, dt decimal(1,0));
-- test for insertion of type[int]
insert into numtable values(1,1);
insert into numtable values(2,2);
insert into numtable values(2,-2);
-- expectation:return error
insert into numtable values(2,20);
select * from numtable;

drop table if exists numtable;
-- test for normal length of decimal type
create table numtable(id int, dt decimal(10,5));
-- test for insertion of type[int]
insert into numtable values(1,1);
insert into numtable values(2,99999);
insert into numtable values(2,-99999);
-- expectation:return error
insert into numtable values(3,100000);
select * from numtable;

drop table if exists numtable;
create table numtable(id int, dt decimal(10,5));
-- test for insertion of type[float]
insert into numtable values(1,1.0);
insert into numtable values(2,99999.999999);
insert into numtable values(2,99999.9999999);
insert into numtable values(2,99999.99999);
insert into numtable values(2,12345.12345);
insert into numtable values(2,9999.999999);
insert into numtable values(2,9999.999994);
insert into numtable values(2,9999.9999);
insert into numtable values(2,-9999.9999);
insert into numtable values(2,-9999.999999);
-- insert into numtable values(2, 1.01e4);
-- insert into numtable values(5,9.9999999994e4);
select * from numtable;
-- test for rounding
insert into numtable values(6,99999.000001);
insert into numtable values(6,99999.000005);
insert into numtable values(7,99999.000011);
insert into numtable values(7,99999.000014);
insert into numtable values(7,99999.000025);
insert into numtable values(4,99999.999985);
insert into numtable values(4,-99999.999985);
insert into numtable values(4,99999.999994);

-- expectation:return error
insert into numtable values(3,100000.0);
insert into numtable values(3,999999.9999);
insert into numtable values(5,99999.999995);
insert into numtable values(5,-99999.999995);
insert into numtable values(5,9.9999999995e4);
select * from numtable;

drop table if exists numtable;
create table numtable(id int, dt decimal(10,5));
-- test for insertion of type[transferabled char]
insert into numtable values(1,'123.45');
insert into numtable values(2,'99999.999985');
-- test for insertion of null
insert into numtable values(1,null);
-- test for insertion of illegal value
insert into numtable values(1,'abc');
select * from numtable;

drop table if exists numtable;
-- test for max length of decimal type
create table numtable(id int, dt decimal(38,10));
insert into numtable values(1,123.45);
insert into numtable values(1,1000000000000000000000000000.45);
insert into numtable values(1,100000000000000000000000000000.45);
select * from numtable;
drop table if exists numtable;


drop table if exists numtable;
-- test for default value of precision(10) and scale(0)
create table dec_s_defalut(id int, dt decimal(5));
insert into dec_s_defalut values(1,100);
insert into dec_s_defalut values(2,99.99);
select * from dec_s_defalut;
create table dec_p_defalut(id int, dt decimal);
insert into dec_p_defalut values(1,100);
insert into dec_p_defalut values(2,9999.999999);
insert into dec_p_defalut values(2,999999999.9);
insert into dec_p_defalut values(2,9999999999.4);
insert into dec_p_defalut values(2,10000000000.4);
select * from dec_p_defalut;
drop table if exists dec_s_defalut;
drop table if exists dec_p_defalut;

-- test for exception of ddl
#maybe error
create table numtable(id int, dt decimal(0,0));
create table numtable(id int, dt decimal(-1,0));
create table numtable(id int, dt decimal(39,10));
create table numtable(id int, dt decimal(a,10));
create table numtable(id int, dt decimal(10,-1));
create table numtable(id int, dt decimal(10,11));
create table numtable(id int, dt decimal(10,b));

-- test for distinct type of decimal column
drop table if exists dt_dis;
create table dt_dis(id int, dt decimal(10,5));
insert into dt_dis values(1,100);
insert into dt_dis values(1,100.00);
insert into dt_dis values(1,'100.00');
insert into dt_dis values(1,99999.99999);
insert into dt_dis values(1,99999.999994);
insert into dt_dis values(1,99998.999995);
insert into dt_dis values(1,99999);
insert into dt_dis values(1,99999.00);
insert into dt_dis values(1,'99998.999995');
insert into dt_dis values(2,null);
insert into dt_dis values(3,null);
select distinct dt from dt_dis;
drop table if exists dt_dis;

-- test for numeric func of decimal column
drop table if exists dt_func;
create table dt_func(id int, dt decimal(10,5));
insert into dt_func values(1,100),(2,200),(2,-200.12),(2,-0.321);
insert into dt_func values(1,100.00);
insert into dt_func values(1,99999.99999);
insert into dt_func values(1,99999.999994);
insert into dt_func values(1,-99999.99999);
insert into dt_func values(2,null);
insert into dt_func values(3,null);
select min(dt) from dt_func;
select max(dt) from dt_func;
select avg(dt) from dt_func;
select sum(dt) from dt_func;
select count(dt) from dt_func;
select round(dt) from dt_func;
drop table if exists dt_func;

--test for operation
drop table if exists dt_opt;
create table dt_opt(id int, dt decimal(10,5));
insert into dt_opt values(1,100),(2,200),(2,-200.12),(2,-0.321);
insert into dt_opt values(1,100.00);
insert into dt_opt values(1,99999.99999);
insert into dt_opt values(1,99999.999994);
insert into dt_opt values(1,-99999.99999);
insert into dt_opt values(2,null);
insert into dt_opt values(3,null);
select dt+1 from dt_opt;
select dt+1 from dt_opt;
select dt-10 from dt_opt;
select dt*3.5 from dt_opt;
select dt*5 from dt_opt;
select dt/2 from dt_opt;
select dt/4.3 from dt_opt;
select dt%10 from dt_opt;
select * from dt_opt where dt > 0;
select * from dt_opt where dt < 100.30;
select * from dt_opt where dt <> 99999.99999;

-- test for addition for the length > 18
drop table if exists dt_opt;
create table dt_opt(id int, dt decimal(38,5));
insert into dt_opt values(1,999999999999999999);
select dt + 999999999999999999999999999999999999.11 from dt_opt;
drop table if exists dt_opt;