package com.foldertree.ui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/**
 * 增强的文本面板，支持语法高亮
 * 可选组件，如果不需要语法高亮可以直接使用JTextArea
 */
public class TreeTextPane extends JTextPane {
    private StyleContext styleContext;
    private Style defaultStyle;
    private Style folderStyle;
    private Style fileStyle;

    public TreeTextPane() {
        super();
        initStyles();
        setFont(new Font("Monospaced", Font.PLAIN, 13));
        setEditable(true);
    }

    private void initStyles() {
        styleContext = StyleContext.getDefaultStyleContext();
        defaultStyle = styleContext.getStyle(StyleContext.DEFAULT_STYLE);

        // 文件夹样式（粗体）
        folderStyle = addStyle("FolderStyle", defaultStyle);
        StyleConstants.setBold(folderStyle, true);

        // 文件样式（普通）
        fileStyle = addStyle("FileStyle", defaultStyle);
        StyleConstants.setBold(fileStyle, false);
    }

    /**
     * 设置带有样式的文本
     */
    public void setStyledText(String text) {
        // 清空现有内容
        setText("");

        // 按行处理
        String[] lines = text.split("\n");
        for (String line : lines) {
            appendLine(line);
        }
    }

    private void appendLine(String line) {
        try {
            Document doc = getDocument();

            // 判断行类型并应用相应样式
            if (line.endsWith("/")) {
                doc.insertString(doc.getLength(), line + "\n", folderStyle);
            } else {
                doc.insertString(doc.getLength(), line + "\n", fileStyle);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}