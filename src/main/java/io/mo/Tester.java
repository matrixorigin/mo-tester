package io.mo;

import io.mo.cases.TestScript;
import io.mo.constant.COMMON;
import io.mo.db.Executor;
import io.mo.result.TestReport;
import io.mo.util.RunConfUtil;
import io.mo.util.ScriptParser;
import org.apache.log4j.Logger;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import org.apache.commons.io.FileUtils;

public class Tester {
    private static final TestReport report = new TestReport();
    private static final Logger LOG = Logger.getLogger(Tester.class.getName());
    private static String[] includes = null;
    private static String[] excludes = null;
    private static Executor executor = new Executor();

    // parallelEnabled is false by default
    // because:
    // 1. mo_ctl
    // 2. switch to sys account
    // 3. show accounts
    private static boolean parallelEnabled = true;

    // 常量定义
    private static final String METHOD_RUN = "run";
    private static final String METHOD_GENRS = "genrs";
    private static final String SQL_SUFFIX = ".sql";
    private static final String TEST_SUFFIX = ".test";

    public static void main(String[] args) {
        // 初始化配置
        RunConfUtil.setStaticResourcePathFromConfig();
        COMMON.WAIT_TIMEOUT = RunConfUtil.getWaitTime();

        // 解析命令行参数并获取配置
        Config config = parseConfig(args);

        // 验证并执行
        if (!validateConfig(config)) {
            return;
        }

        File file = new File(config.path);
        if (!file.exists()) {
            LOG.error("The scripts file path: " + config.path + " does not exist,please check.");
            return;
        }

        executeMethod(config.method, file, config.path, config.rate);
    }

    /**
     * 配置信息类
     */
    private static class Config {
        String path;
        String method;
        int rate;

        Config(String path, String method, int rate) {
            this.path = path;
            this.method = method;
            this.rate = rate;
        }
    }

    /**
     * 解析配置（从配置文件和命令行参数）
     */
    private static Config parseConfig(String[] args) {
        Config config = new Config(
                RunConfUtil.getPath(),
                RunConfUtil.getMethod(),
                RunConfUtil.getRate());

        if (args != null) {
            parseArguments(args, config);
        }

        return config;
    }

    /**
     * 验证配置
     */
    private static boolean validateConfig(Config config) {
        if (config.path == null) {
            LOG.error("The scripts file path is not configured,please check the config file conf/run.yml.");
            return false;
        }
        if (config.method == null) {
            LOG.error("The method of execution is not configured,please check the config file conf/run.yml.");
            return false;
        }
        return true;
    }

    private static void parseArguments(String[] args, Config config) {
        for (String arg : args) {
            String lowerArg = arg.toLowerCase();

            if (arg.startsWith("path")) {
                config.path = parseParameterValue(arg, "path");
                if (config.path != null) {
                    RunConfUtil.setDerivedStaticResourcePath(config.path);
                    LOG.info("The path is: " + config.path);
                    LOG.info("The resource path is: " + COMMON.RESOURCE_PATH);
                }
            } else if (arg.startsWith("method")) {
                config.method = parseParameterValue(arg, "method");
            } else if (arg.startsWith("rate")) {
                String rateStr = parseParameterValue(arg, "rate");
                if (rateStr != null) {
                    config.rate = Integer.parseInt(rateStr);
                }
            } else if (lowerArg.equals("ignore")) {
                COMMON.IGNORE_MODEL = true;
            } else if (arg.startsWith("include")) {
                String value = parseParameterValue(arg, "include");
                if (value != null) {
                    includes = value.split(",");
                }
            } else if (arg.startsWith("exclude")) {
                String value = parseParameterValue(arg, "exclude");
                if (value != null) {
                    excludes = value.split(",");
                }
            } else if (arg.startsWith("resource")) {
                String resourcePath = parseParameterValue(arg, "resource");
                if (resourcePath != null && !resourcePath.isEmpty()) {
                    COMMON.RESOURCE_PATH = resourcePath;
                }
            } else if (lowerArg.equals("nometa")) {
                COMMON.IS_COMPARE_META = false;
            } else if (lowerArg.equals("pprof")) {
                COMMON.NEED_PPROF = true;
            }
        }
    }

    /**
     * 解析带值的参数（格式：key=value）
     */
    private static String parseParameterValue(String arg, String paramName) {
        if (!arg.contains("=")) {
            LOG.error("The format of para[" + paramName + "] is not valid,please check......");
            System.exit(1);
        }
        return arg.split("=", 2)[1];
    }

    /**
     * 执行对应的方法
     */
    private static void executeMethod(String method, File file, String path, int rate) {
        String methodLower = method.toLowerCase();

        if (METHOD_RUN.equals(methodLower)) {
            executeRunMethod(file, path, rate);
        } else if (METHOD_GENRS.equals(methodLower)) {
            executeGenRsMethod(file, path);
        } else {
            LOG.info("The method is [" + method + "] can not been supported.Only[run,genrs] can be supported.");
        }
    }

    /**
     * 执行 run 方法
     */
    private static void executeRunMethod(File file, String path, int rate) {
        prepareExecution();

        LOG.info("The method is [run],now start to run the scripts in the path[" + path + "].");
        run(file);
        LOG.info("All the scripts in the path[" + path + "] have been excuted.Now start to create the test report.");
        report.write();
        LOG.info("The test report has been generated in files[report.txt,report.xml].");
        removeOutfiles();

        int successRate = report.getRate();
        if (successRate < rate) {
            LOG.error(
                    String.format("The execution success rate is %d%%, and less than config value %d%%,this test fail.",
                            successRate, rate));
            System.exit(1);
        } else {
            LOG.info(String.format(
                    "The execution success rate is %d%%, and not less than config value %d%%,this test succeed.",
                    successRate, rate));
            System.exit(0);
        }
    }

    /**
     * 执行 genrs 方法
     */
    private static void executeGenRsMethod(File file, String path) {
        prepareExecution();
        LOG.info("The method is [genrs],now start to generate the checkpoints in the path[" + path + "].");
        generateRs(file);
    }

    /**
     * 准备执行：清理数据库和输出文件
     */
    private static void prepareExecution() {
        LOG.info("Now start to clean up databases and outfiles.");
        executor.cleanDatabases();
        executor.dropAccountForTest();
        removeOutfiles();
    }

    /**
     * 运行测试脚本
     */
    public static void run(File file) {
        if (shouldParallelize(file)) {
            runParallel(file);
        } else {
            runSerial(file);
        }
    }

    /**
     * 串行执行测试脚本（复用原有逻辑）
     */
    private static void runSerial(File file) {
        processFiles(file, script -> {
            executor.run(script);
            report.collect(script);
        }, true);
    }

    /**
     * 并行执行测试脚本
     */
    private static void runParallel(File dir) {
        File[] subDirs = getDirectoriesOnly(dir);
        sort(subDirs);
        List<List<File>> groups = splitDirectories(subDirs);
        List<File> groupAssigned = groups.get(0); // 指定的目录组
        List<File> groupDefault = groups.get(1); // 其余目录组

        LOG.info(String.format(
                "Parallel execution: Group1 (specified directories) has %d directories, Group2 (others) has %d directories",
                groupAssigned.size(), groupDefault.size()));

        int accountId = executor.createAccountForTest();

        // 创建新的 executor2 用于指定目录组，在一个独立的租户中运行
        Executor executor2 = new Executor("shuyuan:kongzi", "111");

        executor2.setAccountId(accountId);

        // 使用现有 executor 处理第一组
        Thread thread1 = new Thread(() -> {
            groupAssigned.forEach(subDir -> {
                processFiles(subDir, script -> {
                    executor2.run(script);
                    report.collect(script);
                }, true);
            });
        });

        // 使用新 executor2 处理第二组
        Thread thread2 = new Thread(() -> {
            groupDefault.forEach(subDir -> {
                processFiles(subDir, script -> {
                    executor.run(script);
                    report.collect(script);
                }, true);
            });
        });

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            LOG.error("Parallel execution was interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 检查是否应该并行化执行
     * 
     * @param file 要检查的文件或目录
     * @return 如果是目录且直接子目录数 > 10，返回 true
     */
    private static boolean shouldParallelize(File file) {
        if (!parallelEnabled) {
            return false;
        }
        if (!file.isDirectory()) {
            return false;
        }
        File[] subDirs = getDirectoriesOnly(file);
        return subDirs != null && subDirs.length > 10;
    }

    /**
     * 获取目录的直接子目录（仅目录，不包括文件）
     * 
     * @param dir 目录
     * @return 直接子目录数组，如果 dir 不是目录或获取失败返回 null
     */
    private static File[] getDirectoriesOnly(File dir) {
        if (!dir.isDirectory()) {
            return null;
        }
        return dir.listFiles(File::isDirectory);
    }

    /**
     * 将目录数组分成两组：目录名为1_parallel的编入组1（使用executor2），其余编入组2
     * 
     * @param dirs 目录数组
     * @return 包含两个列表的列表，第一个是1_parallel目录组，第二个是其余目录组
     */
    private static List<List<File>> splitDirectories(File[] dirs) {
        List<File> group1 = new ArrayList<>(); // 1_parallel目录组
        List<File> group2 = new ArrayList<>(); // 其余目录组

        for (File dir : dirs) {
            if (dir.getName().matches("\\d+_parallel")) {
                group1.add(dir);
            } else {
                group2.add(dir);
            }
        }

        return Arrays.asList(group1, group2);
    }

    /**
     * 生成结果文件
     */
    public static void generateRs(File file) {
        processFiles(file, script -> {
            if (executor.genRS(script)) {
                LOG.info("The results for the test script file[" + script.getFileName()
                        + "] have been generated or updated successfully.");
            } else {
                LOG.info("The results for the test script file[" + script.getFileName()
                        + "] have been generated or updated failed.");
            }
        }, true);
    }

    /**
     * 通用的文件处理逻辑
     * 
     * @param file            要处理的文件或目录
     * @param scriptProcessor 脚本处理器
     * @param needSort        是否需要排序
     */
    private static void processFiles(File file, Consumer<TestScript> scriptProcessor, boolean needSort) {
        if (file.isFile()) {
            if (isValidTestFile(file) && isInclude(file.getPath())) {
                TestScript script = parseScript(file.getPath());
                if (script != null) {
                    scriptProcessor.accept(script);
                }
            }
            return;
        }

        File[] files = file.listFiles();
        if (files == null) {
            return;
        }

        if (needSort) {
            sort(files);
        }

        Arrays.stream(files).forEach(f -> processFiles(f, scriptProcessor, needSort));
    }

    /**
     * 检查文件是否为有效的测试文件
     */
    private static boolean isValidTestFile(File file) {
        String name = file.getName();
        return name.endsWith(SQL_SUFFIX) || name.endsWith(TEST_SUFFIX);
    }

    /**
     * 解析脚本文件
     */
    private static TestScript parseScript(String filePath) {
        ScriptParser parser = new ScriptParser();
        TestScript script = parser.parseScript(filePath);
        if (script == null) {
            LOG.error("Failed to parse script: " + filePath);
        }
        return script;
    }

    /**
     * 对文件数组进行排序：目录在前，文件在后，同类型按名称排序
     */
    public static void sort(File[] files) {
        Arrays.sort(files, (f1, f2) -> {
            boolean f1IsDir = f1.isDirectory();
            boolean f2IsDir = f2.isDirectory();
            if (f1IsDir != f2IsDir) {
                return f1IsDir ? -1 : 1;
            }
            return f1.getName().compareTo(f2.getName());
        });
    }

    /**
     * 判断文件是否应该被包含在测试中
     * 如果指定了 includes，则必须匹配 includes 中的任一模式
     * 如果指定了 excludes，则不能匹配 excludes 中的任一模式
     */
    public static boolean isInclude(String name) {
        if (includes != null) {
            return Arrays.stream(includes).anyMatch(name::contains);
        }
        if (excludes != null) {
            return Arrays.stream(excludes).noneMatch(name::contains);
        }
        return true;
    }

    /**
     * 删除输出文件
     */
    public static void removeOutfiles() {
        Arrays.stream(RunConfUtil.getOutFiles())
                .map(outFile -> Paths.get(COMMON.RESOURCE_PATH, outFile).toFile())
                .filter(File::exists)
                .forEach(file -> {
                    if (file.isDirectory() && new File(file, ".gitignore").exists()) {
                        deleteDirectoryContentsExceptGitignore(file);
                    } else if (!FileUtils.deleteQuietly(file)) {
                        LOG.error(String.format("Failed to remove [%s]", file));
                        System.exit(1);
                    }
                });
    }
    
    private static void deleteDirectoryContentsExceptGitignore(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.getName().equals(".gitignore")) continue;
            FileUtils.deleteQuietly(file);
        }
    }
}
