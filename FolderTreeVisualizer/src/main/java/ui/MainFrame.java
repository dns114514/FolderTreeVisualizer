package com.foldertree.ui;

import com.foldertree.core.FolderScanner;
import com.foldertree.util.AppLogger;
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
 * 主窗口类 - 带选项卡的双模式功能
 */
public class MainFrame extends JFrame {
    private FolderScanner folderScanner;

    // 选项卡面板
    private JTabbedPane tabbedPane;

    // 查看器模式组件
    private JTextField pathField;
    private JSpinner depthSpinner;
    private JCheckBox showFilesCheckBox;
    private FileTreeTextPane treeTextPane;
    private JButton copyButton;
    private JButton saveButton;

    // 创建器模式组件
    private CreatorPanel creatorPanel;

    // 共享组件
    private JLabel statusLabel;
    private JLabel statsLabel;

    // 进度监控组件（仅查看器模式）
    private JProgressBar progressBar;
    private JLabel progressLabel;
    private JLabel speedLabel;
    private JLabel timeLabel;
    private JLabel currentPathLabel;

    // 进度监控变量
    private long scanStartTime;

    public MainFrame() {
        AppLogger.info("主窗口初始化开始");

        folderScanner = new FolderScanner();
        initComponents();
        setupWindowProperties();

        AppLogger.info("主窗口初始化完成");
    }

    private void initComponents() {
        AppLogger.debug("初始化主窗口组件");

        // 设置窗口标题
        setTitle("文件夹树状图工具 - Java 17");

        // 先创建状态标签（但不创建整个状态面板）
        statusLabel = new JLabel("就绪 - 请选择模式");
        statsLabel = new JLabel("");

        // 创建选项卡面板
        tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));

        // 创建查看器面板
        JPanel viewerPanel = createViewerPanel();
        tabbedPane.addTab("文件夹查看器", createIcon('📁'), viewerPanel, "查看和分析文件夹结构");
        AppLogger.debug("查看器面板创建完成");

        // 创建创建器面板 - 传入正确的statusLabel
        creatorPanel = new CreatorPanel(statusLabel);
        tabbedPane.addTab("结构创建器", createIcon('➕'), creatorPanel, "从树形图创建文件和文件夹结构");
        AppLogger.debug("创建器面板创建完成");

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // 创建状态栏（现在tabbedPane已经初始化）
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        // 设置内容面板
        setContentPane(mainPanel);

        // 设置快捷键
        setupKeyBindings();

        AppLogger.debug("主窗口组件初始化完成");
    }

    private JPanel createViewerPanel() {
        AppLogger.debug("创建查看器面板");

        JPanel viewerPanel = new JPanel(new BorderLayout(10, 10));
        viewerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建控制面板
        JPanel controlPanel = createControlPanel();
        viewerPanel.add(controlPanel, BorderLayout.NORTH);

        // 创建进度监控面板
        JPanel progressPanel = createProgressPanel();

        // 创建树状图显示区域
        JPanel treePanel = createTreePanel();
        viewerPanel.add(treePanel, BorderLayout.CENTER);

        // 创建查看器按钮面板
        JPanel viewerButtonPanel = createViewerButtonPanel();

        // 创建一个容器面板，将进度面板和按钮面板放在底部
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(progressPanel, BorderLayout.NORTH);
        bottomPanel.add(viewerButtonPanel, BorderLayout.SOUTH);

        viewerPanel.add(bottomPanel, BorderLayout.SOUTH);

        AppLogger.debug("查看器面板创建完成");
        return viewerPanel;
    }

    private JPanel createControlPanel() {
        AppLogger.debug("创建控制面板");

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("扫描设置"));

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

        // 设置按钮的禁用状态外观
        generateButton.setEnabled(false);

        // 添加到创建器按钮
        JButton sendToCreatorButton = new JButton("发送到创建器");
        sendToCreatorButton.setBackground(new Color(60, 179, 113));
        sendToCreatorButton.setForeground(Color.WHITE);
        sendToCreatorButton.setToolTipText("将当前树状图发送到创建器模式");
        sendToCreatorButton.addActionListener(e -> sendToCreator());
        sendToCreatorButton.setEnabled(false);

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
        panel.add(Box.createHorizontalStrut(10));
        panel.add(sendToCreatorButton);

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
                boolean hasPath = !path.isEmpty();
                generateButton.setEnabled(hasPath);
                sendToCreatorButton.setEnabled(hasPath);

                // 更新按钮颜色
                if (hasPath) {
                    generateButton.setBackground(new Color(70, 130, 180));
                    sendToCreatorButton.setBackground(new Color(60, 179, 113));
                } else {
                    generateButton.setBackground(Color.GRAY);
                    sendToCreatorButton.setBackground(Color.GRAY);
                }

                AppLogger.debug("路径字段更新，状态: " + (hasPath ? "启用" : "禁用"));
            }
        });

        AppLogger.debug("控制面板创建完成");
        return panel;
    }

    private JPanel createProgressPanel() {
        AppLogger.debug("创建进度面板");

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

        AppLogger.debug("进度面板创建完成");
        return panel;
    }

    private JPanel createTreePanel() {
        AppLogger.debug("创建树状图面板");

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("文件夹树状图 (双击打开文件/夹，右键菜单)"));

        // 创建支持文件操作的文本面板
        treeTextPane = new FileTreeTextPane("", statusLabel);

        // 设置刷新回调
        treeTextPane.setFileMenuRefreshCallback(() -> {
            // 如果路径不为空，则重新生成树状图
            if (!pathField.getText().trim().isEmpty()) {
                AppLogger.debug("刷新回调触发，重新生成树状图");
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

        AppLogger.debug("树状图面板创建完成");
        return panel;
    }

    private JPanel createViewerButtonPanel() {
        AppLogger.debug("创建查看器按钮面板");

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

        // 刷新按钮
        JButton refreshButton = new JButton("刷新");
        refreshButton.setToolTipText("刷新当前树状图");
        refreshButton.addActionListener(e -> {
            if (!pathField.getText().trim().isEmpty()) {
                AppLogger.debug("刷新按钮点击");
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

        AppLogger.debug("查看器按钮面板创建完成");
        return panel;
    }

    private JPanel createStatusPanel() {
        AppLogger.debug("创建状态面板");

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());

        // 添加模式指示器
        JLabel modeLabel = new JLabel("模式: 查看器");
        modeLabel.setForeground(Color.BLUE);

        // 选项卡切换时更新模式指示器
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            if (index == 0) {
                modeLabel.setText("模式: 查看器");
                statusLabel.setText("查看器模式就绪");
                AppLogger.debug("切换到查看器模式");
            } else {
                modeLabel.setText("模式: 创建器");
                statusLabel.setText("创建器模式就绪");
                AppLogger.debug("切换到创建器模式");
            }
        });

        panel.add(modeLabel, BorderLayout.WEST);
        panel.add(statusLabel, BorderLayout.CENTER);
        panel.add(statsLabel, BorderLayout.EAST);

        AppLogger.debug("状态面板创建完成");
        return panel;
    }

    private void setupWindowProperties() {
        AppLogger.debug("设置窗口属性");

        setSize(1100, 850); // 稍微增加窗口大小以适应选项卡
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 750));

        // 设置窗口图标
        try {
            ImageIcon icon = createImageIcon("/icons/folder_icon.png", "文件夹图标");
            if (icon != null) {
                setIconImage(icon.getImage());
                AppLogger.debug("窗口图标设置成功");
            } else {
                AppLogger.warn("无法加载窗口图标");
            }
        } catch (Exception e) {
            AppLogger.error("设置窗口图标时发生错误", e);
        }

        AppLogger.debug("窗口属性设置完成");
    }

    private void setupKeyBindings() {
        AppLogger.debug("设置快捷键绑定");

        // 设置全局快捷键
        KeyStroke ctrlC = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);
        KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK);
        KeyStroke ctrl1 = KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.CTRL_DOWN_MASK);
        KeyStroke ctrl2 = KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.CTRL_DOWN_MASK);

        // 获取根面板的输入映射
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        // 复制快捷键
        inputMap.put(ctrlC, "copy");
        actionMap.put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppLogger.debug("全局快捷键 Ctrl+C 触发");
                int index = tabbedPane.getSelectedIndex();
                if (index == 0 && !treeTextPane.getText().trim().isEmpty()) {
                    copyToClipboard();
                }
            }
        });

        // 保存快捷键
        inputMap.put(ctrlS, "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppLogger.debug("全局快捷键 Ctrl+S 触发");
                int index = tabbedPane.getSelectedIndex();
                if (index == 0 && !treeTextPane.getText().trim().isEmpty()) {
                    saveToFile();
                }
            }
        });

        // 切换到查看器模式
        inputMap.put(ctrl1, "switchToViewer");
        actionMap.put("switchToViewer", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppLogger.debug("全局快捷键 Ctrl+1 触发 - 切换到查看器模式");
                tabbedPane.setSelectedIndex(0);
            }
        });

        // 切换到创建器模式
        inputMap.put(ctrl2, "switchToCreator");
        actionMap.put("switchToCreator", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppLogger.debug("全局快捷键 Ctrl+2 触发 - 切换到创建器模式");
                tabbedPane.setSelectedIndex(1);
            }
        });

        AppLogger.debug("快捷键绑定设置完成");
    }

    private Icon createIcon(char iconChar) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(Color.BLUE);
                g.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
                FontMetrics fm = g.getFontMetrics();
                int charWidth = fm.charWidth(iconChar);
                int charHeight = fm.getAscent();
                g.drawString(String.valueOf(iconChar), x + (20 - charWidth) / 2, y + charHeight);
            }

            @Override
            public int getIconWidth() {
                return 20;
            }

            @Override
            public int getIconHeight() {
                return 20;
            }
        };
    }

    private ImageIcon createImageIcon(String path, String description) {
        java.net.URL imgURL = getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            AppLogger.warn("找不到图标文件: " + path);
            return null;
        }
    }

    /**
     * 浏览文件夹（查看器模式）
     */
    private void browseFolder() {
        AppLogger.info("开始浏览文件夹");

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
                AppLogger.debug("设置浏览初始目录: " + currentPath);
            }
        }

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = fileChooser.getSelectedFile();
            String selectedPath = selectedFolder.getAbsolutePath();
            pathField.setText(selectedPath);

            AppLogger.info("选择文件夹: " + selectedPath);
            generateTree();
        } else {
            AppLogger.debug("用户取消选择文件夹");
        }
    }

    /**
     * 生成树状图（查看器模式）
     */
    public void generateTree() {
        AppLogger.info("开始生成树状图");

        String folderPath = pathField.getText().trim();
        if (folderPath.isEmpty()) {
            AppLogger.warn("文件夹路径为空");
            JOptionPane.showMessageDialog(this,
                    "请输入文件夹路径",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            AppLogger.error("路径不存在或不是文件夹: " + folderPath);
            JOptionPane.showMessageDialog(this,
                    "指定的路径不存在或不是一个文件夹",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 获取用户设置
        int maxDepth = (int) depthSpinner.getValue();
        boolean showFiles = showFilesCheckBox.isSelected();

        AppLogger.debug("生成树状图参数 - 路径: " + folderPath + ", 深度: " + maxDepth + ", 显示文件: " + showFiles);

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
                AppLogger.info("后台线程开始扫描文件夹");
                try {
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
                } catch (Exception e) {
                    AppLogger.error("后台线程扫描文件夹时发生异常", e);
                    return "错误: " + e.getMessage();
                }
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

                    AppLogger.debug("扫描进度 - 处理: " + data.processed + "/" + data.total +
                            " (" + data.progress + "%), 速度: " + String.format("%.1f", data.speed) + " 项/秒");
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

                    // 更新按钮颜色
                    if (hasContent) {
                        copyButton.setBackground(null); // 使用默认颜色
                        saveButton.setBackground(null);
                    }

                    statusLabel.setText("就绪");

                    progressBar.setValue(100);
                    progressBar.setString("完成");
                    progressLabel.setText("进度: 完成");

                    long totalTime = System.currentTimeMillis() - scanStartTime;
                    speedLabel.setText(String.format("总时间: %s", formatTime(totalTime)));
                    timeLabel.setText("预计时间: 完成");
                    currentPathLabel.setText("当前路径: 扫描完成");

                    treeTextPane.setCaretPosition(0);

                    AppLogger.info("树状图生成完成，总时间: " + formatTime(totalTime) +
                            ", 统计: " + stats + ", 内容长度: " + tree.length() + " 字符");

                } catch (Exception e) {
                    AppLogger.error("处理树状图结果时发生异常", e);
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
        AppLogger.info("树状图生成任务已提交到后台线程");
    }

    /**
     * 发送当前树状图到创建器模式
     */
    private void sendToCreator() {
        AppLogger.info("发送树状图到创建器模式");

        String treeText = treeTextPane.getText().trim();
        String currentPath = pathField.getText().trim();

        if (treeText.isEmpty()) {
            AppLogger.warn("树状图为空，无法发送");
            JOptionPane.showMessageDialog(this,
                    "请先生成树状图",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        AppLogger.debug("树状图文本长度: " + treeText.length() + " 字符");
        AppLogger.debug("当前路径: " + currentPath);

        // 切换到创建器模式
        tabbedPane.setSelectedIndex(1);
        AppLogger.debug("切换到创建器模式");

        // 设置创建器内容
        creatorPanel.setLocation(currentPath);
        // 重要：设置树形图文本
        creatorPanel.setTreeText(treeText);

        AppLogger.info("树状图已发送到创建器模式");

        JOptionPane.showMessageDialog(this,
                "树状图已发送到创建器模式",
                "成功",
                JOptionPane.INFORMATION_MESSAGE);
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
        AppLogger.info("扫描指定文件夹: " + folderPath);
        pathField.setText(folderPath);
        generateTree();
    }

    /**
     * 复制到剪贴板
     */
    private void copyToClipboard() {
        AppLogger.info("复制树状图到剪贴板");

        String text = treeTextPane.getText();
        if (text.trim().isEmpty()) {
            AppLogger.warn("树状图为空，无法复制");
            return;
        }

        try {
            StringSelection selection = new StringSelection(text);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);

            AppLogger.info("树状图复制成功，长度: " + text.length() + " 字符");

            JOptionPane.showMessageDialog(this,
                    "树状图已复制到剪贴板",
                    "成功",
                    JOptionPane.INFORMATION_MESSAGE);

            statusLabel.setText("已复制到剪贴板");
        } catch (Exception e) {
            AppLogger.error("复制到剪贴板失败", e);
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
        AppLogger.info("保存树状图到文件");

        String text = treeTextPane.getText();
        if (text.trim().isEmpty()) {
            AppLogger.warn("树状图为空，无法保存");
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
                AppLogger.debug("添加.txt扩展名: " + file.getAbsolutePath());
            }

            try {
                Files.write(Paths.get(file.getAbsolutePath()), text.getBytes());

                AppLogger.info("树状图保存成功: " + file.getAbsolutePath() + ", 长度: " + text.length() + " 字符");

                JOptionPane.showMessageDialog(this,
                        "树状图已保存到: " + file.getAbsolutePath(),
                        "成功",
                        JOptionPane.INFORMATION_MESSAGE);

                statusLabel.setText("已保存到文件");
            } catch (Exception e) {
                AppLogger.error("保存树状图失败", e);
                JOptionPane.showMessageDialog(this,
                        "保存失败: " + e.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            AppLogger.debug("用户取消保存");
        }
    }
}