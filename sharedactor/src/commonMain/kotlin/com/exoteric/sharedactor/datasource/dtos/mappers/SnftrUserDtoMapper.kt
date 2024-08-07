package com.exoteric.sharedactor.datasource.dtos.mappers

import com.exoteric.sharedactor.datasource.cached.models.SnftrUserEntity
import com.exoteric.sharedactor.domain.util.DomainMapper
import com.exoteric.sharedactor.datasource.dtos.ClockUserDto

class SnftrUserDtoMapper : DomainMapper<SnftrUserEntity, ClockUserDto> {
    override fun mapToDomainModel(model: SnftrUserEntity): ClockUserDto {
        return ClockUserDto(
            id = 0,
            uid = model.uid,
            name = model.name,
            email = model.email,
            username = model.username,
            profilePic = model.profilePic,
            backgroundPic = model.backgroundPic,
            favsTime = model.favsTime,
            cAttsTime = model.cAttsTime,
            profilesBlob = model.profilesBlob,
            temperature = model.temperature,
            pressure = model.pressure,
            scoresBlob = model.scoresBlob,
            loggedIn = model.loggedIn
        )
    }

    override fun mapFromDomainModel(domainModel: ClockUserDto): SnftrUserEntity {
        return SnftrUserEntity(
            uid = domainModel.uid,
            name = domainModel.name,
            username = domainModel.username,
            profilePic = domainModel.profilePic,
            backgroundPic = domainModel.backgroundPic,
            email = domainModel.email,
            favsTime = domainModel.favsTime,
            cAttsTime = domainModel.cAttsTime,
            profilesBlob = domainModel.profilesBlob,
            temperature = domainModel.temperature,
            pressure = domainModel.pressure,
            scoresBlob = domainModel.scoresBlob,
            loggedIn = domainModel.loggedIn
        )
    }

    fun toDomainList(initial: List<SnftrUserEntity>?): List<ClockUserDto>? {
        return initial?.map { mapToDomainModel(it) }
    }


    fun toDomainNestedList(initial: List<List<SnftrUserEntity>>?): List<List<ClockUserDto>>? {
        return initial?.map { it.map { inList -> mapToDomainModel(inList) } }
    }

    fun fromDomainList(initial: List<ClockUserDto>? ): List<SnftrUserEntity>? {
        return initial?.map { mapFromDomainModel(it) }
    }
}