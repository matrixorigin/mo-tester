
# What's in MO-Tester?

MO-Tester is a java-based tester suite for MatrixOne. It has built a whole toolchain to run automatic SQL tests. It contains the test cases and results. Once launched, MO-Tester runs all SQL test cases with MatrixOne, and compares all output SQL results with expected results. All successful and failed cases will be logged into reports.

MO-Tester content locations:

* *Cases*: <https://github.com/matrixorigin/matrixone/tree/main/test/cases>

* *Result*: <https://github.com/matrixorigin/mo-tester/tree/main/result>

    + *Result* can also generated in the path of [/cases](https://github.com/matrixorigin/matrixone/tree/main/test/cases/), for example, [/cases/auto_increment](https://github.com/matrixorigin/matrixone/tree/main/test/cases/auto_increment).

* *Report*: once finished running, a `mo-tester/report` will be generated in the local directory.

The Cases and Results are 1-1 correspondence, and they are actually `git submodules` from MatrixOne repository. Adding new cases and results should be in MatrixOne repo: <https://github.com/matrixorigin/matrixone/tree/main/test>

# How to use MO-Tester?

## 1. Prepare the testing environment

* Make sure you have installed jdk8.

* Launch MatrixOne or other database instance. Please refer to more information about [how to install and launch MatrixOne](https://github.com/matrixorigin/matrixorigin.io/blob/main/docs/MatrixOne/Get-Started/install-standalone-matrixone.md).

* Clone *mo-tester* repository.

  ```
  git clone https://github.com/matrixorigin/mo-tester.git
  ```

* Clone *matrixOne* repository.

   ```
   git clone https://github.com/matrixorigin/matrixone.git
   ```

## 2. Configure `mo-tester`

* In `mo.yml` file, configure the server address, default database name, username, and password, etc. MO-tester is based on java, so these parameters are required for the JDBC(JDBC，Java Database Connectivity) driver. Below is a default example for a local standalone version MatrixOne.

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
      socketTimeout: 30000
  #users
  user:
    name: "dump"
    passwrod: "111"
  ```
* In `kafka.yml` file, configure the kafka server address, only for the cases that need to produce or consume messages from kafka server.
## 3. Run mo-tester

* With the simple below command, all the SQL test cases will automatically run and generate reports and error messages to *report/report.txt* and *report/error.txt*.

```
> ./run.sh -p {path_name}/matrixone/test/cases
```

If you'd like to adjust the test range, you can just change the `path` parameter of `run.yml`. And you can also specify some parameters when executing the command `./run.sh`, parameters are as followings:

| Parameters |Description|
|------------|---|
| -p         |set the path of test cases needed to be executed by mo-tester, the default value is configured by the `path` in `run.yaml`|
| -m         |set the method that mo-tester will run with, the default value is configured by the `method` in `run.yaml`|
| -t         |set the times that mo-tester will execute cases for, must be numeric, default is 1|
| -r         |set the success rate that test cases should reach, the default value is configured by the `rate` in `run.yaml`|
| -i         |set the including list, and only script files in the path whose name contains one of the lists will be executed, if more than one, separated by `,`, if not specified, refers to all cases included|
| -e         |set the excluding list, and script files in the path whose name contains one of the lists will not be executed, if more than one, separated by `,`, if not specified, refers to none of the cases excluded|
| -g         |means SQL commands which is marked with [bvt:issue] flag will not be executed,this flag starts with [-- @bvt:issue#{issueNO.}],and ends with [-- @bvt:issue],eg:<br>-- @bvt:issue#3236<br/><br>select date_add("1997-12-31 23:59:59",INTERVAL "-10000:1" HOUR_MINUTE);<br/><br>select date_add("1997-12-31 23:59:59",INTERVAL "-100 1" YEAR_MONTH);<br/><br>-- @bvt:issue<br/><br>Those two sql commands are associated with issue#3236, and they will not be executed in bvt test, until the flag is removed when issue#3236 is fixed.<br/>|
| -n         |Global flag: means the metadata of the resultset will be ignored when comparing the result. This can be overridden by document-level (`--- @metacmp(boolean)`) or SQL-level (`-- @metacmp(boolean)`) flags.|
| -s         |set the resource path that mo-tester use to store resources, and can be refered to in test file. The default value is derived from `path`. |

**Examples**:

```
./run.sh -p {path_name}/matrixone/test/cases -m run -t script -r 100 -i select,subquery -e substring -g
```

If you want to automatically generate SQL results for the new SQL cases, you can just change the `method` parameter of *run.yml* file to `genrs`, or you can just change the command `-m run` to `-m genrs`, then running the `./run.sh` scripts will directly record test results in the same path of the new SQL case file. For more information on example, see <a href="#new_test_scenario">Example 4</a>.

!!! note
Every time running `run.sh` will overwrite the report of the  *error.txt* file, *report.txt* file, and *success.txt* file in the  *mo-tester* repository.


## 4. Set tags in case scripts
Sometimes, to achieve some specific purposes, such as pausing or creating a new connection, you can add some special tags to the script file. The mo tester provides the following tags for use:

| Tags                                                          | Description                                                                                                                                                                                           |
|---------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| -- @skip:issue#{IssueNo.}                                     | If set, the whole script file will be skipped, and not be executed any more for issue{IssueNo.}                                                                                                       |
| -- @bvt:issue#{IssueNo.}<br/>-- @bvt:issue                    | The sql statements between those two tags will be not executed for issue{IssueNo.}                                                                                                                    |
| -- @sleep:{time}                                              | The mo-tester will wait for {time} s                                                                                                                                                                  |
| -- @wait_expect({interval}, {timeout})                        | Intelligent polling mechanism that repeatedly executes the SQL until the result matches the expected output or timeout is reached. `{interval}` is the polling interval in seconds (how often to retry), and `{timeout}` is the maximum wait time in seconds. This replaces fixed sleep times with smart waiting.<br/><br/>**Example:**<br/>`-- @wait_expect(1, 10)`<br/>`SELECT * FROM orders WHERE status = 'completed';`<br/><br/>This will poll every 1 second for up to 10 seconds until the query returns the expected result. Useful for testing asynchronous operations, data replication, or eventual consistency scenarios.<br/><br/>**Modes:**<br/>- **run mode**: Polls until result matches or timeout<br/>- **genrs mode**: Sleeps for timeout seconds, then executes once<br/><br/>**Features:**<br/>- Supports query results (SELECT)<br/>- Supports error matching (waiting for errors to appear/disappear)<br/>- Detailed logging of retry attempts and timing |
| -- @session:id=2&user=root&password=111<br/> -- @session      | The mo-tester will create a new connetion to execute sql statements between those two tags.<br/>Default value of id is 1, max is 10.<br/>Defualt value of user and password is configured in `mo.yml`. |
| -- @sortkey:                                                  | If the result is sorted, need set this tag for the sql statement. e.g.<br/> -- @sortkey:0,1: means sort keys are first column and second colum.                                                       |
| -- @system {C}                                                | Set System Command that will be executed by the runner system                                                                                                                                         |
| -- @wait:{D}:[commit or wait]                                 | means this command will be blocked until the connection[id={D}] commit or rollback                                                                                                                    |
| -- @ignore:{num},...{num}                                     | means the designated columns which index are in {num}s will not be check.                                                                                                                             |
| -- @regex(<pattern:string, include:boolean>)                  | the regex check feature allows you to specify patterns that must be matched in the result set. A pattern will be checked for inclusion in the results if `include = true`. Multiple regex patterns can be added, and all will be joined with an AND operator, meaning all conditions must be satisfied.                                                                                                                 |
| --- @metacmp(boolean)                                         | Document-level flag to control whether to compare metadata (column names, types, etc.) of result sets. Set to `true` to enable meta comparison, `false` to disable. This flag applies to all SQL statements in the file until overridden by SQL-level flags. Priority: SQL-level > Document-level > Global (command-line `-n` flag). Example: `--- @metacmp(false)` disables meta comparison for the entire file.                                                                                                                             |
| -- @metacmp(boolean)                                          | SQL-level flag to control whether to compare metadata for a specific SQL statement. Set to `true` to enable meta comparison, `false` to disable. This flag has the highest priority and overrides document-level and global settings. Example: `-- @metacmp(true)` enables meta comparison for the following SQL statement only.                                                                                                                             |




## 5. Check the report

* Once the test is finished, *mo-tester* generates *error.txt* file, *report.txt* file and *success.txt* file reports.

* An example of *report.txt* file looks like this:

```
[SUMMARY] COST : 98s, TOTAL :12702, SUCCESS : 11851, FAILED :13, IGNORED :838, ABNORAML :0, SUCCESS RATE : 99%
[{path_name}/matrixone/test/cases/auto_increment/auto_increment_columns.sql] COST : 2.159s, TOTAL :185, SUCCESS :163, FAILED :0, IGNORED :22, ABNORAML :0, SUCCESS RATE : 100%
[{path_name}/matrixone/test/cases/benchmark/tpch/01_DDL/01_create_table.sql] COST : 0.226s, TOTAL :11, SUCCESS :11, FAILED :0, IGNORED :0, ABNORAML :0, SUCCESS RATE : 100%
[{path_name}/matrixone/test/cases/benchmark/tpch/02_LOAD/02_insert_customer.sql] COST : 0.357s, TOTAL :16, SUCCESS :16, FAILED :0, IGNORED :0, ABNORAML :0, SUCCESS RATE : 100%
```

|Report Keywords|Description|
|---|---|
|TOTAL|the total number of executed test cases (SQL)|
|SUCCESS|The total number of successfully executed test cases(SQL)|
|FAILED|the total number of failed executed test case(SQL)|
|IGNORED| the total number of ignored executed test cases (SQL), especially with the `--bvt:issue` tag test cases (SQL)|
|ABNORAML|the total number of abnormal executed test cases (SQL), such as the execution of MatrixOne can't determine the actual result is a system exception or *.result* file parsing error, etc|
|SUCCESS RATE|success rate: SUCCESS/(TOTAL - IGNORED)|

* An example of *error.txt* file looks like this:

```
[ERROR]
[SCRIPT   FILE]: cases/transaction/atomicity.sql
[ROW    NUMBER]: 14
[SQL STATEMENT]: select * from test_11 ;
[EXPECT RESULT]:
c	d
1	1
2 2
[ACTUAL RESULT]:
c	d
1	1
```

## 6. Test Examples

### Example 1

**Example Description**: Run all test cases in the */cases* path of the *matrixone* repository.

**Steps**:

1. Get the latest *matrixone* code.

   ```
   cd matrixone
   git pull https://github.com/matrixorigin/matrixone.git
   ```

2. To run all the test cases of the *matrixone* repository, you need switch into the  *mo-tester* repository first, see the following commands:

   ```
   cd mo-tester
   ./run.sh -p {path_name}/matrixone/test/cases
   ```

3. Check the result reports in the *error.txt* file, *report.txt* file, and *success.txt* file in the *mo-tester/report/* path.

### Example 2

**Example Description**: Run the test cases in the */cases/transaction/* path of the *matrixone* repository.

**Steps**:

1. Get the latest *matrixone* code.

   ```
   cd matrixone
   git pull https://github.com/matrixorigin/matrixone.git
   ```

2. To run the test cases in the *cases/transaction/* path of the *matrixone* repository, you need switch into the  *mo-tester* repository first, see the following commands:

   ```
   cd mo-tester
   ./run.sh -p {path_name}/matrixone/test/cases/transaction/
   ```

3. Check the result reports in the *error.txt* file, *report.txt* file, and *success.txt* file in the *mo-tester/report/* path. The example of the expected *report.txt* looks like this:

   ```
   [SUMMARY] COST : 5s, TOTAL :1362, SUCCESS : 1354, FAILED :0, IGNORED :8, ABNORAML :0, SUCCESS RATE : 100%
   [{path_name}/matrixone/test/cases/transaction/atomicity.sql] COST : 0.575s, TOTAL :66, SUCCESS :66, FAILED :0, IGNORED :0, ABNORAML :0, SUCCESS RATE : 100%
   [{path_name}/matrixone/test/cases/transaction/autocommit.test] COST : 0.175s, TOTAL :50, SUCCESS :50, FAILED :0, IGNORED :0, ABNORAML :0, SUCCESS RATE : 100%
   [{path_name}/matrixone/test/cases/transaction/autocommit_1.sql] COST : 1.141s, TOTAL :296, SUCCESS :288, FAILED :0, IGNORED :8, ABNORAML :0, SUCCESS RATE : 100%
   [{path_name}/matrixone/test/cases/transaction/autocommit_atomicity.sql] COST : 0.52s, TOTAL :75, SUCCESS :75, FAILED :0, IGNORED :0, ABNORAML :0, SUCCESS RATE : 100%
   [{path_name}/matrixone/test/cases/transaction/autocommit_isolation.sql] COST : 1.607s, TOTAL :215, SUCCESS :215, FAILED :0, IGNORED :0, ABNORAML :0, SUCCESS RATE : 100%
   [{path_name}/matrixone/test/cases/transaction/autocommit_isolation_1.sql] COST : 1.438s, TOTAL :241, SUCCESS :241, FAILED :0, IGNORED :0, ABNORAML :0, SUCCESS RATE : 100%
   [{path_name}/matrixone/test/cases/transaction/isolation.sql] COST : 1.632s, TOTAL :202, SUCCESS :202, FAILED :0, IGNORED :0, ABNORAML :0, SUCCESS RATE : 100%
   [{path_name}/matrixone/test/cases/transaction/isolation_1.sql] COST : 1.512s, TOTAL :217, SUCCESS :217, FAILED :0, IGNORED :0, ABNORAML :0, SUCCESS RATE : 100%
   ```

### Example 3

**Example Description**: Run the single test case *cases/transaction/atomicity.sql*.

**Steps**:

1. Get the latest *matrixone* code.

   ```
   cd matrixone
   git pull https://github.com/matrixorigin/matrixone.git
   ```

2. To run the test cases *cases/transaction/atomicity.sql*, you need switch into the  *mo-tester* repository first, see the following commands:

   ```
   cd mo-tester
   ./run.sh -p {path_name}/matrixone/test/cases/transaction/atomicity.sql
   ```

3. Check the result reports in the *error.txt* file, *report.txt* file, and *success.txt* file in the *mo-tester/report/* path. The example of the expected *report.txt* looks like this:

   ```
   [SUMMARY] COST : 0s, TOTAL :66, SUCCESS : 66, FAILED :0, IGNORED :0, ABNORAML :0, SUCCESS RATE : 100%
   [{path_name}/matrixone/test/cases/transaction/atomicity.sql] COST : 0.56s, TOTAL :66, SUCCESS :66, FAILED :0, IGNORED :0, ABNORAML :0, SUCCESS RATE : 100%
   ```

### <h3><a name="new_test_scenario">Example 4</a></h3>

**Example Description**:

- Create a new folder named *local_test* and place it in *{path_name}/matrixone/test/cases*
- Add a test file named *new_test.sql* to *{path_name}/matrixone/test/cases/local_test/*
- Only run the single test case *new_test.sql**

**Steps**

1. Get the latest *matrixone* code.

   ```
   cd matrixone
   git pull https://github.com/matrixorigin/matrixone.git
   ```

2. Generate test results:

    - Method 1: To generate the test result, you need switch into the  *mo-tester* repository first, then, run the following command.

       ```
       cd mo-tester
       ./run.sh -p {path_name}/matrixone/test/cases/local_test/new_test.sql -m genrs -g
       ```

    - Method 2: Open the *run.yml* file in the *mo-tester* repository, change the *method* parameter from the default `run` to `genrs`, and run the following command to generate the test result.

       ```
       cd mo-tester
       ./run.sh -p {path_name}/matrixone/test/cases/local_test/new_test.sql
       ```

3. Check the result file in the *test/cases、result/* path of the *matrixone* repository.

4. Check the result reports in the *error.txt* file, *report.txt* file, and *success.txt* file in the *mo-tester/report/* path. The example of the expected *report.txt* looks like this:

   ```
   [SUMMARY] COST : 0s, TOTAL :66, SUCCESS : 66, FAILED :0, IGNORED :0, ABNORAML :0, SUCCESS RATE : 100%
   [{path_name}/matrixone/test/cases/transaction/atomicity.sql] COST : 0.56s, TOTAL :66, SUCCESS :66, FAILED :0, IGNORED :0, ABNORAML :0, SUCCESS RATE : 100%
   ```

## Development

### Main Dependencies

- Java 8 or later
- Maven for dependency and build management
- [log4j](https://logging.apache.org/log4j/)
- [commons-io](https://commons.apache.org/proper/commons-io/)

### How to Build

1. Make sure you have Java JDK and Maven installed.
2. From the root of the `mo-tester` project, run:

   ```sh
   mvn clean compile jar:jar -DskipTests
   ```

3. After a successful build, an executable JAR (if configured) will be created in the `target/` directory.

### How to release

copy the ./mo-tester-1.0-SNAPSHOT.jar to ./lib

### How to Test

```sh
mvn test
mvn test -D test='ScriptParserTest'
mvn test -D test='RegexParsingTest#testParseEscapedCharacters'
```


## Notes

### Parallel Execution Rules

MO-Tester supports parallel execution of test cases. Directories are split into two groups based on naming convention:

- **Group 1 (Parallel Execution)**: Directories whose name matches the pattern `{digit}_parallel` (e.g., `1_parallel`, `2_parallel`) will be executed using a separate executor (`executor2`) in parallel. The `executor2` uses a non-sys account (account: shuyuan, user: kongzi, password: 111).
- **Group 2 (Sequential Execution)**: All other directories will be executed using the default executor (`executor`) with the sys account.

**Restrictions for parallel directories (`{digit}_parallel`):**

1. No `mo_ctl` commands, except `mo_ctl('dn', 'flush', '<db>.<table>')` is allowed
2. No `create/drop account` operations
3. No session on sys account (e.g., `-- @session:id=1&user=sys:dump&password=111`). Note: In Group 1, using `-- @session:id=<digit>` will open a new session with the `shuyuan:kongzi` account by default

These restrictions ensure that parallel test cases do not interfere with each other during concurrent execution.

## Wait_Expect Feature

### Overview

The `@wait_expect` tag provides an intelligent polling mechanism that replaces fixed sleep times with smart waiting. Instead of sleeping for a fixed duration and hoping the data is ready, `wait_expect` repeatedly checks the result until it matches the expected output or a timeout is reached.

### Syntax

```sql
-- @wait_expect({interval}, {timeout})
SELECT ...
```

**Parameters:**
- `{interval}`: Polling interval in seconds (how often to retry)
- `{timeout}`: Maximum wait time in seconds

### Use Cases

1. **Asynchronous Operations**: Wait for background tasks to complete
2. **Data Replication**: Wait for data to be replicated across nodes
3. **Eventual Consistency**: Wait for distributed systems to reach consistency
4. **State Transitions**: Wait for status changes (e.g., 'pending' → 'completed')
5. **Error Recovery**: Wait for errors to appear or disappear

### Examples

#### Example 1: Wait for Data to Appear

```sql
CREATE TABLE orders (id INT, status VARCHAR(20));
INSERT INTO orders VALUES (1, 'pending');

-- Wait for status to become 'completed'
-- Polls every 1 second for up to 10 seconds
-- @wait_expect(1, 10)
SELECT * FROM orders WHERE status = 'completed';

-- In another session or background process:
UPDATE orders SET status = 'completed' WHERE id = 1;
```

**Behavior:**
- Executes the SELECT query
- If result doesn't match expected, waits 1 second and retries
- Continues polling until result matches or 10 seconds elapsed
- Much more efficient than `-- @sleep:10`

#### Example 2: Wait for Count to Increase

```sql
CREATE TABLE events (id INT, processed BOOLEAN);

-- Wait for at least 5 processed events
-- @wait_expect(1, 30)
SELECT COUNT(*) FROM events WHERE processed = true;
```

**Expected result file:**
```
SELECT COUNT(*) FROM events WHERE processed = true;
count(*)
5
```

#### Example 3: Wait for Table Creation (Error Retry)

```sql
-- Query a table that doesn't exist yet
-- Will retry on error until table is created
-- @wait_expect(1, 10)
SELECT * FROM new_table;

-- Table gets created by another process
CREATE TABLE new_table (id INT, name VARCHAR(50));
INSERT INTO new_table VALUES (1, 'test');
```

**Behavior:**
- First attempt: SQLException (table not found)
- Waits 1 second and retries
- Continues until table exists or timeout
- Supports waiting for errors to disappear

#### Example 4: Wait for Aggregation Result

```sql
CREATE TABLE metrics (value INT);

-- Wait for sum to exceed 100
-- @wait_expect(2, 20)
SELECT SUM(value) as total FROM metrics WHERE total > 100;

-- Data gets inserted gradually
INSERT INTO metrics VALUES (50);
INSERT INTO metrics VALUES (60);
```

#### Example 5: Combining with Other Tags

```sql
-- Use with @session for multi-connection scenarios
-- @session:id=2
INSERT INTO orders VALUES (1, 'pending');
-- @session

-- Main session waits for the insert
-- @wait_expect(1, 5)
SELECT COUNT(*) FROM orders;

-- Use with @sleep for initial delay
-- @sleep:2
-- @wait_expect(1, 10)
SELECT * FROM orders WHERE status = 'completed';
```

### Modes

#### Run Mode (Default)

```bash
./run.sh -p cases/test.sql -m run
```

**Behavior:**
- Polls the SQL statement at `{interval}` seconds
- Compares actual result with expected result
- Stops when result matches or `{timeout}` is reached
- Logs retry attempts and timing

#### GenRS Mode (Generate Results)

```bash
./run.sh -p cases/test.sql -m genrs
```

**Behavior:**
- Sleeps for `{timeout}` seconds (one-time wait)
- Executes the SQL once
- Captures the result as expected output
- Useful for generating baseline results

### Performance Benefits

**Traditional approach with @sleep:**
```sql
-- @sleep:10
SELECT * FROM orders WHERE status = 'completed';
```
- Always waits 10 seconds, even if data is ready after 1 second
- Wastes 9 seconds in this case

**Smart approach with @wait_expect:**
```sql
-- @wait_expect(1, 10)
SELECT * FROM orders WHERE status = 'completed';
```
- Checks every 1 second
- Stops as soon as data is ready
- Typical time savings: 50-80%

### Logging

When using `@wait_expect`, detailed logs are generated:

```
2024-02-10 10:00:00 INFO - [test.sql][row:5] Starting wait_expect: interval=1s, timeout=10s
2024-02-10 10:00:01 INFO - Retry attempt 1
2024-02-10 10:00:02 INFO - Retry attempt 2
2024-02-10 10:00:03 INFO - Result matched, test passed
```

Or on timeout:
```
2024-02-10 10:00:00 INFO - [test.sql][row:5] Starting wait_expect: interval=1s, timeout=10s
2024-02-10 10:00:10 WARN - [test.sql][row:5] wait_expect timeout, result did not match expected
```

### Best Practices

1. **Choose appropriate intervals:**
   - Fast operations: 0.5-1 second
   - Slow operations: 2-5 seconds
   - Very slow operations: 5-10 seconds

2. **Set reasonable timeouts:**
   - Development: 10-30 seconds
   - CI/CD: 30-60 seconds
   - Stress tests: 60-300 seconds

3. **Use with specific conditions:**
   ```sql
   -- Good: Specific condition
   -- @wait_expect(1, 10)
   SELECT * FROM orders WHERE id = 1 AND status = 'completed';
   
   -- Avoid: Too broad
   -- @wait_expect(1, 10)
   SELECT * FROM orders;
   ```

4. **Combine with other features:**
   ```sql
   -- Use with @session for multi-connection tests
   -- Use with @sortkey for ordered results
   -- Use with @regex for pattern matching
   ```

### Troubleshooting

**Problem: Test always times out**

Solution:
- Check if expected result is correct
- Increase timeout value
- Verify data is actually being updated
- Check logs for error messages

**Problem: Test passes but takes too long**

Solution:
- Decrease interval for faster polling
- Optimize the SQL query
- Check database performance

**Problem: Inconsistent results**

Solution:
- Ensure expected result file is up to date
- Regenerate results with `genrs` mode
- Check for race conditions in multi-session tests

### Migration from @sleep

**Before:**
```sql
INSERT INTO orders VALUES (1, 'pending');
-- @sleep:10
SELECT * FROM orders WHERE status = 'completed';
```

**After:**
```sql
INSERT INTO orders VALUES (1, 'pending');
-- @wait_expect(1, 10)
SELECT * FROM orders WHERE status = 'completed';
```

**Benefits:**
- Faster test execution (typically 50-80% faster)
- More reliable (doesn't depend on fixed timing)
- Better logging and debugging
- Handles variable latency gracefully
