# 数据库初始化
-- 创建库
create database if not exists WiseNest;
-- 切换库
use WiseNest;
-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    unionId      varchar(256)                           null comment '微信开放平台id', -- 保留字段
    mpOpenId     varchar(256)                           null comment '公众号openId',   -- 保留字段
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    index idx_unionId (unionId)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 应用表
create table if not exists app
(
    id              bigint auto_increment comment 'id' primary key,
    appName         varchar(128)                       not null comment '应用名',
    appDesc         varchar(2048)                      null comment '应用描述',
    appIcon         varchar(1024)                      null comment '应用图标',
    appType         tinyint  default 0                 not null comment '应用类型（0-得分类，1-测评类）',
    scoringStrategy tinyint  default 0                 not null comment '评分策略（0-自定义，1-AI）',
    reviewStatus    int      default 0                 not null comment '审核状态：0-待审核, 1-通过, 2-拒绝',
    reviewMessage   varchar(512)                       null comment '审核信息',
    reviewerId      bigint                             null comment '审核人 id',
    reviewTime      datetime                           null comment '审核时间',
    userId          bigint                             not null comment '创建用户 id',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint  default 0                 not null comment '是否删除',
    index idx_appName (appName)
) comment '应用' collate = utf8mb4_unicode_ci;

-- 题目表
create table if not exists question
(
    id              bigint auto_increment comment 'id' primary key,
    questionContent text                               null comment '题目内容（json格式）',
    appId           bigint                             not null comment '应用 id',
    userId          bigint                             not null comment '创建用户 id',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint  default 0                 not null comment '是否删除',
    index idx_appId (appId)
) comment '题目' collate = utf8mb4_unicode_ci;

-- 评分结果表
create table if not exists scoring_result
(
    id               bigint auto_increment comment 'id' primary key,
    resultName       varchar(128)                       not null comment '结果名称，如物流师',
    resultDesc       text                               null comment '结果描述',
    resultPicture    varchar(1024)                      null comment '结果图片',
    resultProp       varchar(128)                       null comment '结果属性集合 JSON，如 [I,S,T,J]',
    resultScoreRange int                                null comment '结果得分范围，如 80，表示 80及以上的分数命中此结果',
    appId            bigint                             not null comment '应用 id',
    userId           bigint                             not null comment '创建用户 id',
    createTime       datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime       datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete         tinyint  default 0                 not null comment '是否删除',
    index idx_appId (appId)
) comment '评分结果' collate = utf8mb4_unicode_ci;

-- 用户答题记录表
create table if not exists user_answer
(
    id              bigint auto_increment primary key,
    appId           bigint                             not null comment '应用 id',
    appType         tinyint  default 0                 not null comment '应用类型（0-得分类，1-角色测评类）',
    scoringStrategy tinyint  default 0                 not null comment '评分策略（0-自定义，1-AI）',
    choices         text                               null comment '用户答案（JSON 数组）',
    resultId        bigint                             null comment '评分结果 id',
    resultName      varchar(128)                       null comment '结果名称，如物流师',
    resultDesc      text                               null comment '结果描述',
    resultPicture   varchar(1024)                      null comment '结果图标',
    resultScore     int                                null comment '得分',
    userId          bigint                             not null comment '用户 id',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint  default 0                 not null comment '是否删除',
    index idx_appId (appId),
    index idx_userId (userId)
) comment '用户答题记录' collate = utf8mb4_unicode_ci;

-- 用户答题记录表 0 shardingSphere
create table if not exists user_answer_0
(
    id              bigint auto_increment primary key,
    appId           bigint                             not null comment '应用 id',
    appType         tinyint  default 0                 not null comment '应用类型（0-得分类，1-角色测评类）',
    scoringStrategy tinyint  default 0                 not null comment '评分策略（0-自定义，1-AI）',
    choices         text                               null comment '用户答案（JSON 数组）',
    resultId        bigint                             null comment '评分结果 id',
    resultName      varchar(128)                       null comment '结果名称，如物流师',
    resultDesc      text                               null comment '结果描述',
    resultPicture   varchar(1024)                      null comment '结果图标',
    resultScore     int                                null comment '得分',
    userId          bigint                             not null comment '用户 id',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint  default 0                 not null comment '是否删除',
    index idx_appId (appId),
    index idx_userId (userId)
) comment '用户答题记录 0' collate = utf8mb4_unicode_ci;

-- 用户答题记录表1 shardingSphere
create table if not exists user_answer_1
(
    id              bigint auto_increment primary key,
    appId           bigint                             not null comment '应用 id',
    appType         tinyint  default 0                 not null comment '应用类型（0-得分类，1-角色测评类）',
    scoringStrategy tinyint  default 0                 not null comment '评分策略（0-自定义，1-AI）',
    choices         text                               null comment '用户答案（JSON 数组）',
    resultId        bigint                             null comment '评分结果 id',
    resultName      varchar(128)                       null comment '结果名称，如物流师',
    resultDesc      text                               null comment '结果描述',
    resultPicture   varchar(1024)                      null comment '结果图标',
    resultScore     int                                null comment '得分',
    userId          bigint                             not null comment '用户 id',
    createTime      datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint  default 0                 not null comment '是否删除',
    index idx_appId (appId),
    index idx_userId (userId)
) comment '用户答题记录 1' collate = utf8mb4_unicode_ci;

-- 用户点赞记录表
CREATE TABLE app_like_record
(
    appId      BIGINT NOT NULL COMMENT '应用ID',
    userId     BIGINT NOT NULL COMMENT '用户ID',
    isLike     TINYINT(1) DEFAULT 0 COMMENT '是否取消点赞（0-有效，1-取消）',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (appId,userId),
    UNIQUE (appId,userId),
    INDEX idx_userId_isLike (userId, isLike)
) COMMENT='用户点赞记录' COLLATE=utf8mb4_unicode_ci;

-- 用户点赞记录表
CREATE TABLE app_like_record_0
(
    appId      BIGINT NOT NULL COMMENT '应用ID',
    userId     BIGINT NOT NULL COMMENT '用户ID',
    isLike     TINYINT(1) DEFAULT 0 COMMENT '是否取消点赞（0-有效，1-取消）',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (appId,userId),
    UNIQUE (appId,userId),
    INDEX idx_userId_isLike (userId, isLike)
) comment '用户点赞记录 0' collate = utf8mb4_unicode_ci;

-- 用户点赞记录表
CREATE TABLE app_like_record_1
(
    appId      BIGINT NOT NULL COMMENT '应用ID',
    userId     BIGINT NOT NULL COMMENT '用户ID',
    isLike     TINYINT(1) DEFAULT 0 COMMENT '是否取消点赞（0-有效，1-取消）',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT(1) DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (appId,userId),
    UNIQUE (appId,userId),
    INDEX idx_userId_isLike (userId, isLike)
) comment '用户点赞记录 1' collate = utf8mb4_unicode_ci;

-- 点赞数表
CREATE TABLE app_like_count
(
    appId      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '应用ID',
    likeCount  BIGINT DEFAULT 0 COMMENT '点赞数',
    createTime DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT(1)      DEFAULT 0 COMMENT '是否删除',
    INDEX idx_app_id (appId),
    INDEX idx_like_count (likeCount),
    INDEX idx_appId_likeCount (appId, likeCount)
) comment '应用点赞总数表' collate = utf8mb4_unicode_ci;
