package com.videoworkshop.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移集合。
 *
 * 所有表结构变更必须提供显式迁移，禁止依赖 destructive migration。
 *
 * [Migration2To3] 同时暴露 [Migration2To3.schemaStatements]（纯 SQL 语句列表），
 * 供 JVM 单元测试（sqlite-jdbc）直接执行校验，避免在测试中实现庞大的
 * [SupportSQLiteDatabase] 接口。
 */

/** P1-01：新增 projects 与 goods_snapshots 表，为项目制数据模型打基础。 */
object Migration2To3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        schemaStatements.forEach { db.execSQL(it) }
    }

    /** 迁移所需的纯 SQL 语句，与 [migrate] 实际执行内容完全一致。 */
    val schemaStatements: List<String> = listOf(
        // projects 表
        """
        CREATE TABLE IF NOT EXISTS `projects` (
            `id` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `type` TEXT NOT NULL,
            `status` TEXT NOT NULL,
            `goodsSnapshotId` TEXT,
            `targetPlatforms` TEXT NOT NULL,
            `coverAssetId` TEXT,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            `lastOpenedAt` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_projects_updatedAt` ON `projects` (`updatedAt`)",
        "CREATE INDEX IF NOT EXISTS `index_projects_status` ON `projects` (`status`)",
        // goods_snapshots 表
        """
        CREATE TABLE IF NOT EXISTS `goods_snapshots` (
            `id` TEXT NOT NULL,
            `projectId` TEXT NOT NULL,
            `provider` TEXT NOT NULL,
            `externalGoodsId` TEXT,
            `name` TEXT NOT NULL,
            `price` REAL,
            `originalPrice` REAL,
            `commissionRate` REAL,
            `promoUrl` TEXT,
            `imageUrls` TEXT NOT NULL,
            `videoUrls` TEXT NOT NULL,
            `sellingPoints` TEXT NOT NULL,
            `capturedAt` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS `index_goods_snapshots_projectId` ON `goods_snapshots` (`projectId`)"
    )
}

/** 供 [com.videoworkshop.core.database.DatabaseModule] 以 [Migration] 类型注册。 */
val MIGRATION_2_3: Migration = Migration2To3