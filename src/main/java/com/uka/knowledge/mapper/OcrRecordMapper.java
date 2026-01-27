package com.uka.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.uka.knowledge.model.entity.OcrRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * OCR记录Mapper接口
 * <p>
 * 继承MyBatis-Plus的BaseMapper，提供基础的CRUD操作
 * </p>
 *
 * @author uka
 * @version 1.0
 */
@Mapper
public interface OcrRecordMapper extends BaseMapper<OcrRecord> {

}
