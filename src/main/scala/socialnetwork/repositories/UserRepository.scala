package socialnetwork.repositories

import cats.data.EitherT
import cats.effect.Concurrent
import socialnetwork.model._

trait UserRepository[F[_]] {
  def findUser(name: UserName): EitherT[F, SocialError, User]
  def updateUser(newUser: User): F[Unit]
}

object UserRepository {
  def inMemory[F[_] : Concurrent]: F[UserRepository[F]] = inmemory.UserRepositoryImpl[F]
}
