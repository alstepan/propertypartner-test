package socialnetwork.services

import cats.data.EitherT
import cats.implicits._
import fs2._
import cats.effect.{Clock, Concurrent}
import socialnetwork.model._
import socialnetwork.repositories.{PostRepository, UserRepository}

trait SocialNetwork[F[_]] {
  def post(userName: UserName, text: String): F[Unit]
  def wall(userName: UserName): EitherT[F, SocialError, Stream[F, Post]]
  def follow(follower: UserName, leader: UserName): EitherT[F, SocialError, Unit]
  def read(userName: UserName): EitherT[F, SocialError, Stream[F, Post]]
}

object SocialNetwork {
  def apply[F[_]: Concurrent: Clock](userRepo: UserRepository[F], postRepo: PostRepository[F]): F[SocialNetwork[F]] =
    new SocialNetworkImpl[F](userRepo, postRepo).asInstanceOf[SocialNetwork[F]].pure[F]

  private class SocialNetworkImpl[F[_]: Concurrent: Clock](userRepo: UserRepository[F], postRepo: PostRepository[F])
    extends SocialNetwork[F] {

    override def post(userName: UserName, text: String): F[Unit] =
      for {
        user <- userRepo.findUser(userName).getOrElse(User(userName))
        time <- Clock[F].realTime
        _ <- userRepo.updateUser(user)
        _ <- postRepo.addPost(Post(time, userName, text))
      } yield ()

    override def wall(userName: UserName): EitherT[F, SocialError, Stream[F, Post]] =
      for {
        user <- userRepo.findUser(userName)
        posts <- EitherT.right(postRepo.getPosts(userName :: user.follows: _*))
      } yield posts

    override def follow(follower: UserName, leader: UserName): EitherT[F, SocialError, Unit] =
      for {
        user1 <- userRepo.findUser(follower)
        user2 <- userRepo.findUser(leader)
        _ <- EitherT.right(userRepo.updateUser(user1.copy(follows =user2.name :: user1.follows)))
      } yield ()

    override def read(userName: UserName): EitherT[F, SocialError, Stream[F, Post]] =
      for {
        _ <- userRepo.findUser(userName)
        posts <- EitherT.right(postRepo.getPosts(userName))
      } yield posts
  }
}
