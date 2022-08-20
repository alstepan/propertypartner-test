package socialnetwork.repositories

import cats.effect.Concurrent
import fs2._
import socialnetwork.model._

trait PostRepository[F[_]] {
  def addPost(post: Post): F[Unit]
  def getPosts(user: UserName*): F[Stream[F, Post]]
}

object PostRepository {
  def inMemory[F[_] :  Concurrent]: F[PostRepository[F]] = inmemory.PostRepositoryImpl[F]
}
