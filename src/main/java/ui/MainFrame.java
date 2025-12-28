package com.foldertree.ui;

import com.foldertree.core.FolderScanner;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 主窗口类 - 带进度监控和文件操作功能
 */
public class MainFrame extends JFrame {
    private FolderScanner folderScanner;
    private JTextField pathField;
    private JSpinner depthSpinner;
    private JCheckBox showFilesCheckBox;
    private FileTreeTextPane treeTextPane;
    private JButton copyButton;
    private JButton saveButton;
    private JLabel statusLabel;
    private JLabel statsLabel;

    // 进度监控组件
    private JProgressBar progressBar;
    private JLabel progressLabel;
    private JLabel speedLabel;
    private JLabel timeLabel;
    private JLabel currentPathLabel;

    // 进度监控变量
    private long scanStartTime;

    public MainFrame() {
        folderScanner = new FolderScanner();
        initComponents();
        setupWindowProperties();
    }

    private void initComponents() {
        // 设置窗口标题
        setTitle("文件夹树状图生成器 - Java 17 (支持文件操作)");

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建控制面板
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.NORTH);

        // 创建进度监控面板
        JPanel progressPanel = createProgressPanel();
        mainPanel.add(progressPanel, BorderLayout.SOUTH);

        // 创建树状图显示区域
        JPanel treePanel = createTreePanel();
        mainPanel.add(treePanel, BorderLayout.CENTER);

        // 创建按钮面板
        JPanel buttonPanel = createButtonPanel();

        // 创建状态栏
        JPanel statusPanel = createStatusPanel();

        // 将按钮面板和状态面板放入南部区域
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.NORTH);
        southPanel.add(statusPanel, BorderLayout.SOUTH);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        // 设置内容面板
        setContentPane(mainPanel);

        // 设置快捷键
        setupKeyBindings();
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("设置"));

        // 路径选择组件
        JLabel pathLabel = new JLabel("文件夹路径:");
        pathField = new JTextField(30);
        pathField.setToolTipText("输入文件夹路径或使用浏览按钮选择");

        JButton browseButton = new JButton("浏览...");
        browseButton.setToolTipText("选择文件夹");
        browseButton.addActionListener(e -> browseFolder());

        // 深度控制组件
        JLabel depthLabel = new JLabel("递归深度 (0=无限):");
        SpinnerNumberModel depthModel = new SpinnerNumberModel(3, 0, 20, 1);
        depthSpinner = new JSpinner(depthModel);
        depthSpinner.setToolTipText("设置扫描深度，0表示无限制");

        // 文件显示选项
        showFilesCheckBox = new JCheckBox("显示文件", true);
        showFilesCheckBox.setToolTipText("是否在树状图中显示文件");

        // 生成按钮
        JButton generateButton = new JButton("生成树状图");
        generateButton.setBackground(new Color(70, 130, 180));
        generateButton.setForeground(Color.WHITE);
        generateButton.setToolTipText("生成文件夹树状图");
        generateButton.addActionListener(e -> generateTree());

        // 添加组件到控制面板
        panel.add(pathLabel);
        panel.add(pathField);
        panel.add(browseButton);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(depthLabel);
        panel.add(depthSpinner);
        panel.add(showFilesCheckBox);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(generateButton);

        // 监听路径字段变化
        pathField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateGenerateButtonState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateGenerateButtonState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateGenerateButtonState();
            }

            private void updateGenerateButtonState() {
                String path = pathField.getText().trim();
                generateButton.setEnabled(!path.isEmpty());
            }
        });

        // 初始状态
        generateButton.setEnabled(false);

        return panel;
    }

    private JPanel createProgressPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("扫描进度"));
        panel.setPreferredSize(new Dimension(800, 100));

        // 进度条
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(0, 150, 0));

        // 进度标签面板
        JPanel labelsPanel = new JPanel(new GridLayout(2, 2, 10, 5));

        progressLabel = new JLabel("进度: 0%");
        progressLabel.setForeground(Color.BLUE);

        speedLabel = new JLabel("速度: 0 项/秒");
        speedLabel.setForeground(Color.DARK_GRAY);

        timeLabel = new JLabel("预计时间: 计算中...");
        timeLabel.setForeground(Color.DARK_GRAY);

        currentPathLabel = new JLabel("当前路径: 等待开始...");
        currentPathLabel.setForeground(new Color(139, 69, 19));

        labelsPanel.add(progressLabel);
        labelsPanel.add(speedLabel);
        labelsPanel.add(timeLabel);
        labelsPanel.add(currentPathLabel);

        panel.add(progressBar, BorderLayout.NORTH);
        panel.add(labelsPanel, BorderLayout.CENTER);

        // 初始状态
        progressBar.setValue(0);
        progressBar.setString("等待开始");

        return panel;
    }

    private JPanel createTreePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("文件夹树状图 (双击打开文件/夹，右键菜单)"));

        // 创建支持文件操作的文本面板
        treeTextPane = new FileTreeTextPane("", statusLabel);

        // 设置刷新回调
        treeTextPane.setFileMenuRefreshCallback(() -> {
            // 如果路径不为空，则重新生成树状图
            if (!pathField.getText().trim().isEmpty()) {
                generateTree();
            }
        });

        treeTextPane.setToolTipText("<html>生成的树状图，支持以下操作:<br>" +
                "• 双击文件: 用默认程序打开<br>" +
                "• 双击文件夹: 用资源管理器打开<br>" +
                "• 右键菜单: 复制、删除、重命名等操作<br>" +
                "• 快捷键: Enter(打开), Delete(删除), Ctrl+C(复制)</html>");

        // 添加滚动条
        JScrollPane scrollPane = new JScrollPane(treeTextPane);
        scrollPane.setPreferredSize(new Dimension(800, 400));

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        // 刷新按钮
        JButton refreshButton = new JButton("刷新");
        refreshButton.setToolTipText("刷新当前树状图");
        refreshButton.addActionListener(e -> {
            if (!pathField.getText().trim().isEmpty()) {
                generateTree();
            }
        });

        // 复制按钮
        copyButton = new JButton("复制树状图");
        copyButton.setToolTipText("将整个树状图复制到剪贴板");
        copyButton.setEnabled(false);
        copyButton.addActionListener(e -> copyToClipboard());

        // 保存按钮
        saveButton = new JButton("保存树状图");
        saveButton.setToolTipText("将树状图保存到文本文件");
        saveButton.setEnabled(false);
        saveButton.addActionListener(e -> saveToFile());

        panel.add(refreshButton);
        panel.add(copyButton);
        panel.add(saveButton);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());

        statusLabel = new JLabel("就绪");
        statsLabel = new JLabel("");

        panel.add(statusLabel, BorderLayout.WEST);
        panel.add(statsLabel, BorderLayout.EAST);

        return panel;
    }

    private void setupWindowProperties() {
        setSize(1000, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 700));

        // 设置窗口图标
        try {
            ImageIcon icon = createImageIcon("/icons/folder_icon.png", "文件夹图标");
            if (icon != null) {
                setIconImage(icon.getImage());
            }
        } catch (Exception e) {
            System.err.println("无法加载图标: " + e.getMessage());
        }
    }

    private void setupKeyBindings() {
        // 设置键盘快捷键
        KeyStroke ctrlC = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);
        KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK);

        // 复制快捷键
        treeTextPane.getInputMap(JComponent.WHEN_FOCUSED).put(ctrlC, "copy");
        treeTextPane.getActionMap().put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!treeTextPane.getText().trim().isEmpty()) {
                    copyToClipboard();
                }
            }
        });

        // 保存快捷键
        treeTextPane.getInputMap(JComponent.WHEN_FOCUSED).put(ctrlS, "save");
        treeTextPane.getActionMap().put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!treeTextPane.getText().trim().isEmpty()) {
                    saveToFile();
                }
            }
        });
    }

    private ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("找不到图标文件: " + path);
            return null;
        }
    }

    /**
     * 浏览文件夹
     */
    private void browseFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("选择文件夹");
        fileChooser.setApproveButtonText("选择");

        // 设置初始目录
        String currentPath = pathField.getText().trim();
        if (!currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists()) {
                fileChooser.setCurrentDirectory(currentDir);
            }
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            pathField.setText(selectedFolder.getAbsolutePath());
            generateTree();
        }
    }

    /**
     * 生成树状图
     */
    public void generateTree() {
        String folderPath = pathField.getText().trim();
        if (folderPath.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "请输入文件夹路径",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                    "指定的路径不存在或不是一个文件夹",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 获取用户设置
        int maxDepth = (int) depthSpinner.getValue();
        boolean showFiles = showFilesCheckBox.isSelected();

        // 初始化进度监控变量
        scanStartTime = System.currentTimeMillis();

        // 重置进度显示
        progressBar.setValue(0);
        progressBar.setString("准备扫描...");
        progressLabel.setText("进度: 0%");
        speedLabel.setText("速度: 0 项/秒");
        timeLabel.setText("预计时间: 计算中...");
        currentPathLabel.setText("当前路径: 初始化...");

        // 更新状态
        statusLabel.setText("正在扫描文件夹...");
        statsLabel.setText("");
        copyButton.setEnabled(false);
        saveButton.setEnabled(false);
        treeTextPane.setText("");

        // 在后台线程中执行扫描任务
        SwingWorker<String, ProgressData> worker = new SwingWorker<String, ProgressData>() {
            @Override
            protected String doInBackground() throws Exception {
                return folderScanner.generateTreeWithProgress(folderPath, maxDepth, showFiles,
                        (processed, total, currentPath) -> {
                            int progress = total > 0 ? (int)((processed * 100.0) / total) : 0;
                            long currentTime = System.currentTimeMillis();
                            long elapsedTime = currentTime - scanStartTime;
                            double speed = elapsedTime > 0 ? (processed * 1000.0 / elapsedTime) : 0;
                            long estimatedRemaining = 0;

                            if (processed > 0 && total > 0) {
                                double itemsPerMs = processed / (double)elapsedTime;
                                if (itemsPerMs > 0) {
                                    estimatedRemaining = (long)((total - processed) / itemsPerMs);
                                }
                            }

                            publish(new ProgressData(progress, processed, total,
                                    speed, estimatedRemaining, currentPath));
                        });
            }

            @Override
            protected void process(java.util.List<ProgressData> chunks) {
                if (!chunks.isEmpty()) {
                    ProgressData data = chunks.get(chunks.size() - 1);

                    progressBar.setValue(data.progress);
                    progressBar.setString(String.format("%d%%", data.progress));
                    progressLabel.setText(String.format("进度: %d/%d (%d%%)",
                            data.processed, data.total, data.progress));
                    speedLabel.setText(String.format("速度: %.1f 项/秒", data.speed));

                    if (data.estimatedRemaining > 0) {
                        String timeStr = formatTime(data.estimatedRemaining);
                        timeLabel.setText(String.format("预计剩余: %s", timeStr));
                    } else {
                        timeLabel.setText("预计时间: 计算中...");
                    }

                    String displayPath = data.currentPath;
                    if (displayPath.length() > 50) {
                        displayPath = "..." + displayPath.substring(displayPath.length() - 50);
                    }
                    currentPathLabel.setText(String.format("当前: %s", displayPath));
                }
            }

            @Override
            protected void done() {
                try {
                    String tree = get();
                    treeTextPane.setText(tree);
                    treeTextPane.setBasePath(folderPath);

                    String stats = folderScanner.getFolderStats(folderPath);
                    statsLabel.setText(stats);

                    boolean hasContent = !tree.trim().isEmpty() && !tree.startsWith("错误:");
                    copyButton.setEnabled(hasContent);
                    saveButton.setEnabled(hasContent);

                    statusLabel.setText("就绪");

                    progressBar.setValue(100);
                    progressBar.setString("完成");
                    progressLabel.setText("进度: 完成");

                    long totalTime = System.currentTimeMillis() - scanStartTime;
                    speedLabel.setText(String.format("总时间: %s", formatTime(totalTime)));
                    timeLabel.setText("预计时间: 完成");
                    currentPathLabel.setText("当前路径: 扫描完成");

                    treeTextPane.setCaretPosition(0);

                } catch (Exception e) {
                    treeTextPane.setText("生成树状图时发生错误: " + e.getMessage());
                    statusLabel.setText("错误");

                    progressBar.setValue(0);
                    progressBar.setString("错误");
                    progressLabel.setText("进度: 错误");
                    currentPathLabel.setText("当前路径: 发生错误");
                }
            }
        };

        worker.execute();
    }

    /**
     * 格式化时间（毫秒转换为可读格式）
     */
    private String formatTime(long milliseconds) {
        if (milliseconds < 1000) {
            return String.format("%d毫秒", milliseconds);
        }

        long seconds = milliseconds / 1000;
        if (seconds < 60) {
            return String.format("%d秒", seconds);
        }

        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes < 60) {
            return String.format("%d分%d秒", minutes, seconds);
        }

        long hours = minutes / 60;
        minutes = minutes % 60;
        return String.format("%d小时%d分%d秒", hours, minutes, seconds);
    }

    /**
     * 进度数据类
     */
    private static class ProgressData {
        int progress;
        int processed;
        int total;
        double speed;
        long estimatedRemaining;
        String currentPath;

        ProgressData(int progress, int processed, int total,
                     double speed, long estimatedRemaining, String currentPath) {
            this.progress = progress;
            this.processed = processed;
            this.total = total;
            this.speed = speed;
            this.estimatedRemaining = estimatedRemaining;
            this.currentPath = currentPath;
        }
    }

    /**
     * 扫描指定文件夹（供外部调用）
     */
    public void scanFolder(String folderPath) {
        pathField.setText(folderPath);
        generateTree();
    }

    /**
     * 复制到剪贴板
     */
    private void copyToClipboard() {
        String text = treeTextPane.getText();
        if (text.trim().isEmpty()) {
            return;
        }

        try {
            StringSelection selection = new StringSelection(text);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);

            JOptionPane.showMessageDialog(this,
                    "树状图已复制到剪贴板",
                    "成功",
                    JOptionPane.INFORMATION_MESSAGE);

            statusLabel.setText("已复制到剪贴板");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "复制失败: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 保存到文件
     */
    private void saveToFile() {
        String text = treeTextPane.getText();
        if (text.trim().isEmpty()) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存树状图");
        fileChooser.setSelectedFile(new File("folder_tree.txt"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            if (!file.getName().toLowerCase().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }

            try {
                Files.write(Paths.get(file.getAbsolutePath()), text.getBytes());

                JOptionPane.showMessageDialog(this,
                        "树状图已保存到: " + file.getAbsolutePath(),
                        "成功",
                        JOptionPane.INFORMATION_MESSAGE);

                statusLabel.setText("已保存到文件");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "保存失败: " + e.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}