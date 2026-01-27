package com.uka.knowledge.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装类
 * <p>
 * 用于封装分页查询的结果数据
 * </p>
 *
 * @param <T> 数据类型
 * @author uka
 * @version 1.0
 */
@Data
@Schema(description = "分页结果")
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码", example = "1")
    private Long current;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", example = "10")
    private Long size;

    /**
     * 总记录数
     */
    @Schema(description = "总记录数", example = "100")
    private Long total;

    /**
     * 总页数
     */
    @Schema(description = "总页数", example = "10")
    private Long pages;

    /**
     * 数据列表
     */
    @Schema(description = "数据列表")
    private List<T> records;

    /**
     * 是否有上一页
     */
    @Schema(description = "是否有上一页")
    private Boolean hasPrevious;

    /**
     * 是否有下一页
     */
    @Schema(description = "是否有下一页")
    private Boolean hasNext;

    /**
     * 私有构造函数
     */
    private PageResult() {
    }

    /**
     * 创建分页结果
     *
     * @param current 当前页码
     * @param size    每页大小
     * @param total   总记录数
     * @param records 数据列表
     * @param <T>     数据类型
     * @return 分页结果对象
     */
    public static <T> PageResult<T> of(Long current, Long size, Long total, List<T> records) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setCurrent(current);
        pageResult.setSize(size);
        pageResult.setTotal(total);
        pageResult.setRecords(records);

        // 计算总页数
        long pages = total % size == 0 ? total / size : total / size + 1;
        pageResult.setPages(pages);

        // 计算是否有上一页和下一页
        pageResult.setHasPrevious(current > 1);
        pageResult.setHasNext(current < pages);

        return pageResult;
    }
}
