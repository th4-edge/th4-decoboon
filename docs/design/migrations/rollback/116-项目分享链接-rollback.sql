-- 116 回滚：项目分享链接
-- 注意：直接删表会让所有已发出的亲友观看链接立即失效（意料之中），
-- 同时丢失「谁在什么时候生成过观看链接」的审计事实。
DROP TABLE IF EXISTS cust_project_share_link;
