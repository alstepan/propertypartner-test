package socialnetwork.repositories.inmemory

import cats.implicits._
import cats.effect.{Concurrent, IO}
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import socialnetwork.model.{Post, UserName}
import socialnetwork.repositories.PostRepository

import scala.concurrent.duration.DurationInt

class PostRepositoryTest extends AsyncFreeSpec with AsyncIOSpec with Matchers {
  def getRepo[F[_]: Concurrent]: F[PostRepository[F]] = PostRepositoryImpl.apply[F]

  val posts: List[Post] = List(
    Post(1.seconds, UserName("Alex"), "hello world"),
    Post(2.seconds, UserName("Alex"), "hello world"),
    Post(3.seconds, UserName("Alex"), "hello world again and again"),
    Post(1.seconds, UserName("Alice"), "hello world from Alice"),
    Post(3.seconds, UserName("Alice"), "hello world from Alice again"),
    Post(1.seconds, UserName("Bob"), "hello world from Bob"),
    Post(2.seconds, UserName("Bob"), "hello world from Bob and Alice")
  )

  "addPost" - {
    "should add all posts to the repo" in {
        (for {
          repo <- getRepo[IO]
          _ <- posts.map(p => repo.addPost(p)).sequence
          postStream <- repo.getPosts(UserName("Alex"), UserName("Alice"), UserName("Bob"))
          received <- postStream.compile.toList
        } yield received).asserting(p => p should contain theSameElementsAs posts)
    }
  }

  "getPosts" - {
    "should filter posts by user" in {
      (for {
        repo <- getRepo[IO]
        _ <- posts.map(p => repo.addPost(p)).sequence
        postStream <- repo.getPosts(UserName("Bob"), UserName("Alice"))
        received <- postStream.compile.toList
      } yield received)
        .asserting(p => p should contain theSameElementsAs posts.filterNot(_.user.name == "Alex"))
    }

    "should return posts in reversed historical order" in {
      (for {
        repo <- getRepo[IO]
        _ <- posts.map(p => repo.addPost(p)).sequence
        postStream <- repo.getPosts(UserName("Bob"), UserName("Alice"))
        received <- postStream.compile.toList
      } yield received)
        .asserting(p => p should contain theSameElementsInOrderAs
          posts.sorted((p1: Post, p2: Post) => p2.time.compare(p1.time) match {
            case 0 => p1.user.name.compare(p2.user.name)
            case c => c
          } ).filterNot(_.user.name == "Alex"))
    }

    "should return empty list for unknown user" in {
      (for {
        repo <- getRepo[IO]
        _ <- posts.map(p => repo.addPost(p)).sequence
        postStream <- repo.getPosts(UserName("Ivan"))
        received <- postStream.compile.toList
      } yield received).asserting(p => p shouldBe empty)
    }
  }

}
