package com.videoworkshop.data.alliance

/**
 * 联盟平台路由键。
 *
 * 与 domain 层 [com.videoworkshop.domain.model.AllianceProvider] 完全一致，
 * 这里通过 typealias 直接复用 domain 枚举（TAOBAO / JD / PDD），作为数据层
 * 把请求分发到各 provider（淘宝 TOP / 京东联盟 / 拼多多 DDK）的路由依据。
 *
 * 使用 typealias 而非重新声明枚举，可避免 domain 与 data 两侧重复定义，
 * 同时保证 [AllianceRepositoryImpl] 实现的 `GoodsRepository` 方法签名与
 * domain 接口严格一致。
 */
typealias AllianceProvider = com.videoworkshop.domain.model.AllianceProvider
