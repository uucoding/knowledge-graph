package com.uka.knowledge.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 上传文件名解析测试类
 * <p>
 * 覆盖 null 文件名、空文件名、路径成分、控制字符等不可信输入，
 * 保证解析结果确定且不依赖具体扩展库的实现
 * </p>
 *
 * @author uka
 * @version 1.0
 */
class FileNameUtilTest {

    @Test
    void shouldReturnNullWhenNameIsMissing() {
        assertNull(FileNameUtil.bareName(null));
        assertNull(FileNameUtil.baseName(null));
        assertNull(FileNameUtil.extension(null));

        assertNull(FileNameUtil.bareName(""));
        assertNull(FileNameUtil.extension(""));

        // 只有空白字符
        assertNull(FileNameUtil.bareName("   "));
        assertNull(FileNameUtil.extension("   "));
    }

    @Test
    void shouldParseNormalFileName() {
        assertEquals("report.pdf", FileNameUtil.bareName("report.pdf"));
        assertEquals("report", FileNameUtil.baseName("report.pdf"));
        assertEquals("pdf", FileNameUtil.extension("report.pdf"));
    }

    @Test
    void shouldLowercaseExtensionAndKeepLastSegmentOfDotName() {
        assertEquals("pdf", FileNameUtil.extension("Report.PDF"));
        assertEquals("gz", FileNameUtil.extension("archive.tar.gz"));
        assertEquals("archive.tar", FileNameUtil.baseName("archive.tar.gz"));
    }

    @Test
    void shouldDropPathComponents() {
        assertEquals("x.pdf", FileNameUtil.bareName("doc/../x.pdf"));
        assertEquals("x.pdf", FileNameUtil.bareName("/etc/passwd/../x.pdf"));
        assertEquals("x.pdf", FileNameUtil.bareName("C:\\uploads\\x.pdf"));
        assertEquals("pdf", FileNameUtil.extension("../../x.pdf"));

        // 目录成分不能被当成扩展名
        assertNull(FileNameUtil.extension("x.pdf/../../evil"));
        assertNull(FileNameUtil.extension("x.pdf/"));
    }

    @Test
    void shouldDropControlCharacters() {
        // 换行无法伪造扩展名，也不会注入日志
        assertEquals("a.txt", FileNameUtil.bareName("a.txt\n"));
        assertEquals("txt", FileNameUtil.extension("a.txt\n"));

        // 空字节拼接得到的是合并后的未知扩展名，不会解析出 exe，最终由类型白名单拦截
        assertEquals("txtexe", FileNameUtil.extension("a.txt\0exe"));

        // 扩展名内外的空白被裁剪后仍然合法
        assertEquals("pdf", FileNameUtil.extension("  my file.pdf  "));
    }

    @Test
    void shouldRejectMalformedExtension() {
        // 无扩展名
        assertNull(FileNameUtil.extension("README"));
        // 以点结尾
        assertNull(FileNameUtil.extension("x."));
        // 扩展名含非法字符
        assertNull(FileNameUtil.extension("x.pd/f"));
        assertNull(FileNameUtil.extension("x.pdf!"));
        // 扩展名过长
        assertNull(FileNameUtil.extension("x." + "a".repeat(11)));

        // 合法边界
        assertEquals("a".repeat(10), FileNameUtil.extension("x." + "a".repeat(10)));
        assertEquals("1", FileNameUtil.extension("x.1"));
    }

    @Test
    void shouldKeepDotPrefixedNameAsBaseName() {
        assertEquals("pdf", FileNameUtil.extension(".pdf"));
        assertEquals(".pdf", FileNameUtil.baseName(".pdf"));
    }

    @Test
    void shouldSupportUnicodeFileName() {
        assertEquals("年度报告.pdf", FileNameUtil.bareName("年度报告.pdf"));
        assertEquals("年度报告", FileNameUtil.baseName("年度报告.pdf"));
        assertEquals("pdf", FileNameUtil.extension("年度报告.pdf"));
    }
}
