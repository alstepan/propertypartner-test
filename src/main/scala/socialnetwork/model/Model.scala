package socialnetwork.model

import scala.concurrent.duration.Duration

case class UserName(name: String) extends AnyVal

case class Post(time: Duration, user: UserName, text: String)

case class User(name: UserName, follows: List[UserName] = List())

sealed trait SocialError
case class UserNotFound(userName: UserName) extends SocialError
