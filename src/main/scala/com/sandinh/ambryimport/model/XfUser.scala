package com.sandinh.ambryimport.model

case class XfUser(userId: Int, username: String, email: String,
                  gender: String, //'', 'male', 'female'
                  customTitle: String, languageId: Int, styleId: Int, timezone: String, visible: Byte,
                  activityVisible: Byte, userGroup_id: Int,
                  secondaryGroupIds: Array[Byte],
                  displayStyleGroupId: Int, permissionCombinationId: Int,
                  messageCount: Int, conversationsUnread: Short, registerDate: Int, lastActivity: Int,
                  trophyPoints: Int, alertsUnread: Short,
                  avatarDate: Int, avatarWidth: Short, avatarHeight: Short, gravatar: String,
                  userState: String, //'valid', 'email_confirm', 'email_confirm_edit', 'moderated', 'email_bounce'
                  isModerator: Byte, isAdmin: Byte, isBanned: Byte,
                  likeCount: Int, warningPoints: Int, isStaff: Byte,
                  ambry: String)

case class AmbryAvatar(l: String, m: String, s: String)
object AmbryAvatar {
  def apply(ambry: List[(String, String)]): AmbryAvatar = {
    val a = ambry.toMap
    AmbryAvatar(a("l"), a("m"), a("s"))
  }

  import play.api.libs.json._
  implicit val writer = Json.writes[AmbryAvatar]
}
