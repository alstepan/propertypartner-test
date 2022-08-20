package socialnetwork

import cats.effect.{ExitCode, IO, IOApp}
import socialnetwork.repositories.{PostRepository, UserRepository}
import socialnetwork.services.{Console, SocialNetwork}

object Application extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    (for {
      userRepo <- UserRepository.inMemory[IO]
      postRepo <- PostRepository.inMemory[IO]
      socialNetwork <- SocialNetwork.apply[IO](userRepo, postRepo)
      console <- Console.apply[IO](socialNetwork)
      _ <- console.usage
      _ <- console.userInputLoop
    } yield ()).as(ExitCode.Success)
}
