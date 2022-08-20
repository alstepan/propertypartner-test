package socialnetwork.repositories.inmemory

import cats.implicits._
import fs2._
import cats.effect.{Concurrent, Ref}
import socialnetwork.model.{Post, UserName}
import socialnetwork.repositories.PostRepository

import scala.collection.immutable.SortedSet

class PostRepositoryImpl[F[_]: Concurrent](posts: Ref[F, SortedSet[Post]]) extends PostRepository[F]  {
  override def addPost(post: Post): F[Unit] =
    posts.update(p => p + post)

  override def getPosts(users: UserName*): F[Stream[F, Post]] =
    posts
      .get
      .map((_, users.toSet))
      .map(p => p._1.filter(post => p._2.contains(post.user)))
      .map(s => Stream.unfold(s.iterator)(i => if (i.hasNext) Some(i.next, i) else None))
}

object PostRepositoryImpl {

  implicit val ordering: Ordering[Post] = (x: Post, y:Post) =>
    y.time.compareTo(x.time) match {
      case 0 => x.user.name.compare(y.user.name)
      case c => c
    }

  def apply[F[_]: Concurrent]: F[PostRepository[F]] =
    for {
      posts <- Ref.of[F, SortedSet[Post]](SortedSet())
      impl = new PostRepositoryImpl[F](posts)
    } yield impl
}
