package com.foldertree.core;

import com.foldertree.util.AppLogger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * 树形图创建器 - 从文本创建文件和文件夹
 */
public class TreeCreator {

    /**
     * 从树形图文本创建文件和文件夹结构
     *
     * @param treeText 树形图文本
     * @param basePath 基础路径（创建的根目录）
     * @return 创建结果信息
     */
    public String createFromTree(String treeText, String basePath) {
        AppLogger.info("开始从树形图创建文件结构");
        AppLogger.debug("基础路径: " + basePath);
        AppLogger.debug("树形图预览: " +
                (treeText.length() > 200 ? treeText.substring(0, 200) + "..." : treeText));

        if (treeText == null || treeText.trim().isEmpty()) {
            AppLogger.error("树形图文本为空");
            return "错误: 树形图文本不能为空";
        }

        if (basePath == null || basePath.trim().isEmpty()) {
            AppLogger.error("基础路径为空");
            return "错误: 请指定创建位置";
        }

        try {
            File baseDir = new File(basePath);
            if (!baseDir.exists()) {
                AppLogger.info("基础目录不存在，尝试创建: " + basePath);
                boolean created = baseDir.mkdirs();
                if (!created) {
                    AppLogger.error("无法创建基础目录: " + basePath);
                    return "错误: 无法创建目录 " + basePath;
                }
                AppLogger.info("基础目录创建成功: " + basePath);
            }

            // 解析树形图
            AppLogger.info("开始解析树形图");
            List<TreeItem> items = parseTree(treeText);
            if (items.isEmpty()) {
                AppLogger.error("无法解析树形图，项目列表为空");
                return "错误: 无法解析树形图";
            }

            AppLogger.info("解析完成，项目总数: " + items.size());

            // 统计信息
            int foldersCreated = 0;
            int filesCreated = 0;
            int errors = 0;
            List<String> errorMessages = new ArrayList<>();

            // 创建第一个项目（根目录）
            TreeItem root = items.get(0);
            if (!root.isDirectory()) {
                AppLogger.error("第一行不是目录: " + root.getName());
                return "错误: 第一行必须是目录";
            }

            String rootPath = basePath + File.separator + root.getName();
            AppLogger.info("根目录路径: " + rootPath);

            File rootDir = new File(rootPath);
            if (!rootDir.exists()) {
                AppLogger.info("根目录不存在，尝试创建: " + rootPath);
                if (rootDir.mkdirs()) {
                    foldersCreated++;
                    AppLogger.info("根目录创建成功: " + rootPath);
                } else {
                    AppLogger.error("无法创建根目录: " + rootPath);
                    return "错误: 无法创建根目录 " + rootPath;
                }
            } else {
                AppLogger.info("根目录已存在: " + rootPath);
            }

            // 构建层级关系
            AppLogger.info("开始构建层级关系");
            buildHierarchy(items);

            // 创建其他项目
            AppLogger.info("开始创建文件和文件夹");
            for (int i = 1; i < items.size(); i++) {
                TreeItem item = items.get(i);
                String fullPath = rootPath + File.separator + item.getFullPath();

                AppLogger.debug("处理项目 " + i + "/" + items.size() + ": " +
                        (item.isDirectory() ? "[目录] " : "[文件] ") + item.getName() +
                        " -> " + fullPath);

                try {
                    if (item.isDirectory()) {
                        File dir = new File(fullPath);
                        if (!dir.exists()) {
                            if (dir.mkdirs()) {
                                foldersCreated++;
                                AppLogger.debug("目录创建成功: " + fullPath);
                            } else {
                                errors++;
                                errorMessages.add("无法创建目录: " + fullPath);
                                AppLogger.error("无法创建目录: " + fullPath);
                            }
                        } else {
                            AppLogger.debug("目录已存在: " + fullPath);
                        }
                    } else {
                        // 创建文件
                        File file = new File(fullPath);
                        File parentDir = file.getParentFile();

                        // 确保父目录存在
                        if (parentDir != null && !parentDir.exists()) {
                            AppLogger.debug("父目录不存在，尝试创建: " + parentDir.getAbsolutePath());
                            parentDir.mkdirs();
                        }

                        // 创建空文件
                        if (file.createNewFile()) {
                            filesCreated++;
                            AppLogger.debug("文件创建成功: " + fullPath);
                        } else if (!file.exists()) {
                            // 如果文件已存在但不是这次创建的，也算创建成功
                            filesCreated++;
                            AppLogger.debug("文件已存在: " + fullPath);
                        } else {
                            AppLogger.debug("文件未创建（可能已存在）: " + fullPath);
                        }
                    }
                } catch (IOException e) {
                    errors++;
                    errorMessages.add("创建失败 " + item.getName() + ": " + e.getMessage());
                    AppLogger.error("创建失败: " + item.getName() + " -> " + fullPath, e);
                } catch (SecurityException e) {
                    errors++;
                    errorMessages.add("权限不足: " + fullPath);
                    AppLogger.error("权限不足: " + fullPath, e);
                }
            }

            // 构建结果信息
            StringBuilder result = new StringBuilder();
            result.append("创建完成!\n\n");
            result.append("创建位置: ").append(rootPath).append("\n");
            result.append("文件夹创建: ").append(foldersCreated).append(" 个\n");
            result.append("文件创建: ").append(filesCreated).append(" 个\n");
            result.append("错误: ").append(errors).append(" 个\n");

            if (!errorMessages.isEmpty()) {
                result.append("\n错误详情:\n");
                for (String error : errorMessages) {
                    result.append("- ").append(error).append("\n");
                }
            }

            String resultStr = result.toString();
            AppLogger.info("创建完成: " +
                    (resultStr.length() > 300 ? resultStr.substring(0, 300) + "..." : resultStr));

            return resultStr;

        } catch (Exception e) {
            AppLogger.error("从树形图创建文件结构时发生异常", e);
            return "错误: " + e.getMessage();
        }
    }

    /**
     * 构建层级关系
     */
    private void buildHierarchy(List<TreeItem> items) {
        if (items.size() <= 1) return;

        AppLogger.debug("开始构建层级关系，项目数: " + items.size());

        TreeItem root = items.get(0);
        for (int i = 1; i < items.size(); i++) {
            TreeItem current = items.get(i);

            // 找到父级
            TreeItem parent = null;
            for (int j = i - 1; j >= 0; j--) {
                TreeItem candidate = items.get(j);
                if (candidate.getLevel() < current.getLevel() && candidate.isDirectory()) {
                    parent = candidate;
                    break;
                }
            }

            // 构建完整路径
            if (parent != null) {
                String parentPath = parent.getFullPath();
                if (parentPath == null) {
                    parentPath = "";
                }
                current.setFullPath((parentPath.isEmpty() ? "" : parentPath + File.separator) + current.getName());
                AppLogger.debug("项目 " + current.getName() + " 的父级: " + parent.getName() + ", 完整路径: " + current.getFullPath());
            } else {
                current.setFullPath(current.getName());
                AppLogger.debug("项目 " + current.getName() + " 没有父级，完整路径: " + current.getFullPath());
            }
        }

        AppLogger.debug("层级关系构建完成");
    }

    /**
     * 解析树形图文本
     */
    private List<TreeItem> parseTree(String treeText) {
        AppLogger.debug("开始解析树形图文本");

        List<TreeItem> items = new ArrayList<>();
        String[] lines = treeText.split("\n");

        AppLogger.debug("总行数: " + lines.length);

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum];
            if (line.trim().isEmpty()) {
                AppLogger.debug("跳过空行: " + lineNum);
                continue;
            }

            TreeItem item = parseLine(line);
            if (item != null) {
                items.add(item);
                AppLogger.debug("解析行 " + lineNum + ": " +
                        (item.isDirectory() ? "[目录] " : "[文件] ") +
                        item.getName() + " (层级: " + item.getLevel() + ")");
            } else {
                AppLogger.warn("无法解析行 " + lineNum + ": " + line);
            }
        }

        AppLogger.debug("解析完成，有效项目数: " + items.size());

        return items;
    }

    /**
     * 解析单行树形图
     */
    private TreeItem parseLine(String line) {
        // 计算缩进级别
        int level = 0;
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == ' ' || c == '│' || c == '├' || c == '└') {
                if (c == ' ' || c == '│') {
                    level += 1;
                }
                i++;
            } else if (c == '─') {
                i += 2; // 跳过 "─"
            } else {
                break;
            }
        }

        // 移除前缀字符
        String cleaned = line.substring(i);
        if (cleaned.trim().isEmpty()) {
            AppLogger.warn("行内容为空: " + line);
            return null;
        }

        // 检查是否是目录
        boolean isDirectory = cleaned.endsWith("/");
        if (isDirectory) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        String name = cleaned.trim();
        return new TreeItem(name, isDirectory, level / 2, name);
    }

    /**
     * 树形图项目内部类
     */
    private static class TreeItem {
        private final String name;
        private final boolean directory;
        private final int level;
        private final String path;
        private String fullPath;

        public TreeItem(String name, boolean directory, int level, String path) {
            this.name = name;
            this.directory = directory;
            this.level = level;
            this.path = path;
            this.fullPath = path;
        }

        public String getName() {
            return name;
        }

        public boolean isDirectory() {
            return directory;
        }

        public int getLevel() {
            return level;
        }

        public String getPath() {
            return path;
        }

        public String getFullPath() {
            return fullPath;
        }

        public void setFullPath(String fullPath) {
            this.fullPath = fullPath;
        }

        @Override
        public String toString() {
            return (directory ? "[DIR] " : "[FILE] ") + name + " (level: " + level + ")";
        }
    }
}