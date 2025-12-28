package com.foldertree.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文件夹扫描器类 - 支持进度回调
 */
public class FolderScanner {

    private static final String PREFIX_ENTRY = "├── ";
    private static final String PREFIX_LAST_ENTRY = "└── ";
    private static final String PREFIX_VERTICAL = "│   ";
    private static final String PREFIX_SPACE = "    ";

    // 进度回调接口
    public interface ProgressCallback {
        void onProgress(int processed, int total, String currentPath);
    }

    /**
     * 统计文件夹中的总项目数
     */
    public int countTotalItems(String folderPath, int maxDepth, boolean showFiles) {
        File root = new File(folderPath);
        if (!root.exists() || !root.isDirectory()) {
            return 0;
        }
        return countItemsRecursive(root, maxDepth, 1, showFiles);
    }

    private int countItemsRecursive(File dir, int maxDepth, int currentDepth, boolean showFiles) {
        if (maxDepth > 0 && currentDepth > maxDepth) {
            return 0;
        }

        int count = 0;
        File[] items = dir.listFiles();
        if (items == null) {
            return 0;
        }

        for (File item : items) {
            if (item.isDirectory()) {
                count++;
                count += countItemsRecursive(item, maxDepth, currentDepth + 1, showFiles);
            } else if (showFiles) {
                count++;
            }
        }
        return count;
    }

    /**
     * 生成文件夹树状图 - 带进度回调版本
     */
    public String generateTreeWithProgress(String folderPath, int maxDepth,
                                           boolean showFiles, ProgressCallback callback) {
        File root = new File(folderPath);
        if (!root.exists() || !root.isDirectory()) {
            return "错误: 路径不存在或不是一个文件夹";
        }

        int totalItems = countTotalItems(folderPath, maxDepth, showFiles);
        AtomicInteger processed = new AtomicInteger(0);

        StringBuilder tree = new StringBuilder();
        tree.append(root.getName()).append("/\n");

        try {
            List<File> items = listSortedFiles(root);
            generateTreeRecursiveWithProgress(items, tree, "", maxDepth, 1,
                    showFiles, processed, totalItems,
                    callback, root.getAbsolutePath());
        } catch (SecurityException e) {
            return "错误: 没有权限访问该文件夹";
        }

        return tree.toString();
    }

    private void generateTreeRecursiveWithProgress(List<File> items, StringBuilder tree,
                                                   String prefix, int maxDepth,
                                                   int currentDepth, boolean showFiles,
                                                   AtomicInteger processed, int totalItems,
                                                   ProgressCallback callback, String basePath) {
        if (maxDepth > 0 && currentDepth > maxDepth) {
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            File item = items.get(i);
            boolean isLast = (i == items.size() - 1);

            processed.incrementAndGet();
            if (callback != null) {
                String displayPath = item.getAbsolutePath();
                if (displayPath.startsWith(basePath)) {
                    displayPath = displayPath.substring(basePath.length());
                    if (displayPath.startsWith(File.separator)) {
                        displayPath = displayPath.substring(1);
                    }
                }
                callback.onProgress(processed.get(), totalItems, displayPath);
            }

            tree.append(prefix);
            tree.append(isLast ? PREFIX_LAST_ENTRY : PREFIX_ENTRY);
            tree.append(item.getName());

            if (item.isDirectory()) {
                tree.append("/");
            }
            tree.append("\n");

            if (item.isDirectory()) {
                String newPrefix = prefix + (isLast ? PREFIX_SPACE : PREFIX_VERTICAL);

                try {
                    List<File> subItems = listSortedFiles(item);
                    if (!subItems.isEmpty()) {
                        generateTreeRecursiveWithProgress(subItems, tree, newPrefix,
                                maxDepth, currentDepth + 1,
                                showFiles, processed, totalItems,
                                callback, basePath);
                    }
                } catch (SecurityException e) {
                    tree.append(newPrefix).append("├── [权限拒绝]\n");
                }
            }
        }
    }

    /**
     * 原始生成树状图方法（向后兼容）
     */
    public String generateTree(String folderPath, int maxDepth, boolean showFiles) {
        return generateTreeWithProgress(folderPath, maxDepth, showFiles, null);
    }

    /**
     * 获取文件夹统计信息
     */
    public String getFolderStats(String folderPath) {
        File root = new File(folderPath);
        if (!root.exists() || !root.isDirectory()) {
            return "路径不存在";
        }

        int[] stats = countFilesAndFolders(root, 0);
        return String.format("文件夹: %d, 文件: %d", stats[0], stats[1]);
    }

    /**
     * 递归统计文件和文件夹数量
     */
    private int[] countFilesAndFolders(File dir, int depth) {
        int folders = 0;
        int files = 0;

        File[] items = dir.listFiles();
        if (items == null) {
            return new int[]{0, 0};
        }

        for (File item : items) {
            if (item.isDirectory()) {
                folders++;
                int[] subStats = countFilesAndFolders(item, depth + 1);
                folders += subStats[0];
                files += subStats[1];
            } else {
                files++;
            }
        }

        return new int[]{folders, files};
    }

    /**
     * 获取排序后的文件列表
     */
    private List<File> listSortedFiles(File dir) throws SecurityException {
        File[] files = dir.listFiles();
        if (files == null) {
            return new ArrayList<>();
        }

        List<File> directories = new ArrayList<>();
        List<File> fileList = new ArrayList<>();

        for (File file : files) {
            if (file.isDirectory()) {
                directories.add(file);
            } else {
                fileList.add(file);
            }
        }

        directories.sort(Comparator.comparing(File::getName));
        fileList.sort(Comparator.comparing(File::getName));

        List<File> result = new ArrayList<>(directories);
        result.addAll(fileList);

        return result;
    }
}