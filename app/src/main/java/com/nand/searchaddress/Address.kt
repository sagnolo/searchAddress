package com.nand.searchaddress

data class Address(
    var results: Results
){
    data class Results(
        var common: Common,
        var juso: ArrayList<Juso>
    ){
        data class Common(
            var totalCount: String,
            var currentPage: String,
            var countPerPage: String,
            var errorCode: String,
            var errorMessage: String
        )

        data class Juso(
            var roadAddr: String,
            var roadAddrPart1: String,
            var roadAddrPart2:  String,
            var jibunAddr: String,
            var engAddr: String,
            var zipNo: String,
            var admCd:String,
            var rnMgtSn:String,
            var bdMgtSn :String,
            var detBdNmList :String,
            var bdNm :String,
            var bdKdcd :String,
            var siNm :String,
            var sggNm :String,
            var emdNm :String,
            var liNm :String,
            var rn :String,
            var udrtYn :String,
            var buldMnnm :String,
            var buldSlno :String,
            var mtYn :String,
            var lnbrMnnm :String,
            var lnbrSlno :String,
            var emdNo :String,
        )
    }
}