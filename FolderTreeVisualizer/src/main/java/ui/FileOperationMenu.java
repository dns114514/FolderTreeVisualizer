package com.foldertree.ui;

import com.foldertree.util.AppLogger;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

/**
 * 文件操作右键菜单
 */
public class FileOperationMenu extends JPopupMenu {
    private FileTreeTextPane textPane;
    private String basePath;
    private String lastCopiedPath;
    private JLabel statusLabel;
    private Runnable refreshCallback;

    public FileOperationMenu(FileTreeTextPane textPane, JLabel statusLabel) {
        this.textPane = textPane;
        this.basePath = textPane.getBasePath();
        this.statusLabel = statusLabel;

        AppLogger.info("文件操作菜单初始化，基础路径: " + basePath);

        initMenu();
    }

    private void initMenu() {
        AppLogger.debug("初始化文件操作菜单");

        // 设置菜单字体
        Font menuFont = new Font("Microsoft YaHei", Font.PLAIN, 12);
        UIManager.put("MenuItem.font", menuFont);

        // 打开菜单项
        JMenuItem openItem = new JMenuItem("打开");
        openItem.setIcon(UIManager.getIcon("FileView.fileIcon"));
        openItem.addActionListener(e -> {
            AppLogger.debug("菜单项点击: 打开");
            openSelectedFile();
        });
        add(openItem);

        // 在资源管理器中打开
        JMenuItem openInExplorerItem = new JMenuItem("在资源管理器中打开");
        openInExplorerItem.setIcon(UIManager.getIcon("FileView.directoryIcon"));
        openInExplorerItem.addActionListener(e -> {
            AppLogger.debug("菜单项点击: 在资源管理器中打开");
            openInExplorer();
        });
        add(openInExplorerItem);

        addSeparator();

        // 复制路径
        JMenuItem copyPathItem = new JMenuItem("复制路径");
        copyPathItem.setIcon(createIconFromChar('C'));
        copyPathItem.addActionListener(e -> {
            AppLogger.debug("菜单项点击: 复制路径");
            copyPathToClipboard();
        });
        add(copyPathItem);

        // 复制文件
        JMenuItem copyFileItem = new JMenuItem("复制文件");
        copyFileItem.setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
        copyFileItem.addActionListener(e -> {
            AppLogger.debug("菜单项点击: 复制文件");
            copyFileToClipboard();
        });
        add(copyFileItem);

        // 粘贴文件
        JMenuItem pasteItem = new JMenuItem("粘贴文件");
        pasteItem.setIcon(createIconFromChar('P'));
        pasteItem.addActionListener(e -> {
            AppLogger.debug("菜单项点击: 粘贴文件");
            pasteFile();
        });
        add(pasteItem);

        addSeparator();

        // 用十六进制编辑
        JMenuItem hexEditItem = new JMenuItem("用十六进制编辑(测试功能)");
        hexEditItem.setIcon(createIconFromChar('H'));
        hexEditItem.addActionListener(e -> {
            AppLogger.debug("菜单项点击: 用十六进制编辑");
            openInHexEditor();
        });
        add(hexEditItem);

        addSeparator();

        // 重命名
        JMenuItem renameItem = new JMenuItem("重命名");
        renameItem.setIcon(createIconFromChar('R'));
        renameItem.addActionListener(e -> {
            AppLogger.debug("菜单项点击: 重命名");
            renameFile();
        });
        add(renameItem);

        // 删除
        JMenuItem deleteItem = new JMenuItem("删除");
        deleteItem.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
        deleteItem.addActionListener(e -> {
            AppLogger.debug("菜单项点击: 删除");
            deleteSelectedFile();
        });
        add(deleteItem);

        addSeparator();

        // 属性
        JMenuItem propertiesItem = new JMenuItem("属性");
        propertiesItem.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
        propertiesItem.addActionListener(e -> {
            AppLogger.debug("菜单项点击: 属性");
            showProperties();
        });
        add(propertiesItem);

        AppLogger.debug("文件操作菜单初始化完成");
    }

    private Icon createIconFromChar(char c) {
        return new Icon() {
            @Override
            public void paintIcon(Component comp, Graphics g, int x, int y) {
                g.setColor(Color.BLACK);
                g.setFont(new Font("Dialog", Font.BOLD, 12));
                FontMetrics fm = g.getFontMetrics();
                int charWidth = fm.charWidth(c);
                int charHeight = fm.getAscent();
                g.drawString(String.valueOf(c), x + (16 - charWidth) / 2, y + charHeight);
            }

            @Override
            public int getIconWidth() {
                return 16;
            }

            @Override
            public int getIconHeight() {
                return 16;
            }
        };
    }

    private String getSelectedPath() {
        String selectedText = textPane.getSelectedText();
        if (selectedText == null || selectedText.trim().isEmpty()) {
            AppLogger.debug("没有选中的文本");
            return null;
        }

        String path = textPane.extractPathFromLine(selectedText.trim());
        AppLogger.debug("提取路径: " + path);
        return path;
    }

    private void openSelectedFile() {
        String path = getSelectedPath();
        if (path != null) {
            AppLogger.info("打开文件/文件夹: " + path);
            textPane.openItem(path);
        } else {
            AppLogger.warn("无法打开，路径为空");
        }
    }

    private void openInExplorer() {
        String path = getSelectedPath();
        if (path == null) {
            AppLogger.warn("无法在资源管理器中打开，路径为空");
            return;
        }

        try {
            File file = new File(path);
            AppLogger.info("在资源管理器中打开: " + path);

            // 根据不同操作系统打开资源管理器
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                // Windows
                if (file.isDirectory()) {
                    Runtime.getRuntime().exec("explorer \"" + file.getAbsolutePath() + "\"");
                    updateStatus("已在资源管理器中打开文件夹: " + file.getName());
                } else {
                    Runtime.getRuntime().exec("explorer /select,\"" + file.getAbsolutePath() + "\"");
                    updateStatus("已在资源管理器中选中文件: " + file.getName());
                }
            } else if (os.contains("mac")) {
                // macOS
                Runtime.getRuntime().exec("open -R \"" + file.getAbsolutePath() + "\"");
                updateStatus("已在Finder中打开: " + file.getName());
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux
                Runtime.getRuntime().exec("xdg-open \"" + file.getParent() + "\"");
                updateStatus("已在文件管理器中打开: " + file.getName());
            }
        } catch (IOException e) {
            AppLogger.error("无法打开资源管理器: " + path, e);
            JOptionPane.showMessageDialog(textPane,
                    "无法打开资源管理器: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void copyToClipboard(String path) {
        if (path == null) {
            AppLogger.warn("无法复制到剪贴板，路径为空");
            return;
        }

        StringSelection stringSelection = new StringSelection(path);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
        lastCopiedPath = path;

        AppLogger.info("复制路径到剪贴板: " + path);
        updateStatus("已复制路径到剪贴板: " + new File(path).getName());
    }

    private void copyPathToClipboard() {
        String path = getSelectedPath();
        copyToClipboard(path);
    }

    private void copyFileToClipboard() {
        String path = getSelectedPath();
        if (path == null) {
            AppLogger.warn("无法复制文件，路径为空");
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            AppLogger.error("文件不存在: " + path);
            JOptionPane.showMessageDialog(textPane,
                    "文件不存在: " + path,
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 将文件路径放入剪贴板
        StringSelection stringSelection = new StringSelection(path);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
        lastCopiedPath = path;

        AppLogger.info("复制文件路径: " + path);
        updateStatus("已复制文件路径: " + file.getName());
    }

    private void pasteFile() {
        if (lastCopiedPath == null) {
            AppLogger.warn("剪贴板中没有文件");
            JOptionPane.showMessageDialog(textPane,
                    "剪贴板中没有文件",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        File sourceFile = new File(lastCopiedPath);
        if (!sourceFile.exists()) {
            AppLogger.error("源文件不存在: " + lastCopiedPath);
            JOptionPane.showMessageDialog(textPane,
                    "源文件不存在",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String currentPath = textPane.getBasePath();
        if (currentPath == null) {
            currentPath = System.getProperty("user.home");
            AppLogger.debug("当前路径为空，使用用户主目录: " + currentPath);
        }

        // 询问目标文件名
        String fileName = JOptionPane.showInputDialog(textPane,
                "请输入新文件名:",
                "粘贴文件",
                JOptionPane.QUESTION_MESSAGE);

        if (fileName == null || fileName.trim().isEmpty()) {
            AppLogger.debug("用户取消粘贴或文件名为空");
            return;
        }

        File targetFile = new File(currentPath, fileName.trim());
        AppLogger.info("粘贴文件，源: " + lastCopiedPath + ", 目标: " + targetFile.getAbsolutePath());

        // 如果文件已存在，询问是否覆盖
        if (targetFile.exists()) {
            AppLogger.warn("目标文件已存在: " + targetFile.getAbsolutePath());
            int result = JOptionPane.showConfirmDialog(textPane,
                    "文件已存在，是否覆盖?",
                    "确认",
                    JOptionPane.YES_NO_OPTION);

            if (result != JOptionPane.YES_OPTION) {
                AppLogger.debug("用户选择不覆盖文件");
                return;
            }
        }

        try {
            Files.copy(sourceFile.toPath(), targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            AppLogger.info("文件粘贴成功: " + targetFile.getName());
            updateStatus("已粘贴文件: " + targetFile.getName());

            // 调用刷新回调
            if (refreshCallback != null) {
                AppLogger.debug("调用刷新回调");
                refreshCallback.run();
            }
        } catch (IOException e) {
            AppLogger.error("粘贴文件失败: " + lastCopiedPath + " -> " + targetFile.getAbsolutePath(), e);
            JOptionPane.showMessageDialog(textPane,
                    "粘贴失败: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openInHexEditor() {
        String path = getSelectedPath();
        if (path == null) {
            AppLogger.warn("无法用十六进制编辑，路径为空");
            return;
        }

        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            AppLogger.error("只能编辑文件的十六进制内容: " + path);
            JOptionPane.showMessageDialog(textPane,
                    "只能编辑文件的十六进制内容",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            AppLogger.info("打开十六进制编辑器: " + path);

            // 使用十六进制编辑器对话框
            HexEditorDialog hexDialog = new HexEditorDialog((JFrame) SwingUtilities.getWindowAncestor(textPane), file);
            hexDialog.setVisible(true);
            updateStatus("已打开十六进制编辑器: " + file.getName());
        } catch (Exception e) {
            AppLogger.error("无法打开十六进制编辑器: " + path, e);
            JOptionPane.showMessageDialog(textPane,
                    "无法打开十六进制编辑器: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void renameFile() {
        String path = getSelectedPath();
        if (path == null) {
            AppLogger.warn("无法重命名，没有选中的文件或文件夹");
            JOptionPane.showMessageDialog(textPane,
                    "请先选择要重命名的文件或文件夹",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        renameFile(path);
    }

    private void renameFile(String path) {
        AppLogger.info("重命名文件: " + path);

        File file = new File(path);
        if (!file.exists()) {
            AppLogger.error("文件不存在: " + path);
            JOptionPane.showMessageDialog(textPane,
                    "文件不存在",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String newName = JOptionPane.showInputDialog(textPane,
                "请输入新名称:",
                "重命名 " + file.getName(),
                JOptionPane.QUESTION_MESSAGE);

        if (newName == null || newName.trim().isEmpty()) {
            AppLogger.debug("用户取消重命名或名称为空");
            return;
        }

        File newFile = new File(file.getParent(), newName.trim());
        AppLogger.debug("新文件路径: " + newFile.getAbsolutePath());

        // 检查新文件名是否与旧文件名相同
        if (newFile.getAbsolutePath().equals(file.getAbsolutePath())) {
            AppLogger.debug("文件名未改变: " + file.getName());
            updateStatus("文件名未改变: " + file.getName());
            return;
        }

        // 检查新文件是否已存在
        if (newFile.exists()) {
            AppLogger.warn("目标文件已存在: " + newFile.getAbsolutePath());
            int result = JOptionPane.showConfirmDialog(textPane,
                    "文件 \"" + newName + "\" 已存在，是否覆盖?",
                    "确认覆盖",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (result != JOptionPane.YES_OPTION) {
                AppLogger.debug("用户选择不覆盖文件");
                return;
            }
        }

        if (file.renameTo(newFile)) {
            AppLogger.info("重命名成功: " + file.getName() + " -> " + newFile.getName());
            updateStatus("已重命名为: " + newFile.getName());

            // 调用刷新回调
            if (refreshCallback != null) {
                AppLogger.debug("调用刷新回调");
                refreshCallback.run();
            }
        } else {
            AppLogger.error("重命名失败: " + file.getAbsolutePath() + " -> " + newFile.getAbsolutePath());
            JOptionPane.showMessageDialog(textPane,
                    "重命名失败，文件可能被占用或没有权限",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void deleteFile(String path) {
        if (path == null) {
            path = getSelectedPath();
            if (path == null) {
                AppLogger.warn("无法删除，没有选中的文件或文件夹");
                return;
            }
        }

        AppLogger.info("删除文件: " + path);

        File file = new File(path);
        if (!file.exists()) {
            AppLogger.error("文件不存在: " + path);
            JOptionPane.showMessageDialog(textPane,
                    "文件不存在",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        int result = JOptionPane.showConfirmDialog(textPane,
                "确定要删除 " + file.getName() + " 吗?\n此操作不可恢复!",
                "确认删除",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            AppLogger.debug("用户确认删除文件");
            if (deleteRecursive(file)) {
                AppLogger.info("删除成功: " + file.getName());
                updateStatus("已删除: " + file.getName());

                // 调用刷新回调
                if (refreshCallback != null) {
                    AppLogger.debug("调用刷新回调");
                    refreshCallback.run();
                }
            } else {
                AppLogger.error("删除失败: " + file.getAbsolutePath());
                JOptionPane.showMessageDialog(textPane,
                        "删除失败，文件可能被占用或没有权限",
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            AppLogger.debug("用户取消删除");
        }
    }

    private void deleteSelectedFile() {
        deleteFile(null);
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            AppLogger.debug("删除目录: " + file.getAbsolutePath());
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    if (!deleteRecursive(child)) {
                        return false;
                    }
                }
            }
        }

        boolean result = file.delete();
        AppLogger.debug("删除 " + (file.isDirectory() ? "目录" : "文件") +
                ": " + file.getAbsolutePath() + " 结果: " + result);
        return result;
    }

    private void showProperties() {
        String path = getSelectedPath();
        if (path == null) {
            AppLogger.warn("无法显示属性，路径为空");
            return;
        }

        AppLogger.info("显示文件属性: " + path);

        File file = new File(path);
        if (!file.exists()) {
            AppLogger.error("文件不存在: " + path);
            JOptionPane.showMessageDialog(textPane,
                    "文件不存在",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        StringBuilder properties = new StringBuilder();
        properties.append("名称: ").append(file.getName()).append("\n");
        properties.append("路径: ").append(file.getAbsolutePath()).append("\n");
        properties.append("类型: ").append(file.isDirectory() ? "文件夹" : "文件").append("\n");
        properties.append("大小: ").append(formatFileSize(file.length())).append("\n");
        properties.append("最后修改: ").append(new java.util.Date(file.lastModified())).append("\n");
        properties.append("可读: ").append(file.canRead()).append("\n");
        properties.append("可写: ").append(file.canWrite()).append("\n");
        properties.append("可执行: ").append(file.canExecute()).append("\n");
        properties.append("隐藏: ").append(file.isHidden()).append("\n");

        JTextArea textArea = new JTextArea(properties.toString());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 250));

        JOptionPane.showMessageDialog(textPane, scrollPane,
                "属性 - " + file.getName(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    private void updateStatus(String message) {
        AppLogger.debug("更新状态: " + message);
        if (statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    public void setBasePath(String basePath) {
        AppLogger.debug("设置基础路径: " + basePath);
        this.basePath = basePath;
    }

    public void setRefreshCallback(Runnable refreshCallback) {
        AppLogger.debug("设置刷新回调");
        this.refreshCallback = refreshCallback;
    }
}

/**
 * 十六进制编辑器对话框
 */
class HexEditorDialog extends JDialog {
    private File file;
    private JTextArea hexArea;
    private JTextArea asciiArea;
    private byte[] originalData;
    private byte[] currentData;

    public HexEditorDialog(JFrame parent, File file) {
        super(parent, "十六进制编辑器 - " + file.getName(), true);
        this.file = file;

        AppLogger.info("打开十六进制编辑器: " + file.getAbsolutePath());

        setSize(900, 600);
        setLocationRelativeTo(parent);

        try {
            originalData = Files.readAllBytes(file.toPath());
            currentData = originalData.clone();

            AppLogger.debug("文件大小: " + originalData.length + " 字节");

            initUI();
        } catch (IOException e) {
            AppLogger.error("无法读取文件: " + file.getAbsolutePath(), e);
            JOptionPane.showMessageDialog(this,
                    "无法读取文件: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void initUI() {
        AppLogger.debug("初始化十六进制编辑器UI");

        setLayout(new BorderLayout(5, 5));

        // 工具栏
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(e -> {
            AppLogger.debug("十六进制编辑器: 保存按钮点击");
            saveFile();
        });

        JButton revertButton = new JButton("还原");
        revertButton.addActionListener(e -> {
            AppLogger.debug("十六进制编辑器: 还原按钮点击");
            revertChanges();
        });

        JButton gotoButton = new JButton("转到偏移量");
        gotoButton.addActionListener(e -> {
            AppLogger.debug("十六进制编辑器: 转到偏移量按钮点击");
            gotoOffset();
        });

        toolbar.add(saveButton);
        toolbar.add(revertButton);
        toolbar.add(gotoButton);
        add(toolbar, BorderLayout.NORTH);

        // 主编辑区域
        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // 十六进制区域
        hexArea = new JTextArea();
        hexArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        hexArea.setText(bytesToHex(currentData));
        hexArea.addCaretListener(e -> syncCaretPosition());

        JScrollPane hexScroll = new JScrollPane(hexArea);
        hexScroll.setBorder(BorderFactory.createTitledBorder("十六进制"));

        // ASCII区域
        asciiArea = new JTextArea();
        asciiArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        asciiArea.setText(bytesToAscii(currentData));
        asciiArea.addCaretListener(e -> syncCaretPosition());

        JScrollPane asciiScroll = new JScrollPane(asciiArea);
        asciiScroll.setBorder(BorderFactory.createTitledBorder("ASCII"));

        mainPanel.add(hexScroll);
        mainPanel.add(asciiScroll);
        add(mainPanel, BorderLayout.CENTER);

        // 信息栏
        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel(String.format(
                "文件大小: %s (%d 字节) | 编辑状态: %s",
                formatFileSize(currentData.length),
                currentData.length,
                Arrays.equals(originalData, currentData) ? "未修改" : "已修改"
        ));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        infoPanel.add(infoLabel, BorderLayout.CENTER);

        add(infoPanel, BorderLayout.SOUTH);

        // 设置同步滚动
        hexScroll.getVerticalScrollBar().addAdjustmentListener(e ->
                asciiScroll.getVerticalScrollBar().setValue(e.getValue()));
        asciiScroll.getVerticalScrollBar().addAdjustmentListener(e ->
                hexScroll.getVerticalScrollBar().setValue(e.getValue()));

        AppLogger.debug("十六进制编辑器UI初始化完成");
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0 && i % 16 == 0) {
                hexString.append("\n");
            }
            String hex = Integer.toHexString(0xff & bytes[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex).append(" ");

            // 每8个字节后加一个空格
            if (i % 8 == 7) {
                hexString.append(" ");
            }
        }
        return hexString.toString();
    }

    private String bytesToAscii(byte[] bytes) {
        StringBuilder asciiString = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0 && i % 16 == 0) {
                asciiString.append("\n");
            }
            int b = bytes[i] & 0xFF;
            if (b >= 32 && b <= 126) {
                asciiString.append((char) b);
            } else {
                asciiString.append(".");
            }
            asciiString.append(" ");

            // 每8个字节后加一个空格
            if (i % 8 == 7) {
                asciiString.append(" ");
            }
        }
        return asciiString.toString();
    }

    private void syncCaretPosition() {
        // 同步两个文本区域的光标位置
        // 这里可以实现光标同步逻辑
    }

    private void saveFile() {
        try {
            AppLogger.info("保存十六进制编辑器修改: " + file.getAbsolutePath());

            // 将十六进制文本转换回字节
            byte[] newData = parseHexText(hexArea.getText());
            Files.write(file.toPath(), newData);
            originalData = newData;
            currentData = newData.clone();

            AppLogger.info("文件保存成功: " + file.getAbsolutePath());

            JOptionPane.showMessageDialog(this,
                    "文件保存成功",
                    "成功",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            AppLogger.error("保存文件失败: " + file.getAbsolutePath(), e);
            JOptionPane.showMessageDialog(this,
                    "保存失败: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void revertChanges() {
        AppLogger.debug("还原十六进制编辑器修改");
        hexArea.setText(bytesToHex(originalData));
        asciiArea.setText(bytesToAscii(originalData));
        currentData = originalData.clone();
    }

    private void gotoOffset() {
        String offsetStr = JOptionPane.showInputDialog(this,
                "请输入十六进制偏移量:",
                "转到偏移量",
                JOptionPane.QUESTION_MESSAGE);

        if (offsetStr != null && !offsetStr.trim().isEmpty()) {
            try {
                int offset = Integer.parseInt(offsetStr.trim(), 16);
                AppLogger.debug("转到偏移量: 0x" + Integer.toHexString(offset));

                if (offset >= 0 && offset < currentData.length) {
                    // 计算在文本中的位置
                    int row = offset / 16;
                    int col = (offset % 16) * 3;
                    if (offset % 16 >= 8) col += 2; // 考虑中间的空格

                    int pos = row * 53 + col; // 每行53个字符（包括换行符）
                    hexArea.setCaretPosition(Math.min(pos, hexArea.getText().length()));
                    hexArea.requestFocus();

                    AppLogger.debug("设置光标位置: " + pos);
                } else {
                    AppLogger.warn("偏移量超出范围: 0x" + Integer.toHexString(offset) +
                            ", 文件大小: " + currentData.length);
                    JOptionPane.showMessageDialog(this,
                            "偏移量超出范围",
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException e) {
                AppLogger.error("无效的十六进制数: " + offsetStr, e);
                JOptionPane.showMessageDialog(this,
                        "无效的十六进制数",
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private byte[] parseHexText(String hexText) {
        // 移除空格和换行
        hexText = hexText.replaceAll("\\s+", "");

        // 验证长度
        if (hexText.length() % 2 != 0) {
            throw new IllegalArgumentException("十六进制文本长度必须是偶数");
        }

        byte[] data = new byte[hexText.length() / 2];
        for (int i = 0; i < data.length; i++) {
            String hexByte = hexText.substring(i * 2, i * 2 + 2);
            data[i] = (byte) Integer.parseInt(hexByte, 16);
        }
        return data;
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }
}