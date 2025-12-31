package com.foldertree;

import com.foldertree.ui.MainFrame;
import com.foldertree.util.AppLogger;
import com.foldertree.util.GlobalExceptionHandler;
import javax.swing.*;
import java.awt.*;

/**
 * 程序主入口 - 启动双模式文件夹工具
 */
public class Main {
    public static void main(String[] args) {
        // 设置全局未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler(new GlobalExceptionHandler());

        AppLogger.info("================================================================================");
        AppLogger.info("应用程序启动");
        AppLogger.info("Java版本: " + System.getProperty("java.version"));
        AppLogger.info("操作系统: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        AppLogger.info("用户目录: " + System.getProperty("user.home"));
        AppLogger.info("工作目录: " + System.getProperty("user.dir"));
        AppLogger.info("命令行参数: " + String.join(" ", args));

        // 使用SwingUtilities.invokeLater确保GUI在事件调度线程中创建
        SwingUtilities.invokeLater(() -> {
            try {
                AppLogger.info("开始初始化GUI");

                // 设置系统外观，使应用看起来更原生
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                AppLogger.info("系统外观设置完成");

                // 设置进度条UI改进
                UIManager.put("ProgressBar.foreground", new Color(0, 150, 0));
                UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
                UIManager.put("ProgressBar.selectionBackground", Color.BLACK);

                // 设置全局字体，确保中文显示正常
                Font chineseFont = new Font("Microsoft YaHei", Font.PLAIN, 12);
                UIManager.put("Label.font", chineseFont);
                UIManager.put("Button.font", chineseFont);
                UIManager.put("TextField.font", chineseFont);
                UIManager.put("TextArea.font", chineseFont);
                UIManager.put("TabbedPane.font", chineseFont);

                AppLogger.info("UI组件字体设置完成");

                // 设置按钮UI
                UIManager.put("Button.background", new Color(240, 240, 240));
                UIManager.put("Button.foreground", Color.BLACK);
                UIManager.put("Button.select", new Color(200, 200, 200));

            } catch (Exception e) {
                AppLogger.error("设置系统外观时发生错误", e);
                JOptionPane.showMessageDialog(null,
                        "无法设置系统外观，将使用默认外观: " + e.getMessage(),
                        "警告",
                        JOptionPane.WARNING_MESSAGE);
            }

            try {
                AppLogger.info("开始创建主窗口");

                // 创建并显示主窗口
                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);

                AppLogger.info("主窗口创建完成并显示");

                // 如果通过命令行传递了路径参数，直接打开该路径
                if (args.length > 0) {
                    String path = args[0];
                    AppLogger.info("从命令行参数打开路径: " + path);
                    mainFrame.scanFolder(path);
                }

            } catch (Exception e) {
                AppLogger.error("创建主窗口时发生错误", e);
                JOptionPane.showMessageDialog(null,
                        "启动应用程序时发生错误: " + e.getMessage(),
                        "启动错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}