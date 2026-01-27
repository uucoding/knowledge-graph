package com.uka.knowledge.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.uka.knowledge.exception.BusinessException;
import com.uka.knowledge.common.ResultCode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析工具类
 * <p>
 * 支持解析多种格式的文档，提取其中的文本内容
 * 支持格式：PDF、DOC、DOCX、TXT、MD
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Slf4j
@Component
public class DocumentParser {

    /**
     * 页面内容封装类
     */
    @Data
    public static class PageContent {
        /**
         * 页码（从1开始，0表示无页码概念）
         */
        private int pageNum;

        /**
         * 页面内容
         */
        private String content;

        public PageContent(int pageNum, String content) {
            this.pageNum = pageNum;
            this.content = content;
        }
    }

    /**
     * 解析结果封装类
     */
    @Data
    public static class ParseResult {
        /**
         * 完整内容
         */
        private String fullContent;

        /**
         * 按页拆分的内容列表
         */
        private List<PageContent> pages;

        /**
         * 总页数
         */
        private int totalPages;

        public ParseResult(String fullContent, List<PageContent> pages) {
            this.fullContent = fullContent;
            this.pages = pages;
            this.totalPages = pages.size();
        }
    }

    /**
     * 解析文档，返回带页码信息的结果
     *
     * @param filePath 文件路径
     * @param fileType 文件类型
     * @return 解析结果（包含页码信息）
     */
    public ParseResult parseWithPages(String filePath, String fileType) {
        if (StrUtil.isBlank(filePath) || StrUtil.isBlank(fileType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件路径和类型不能为空");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在: " + filePath);
        }

        String lowerType = fileType.toLowerCase();
        try {
            return switch (lowerType) {
                case "pdf" -> parsePdfWithPages(file);
                case "doc" -> parseDocWithPages(file);
                case "docx" -> parseDocxWithPages(file);
                case "txt" -> parseTxtWithPages(file);
                case "md", "markdown" -> parseMarkdownWithPages(file);
                default -> throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORT,
                        "不支持的文件类型: " + fileType);
            };
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档解析失败: filePath={}, fileType={}", filePath, fileType, e);
            throw new BusinessException(ResultCode.FILE_PARSE_FAILED, "文档解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析文档，提取文本内容
     *
     * @param filePath 文件路径
     * @param fileType 文件类型
     * @return 文档文本内容
     */
    public String parse(String filePath, String fileType) {
        if (StrUtil.isBlank(filePath) || StrUtil.isBlank(fileType)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件路径和类型不能为空");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在: " + filePath);
        }

        // 根据文件类型选择解析方法
        String lowerType = fileType.toLowerCase();
        try {
            return switch (lowerType) {
                case "pdf" -> parsePdf(file);
                case "doc" -> parseDoc(file);
                case "docx" -> parseDocx(file);
                case "txt" -> parseTxt(file);
                case "md", "markdown" -> parseMarkdown(file);
                default -> throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORT,
                        "不支持的文件类型: " + fileType);
            };
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档解析失败: filePath={}, fileType={}", filePath, fileType, e);
            throw new BusinessException(ResultCode.FILE_PARSE_FAILED, "文档解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析PDF文档
     *
     * @param file PDF文件
     * @return 文本内容
     */
    private String parsePdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            // 设置排序，使文本按阅读顺序排列
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            log.info("PDF解析完成，页数: {}, 字符数: {}", document.getNumberOfPages(), text.length());
            return text;
        }
    }

    /**
     * 解析DOC文档（Word 97-2003格式）
     *
     * @param file DOC文件
     * @return 文本内容
     */
    private String parseDoc(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {
            String text = extractor.getText();
            log.info("DOC解析完成，字符数: {}", text.length());
            return text;
        }
    }

    /**
     * 解析DOCX文档（Word 2007及以上格式）
     *
     * @param file DOCX文件
     * @return 文本内容
     */
    private String parseDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder sb = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();

            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText();
                if (StrUtil.isNotBlank(text)) {
                    sb.append(text).append("\n");
                }
            }

            String result = sb.toString();
            log.info("DOCX解析完成，段落数: {}, 字符数: {}", paragraphs.size(), result.length());
            return result;
        }
    }

    /**
     * 解析TXT文档
     *
     * @param file TXT文件
     * @return 文本内容
     */
    private String parseTxt(File file) {
        String text = FileUtil.readString(file, StandardCharsets.UTF_8);
        log.info("TXT解析完成，字符数: {}", text.length());
        return text;
    }

    /**
     * 解析Markdown文档
     * 将Markdown转换为纯文本
     *
     * @param file Markdown文件
     * @return 文本内容
     */
    private String parseMarkdown(File file) {
        String markdown = FileUtil.readString(file, StandardCharsets.UTF_8);

        // 使用CommonMark解析器
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);

        // 渲染为纯文本
        TextContentRenderer renderer = TextContentRenderer.builder().build();
        String text = renderer.render(document);

        log.info("Markdown解析完成，原始字符数: {}, 纯文本字符数: {}", markdown.length(), text.length());
        return text;
    }

    // ==================== 带页码的解析方法 ====================

    /**
     * 解析PDF文档（带页码）
     */
    private ParseResult parsePdfWithPages(File file) throws IOException {
        List<PageContent> pages = new ArrayList<>();
        StringBuilder fullContent = new StringBuilder();

        try (PDDocument document = Loader.loadPDF(file)) {
            int totalPages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            for (int i = 1; i <= totalPages; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document);

                if (StrUtil.isNotBlank(pageText)) {
                    pages.add(new PageContent(i, pageText.trim()));
                    fullContent.append(pageText);
                }
            }

            log.info("PDF解析完成（带页码），页数: {}, 字符数: {}", totalPages, fullContent.length());
        }

        return new ParseResult(fullContent.toString(), pages);
    }

    /**
     * 解析DOC文档（带页码）
     * DOC格式不支持精确页码，按段落分组模拟
     */
    private ParseResult parseDocWithPages(File file) throws IOException {
        List<PageContent> pages = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument document = new HWPFDocument(fis);
             WordExtractor extractor = new WordExtractor(document)) {

            String fullText = extractor.getText();
            // DOC不支持页码，作为单页处理
            pages.add(new PageContent(0, fullText.trim()));

            log.info("DOC解析完成（带页码），字符数: {}", fullText.length());
            return new ParseResult(fullText, pages);
        }
    }

    /**
     * 解析DOCX文档（带页码）
     * DOCX可以通过分页符分割，但这里简化处理
     */
    private ParseResult parseDocxWithPages(File file) throws IOException {
        List<PageContent> pages = new ArrayList<>();
        StringBuilder fullContent = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            List<XWPFParagraph> paragraphs = document.getParagraphs();
            StringBuilder pageContent = new StringBuilder();
            int pageNum = 1;
            int charCount = 0;
            final int CHARS_PER_PAGE = 2000; // 估算每页字符数

            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText();
                if (StrUtil.isNotBlank(text)) {
                    pageContent.append(text).append("\n");
                    fullContent.append(text).append("\n");
                    charCount += text.length();

                    // 模拟分页（基于字符数）
                    if (charCount >= CHARS_PER_PAGE) {
                        pages.add(new PageContent(pageNum++, pageContent.toString().trim()));
                        pageContent = new StringBuilder();
                        charCount = 0;
                    }
                }
            }

            // 添加最后一页
            if (pageContent.length() > 0) {
                pages.add(new PageContent(pageNum, pageContent.toString().trim()));
            }

            log.info("DOCX解析完成（带页码），段落数: {}, 估算页数: {}", paragraphs.size(), pages.size());
        }

        return new ParseResult(fullContent.toString(), pages);
    }

    /**
     * 解析TXT文档（带页码）
     * 按行数分页
     */
    private ParseResult parseTxtWithPages(File file) {
        String fullText = FileUtil.readString(file, StandardCharsets.UTF_8);
        List<PageContent> pages = new ArrayList<>();

        String[] lines = fullText.split("\n");
        StringBuilder pageContent = new StringBuilder();
        int pageNum = 1;
        int lineCount = 0;
        final int LINES_PER_PAGE = 50; // 每页行数

        for (String line : lines) {
            pageContent.append(line).append("\n");
            lineCount++;

            if (lineCount >= LINES_PER_PAGE) {
                pages.add(new PageContent(pageNum++, pageContent.toString().trim()));
                pageContent = new StringBuilder();
                lineCount = 0;
            }
        }

        // 添加最后一页
        if (pageContent.length() > 0) {
            pages.add(new PageContent(pageNum, pageContent.toString().trim()));
        }

        log.info("TXT解析完成（带页码），总行数: {}, 分页数: {}", lines.length, pages.size());
        return new ParseResult(fullText, pages);
    }

    /**
     * 解析Markdown文档（带页码）
     * 按标题分页
     */
    private ParseResult parseMarkdownWithPages(File file) {
        String markdown = FileUtil.readString(file, StandardCharsets.UTF_8);

        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        TextContentRenderer renderer = TextContentRenderer.builder().build();
        String fullText = renderer.render(document);

        List<PageContent> pages = new ArrayList<>();

        // 按一级或二级标题分割
        String[] sections = markdown.split("(?=^#{1,2}\\s)", java.util.regex.Pattern.MULTILINE);
        int pageNum = 1;

        for (String section : sections) {
            if (StrUtil.isNotBlank(section)) {
                Node sectionNode = parser.parse(section);
                String sectionText = renderer.render(sectionNode);
                if (StrUtil.isNotBlank(sectionText)) {
                    pages.add(new PageContent(pageNum++, sectionText.trim()));
                }
            }
        }

        // 如果没有标题，整体作为一页
        if (pages.isEmpty()) {
            pages.add(new PageContent(0, fullText.trim()));
        }

        log.info("Markdown解析完成（带页码），分段数: {}", pages.size());
        return new ParseResult(fullText, pages);
    }
}
