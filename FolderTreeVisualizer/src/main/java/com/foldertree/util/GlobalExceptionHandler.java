package com.foldertree.util;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.lang.Thread.UncaughtExceptionHandler;

/**
 * 全局未捕获异常处理器
 */
public class GlobalExceptionHandler implements UncaughtExceptionHandler {

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        // 记录异常
        AppLogger.error("未捕获的异常在线程 " + t.getName() + " 中", e);

        // 在EDT线程中显示错误对话框
        if (SwingUtilities.isEventDispatchThread()) {
            showErrorDialog("程序发生未捕获异常", e);
        } else {
            SwingUtilities.invokeLater(() -> showErrorDialog("程序发生未捕获异常", e));
        }
    }

    private void showErrorDialog(String title, Throwable e) {
        try {
            // 构建错误信息
            StringBuilder errorInfo = new StringBuilder();
            errorInfo.append("线程: ").append(Thread.currentThread().getName()).append("\n");
            errorInfo.append("时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())).append("\n");
            errorInfo.append("异常类型: ").append(e.getClass().getName()).append("\n");
            errorInfo.append("异常信息: ").append(e.getMessage()).append("\n\n");
            errorInfo.append("堆栈跟踪:\n");

            for (StackTraceElement element : e.getStackTrace()) {
                errorInfo.append("    at ").append(element).append("\n");
            }

            // 如果有原因异常
            Throwable cause = e.getCause();
            if (cause != null) {
                errorInfo.append("\n原因异常:\n");
                errorInfo.append(cause.toString()).append("\n");
                for (StackTraceElement element : cause.getStackTrace()) {
                    errorInfo.append("    at ").append(element).append("\n");
                }
            }

            // 创建对话框
            JTextArea textArea = new JTextArea(errorInfo.toString(), 20, 60);
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
            JScrollPane scrollPane = new JScrollPane(textArea);

            // 选项
            Object[] options = {"复制错误信息", "查看日志目录", "保存错误日志", "关闭"};

            int result = JOptionPane.showOptionDialog(null,
                    scrollPane,
                    title,
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.ERROR_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (result == 0) {
                // 复制错误信息
                StringSelection selection = new StringSelection(errorInfo.toString());
                java.awt.datatransfer.Clipboard clipboard =
                        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, null);
                JOptionPane.showMessageDialog(null, "错误信息已复制到剪贴板");
            } else if (result == 1) {
                // 查看日志目录
                try {
                    java.awt.Desktop.getDesktop().open(new java.io.File(AppLogger.getLogDir()));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null,
                            "无法打开日志目录: " + ex.getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else if (result == 2) {
                // 保存错误日志
                String savedPath = AppLogger.saveLogToFile();
                if (savedPath != null) {
                    JOptionPane.showMessageDialog(null, "错误日志已保存到: " + savedPath);
                } else {
                    JOptionPane.showMessageDialog(null, "保存错误日志失败");
                }
            }

        } catch (Exception ex) {
            // 如果错误对话框也出错了，至少记录一下
            ex.printStackTrace();
        }
    }
}