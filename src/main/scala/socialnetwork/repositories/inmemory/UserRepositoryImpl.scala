package socialnetwork.repositories.inmemory

import cats.data.EitherT
import cats.implicits._
import cats.effect.{Concurrent, Ref}
import socialnetwork.model.{SocialError, User, UserName, UserNotFound}
import socialnetwork.repositories.UserRepository

class UserRepositoryImpl[F[_]: Concurrent](users: Ref[F, Map[UserName, User]]) extends UserRepository[F] {

  override def findUser(name: UserName): EitherT[F, SocialError, User] =
   EitherT.fromOptionF(users.get.map(_.get(name)), UserNotFound(name))

  override def updateUser(newUser: User): F[Unit] =
    users.update(u => u + (newUser.name -> newUser))
}

object UserRepositoryImpl {
  def apply[F[_]: Concurrent]: F[UserRepository[F]] =
    for {
      users <- Ref.of[F, Map[UserName, User]](Map())
      impl = new UserRepositoryImpl[F](users)
    } yield impl
}
