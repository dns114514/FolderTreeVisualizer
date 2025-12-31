package com.foldertree.ui;

import com.foldertree.util.AppLogger;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

/**
 * 支持文件操作的特殊文本面板
 * 可以点击文件夹/文件，并支持右键菜单
 */
public class FileTreeTextPane extends JTextPane {
    private String basePath;
    private FileOperationMenu fileMenu;
    private JLabel statusLabel;

    // 鼠标悬浮相关
    private Timer hoverTimer;
    private String lastHoveredPath;
    private int lastHoveredLine = -1;

    public FileTreeTextPane(String basePath, JLabel statusLabel) {
        super();
        this.basePath = basePath;
        this.statusLabel = statusLabel;

        AppLogger.info("文件树文本面板初始化，基础路径: " + basePath);

        this.fileMenu = new FileOperationMenu(this, statusLabel);

        setupTextPane();
        setupMouseListener();
        setupMouseMotionListener();
        setupKeyBindings();
        setupHoverTimer();

        AppLogger.info("文件树文本面板初始化完成");
    }

    private void setupTextPane() {
        AppLogger.debug("设置文本面板属性");

        setFont(new Font("Monospaced", Font.PLAIN, 13));
        setEditable(false);
        setBackground(new Color(245, 245, 245));

        // 添加右键菜单
        setComponentPopupMenu(fileMenu);

        AppLogger.debug("文本面板属性设置完成");
    }

    private void setupMouseListener() {
        AppLogger.debug("设置鼠标监听器");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 双击
                    AppLogger.debug("鼠标双击事件");
                    handleDoubleClick(e);
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    AppLogger.debug("鼠标右键点击事件");
                    // 右键点击时选择文本
                    int pos = viewToModel2D(e.getPoint());
                    if (pos >= 0) {
                        setCaretPosition(pos);
                        selectLineAtPosition(pos);
                        AppLogger.debug("右键点击选择文本，位置: " + pos);
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 鼠标离开时清除悬浮效果
                clearHoverSelection();
            }
        });
    }

    private void setupMouseMotionListener() {
        AppLogger.debug("设置鼠标移动监听器");

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseHover(e);
            }
        });
    }

    private void setupHoverTimer() {
        AppLogger.debug("设置鼠标悬停计时器");

        hoverTimer = new Timer(300, e -> {
            if (lastHoveredLine >= 0 && lastHoveredPath != null) {
                selectLineAtPosition(lastHoveredLine);
                updateStatus("已选中: " + new File(lastHoveredPath).getName());
                AppLogger.debug("悬停选中: " + lastHoveredPath);
            }
        });
        hoverTimer.setRepeats(false);
    }

    private void handleMouseHover(MouseEvent e) {
        int pos = viewToModel2D(e.getPoint());
        if (pos >= 0) {
            String hoveredPath = getPathAtPosition(pos);
            if (hoveredPath != null && !hoveredPath.equals(lastHoveredPath)) {
                lastHoveredPath = hoveredPath;
                lastHoveredLine = pos;
                hoverTimer.restart();
                AppLogger.debug("鼠标悬停在: " + hoveredPath);
            }
        } else {
            clearHoverSelection();
        }
    }

    private void clearHoverSelection() {
        lastHoveredPath = null;
        lastHoveredLine = -1;
        hoverTimer.stop();
    }

    private void setupKeyBindings() {
        AppLogger.debug("设置快捷键绑定");

        // 快捷键
        InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        // 回车键 - 打开选中的文件/文件夹
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openSelected");
        actionMap.put("openSelected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppLogger.debug("快捷键 Enter: 打开选中的文件/文件夹");
                openSelectedItem();
            }
        });

        // Delete键 - 删除选中的文件/文件夹
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelected");
        actionMap.put("deleteSelected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppLogger.debug("快捷键 Delete: 删除选中的文件/文件夹");
                deleteSelectedItem();
            }
        });

        // Ctrl+C - 复制
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), "copy");
        actionMap.put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppLogger.debug("快捷键 Ctrl+C: 复制");
                copyToClipboard();
            }
        });

        // F2 - 重命名
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "rename");
        actionMap.put("rename", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppLogger.debug("快捷键 F2: 重命名");
                fileMenu.renameFile();
            }
        });

        AppLogger.debug("快捷键绑定设置完成");
    }

    private void handleDoubleClick(MouseEvent e) {
        int pos = viewToModel2D(e.getPoint());
        if (pos >= 0) {
            String selectedPath = getPathAtPosition(pos);
            if (selectedPath != null && !selectedPath.isEmpty()) {
                AppLogger.info("双击打开: " + selectedPath);
                openItem(selectedPath);
            }
        }
    }

    private String getPathAtPosition(int position) {
        try {
            // 获取点击位置的行 - 使用自定义方法替代Utilities
            int lineStart = getRowStart(position);
            int lineEnd = getRowEnd(position);

            if (lineStart >= 0 && lineEnd > lineStart) {
                String line = getText(lineStart, lineEnd - lineStart).trim();
                String path = extractPathFromLine(line);
                AppLogger.debug("位置 " + position + " 的路径: " + path);
                return path;
            }
        } catch (Exception e) {
            AppLogger.error("获取位置路径时发生错误", e);
        }
        return null;
    }

    // 替换Utilities.getRowStart的自定义实现
    private int getRowStart(int pos) {
        try {
            Element root = getDocument().getDefaultRootElement();
            int lineIndex = root.getElementIndex(pos);
            Element line = root.getElement(lineIndex);
            return line.getStartOffset();
        } catch (Exception e) {
            return -1;
        }
    }

    // 替换Utilities.getRowEnd的自定义实现
    private int getRowEnd(int pos) {
        try {
            Element root = getDocument().getDefaultRootElement();
            int lineIndex = root.getElementIndex(pos);
            Element line = root.getElement(lineIndex);
            return line.getEndOffset();
        } catch (Exception e) {
            return -1;
        }
    }

    // 改为public以便FileOperationMenu访问
    public String extractPathFromLine(String line) {
        AppLogger.debug("从行提取路径: " + line);

        // 移除树状图的前缀符号
        line = line.replaceAll("^[├└│\\s─]*", "").trim();

        // 检查是否是文件夹（以/结尾）
        boolean isFolder = line.endsWith("/");
        if (isFolder) {
            line = line.substring(0, line.length() - 1);
        }

        // 构建完整路径
        if (!line.isEmpty()) {
            // 递归构建路径
            String path = findFullPath(line, basePath);
            AppLogger.debug("提取的路径: " + path);
            return path;
        }

        return null;
    }

    private String findFullPath(String itemName, String baseDir) {
        File base = new File(baseDir);
        if (!base.exists() || !base.isDirectory()) {
            AppLogger.warn("基础目录不存在或不是目录: " + baseDir);
            return null;
        }

        // 递归查找文件
        String path = findFileRecursive(base, itemName);
        AppLogger.debug("查找文件 " + itemName + " 在 " + baseDir + " 中的完整路径: " + path);
        return path;
    }

    private String findFileRecursive(File dir, String targetName) {
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (file.getName().equals(targetName)) {
                return file.getAbsolutePath();
            }

            if (file.isDirectory()) {
                String found = findFileRecursive(file, targetName);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private void selectLineAtPosition(int position) {
        try {
            int lineStart = getRowStart(position);
            int lineEnd = getRowEnd(position);

            if (lineStart >= 0 && lineEnd > lineStart) {
                setSelectionStart(lineStart);
                setSelectionEnd(lineEnd - 1); // 减1以避免选中换行符
                AppLogger.debug("选择行: " + lineStart + " 到 " + (lineEnd - 1));
            }
        } catch (Exception e) {
            AppLogger.error("选择行时发生错误", e);
        }
    }

    public void openSelectedItem() {
        String selectedText = getSelectedText();
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            String path = extractPathFromLine(selectedText.trim());
            if (path != null) {
                AppLogger.info("打开选中的项目: " + path);
                openItem(path);
            }
        } else {
            AppLogger.warn("没有选中的文本");
        }
    }

    public void deleteSelectedItem() {
        String selectedText = getSelectedText();
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            String path = extractPathFromLine(selectedText.trim());
            if (path != null) {
                AppLogger.info("删除选中的项目: " + path);
                fileMenu.deleteFile(path);
            }
        } else {
            AppLogger.warn("没有选中的文本");
        }
    }

    // 改为public以便FileOperationMenu访问
    public void openItem(String path) {
        if (path == null) {
            AppLogger.warn("打开项目失败，路径为空");
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            AppLogger.error("文件不存在: " + path);
            JOptionPane.showMessageDialog(this,
                    "文件不存在: " + path,
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (file.isDirectory()) {
                // 打开文件夹 - 使用系统资源管理器
                AppLogger.info("打开文件夹: " + path);
                Desktop.getDesktop().open(file);
                updateStatus("已打开文件夹: " + file.getName());
            } else {
                // 打开文件
                AppLogger.info("打开文件: " + path);
                Desktop.getDesktop().open(file);
                updateStatus("已打开文件: " + file.getName());
            }
        } catch (IOException e) {
            AppLogger.error("无法打开文件: " + path, e);
            JOptionPane.showMessageDialog(this,
                    "无法打开文件: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        } catch (UnsupportedOperationException e) {
            AppLogger.error("不支持的操作系统", e);
            JOptionPane.showMessageDialog(this,
                    "不支持的操作系统",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setBasePath(String basePath) {
        AppLogger.debug("设置基础路径: " + basePath);
        this.basePath = basePath;
        if (fileMenu != null) {
            fileMenu.setBasePath(basePath);
        }
    }

    public String getBasePath() {
        return basePath;
    }

    private void copyToClipboard() {
        String selectedText = getSelectedText();
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            String path = extractPathFromLine(selectedText.trim());
            if (path != null) {
                AppLogger.info("复制到剪贴板: " + path);
                fileMenu.copyToClipboard(path);
            }
        } else {
            AppLogger.warn("没有选中的文本");
        }
    }

    public void setFileMenuRefreshCallback(Runnable refreshCallback) {
        AppLogger.debug("设置文件菜单刷新回调");
        if (fileMenu != null) {
            fileMenu.setRefreshCallback(refreshCallback);
        }
    }

    // 添加公共方法来更新状态
    public void updateStatus(String message) {
        AppLogger.debug("更新状态: " + message);
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    public FileOperationMenu getFileMenu() {
        return fileMenu;
    }
}