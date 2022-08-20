package socialnetwork.repositories.inmemory

import cats.data.EitherT
import cats.effect.{Concurrent, IO}
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import socialnetwork.model.{User, UserName, UserNotFound}
import socialnetwork.repositories.UserRepository

class UserRepositoryTest extends AsyncFreeSpec with AsyncIOSpec with Matchers{

  def getRepo[F[_]: Concurrent]: F[UserRepository[F]] = UserRepositoryImpl.apply[F]

  "updateUser" - {
    "should add new user if no such user exist in repo" in {
      (for {
        repo <- EitherT.right(getRepo[IO])
        _ <- EitherT.right(repo.updateUser(User(UserName("Alex"), List(UserName("Alice")))))
        user <- repo.findUser(UserName("Alex"))
      } yield user).foldF(
        err => fail(err.toString),
        user => IO( user shouldBe User(UserName("Alex"), List(UserName("Alice"))))
      )
    }

    "should update properties of existing user" in {
      (for {
        repo <- EitherT.right(getRepo[IO])
        _ <- EitherT.right(repo.updateUser(User(UserName("Alex"), List(UserName("Alice")))))
        _ <- EitherT.right(repo.updateUser(User(UserName("Alex"), List(UserName("Alice"), UserName("Bob")))))
        user <- repo.findUser(UserName("Alex"))
      } yield user).foldF(
        err => fail(err.toString),
        user => IO(user shouldBe User(UserName("Alex"), List(UserName("Alice"), UserName("Bob"))))
      )
    }
  }

  "findUser" - {
    "should return UserNotFound error if non-existing user was requested" in {
      (for {
        repo <- EitherT.right(getRepo[IO])
        _ <- EitherT.right(repo.updateUser(User(UserName("Alex"))))
        user <- repo.findUser(UserName("Alice"))
      } yield user).foldF(
        err => IO( err shouldBe UserNotFound(UserName("Alice"))),
        user => fail(s"No user should be returned, however got $user")
      )
    }
  }
}
