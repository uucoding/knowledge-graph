package com.uka.knowledge.service.impl;

import com.uka.knowledge.service.DocumentService;
import com.uka.knowledge.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步文档处理服务
 * <p>
 * 将异步方法放在单独的类中，避免同类调用导致@Async失效的问题
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Slf4j
@Service
public class AsyncService {

    private final DocumentService documentService;

    private final OcrService ocrService;

    /**
     * 使用@Lazy解决循环依赖问题
     */
    public AsyncService(@Lazy DocumentService documentService, @Lazy OcrService ocrService) {
        this.documentService = documentService;
        this.ocrService = ocrService;
    }

    /**
     * 异步解析文档
     *
     * @param documentId 文档ID
     */
    @Async
    public void asyncParseDocument(Long documentId) {
        log.info("开始异步解析文档, documentId={}, thread={}", documentId, Thread.currentThread().getName());
        try {
            documentService.parseDocument(documentId);
            log.info("异步解析文档完成, documentId={}", documentId);
        } catch (Exception e) {
            log.error("异步解析文档失败, documentId={}", documentId, e);
        }
    }


    /**
     * 异步进行OCR识别
     *
     * @param recordId 记录ID
     */
    @Async
    public void asyncRecognize(Long recordId) {
        try {
            ocrService.recognizeImage(recordId);
        } catch (Exception e) {
            log.error("异步OCR识别失败, recordId={}", recordId, e);
        }
    }
}
