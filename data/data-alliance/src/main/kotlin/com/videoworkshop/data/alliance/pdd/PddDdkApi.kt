package com.videoworkshop.data.alliance.pdd

import com.videoworkshop.data.alliance.pdd.model.PddGoodsDetailResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 拼多多联盟（DDK）Retrofit 接口。
 *
 * 网关基地址：`https://gw-api.pinduoduo.com/`，统一入口 `api`，
 * 所有参数（含公共参数与 `sign`）以 JSON Body 形式提交。
 *
 * 资质未申请期间该接口不会被实际调用，仓库层返回 Mock 数据。
 */
interface PddDdkApi {

    /**
     * 获取商品详情（含视频地址）。
     *
     * 对应方法：`pdd.ddk.goods.detail`。
     *
     * @param params 已签名的完整请求参数（公共 + 业务 + sign）。
     */
    @POST("api")
    suspend fun getGoodsDetail(@Body params: Map<String, String>): PddGoodsDetailResponse
}
