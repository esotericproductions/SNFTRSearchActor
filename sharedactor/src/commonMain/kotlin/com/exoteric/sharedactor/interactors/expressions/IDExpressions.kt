package com.exoteric.sharedactor.interactors.expressions

import com.exoteric.snftrdblib.cached.SnftrDatabase


// used internally in the CommentsFlowers
// to pass expressions data.
data class SnftrUserExpressions(val fav: Long, val up: Long, val down: Long, val flagged: Long)

/**
 * Used across bkmks, pstrs & cmmts:
 *
 * User expressions (thumbsup/thumbsdown) are stored in their own table in the db.
 * 1) a batch of results are retrieved from
 * Firestore and are transformed (e.g. from FB Document -> SnftrBkmkEntity),
 * put into the db and then emitted & shown on the current screen.
 * 2) expressions are decoupled from entity and DTO objects and use
 * their own table so updates are context-less and do not require modifying
 * the SnftrObjectRt db row.
 */
fun getUserExpressionsForSnftrDto(uuid: String,
                                  userUid: String,
                                  snftrDatabase: SnftrDatabase,
                                  myCallback: (entity: SnftrUserExpressions) -> Unit) {
    val query = snftrDatabase.snftrUserExpressionsQueries
    val expr = query.getCmmtUserExpressionsForUserUid(uuid, userUid).executeAsOneOrNull()
//    if (expr != null) {
//        println("getUserExpressionsForComment(): user has expressions for comment!")
//    }
    myCallback(
        SnftrUserExpressions(
            fav = expr?.isFav ?: 0,
            up = expr?.thumbsups ?: 0,
            down = expr?.thumbsdowns ?: 0,
            flagged = expr?.flagged ?: 0
        )
    )
}