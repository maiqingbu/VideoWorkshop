package com.videoworkshop.core.database.migration

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * P1-01 数据库迁移（v2 → v3）单元测试。
 *
 * 使用 sqlite-jdbc 在 JVM 上复现 v2 架构，执行 [Migration2To3.schemaStatements] 后校验：
 * - 旧表（goods / materials / tasks / drafts）仍存在且数据完好。
 * - 新表（projects / goods_snapshots）已创建且列结构正确。
 * - 新表索引已建立。
 * - 迁移可重复执行（幂等）。
 *
 * 该测试不依赖 Android 设备，可在 `testDebugUnitTest` 中运行。
 */
class Migration2To3Test {

    private lateinit var connection: Connection

    @Before
    fun setUp() {
        Class.forName("org.sqlite.JDBC")
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
        createV2Schema(connection)
    }

    @After
    fun tearDown() {
        connection.close()
    }

    @Test
    fun `createV2Schema_seedData_shouldPreserveOldData`() {
        insertOldData(connection)

        applyMigration()

        assertTrue("旧表 goods 应保留", tableExists("goods"))
        assertTrue("旧表 materials 应保留", tableExists("materials"))
        assertTrue("旧表 tasks 应保留", tableExists("tasks"))
        assertTrue("旧表 drafts 应保留", tableExists("drafts"))

        assertEquals(1, countRows("goods"))
        assertEquals(1, countRows("materials"))
        assertEquals(1, countRows("tasks"))
        assertEquals(1, countRows("drafts"))
    }

    @Test
    fun `migration_shouldCreateProjectsTableWithExpectedColumns`() {
        applyMigration()

        assertTrue("projects 表应创建", tableExists("projects"))

        val expectedColumns = listOf(
            "id", "title", "type", "status", "goodsSnapshotId",
            "targetPlatforms", "coverAssetId", "createdAt", "updatedAt", "lastOpenedAt"
        )
        assertEquals(expectedColumns, columnNames("projects"))
    }

    @Test
    fun `migration_shouldCreateGoodsSnapshotsTableWithExpectedColumns`() {
        applyMigration()

        assertTrue("goods_snapshots 表应创建", tableExists("goods_snapshots"))

        val expectedColumns = listOf(
            "id", "projectId", "provider", "externalGoodsId", "name",
            "price", "originalPrice", "commissionRate", "promoUrl",
            "imageUrls", "videoUrls", "sellingPoints", "capturedAt"
        )
        assertEquals(expectedColumns, columnNames("goods_snapshots"))
    }

    @Test
    fun `migration_shouldCreateIndexesOnNewTables`() {
        applyMigration()

        assertTrue(indexExists("index_projects_updatedAt"))
        assertTrue(indexExists("index_projects_status"))
        assertTrue(indexExists("index_goods_snapshots_projectId"))
    }

    @Test
    fun `migration_shouldBeIdempotent`() {
        insertOldData(connection)
        applyMigration()
        applyMigration()

        assertTrue(tableExists("projects"))
        assertTrue(tableExists("goods_snapshots"))
        assertEquals(1, countRows("goods"))
    }

    @Test
    fun `migration_shouldAllowInsertingProjectAndSnapshot`() {
        applyMigration()

        val projectId = "00000000-0000-0000-0000-000000000001"
        val snapshotId = "00000000-0000-0000-0000-000000000002"

        connection.createStatement().use { stmt ->
            stmt.executeUpdate(
                """
                INSERT INTO projects
                    (id, title, type, status, goodsSnapshotId, targetPlatforms,
                     coverAssetId, createdAt, updatedAt, lastOpenedAt)
                VALUES
                    ('$projectId', '测试项目', 'VIDEO_COMMERCE', 'DRAFT', NULL,
                     '["DOUYIN"]', NULL, 1000, 1000, 1000)
                """.trimIndent()
            )
            stmt.executeUpdate(
                """
                INSERT INTO goods_snapshots
                    (id, projectId, provider, externalGoodsId, name,
                     price, originalPrice, commissionRate, promoUrl,
                     imageUrls, videoUrls, sellingPoints, capturedAt)
                VALUES
                    ('$snapshotId', '$projectId', 'TAOBAO', 'ext-1', '测试商品',
                     19.9, 29.9, 0.3, 'https://promo', '[]', '[]', '["卖点"]', 1000)
                """.trimIndent()
            )
        }

        assertEquals(1, countRows("projects"))
        assertEquals(1, countRows("goods_snapshots"))
    }

    // ===== 帮助方法 =====

    private fun applyMigration() {
        connection.createStatement().use { stmt ->
            Migration2To3.schemaStatements.forEach { stmt.executeUpdate(it) }
        }
    }

    private fun createV2Schema(conn: Connection) {
        conn.createStatement().use { stmt ->
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS `goods` (
                    `id` TEXT NOT NULL,
                    `source` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `price` REAL NOT NULL,
                    `commissionRate` REAL NOT NULL,
                    `promoUrl` TEXT NOT NULL,
                    `imageUrl` TEXT NOT NULL,
                    `videoUrl` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS `materials` (
                    `id` TEXT NOT NULL,
                    `localPath` TEXT NOT NULL,
                    `source` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `thumbnail` TEXT,
                    `tags` TEXT NOT NULL,
                    `note` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS `tasks` (
                    `id` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `inputPath` TEXT NOT NULL,
                    `outputPath` TEXT,
                    `status` TEXT NOT NULL,
                    `progress` INTEGER NOT NULL,
                    `config` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            stmt.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS `drafts` (
                    `id` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `goodsId` TEXT,
                    `content` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS `index_drafts_goodsId` ON `drafts` (`goodsId`)")
        }
    }

    private fun insertOldData(conn: Connection) {
        conn.createStatement().use { stmt ->
            stmt.executeUpdate(
                """
                INSERT INTO goods (id, source, name, price, commissionRate, promoUrl, imageUrl, videoUrl, createdAt)
                VALUES ('g1', 'taobao', '商品A', 10.0, 0.2, 'promo', 'img', NULL, 1000)
                """.trimIndent()
            )
            stmt.executeUpdate(
                """
                INSERT INTO materials (id, localPath, source, type, thumbnail, tags, note, createdAt)
                VALUES ('m1', '/path/a.mp4', 'import', 'video', NULL, '[]', '备注', 1000)
                """.trimIndent()
            )
            stmt.executeUpdate(
                """
                INSERT INTO tasks (id, type, inputPath, outputPath, status, progress, config, createdAt, updatedAt)
                VALUES ('t1', 'dedup', '/in.mp4', NULL, 'pending', 0, '{}', 1000, 1000)
                """.trimIndent()
            )
            stmt.executeUpdate(
                """
                INSERT INTO drafts (id, type, goodsId, content, createdAt, updatedAt)
                VALUES ('d1', 'script', NULL, '正文', 1000, 1000)
                """.trimIndent()
            )
        }
    }

    private fun tableExists(table: String): Boolean {
        val rs = connection.createStatement().executeQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='$table'"
        )
        return rs.next().also { rs.close() }
    }

    private fun indexExists(index: String): Boolean {
        val rs = connection.createStatement().executeQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='$index'"
        )
        return rs.next().also { rs.close() }
    }

    private fun columnNames(table: String): List<String> {
        val rs = connection.createStatement().executeQuery("PRAGMA table_info('$table')")
        return buildList {
            while (rs.next()) {
                add(rs.getString("name"))
            }
        }.also { rs.close() }
    }

    private fun countRows(table: String): Int {
        val rs = connection.createStatement().executeQuery("SELECT COUNT(*) FROM $table")
        return rs.getInt(1).also { rs.close() }
    }
}