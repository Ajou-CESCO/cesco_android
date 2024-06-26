package com.whdaud.pillinTimeAndroid.domain.repository

import com.whdaud.pillinTimeAndroid.data.remote.dto.RelationDTO
import com.whdaud.pillinTimeAndroid.data.remote.dto.request.RelationReqRequest
import com.whdaud.pillinTimeAndroid.data.remote.dto.response.RelationReqResponse
import com.whdaud.pillinTimeAndroid.data.remote.dto.response.base.BaseResponse

interface RelationRepository {
    suspend fun postRelationRequest(relationReqRequest: RelationReqRequest): Result<BaseResponse<Any>>
    suspend fun getRelations(): Result<BaseResponse<List<RelationDTO>>>
    suspend fun postRelation(requestId: Int): Result<BaseResponse<Any>>
    suspend fun getRelationRequest(): Result<BaseResponse<List<RelationReqResponse>>>
    suspend fun deleteRelation(relationId: Int): Result<BaseResponse<Any>>
}