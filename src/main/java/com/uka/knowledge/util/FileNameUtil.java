package com.uka.knowledge.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 上传文件名解析工具
 * <p>
 * 客户端提交的文件名不可信，可能为 null、包含目录成分（{@code ../}、绝对路径）、
 * 控制字符或缺少扩展名。这里给出与具体扩展库实现无关的确定性解析结果，
 * 非法输入统一返回 null，由调用方决定如何拒绝。
 * </p>
 *
 * @author uka
 * @version 1.0
 */
public final class FileNameUtil {

    /**
     * 合法扩展名：小写字母或数字，长度 1~10
     */
    private static final Pattern EXTENSION_PATTERN = Pattern.compile("[a-z0-9]{1,10}");

    private FileNameUtil() {
    }

    /**
     * 解析去掉目录成分与控制字符后的文件名（含扩展名）
     *
     * @param originalFilename 客户端提交的原始文件名
     * @return 安全文件名，无法得到有效名称时返回 null
     */
    public static String bareName(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }

        String name = originalFilename;
        int separator = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (separator >= 0) {
            name = name.substring(separator + 1);
        }

        StringBuilder cleaned = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c >= 0x20 && c != 0x7f) {
                cleaned.append(c);
            }
        }

        String bare = cleaned.toString().trim();
        return bare.isEmpty() ? null : bare;
    }

    /**
     * 解析文件主名（去掉最后一个扩展名后的部分）
     *
     * @param originalFilename 客户端提交的原始文件名
     * @return 文件主名，无有效名称时返回 null
     */
    public static String baseName(String originalFilename) {
        String bare = bareName(originalFilename);
        if (bare == null) {
            return null;
        }

        int dot = bare.lastIndexOf('.');
        String base = dot > 0 ? bare.substring(0, dot) : bare;
        base = base.trim();
        return base.isEmpty() ? null : base;
    }

    /**
     * 解析小写扩展名，仅接受字母和数字组成的短扩展名
     *
     * @param originalFilename 客户端提交的原始文件名
     * @return 小写扩展名，缺失或非法时返回 null
     */
    public static String extension(String originalFilename) {
        String bare = bareName(originalFilename);
        if (bare == null) {
            return null;
        }

        int dot = bare.lastIndexOf('.');
        if (dot < 0 || dot == bare.length() - 1) {
            return null;
        }

        String extension = bare.substring(dot + 1).toLowerCase(Locale.ROOT);
        return EXTENSION_PATTERN.matcher(extension).matches() ? extension : null;
    }
}
