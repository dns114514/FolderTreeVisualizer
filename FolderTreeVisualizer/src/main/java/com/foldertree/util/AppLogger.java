package com.foldertree.util;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 应用程序日志系统
 */
public class AppLogger {
    private static final String LOG_DIR = System.getProperty("user.home") + File.separator + ".FolderTreeTool" + File.separator + "logs";
    private static final String LOG_FILE = "app.log";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    private static FileWriter fileWriter;
    private static String logBuffer = "";

    static {
        try {
            // 确保日志目录存在
            Files.createDirectories(Paths.get(LOG_DIR));

            // 创建日志文件（追加模式）
            fileWriter = new FileWriter(LOG_DIR + File.separator + LOG_FILE, true);

            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                close();
            }));

            info("应用程序日志系统初始化完成");
            info("日志文件位置: " + LOG_DIR + File.separator + LOG_FILE);

        } catch (IOException e) {
            System.err.println("无法创建日志文件: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 记录信息
     */
    public static void info(String message) {
        log("INFO", message, null);
    }

    /**
     * 记录调试信息
     */
    public static void debug(String message) {
        log("DEBUG", message, null);
    }

    /**
     * 记录警告
     */
    public static void warn(String message) {
        log("WARN", message, null);
    }

    /**
     * 记录错误
     */
    public static void error(String message) {
        log("ERROR", message, null);
    }

    /**
     * 记录异常
     */
    public static void error(String message, Throwable throwable) {
        log("ERROR", message, throwable);
    }

    /**
     * 记录日志
     */
    private static void log(String level, String message, Throwable throwable) {
        String timestamp = DATE_FORMAT.format(new Date());
        String logEntry = String.format("[%s] [%s] %s\n", timestamp, level, message);

        // 添加到缓冲区
        logBuffer += logEntry;

        // 输出到控制台
        if (level.equals("ERROR") || level.equals("WARN")) {
            System.err.print(logEntry);
        } else {
            System.out.print(logEntry);
        }

        // 写入文件
        try {
            if (fileWriter != null) {
                fileWriter.write(logEntry);
                if (throwable != null) {
                    writeStackTrace(throwable);
                }
                fileWriter.flush();
            }
        } catch (IOException e) {
            System.err.println("无法写入日志: " + e.getMessage());
        }

        // 如果错误严重，显示错误对话框
        if (level.equals("ERROR") && shouldShowErrorDialog()) {
            showErrorDialog(message, throwable);
        }
    }

    /**
     * 写入异常堆栈跟踪
     */
    private static void writeStackTrace(Throwable throwable) throws IOException {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        fileWriter.write("异常堆栈跟踪:\n");
        fileWriter.write(sw.toString());
        fileWriter.write("\n");
    }

    /**
     * 是否应该显示错误对话框
     */
    private static boolean shouldShowErrorDialog() {
        // 在主线程中才显示对话框
        return !SwingUtilities.isEventDispatchThread();
    }

    /**
     * 显示错误对话框
     */
    private static void showErrorDialog(String message, Throwable throwable) {
        if (SwingUtilities.isEventDispatchThread()) {
            showDialogInEDT(message, throwable);
        } else {
            SwingUtilities.invokeLater(() -> showDialogInEDT(message, throwable));
        }
    }

    private static void showDialogInEDT(String message, Throwable throwable) {
        try {
            // 获取完整的异常信息
            final String fullError = buildFullErrorMessage(message, throwable);

            // 创建对话框面板
            JPanel panel = new JPanel(new BorderLayout(10, 10));

            // 错误信息区域
            JTextArea errorArea = new JTextArea(fullError, 15, 50);
            errorArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
            errorArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(errorArea);

            panel.add(new JLabel("程序发生错误:"), BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);

            // 按钮面板
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JButton copyButton = new JButton("复制错误信息");
            JButton viewLogButton = new JButton("查看日志文件");
            JButton saveLogButton = new JButton("保存错误日志");
            JButton ignoreButton = new JButton("忽略");

            copyButton.addActionListener(e -> {
                StringSelection selection = new StringSelection(fullError);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, null);
                JOptionPane.showMessageDialog(null, "错误信息已复制到剪贴板", "成功", JOptionPane.INFORMATION_MESSAGE);
            });

            viewLogButton.addActionListener(e -> {
                try {
                    Desktop.getDesktop().open(new File(LOG_DIR));
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(null, "无法打开日志目录: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            });

            saveLogButton.addActionListener(e -> {
                String savedPath = saveLogToFile();
                if (savedPath != null) {
                    JOptionPane.showMessageDialog(null, "错误日志已保存到: " + savedPath, "成功", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "保存错误日志失败", "错误", JOptionPane.ERROR_MESSAGE);
                }
            });

            // 创建选项对话框
            Object[] options = {copyButton, saveLogButton, viewLogButton, ignoreButton};

            JOptionPane.showOptionDialog(null, panel, "应用程序错误",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,
                    null, options, options[0]);

        } catch (Exception e) {
            System.err.println("显示错误对话框时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 构建完整的错误信息字符串
     */
    private static String buildFullErrorMessage(String message, Throwable throwable) {
        String errorMessage = message;
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            errorMessage += "\n\n堆栈跟踪:\n" + sw.toString();
        }
        return errorMessage;
    }

    /**
     * 保存日志到指定文件
     */
    public static String saveLogToFile() {
        try {
            String fileName = "error_" + FILE_DATE_FORMAT.format(new Date()) + ".log";
            String filePath = LOG_DIR + File.separator + fileName;

            Files.write(Paths.get(filePath), logBuffer.getBytes());
            return filePath;
        } catch (IOException e) {
            error("保存日志文件失败", e);
            return null;
        }
    }

    /**
     * 获取日志目录
     */
    public static String getLogDir() {
        return LOG_DIR;
    }

    /**
     * 获取最近的错误日志
     */
    public static String getRecentLogs() {
        return logBuffer;
    }

    /**
     * 清空日志缓冲区
     */
    public static void clearBuffer() {
        logBuffer = "";
    }

    /**
     * 关闭日志文件
     */
    public static void close() {
        try {
            if (fileWriter != null) {
                info("应用程序关闭");
                fileWriter.close();
            }
        } catch (IOException e) {
            System.err.println("关闭日志文件时发生错误: " + e.getMessage());
        }
    }
}