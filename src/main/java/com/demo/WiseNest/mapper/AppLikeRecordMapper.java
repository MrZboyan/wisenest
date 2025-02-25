package com.demo.WiseNest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.WiseNest.model.entity.AppLikeRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Zangdibo
 * description 针对表【app_like_record】的数据库操作Mapper
 * createDate 2025-02-13 21:12:12
 * Entity com.demo.WiseNest.model.entity.AppLikeRecord
 */
public interface AppLikeRecordMapper extends BaseMapper<AppLikeRecord> {

    /**
     * 批量插入点赞记录
     *
     * @param records 记录
     */
    void customSaveOrUpdateBatch(@Param("list") List<AppLikeRecord> records);
}




