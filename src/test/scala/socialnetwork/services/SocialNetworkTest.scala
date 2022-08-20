package socialnetwork.services

import cats.data.EitherT
import cats.implicits._
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import cats.effect.{Clock, Concurrent, IO}
import socialnetwork.model.{Post, UserName, UserNotFound}
import socialnetwork.repositories.{PostRepository, UserRepository}

import scala.concurrent.duration._

class SocialNetworkTest extends AsyncFreeSpec with AsyncIOSpec with Matchers {

  def createNetwork[F[_]: Concurrent : Clock]: F[SocialNetwork[F]] =
    for {
      userRepo <- UserRepository.inMemory[F]
      postRepo <- PostRepository.inMemory[F]
      network <- SocialNetwork.apply(userRepo, postRepo)
    } yield network

  def reducePost(p: Post): (String, String) = p.user.name -> p.text

  "post" - {
    "should create new user if user does not exist" in {
      (for {
        network <- EitherT.right(createNetwork[IO])
        _ <- EitherT.right(network.post(UserName("Alice"), "Hello, world!"))
        post <- network.read(UserName("Alice"))
      } yield post.compile.toList)
        .foldF(
          err => fail(s"Expected post from Alice but got an error: $err"),
          p => p.asserting(lp => lp.map(reducePost) shouldEqual List("Alice" -> "Hello, world!"))
        )
    }

    "should add new post to existing user" in {
      (for {
        network <- EitherT.right(createNetwork[IO])
        _ <- EitherT.right(network.post(UserName("Alice"), "Hello, world!") <* IO.sleep(5.milliseconds))
        _ <- EitherT.right(network.post(UserName("Alice"), "Hello, world again!"))
        post <- network.read(UserName("Alice"))
      } yield post.compile.toList)
        .foldF(
          err => fail(s"Expected post from Alice but got an error: $err"),
          p => p.asserting(lp =>
            lp.map(reducePost)  should contain theSameElementsAs List("Alice" -> "Hello, world!", "Alice" -> "Hello, world again!")
          )
        )
    }
  }

  "read" - {
    "should return error when unknown user posts are requested" in {
      (for {
        network <- EitherT.right(createNetwork[IO])
        post <- network.read(UserName("Alice"))
      } yield post.compile.toList)
        .foldF(
          err => IO( err shouldBe UserNotFound(UserName("Alice")) ),
          p => fail(s"Expected unknown user error but got $p")
        )
    }

    "should return all user's posts in reversed chronological order" in {
      (for {
        network <- EitherT.right(createNetwork[IO])
        _ <- EitherT.right(network.post(UserName("Alice"), "Hello, world!") <* IO.sleep(5.milliseconds))
        _ <- EitherT.right(network.post(UserName("Alex"), "Hello, world from Alex") <* IO.sleep(5.milliseconds))
        _ <- EitherT.right(network.post(UserName("Alice"), "Hello, world again!") <* IO.sleep(5.milliseconds))
        post <- network.read(UserName("Alice"))
      } yield post.compile.toList)
        .foldF(
          err => fail(s"Expected posts from Alice but got an error: $err"),
          p => p.asserting(lp =>
            lp.map(reducePost) should contain theSameElementsInOrderAs  List("Alice" -> "Hello, world again!", "Alice" -> "Hello, world!")
          )
        )
    }
  }

  "wall" - {
    "should return error when unknown user wall is requested" in {
      (for {
        network <- EitherT.right(createNetwork[IO])
        post <- network.wall(UserName("Alice"))
      } yield post.compile.toList)
        .foldF(
          err => IO(err shouldBe UserNotFound(UserName("Alice"))),
          p => fail(s"Expected unknown user error but got $p")
        )
    }

    "should return all posts from user and from those whom they follow" in {
      (for {
        network <- EitherT.right(createNetwork[IO])
        _ <- EitherT.right(network.post(UserName("Alice"), "Hello, world!") <* IO.sleep(5.milliseconds))
        _ <- EitherT.right(network.post(UserName("Alex"), "Hello, world from Alex") <* IO.sleep(5.milliseconds))
        _ <- EitherT.right(network.post(UserName("Alice"), "Hello, world again!") <* IO.sleep(5.milliseconds))
        _ <- EitherT.right(network.post(UserName("Bob"), "Hello from Bob!") <* IO.sleep(5.milliseconds))
        _ <- EitherT.right(network.post(UserName("Charlie"), "Hello from Charlie!") <* IO.sleep(5.milliseconds))
        _ <- network.follow(UserName("Bob"), UserName("Alice"))
        _ <- network.follow(UserName("Bob"), UserName("Charlie"))
        post <- network.wall(UserName("Bob"))
      } yield post.compile.toList)
        .foldF(
          err => fail(s"Expected posts from Alice but got an error: $err"),
          p => p.asserting(lp =>
            lp.map(reducePost) should contain theSameElementsInOrderAs
              List(
                "Charlie" -> "Hello from Charlie!",
                "Bob" -> "Hello from Bob!",
                "Alice" -> "Hello, world again!",
                "Alice" -> "Hello, world!"
              )
          )
        )
    }
  }

  "follow" - {
    "should return unknown user error for unknown follower" in {
      (for {
        network <- EitherT.right(createNetwork[IO])
        _ <- EitherT.right(network.post(UserName("Bob"), "Hello, world!"))
        _ <- network.follow(UserName("Alice"), UserName("Bob"))
      } yield())
        .foldF(
          err => IO(err shouldBe UserNotFound(UserName("Alice"))),
          p => fail(s"Expected unknown user error but got $p")
        )
    }

    "should return unknown user error for unknown leader" in {
      (for {
        network <- EitherT.right(createNetwork[IO])
        _ <- EitherT.right(network.post(UserName("Alice"), "Hello, world!"))
        _ <- network.follow(UserName("Alice"), UserName("Bob"))
      } yield ())
        .foldF(
          err => IO(err shouldBe UserNotFound(UserName("Bob"))),
          p => fail(s"Expected unknown user error but got $p")
        )
    }
  }

}
