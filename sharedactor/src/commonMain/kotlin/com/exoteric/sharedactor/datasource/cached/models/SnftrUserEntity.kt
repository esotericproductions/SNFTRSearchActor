package com.exoteric.sharedactor.datasource.cached.models

//goes to SnftrUserDb
data class SnftrUserEntity(
    val uid: String,
    val name: String,
    val username: String,
    val profilePic: String,
    val backgroundPic: String,
    val favsTime: Long,
    val cAttsTime: Long,
    val email: String,
    val profilesBlob: String,
    val temperature: String,
    val pressure: String,
    val scoresBlob: String
) {
    companion object {

        fun fromMap(map: Map<String, Any>): SnftrUserEntity {
            return SnftrUserEntity(
                name = map["name"] as String,
                profilePic = map["profilePic"] as String,
                backgroundPic = map["backgroundPic"] as String,
                profilesBlob = map["profilesBlob"] as String,
                username = map["username"] as String,
                temperature = map["temperature"] as String,
                pressure = map["pressure"] as String,
                scoresBlob = map["scoresBlob"] as String,
                uid = map["uid"] as String,
                email = map["email"] as String,
                favsTime = (map["favoriteTime"] as? Long) ?: 0L, // Providing default value if null
                cAttsTime = (map["cAttsTime"] as? Long) ?: 0L // Providing default value if null
            )
        }
    }
}

