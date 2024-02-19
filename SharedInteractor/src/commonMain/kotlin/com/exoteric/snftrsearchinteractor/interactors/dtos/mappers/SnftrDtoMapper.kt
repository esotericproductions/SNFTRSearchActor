package com.exoteric.snftrsearchinteractor.interactors.dtos.mappers

import com.exoteric.snftrsearchinteractor.datasource.cached.models.SnftrEntity
import com.exoteric.snftrsearchinteractor.domain.util.DomainMapper
import com.exoteric.snftrsearchinteractor.interactors.dtos.SnftrDto

class SnftrDtoMapper : DomainMapper<SnftrEntity, SnftrDto> {

    override fun mapToDomainModel(model: SnftrEntity): SnftrDto {
        return SnftrDto(
            id = model.hashCode(),
            title = model.title,
            creator = model.creator,
            creatorProfilePic = model.creatorProfilePic,
            provider = model.provider,
            sourceUrlDetail = model.sourceUrl_detail,
            sourceUrlThumb = model.sourceUrl_thumb,
            sourceUrlOwner = model.sourceUrl_owner,
            posterUid = "",
            currentQuery = model.current_query,
            randomSeed = 0,
            thymestamp = model.thymeStamp,
            page = model.page,
            latestProfilePic = "",
            caller = model.caller
        )
    }

    override fun mapFromDomainModel(domainModel: SnftrDto): SnftrEntity {
        return SnftrEntity(
            id = domainModel.id.toLong(),
            title = domainModel.title,
            creator = domainModel.creator,
            creatorProfilePic = domainModel.creatorProfilePic,
            provider = domainModel.provider,
            sourceUrl_detail = domainModel.sourceUrlDetail,
            sourceUrl_thumb = domainModel.sourceUrlThumb,
            sourceUrl_owner = domainModel.sourceUrlOwner,
            current_query = domainModel.currentQuery,
            thymeStamp = domainModel.thymestamp,
            page = domainModel.page,
            caller = domainModel.caller
        )
    }

    fun toDomainList(initial: List<SnftrEntity>?): List<SnftrDto>? {
        return initial?.map { mapToDomainModel(it) }
    }


    fun toDomainNestedList(initial: List<List<SnftrEntity>>?): List<List<SnftrDto>>? {
        return initial?.map { it.map { inList -> mapToDomainModel(inList) } }
    }

    fun fromDomainList(initial: List<SnftrDto>? ): List<SnftrEntity>? {
        return initial?.map { mapFromDomainModel(it) }
    }
}