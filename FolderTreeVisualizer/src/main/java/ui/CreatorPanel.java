package com.foldertree.ui;

import com.foldertree.core.TreeCreator;
import com.foldertree.util.AppLogger;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * 创建器模式面板 - 输入树形图并创建文件结构
 */
public class CreatorPanel extends JPanel {
    private JTextArea treeTextArea;
    private JTextField locationField;
    private JButton browseButton;
    private JButton createButton;
    private JTextArea resultArea;
    private JLabel statusLabel;
    private TreeCreator treeCreator;

    public CreatorPanel(JLabel statusLabel) {
        this.statusLabel = statusLabel;
        this.treeCreator = new TreeCreator();

        AppLogger.info("创建器面板初始化开始");

        initComponents();
        setupLayout();
        setupListeners();

        AppLogger.info("创建器面板初始化完成");
    }

    private void initComponents() {
        AppLogger.debug("初始化创建器面板组件");

        // 设置面板背景
        setBackground(new Color(240, 240, 240));

        // 树形图输入区域
        JLabel treeLabel = new JLabel("输入树形图:");
        treeLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));

        treeTextArea = new JTextArea(20, 60);
        treeTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        treeTextArea.setLineWrap(true);
        treeTextArea.setWrapStyleWord(true);
        treeTextArea.setText(getDefaultTreeExample());

        AppLogger.debug("设置默认树形图示例");

        JScrollPane treeScrollPane = new JScrollPane(treeTextArea);
        treeScrollPane.setBorder(BorderFactory.createTitledBorder("树形图输入"));

        // 创建位置选择
        JLabel locationLabel = new JLabel("创建位置:");
        locationLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));

        locationField = new JTextField(40);
        locationField.setToolTipText("选择或输入要创建文件结构的目录路径");

        browseButton = new JButton("浏览...");
        browseButton.setToolTipText("选择目标文件夹");

        // 创建按钮
        createButton = new JButton("创建文件结构");
        createButton.setBackground(new Color(70, 130, 180));
        createButton.setForeground(Color.WHITE);
        createButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        createButton.setToolTipText("根据输入的树形图创建文件和文件夹");

        // 结果输出区域
        JLabel resultLabel = new JLabel("创建结果:");
        resultLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));

        resultArea = new JTextArea(10, 60);
        resultArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        resultArea.setEditable(false);
        resultArea.setBackground(new Color(250, 250, 250));

        JScrollPane resultScrollPane = new JScrollPane(resultArea);
        resultScrollPane.setBorder(BorderFactory.createTitledBorder("创建结果"));

        // 添加组件到面板
        setLayout(new BorderLayout(10, 10));

        // 顶部面板 - 树形图输入
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(treeLabel, BorderLayout.NORTH);
        topPanel.add(treeScrollPane, BorderLayout.CENTER);

        // 中间面板 - 位置选择和创建按钮
        JPanel middlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        middlePanel.add(locationLabel);
        middlePanel.add(locationField);
        middlePanel.add(browseButton);
        middlePanel.add(Box.createHorizontalStrut(20));
        middlePanel.add(createButton);

        // 底部面板 - 结果输出
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.add(resultLabel, BorderLayout.NORTH);
        bottomPanel.add(resultScrollPane, BorderLayout.CENTER);

        // 将所有面板添加到主面板
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.add(topPanel, BorderLayout.CENTER);
        contentPanel.add(middlePanel, BorderLayout.SOUTH);

        add(contentPanel, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.CENTER);

        // 设置边框
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        AppLogger.debug("创建器面板组件初始化完成");
    }

    private void setupLayout() {
        // 布局已经在initComponents中设置
        AppLogger.debug("创建器面板布局设置完成");
    }

    private void setupListeners() {
        AppLogger.debug("设置创建器面板事件监听器");

        // 浏览按钮事件
        browseButton.addActionListener(e -> browseLocation());

        // 创建按钮事件
        createButton.addActionListener(e -> createStructure());

        // 为文本框添加快捷键
        setupKeyBindings();

        AppLogger.debug("创建器面板事件监听器设置完成");
    }

    private void setupKeyBindings() {
        AppLogger.debug("设置创建器面板快捷键绑定");

        // Ctrl+Enter 创建结构
        InputMap inputMap = treeTextArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = treeTextArea.getActionMap();

        KeyStroke ctrlEnter = KeyStroke.getKeyStroke("control ENTER");
        inputMap.put(ctrlEnter, "createStructure");
        actionMap.put("createStructure", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppLogger.debug("快捷键 Ctrl+Enter 触发创建结构");
                createStructure();
            }
        });

        AppLogger.debug("创建器面板快捷键绑定设置完成");
    }

    private void browseLocation() {
        AppLogger.info("开始浏览创建位置");

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("选择创建位置");
        fileChooser.setApproveButtonText("选择");

        // 设置初始目录
        String currentPath = locationField.getText().trim();
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
            locationField.setText(selectedPath);

            AppLogger.info("选择创建位置: " + selectedPath);
        } else {
            AppLogger.debug("用户取消选择创建位置");
        }
    }

    private void createStructure() {
        AppLogger.info("开始创建文件结构");

        String treeText = treeTextArea.getText().trim();
        String location = locationField.getText().trim();

        if (treeText.isEmpty()) {
            AppLogger.warn("树形图文本为空");
            JOptionPane.showMessageDialog(this,
                    "请输入树形图文本",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (location.isEmpty()) {
            AppLogger.warn("创建位置为空");
            JOptionPane.showMessageDialog(this,
                    "请选择或输入创建位置",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        AppLogger.debug("树形图文本长度: " + treeText.length() + " 字符");
        AppLogger.debug("创建位置: " + location);

        // 显示等待状态
        if (statusLabel != null) {
            statusLabel.setText("正在创建文件结构...");
        }
        resultArea.setText("正在创建文件结构...\n请稍候...");
        createButton.setEnabled(false);

        AppLogger.debug("创建按钮已禁用，显示等待状态");

        // 在后台线程中执行创建任务
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    AppLogger.info("后台线程开始执行创建任务");
                    String result = treeCreator.createFromTree(treeText, location);
                    AppLogger.info("后台线程创建任务完成");
                    return result;
                } catch (Exception e) {
                    AppLogger.error("后台线程执行创建任务时发生异常", e);
                    return "创建时发生错误: " + e.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    resultArea.setText(result);

                    AppLogger.debug("创建结果显示: " +
                            (result.length() > 100 ? result.substring(0, 100) + "..." : result));

                    if (result.startsWith("创建完成!")) {
                        AppLogger.info("文件结构创建成功");
                        if (statusLabel != null) {
                            statusLabel.setText("文件结构创建成功");
                        }
                        JOptionPane.showMessageDialog(CreatorPanel.this,
                                "文件结构创建完成!",
                                "成功",
                                JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        AppLogger.warn("文件结构创建失败: " + result);
                        if (statusLabel != null) {
                            statusLabel.setText("文件结构创建失败");
                        }
                    }

                    createButton.setEnabled(true);
                    AppLogger.debug("创建按钮已启用");

                } catch (Exception e) {
                    AppLogger.error("处理创建结果时发生异常", e);
                    resultArea.setText("创建时发生错误: " + e.getMessage());
                    if (statusLabel != null) {
                        statusLabel.setText("错误");
                    }
                    createButton.setEnabled(true);
                }
            }
        };

        worker.execute();
        AppLogger.info("创建任务已提交到后台线程");
    }

    private String getDefaultTreeExample() {
        AppLogger.debug("获取默认树形图示例");
        return "项目名称/\n" +
                "├── src/\n" +
                "│   ├── main/\n" +
                "│   │   ├── java/\n" +
                "│   │   │   └── com/\n" +
                "│   │   │       └── example/\n" +
                "│   │   │           └── Main.java\n" +
                "│   │   └── resources/\n" +
                "│   │       ├── config.properties\n" +
                "│   │       └── log4j2.xml\n" +
                "│   └── test/\n" +
                "│       └── java/\n" +
                "│           └── com/\n" +
                "│               └── example/\n" +
                "│                   └── TestMain.java\n" +
                "├── docs/\n" +
                "│   └── README.md\n" +
                "├── lib/\n" +
                "│   └── 依赖库.jar\n" +
                "├── build/\n" +
                "│   └── classes/\n" +
                "├── .gitignore\n" +
                "└── pom.xml";
    }

    /**
     * 设置状态标签（从外部更新）
     */
    public void setStatusLabel(JLabel statusLabel) {
        AppLogger.debug("设置创建器面板状态标签");
        this.statusLabel = statusLabel;
    }

    /**
     * 清空输入
     */
    public void clear() {
        AppLogger.debug("清空创建器面板输入");
        treeTextArea.setText("");
        locationField.setText("");
        resultArea.setText("");
    }

    /**
     * 设置创建位置
     */
    public void setLocation(String location) {
        AppLogger.debug("设置创建位置: " + location);
        locationField.setText(location);
    }

    /**
     * 获取当前树形图文本
     */
    public String getTreeText() {
        String text = treeTextArea.getText();
        AppLogger.debug("获取树形图文本，长度: " + text.length());
        return text;
    }

    /**
     * 设置树形图文本
     */
    public void setTreeText(String treeText) {
        AppLogger.info("设置树形图文本，长度: " + treeText.length());
        treeTextArea.setText(treeText);
    }
}