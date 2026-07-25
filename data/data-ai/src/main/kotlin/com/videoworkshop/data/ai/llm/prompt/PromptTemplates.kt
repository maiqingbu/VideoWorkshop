package com.videoworkshop.data.ai.llm.prompt

import com.videoworkshop.domain.model.Goods
import com.videoworkshop.domain.model.ImageTemplate

/**
 * AI 文案生成 Prompt 模板中心（核心）。
 *
 * 涵盖：3 版视频带货口播文案、4 套图文模板、标题 + 话题标签。
 * 所有 Prompt 均强制要求模型以纯 JSON 返回，便于下游稳定解析。
 */
object PromptTemplates {

    // ===== 视频带货文案 =====

    /**
     * 生成 3 版不同风格的视频带货口播文案。
     *
     * 返回格式：`{"copies":[{"title":"","body":"","sellingPoints":["","",""],"tags":["","",""]}]}`
     */
    fun videoCopyPrompt(goodsName: String, price: Double, keywords: String?): String {
        val kw = if (keywords.isNullOrBlank()) "无特定关键词，请结合商品名称自行提炼" else keywords
        return """
            你是一名资深的短视频带货文案编剧，请为以下商品撰写 3 版风格不同的口播文案。

            【商品名称】$goodsName
            【当前价格】¥$price
            【关键词/卖点提示】$kw

            【要求】
            1. 3 版风格分别为：痛点共鸣型、场景种草型、限时促销型；
            2. 每版包含：title（吸引人的标题）、body（口播正文）、sellingPoints（3 条核心卖点）、tags（3-5 个话题标签，不带 #）；
            3. 口播正文控制在 80-150 字，节奏明快，适合 15-30 秒短视频；
            4. 标题要有强钩子，标签要与内容高度相关；
            5. 严格以 JSON 格式返回，禁止输出任何解释文字或 Markdown 代码块标记。

            【返回格式】
            {"copies":[{"title":"","body":"","sellingPoints":["","",""],"tags":["","",""]}]}
        """.trimIndent()
    }

    // ===== 图文文案 =====

    /**
     * 根据商品与图文模板风格生成单条图文文案。
     *
     * 返回格式：`{"title":"","body":"","sellingPoints":["","",""],"tags":["","",""]}`
     */
    fun imageCopyPrompt(goods: Goods, template: ImageTemplate): String {
        val (structure, styleHint) = templateGuide(template)
        val originalPriceLine = goods.originalPrice?.let { "【原价】¥$it" } ?: "【原价】无"
        return """
            你是小红书/抖音图文带货文案专家，请根据商品信息与模板风格生成一条图文文案。

            【商品名称】${goods.name}
            【当前价格】¥${goods.price}
            $originalPriceLine
            【模板风格】${template.displayName}（${template.desc}）
            【内容结构要求】$structure

            【要求】
            1. 包含：title（标题）、body（正文）、sellingPoints（3 条卖点）、tags（3-5 个话题标签，不带 #）；
            2. $styleHint；
            3. 正文 150-300 字，分段清晰，适合图文阅读；
            4. 严格以 JSON 格式返回，禁止输出任何解释文字或 Markdown 代码块标记。

            【返回格式】
            {"title":"","body":"","sellingPoints":["","",""],"tags":["","",""]}
        """.trimIndent()
    }

    // ===== 标题 + 标签 =====

    /**
     * 根据正文内容生成标题与话题标签。
     *
     * 返回格式：`{"title":"","tags":["","","","",""]}`
     */
    fun titleTagPrompt(content: String, goodsName: String): String =
        """
            请根据以下正文内容生成一个吸引人的标题与 5 个话题标签。

            【商品名称】$goodsName
            【正文内容】
            $content

            【要求】
            1. 标题 15-25 字，包含核心卖点与情绪钩子，不要使用「标题：」等前缀；
            2. 标签 5 个，不带 #，与正文内容高度相关；
            3. 严格以 JSON 格式返回，禁止输出任何解释文字或 Markdown 代码块标记。

            【返回格式】
            {"title":"","tags":["","","","",""]}
        """.trimIndent()

    // ===== 模板风格映射 =====

    private fun templateGuide(template: ImageTemplate): Pair<String, String> = when (template) {
        ImageTemplate.GOODS_RECOMMEND ->
            "亮点 → 体验 → 推荐理由" to "第一人称种草口吻，突出单品卖点与使用体验"
        ImageTemplate.REVIEW_SCORE ->
            "评分 → 优点 → 缺点 → 总结" to "客观测评口吻，给出明确评分（如 8.5/10）并对比优劣"
        ImageTemplate.LIST_COLLECTION ->
            "编号 + 名称 + 一句话推荐" to "合集清单口吻，每条简洁有力，适合好物盘点"
        ImageTemplate.FLASH_SALE ->
            "原价 → 现价 → 限时 → 行动号召" to "强促销氛围，突出价格优势与紧迫感，引导下单"
    }
}
