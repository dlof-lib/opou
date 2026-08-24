package com.OPEN.OU.data.repository

/**
 * هيكل قاعدة بيانات OPOU على Firebase Realtime Database:
 *
 * /users/{uid}                          -> User
 * /posts/{postId}                       -> Post
 * /comments/{postId}/{commentId}        -> Comment
 * /reactions/{postId}/{uid}             -> "LIKE" | "DISLIKE"
 * /teking/{uid}/{tekerId}               -> true       (من يتابعهم المستخدم uid)
 * /tekers/{uid}/{tekingId}              -> true       (من يتابع المستخدم uid)
 * /usernames/{username}                 -> uid         (فهرسة لأسماء المستخدمين الفريدة)
 */
object FirebasePaths {
    const val USERS = "users"
    const val POSTS = "posts"
    const val COMMENTS = "comments"
    const val REACTIONS = "reactions"
    const val TEKING = "teking"
    const val TEKERS = "tekers"
    const val USERNAMES = "usernames"
    /** فهرس معكوس: /userReactions/{uid}/{postId} -> "LIKE"|"DISLIKE"
     *  يُستخدم لعرض تفاعل المستخدم الحالي فورًا في التغذية دون الحاجة لطلب منفصل لكل فقرة. */
    const val USER_REACTIONS = "userReactions"
    /** /blocks/{uid}/{blockedUid} -> true  (من قام uid بحظرهم) */
    const val BLOCKS = "blocks"
    /** /blockedBy/{uid}/{blockerUid} -> true  (فهرس معكوس: من حظر uid) — لتصفية الظهور بسرعة بلا مسح شامل */
    const val BLOCKED_BY = "blockedBy"
    /** /commentLikes/{commentId}/{uid} -> true  (من أعجبه هذا التعليق) */
    const val COMMENT_LIKES = "commentLikes"
    /** فهرس معكوس: /userCommentLikes/{uid}/{commentId} -> true — لعرض حالة إعجاب المستخدم الحالي فوريًا بلا طلب لكل تعليق */
    const val USER_COMMENT_LIKES = "userCommentLikes"
}
