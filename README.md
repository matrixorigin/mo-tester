# mo-tester
This directory contains a test suite for the MatrixOne engine.

# How to use?

This tester is designed to test MatrixOne or any other database functionalities with SQL. 

### 0. Install JDK8 if you don't have it yet.  

### 1: Run your MatrixOne instance or other DB instance. 

Checkout [Install MatrixOne](https://docs.matrixorigin.io/0.4.0/MatrixOne/Get-Started/install-standalone-matrixone/) to launch a MatrixOne instance.

Or you can launch whatever database software as you want. 

### 2. Fork and clone this mo-tester project. 

### 3. Configure the connection. 

In `mo.yml` file, configure the server address, default database name, username&password etc. MO-tester is based on java, so these parameters are required for JDBC driver.
Below is a default example for a local standalone version MatrixOne.

```
#jdbc
jdbc:
  driver: "com.mysql.cj.jdbc.Driver"
  server:
  - addr: "127.0.0.1:6001"
  database:
    default: "test"
  paremeter:
    characterSetResults: "utf8"
    continueBatchOnError: "false"
    useServerPrepStmts: "true"
    alwaysSendSetIsolation: "false"
    useLocalSessionState: "true"
    zeroDateTimeBehavior: "CONVERT_TO_NULL"
    failoverReadOnly: "false"
    serverTimezone: "Asia/Shanghai"

#users
user:
  name: "dump"
  passwrod: "111"
```
### 4. Run the test.

With the simple below command, all the SQL test cases will automatically run and generate reports and error messages to `report/report.txt` and `report/error.txt`.

```
> ./run.sh
```

If you'd like to adjust the test range, you can just change the `path` parameter of `run.yml`. 

And you can also specify some parameters when executing the command `run.sh`,parameters are as followings:
```
-p  set the path of test cases needed to be executed by mo-tester,default value is configured by the `path` in `run.yaml`
-m  set the method that mo-tester will run with,default value is configured by the `method` in `run.yaml`
-t  set the type of the format that mo-tester execute the sqlcommand in,default value is configured by the `type` in `run.yaml`
-r  set The success rate that test cases should reach,default value is configured by the `rate` in `run.yaml`
-i  set the including list, and only script files in the path which name contain one of the list will be excuted,if more than one,seperated by `,`,if not specified,refers to all cases included
-e  set the excluding list, and script files in the path which name contain one of the list will not be excuted,if more than one,seperated by `,`,if not specified,refers to none of cases excluded
-g  means SQL commands which is marked with ignore-flag will not be executed
-n  means the meta data of the resultset will be ignored when comparing the result
Examples:
bash run.sh -p case -m run -t script -r 100 -i select,subquery -e substring -g
```

If you want to automatically generate SQL results for the new SQL cases, you can just change the `method` parameter of `run.yml` to `genrs`. Running the `run.sh` scripts will directly record test results in the `result/` path with their original filenames.

Note: everytime running `run.sh` will overwrite the `error`, `report` and `success` reports.
