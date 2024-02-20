package com.exoteric.sharedactor.interactors.dtos.mappers

import com.exoteric.sharedactor.datasource.cached.models.SnftrUserEntity
import com.exoteric.sharedactor.domain.util.DomainMapper
import com.exoteric.sharedactor.interactors.dtos.SnftrUserDto

class SnftrUserDtoMapper : DomainMapper<SnftrUserEntity, SnftrUserDto> {
    override fun mapToDomainModel(model: SnftrUserEntity): SnftrUserDto {
        return SnftrUserDto(
            id = 0,
            uid = model.uid,
            name = model.name,
            email = model.email,
            username = model.username,
            profilePic = model.profilePic,
            backgroundPic = model.backgroundPic,
            pstrsTime = model.pstrsTime,
            favsTime = model.favsTime,
            cHistTime = model.cHistTime,
            cAttsTime = model.cAttsTime,
            profilesBlob = model.profilesBlob,
            temperature = model.temperature,
            pressure = model.pressure,
            scoresBlob = model.scoresBlob
        )
    }

    override fun mapFromDomainModel(domainModel: SnftrUserDto): SnftrUserEntity {
        return SnftrUserEntity(
            uid = domainModel.uid,
            name = domainModel.name,
            username = domainModel.username,
            profilePic = domainModel.profilePic,
            backgroundPic = domainModel.backgroundPic,
            email = domainModel.email,
            pstrsTime = domainModel.pstrsTime,
            favsTime = domainModel.favsTime,
            cHistTime = domainModel.cHistTime,
            cAttsTime = domainModel.cAttsTime,
            profilesBlob = domainModel.profilesBlob,
            temperature = domainModel.temperature,
            pressure = domainModel.pressure,
            scoresBlob = domainModel.scoresBlob
        )
    }

    fun toDomainList(initial: List<SnftrUserEntity>?): List<SnftrUserDto>? {
        return initial?.map { mapToDomainModel(it) }
    }


    fun toDomainNestedList(initial: List<List<SnftrUserEntity>>?): List<List<SnftrUserDto>>? {
        return initial?.map { it.map { inList -> mapToDomainModel(inList) } }
    }

    fun fromDomainList(initial: List<SnftrUserDto>? ): List<SnftrUserEntity>? {
        return initial?.map { mapFromDomainModel(it) }
    }
}