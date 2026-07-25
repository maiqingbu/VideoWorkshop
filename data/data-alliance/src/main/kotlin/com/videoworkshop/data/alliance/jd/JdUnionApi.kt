package com.videoworkshop.data.alliance.jd

import com.videoworkshop.data.alliance.jd.model.JdMediaResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 京东联盟（JD Union）Retrofit 接口。
 *
 * 网关基地址：`https://api.jd.com/`，每个方法对应一个独立路径
 * （如 `jdUnionOpenGoodsMediaQuery`），参数以 JSON Body 形式提交，
 * 其中含系统级参数、业务参数与 `sign`。
 *
 * 资质未申请期间该接口不会被实际调用，仓库层返回 Mock 数据。
 */
interface JdUnionApi {

    /**
     * 查询商品媒体（视频）信息。
     *
     * 对应方法：`jd.union.open.goods.media.query`。
     *
     * @param params 已签名的完整请求参数（系统级 + 业务级 + sign）。
     */
    @POST("jdUnionOpenGoodsMediaQuery")
    suspend fun queryMedia(@Body params: Map<String, String>): JdMediaResponse
}
