package com.exoteric.sharedactor.datasource.cached.models

import com.exoteric.sharedactor.domain.util.DomainMapper
import com.exoteric.sharedactor.interactors.dtos.SnftrDto

class SnftrEntityMapper: DomainMapper<SnftrEntity, SnftrDto> {

    override fun mapToDomainModel(model: SnftrEntity): SnftrDto {
        return SnftrDto(
            id = model.id.toInt(),
            title = model.title,
            creator = model.creator,
            creatorProfilePic = model.creatorProfilePic,
            provider = model.provider,
            sourceUrlDetail = model.sourceUrl_detail,
            sourceUrlThumb = model.sourceUrl_thumb,
            sourceUrlOwner = model.sourceUrl_owner,
            posterUid = "",
            latestProfilePic = "",
            currentQuery = model.current_query,
            randomSeed = 0, // the generation is done in SearchSnftrs when the going
                            // into the cache; this can be 0 here since before assignment
            thymestamp = model.thymeStamp,
            page = model.page,
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

    fun convertPypsListToString(pyps: List<String>): String {
        val pypsString = StringBuilder()
        for(pyp in pyps){
            pypsString.append("$pyp,")
        }
        return pypsString.toString()
    }

    fun convertPypsToList(pypsString: String?): List<String>{
        val list: ArrayList<String> = ArrayList()
        pypsString?.let {
            for(pyp in it.split(",")){
                list.add(pyp)
            }
        }
        return list
    }

    fun fromEntityList(initial: List<SnftrEntity>?): List<SnftrDto>? {
        return initial?.map { mapToDomainModel(it) }
    }

    fun toEntityList(initial: List<SnftrDto>?): List<SnftrEntity>? {
        return initial?.map { mapFromDomainModel(it) }
    }


}