package com.foldertree.ui;

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
        this.fileMenu = new FileOperationMenu(this, statusLabel);

        setupTextPane();
        setupMouseListener();
        setupMouseMotionListener();
        setupKeyBindings();
        setupHoverTimer();
    }

    private void setupTextPane() {
        setFont(new Font("Monospaced", Font.PLAIN, 13));
        setEditable(false);
        setBackground(new Color(245, 245, 245));

        // 添加右键菜单
        setComponentPopupMenu(fileMenu);
    }

    private void setupMouseListener() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 双击
                    handleDoubleClick(e);
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    // 右键点击时选择文本
                    int pos = viewToModel2D(e.getPoint());
                    if (pos >= 0) {
                        setCaretPosition(pos);
                        selectLineAtPosition(pos);
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
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseHover(e);
            }
        });
    }

    private void setupHoverTimer() {
        hoverTimer = new Timer(300, e -> {
            if (lastHoveredLine >= 0 && lastHoveredPath != null) {
                selectLineAtPosition(lastHoveredLine);
                updateStatus("已选中: " + new File(lastHoveredPath).getName());
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
        // 快捷键
        InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        // 回车键 - 打开选中的文件/文件夹
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "openSelected");
        actionMap.put("openSelected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSelectedItem();
            }
        });

        // Delete键 - 删除选中的文件/文件夹
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelected");
        actionMap.put("deleteSelected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedItem();
            }
        });

        // Ctrl+C - 复制
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), "copy");
        actionMap.put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyToClipboard();
            }
        });

        // F2 - 重命名
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "rename");
        actionMap.put("rename", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileMenu.renameFile();
            }
        });
    }

    private void handleDoubleClick(MouseEvent e) {
        int pos = viewToModel2D(e.getPoint());
        if (pos >= 0) {
            String selectedPath = getPathAtPosition(pos);
            if (selectedPath != null && !selectedPath.isEmpty()) {
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
                return extractPathFromLine(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            return findFullPath(line, basePath);
        }

        return null;
    }

    private String findFullPath(String itemName, String baseDir) {
        File base = new File(baseDir);
        if (!base.exists() || !base.isDirectory()) {
            return null;
        }

        // 递归查找文件
        return findFileRecursive(base, itemName);
    }

    private String findFileRecursive(File dir, String targetName) {
        File[] files = dir.listFiles();
        if (files == null) return null;

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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void openSelectedItem() {
        String selectedText = getSelectedText();
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            String path = extractPathFromLine(selectedText.trim());
            if (path != null) {
                openItem(path);
            }
        }
    }

    public void deleteSelectedItem() {
        String selectedText = getSelectedText();
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            String path = extractPathFromLine(selectedText.trim());
            if (path != null) {
                fileMenu.deleteFile(path);
            }
        }
    }

    // 改为public以便FileOperationMenu访问
    public void openItem(String path) {
        if (path == null) return;

        File file = new File(path);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this,
                    "文件不存在: " + path,
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (file.isDirectory()) {
                // 打开文件夹 - 使用系统资源管理器
                Desktop.getDesktop().open(file);
                updateStatus("已打开文件夹: " + file.getName());
            } else {
                // 打开文件
                Desktop.getDesktop().open(file);
                updateStatus("已打开文件: " + file.getName());
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "无法打开文件: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        } catch (UnsupportedOperationException e) {
            JOptionPane.showMessageDialog(this,
                    "不支持的操作系统",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setBasePath(String basePath) {
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
                fileMenu.copyToClipboard(path);
            }
        }
    }

    public void setFileMenuRefreshCallback(Runnable refreshCallback) {
        if (fileMenu != null) {
            fileMenu.setRefreshCallback(refreshCallback);
        }
    }

    // 添加公共方法来更新状态
    public void updateStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    public FileOperationMenu getFileMenu() {
        return fileMenu;
    }
}