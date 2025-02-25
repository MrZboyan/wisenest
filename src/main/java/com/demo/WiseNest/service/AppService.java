package com.demo.WiseNest.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.WiseNest.model.dto.app.AppQueryRequest;
import com.demo.WiseNest.model.entity.App;
import com.demo.WiseNest.model.vo.AppVO;
import jakarta.servlet.http.HttpServletRequest;


/**
 * 应用服务
 *
 *
 *  
 */
public interface AppService extends IService<App> {

    /**
     * 校验数据
     * @param add 对创建的数据进行校验
     */
    void validApp(App app, boolean add);

    /**
     * 获取查询条件
     */
    QueryWrapper<App> getQueryWrapper(AppQueryRequest appQueryRequest);
    
    /**
     * 获取应用封装
     */
    AppVO getAppVO(App app, HttpServletRequest request);

    /**
     * 分页获取应用封装
     */
    Page<AppVO> getAppVOPage(Page<App> appPage, HttpServletRequest request);
}
