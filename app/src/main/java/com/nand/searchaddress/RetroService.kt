package com.nand.searchaddress

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface RetroService {

    @GET("addrlink/addrLinkApi.do")
    fun getAddressInfo(
        @Query("confmKey") confmKey: String,
        @Query("currentPage") currentPage: Int,
        @Query("countPerpage") countPerpage: Int,
        @Query("keyword") keyword: String,
        @Query("resultType") resultType: String
    ): Call<Address>
}