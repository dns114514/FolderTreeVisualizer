package com.foldertree;

import com.foldertree.ui.MainFrame;
import javax.swing.*;
import java.awt.*;

/**
 * 程序主入口
 * 启动文件夹树状图生成器应用程序
 */
public class Main {
    public static void main(String[] args) {
        // 使用SwingUtilities.invokeLater确保GUI在事件调度线程中创建
        SwingUtilities.invokeLater(() -> {
            try {
                // 设置系统外观，使应用看起来更原生
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                // 设置进度条UI改进
                UIManager.put("ProgressBar.foreground", new Color(0, 150, 0));
                UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
                UIManager.put("ProgressBar.selectionBackground", Color.BLACK);
            } catch (Exception e) {
                System.err.println("无法设置系统外观，将使用默认外观");
            }

            // 创建并显示主窗口
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);

            // 如果通过命令行传递了路径参数，直接打开该路径
            if (args.length > 0) {
                String path = args[0];
                mainFrame.scanFolder(path);
            }
        });
    }
}