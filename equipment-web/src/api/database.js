import request from '@/utils/request'

// 执行备份
export const backupDB = () => request({
    url: '/system/db/backup',
    method: 'post'
})

// 执行恢复 (注意：使用 params 会将参数拼接到 URL 后，解决之前的 RequestParam 报错)
export const restoreDB = (fileName) => request({
    url: '/system/db/restore',
    method: 'post',
    params: {
        fileName
    }
})

// 获取备份文件列表 
export const getBackupFiles = () => request({
    url: '/system/db/files',
    method: 'get'
})

// 获取数据库备份配置信息（包含存储路径）
export const getBackupConfig = () => request({
    url: '/system/db/config',
    method: 'get'
})