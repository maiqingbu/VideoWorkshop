package com.videoworkshop.data.alliance.taobao

import com.videoworkshop.data.alliance.taobao.model.TaobaoItemDetailResponse
import retrofit2.http.POST
import retrofit2.http.QueryMap

/**
 * 淘宝联盟（TOP）Retrofit 接口。
 *
 * 网关基地址：`https://eco.taobao.com/`，统一入口 `router/rest`，
 * 所有参数（含系统级参数与 `sign`）通过 Query 传递。
 *
 * 资质未申请期间该接口不会被实际调用，仓库层返回 Mock 数据。
 */
interface TaobaoTbkApi {

    /**
     * 获取商品详情（含主图视频）。
     *
     * 对应 TOP 方法：`taobao.tbk.item.info.get`。
     *
     * @param params 已签名的完整请求参数（系统级 + 业务级 + sign）。
     */
    @POST("router/rest")
    suspend fun getItemDetails(@QueryMap params: Map<String, String>): TaobaoItemDetailResponse
}
