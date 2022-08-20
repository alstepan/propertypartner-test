package socialnetwork.services

import cats.effect.{Clock, Sync}
import cats.implicits._
import fs2._
import socialnetwork.model.{Post, SocialError, UserName, UserNotFound}

import scala.concurrent.duration.Duration
import scala.io.StdIn.readLine
import scala.util.matching.Regex

trait Console[F[_]] {
  def usage: F[Unit]
  def userInputLoop: F[String]
}

object Console {

  def apply[F[_]: Sync: Clock](socialNetwork: SocialNetwork[F]): F[Console[F]] =
    Sync[F].delay(new ConsoleImpl[F](socialNetwork))

  private class ConsoleImpl[F[_]: Sync: Clock](network: SocialNetwork[F]) extends Console[F] {

    override def usage: F[Unit] =
      println(
        """Welcome to the social network!
          |
          |Available commands:
          |To post message type <user name> -> <message text>
          |To read user's messages type <user name>
          |To follow type <follower user name> follows <user name>
          |To read user's wall type <user name> wall
          |To quit the network type :quit
          |To read this help again type :help
          |""".stripMargin).pure[F]

    override def userInputLoop: F[String] =
      (for {
        cmd <- userInput
        _ <- processInput(cmd)
      } yield cmd).iterateUntil(_ == ":quit")

    def userInput: F[String] = Sync[F].delay(print("> ")) *> Sync[F].delay(readLine)

    val postCommand: Regex = """^(\w+)\s*->\s*(.*)$""".r
    val followCommand: Regex = """^(\w+)\s+follows\s+(\w+)$""".r
    val wallCommand: Regex = """^(\w+)\s+wall\s*$""".r
    val readCommand: Regex = """^(\w+)\s*$""".r

    def processInput(cmd: String): F[Unit] =
      cmd match {
        case ":quit" => ().pure[F]
        case ":help" | "?" => usage
        case postCommand(user, text) => network.post(UserName(user), text)
        case followCommand(user1, user2) =>
          network
            .follow(UserName(user1), UserName(user2))
            .foldF(err => handleError(err), r => r.pure[F])
        case wallCommand(user) =>
          network
            .wall(UserName(user))
            .map(printPosts)
            .foldF(err => handleError(err), r => r)
        case readCommand(user) =>
          network
            .read(UserName(user))
            .map(printPosts)
            .foldF(err => handleError(err), r => r)
        case other =>
          println(s"Unknown command $other. Please type :help to see available commands").pure[F]
      }

    def printPosts(posts: Stream[F, Post]): F[Unit] =
      posts.foreach(printPost).compile.drain

    def printPost(p: Post): F[Unit] =
      for {
        time <- Clock[F].realTime
        _ <- Sync[F].delay(println(s"${p.user.name} - ${p.text} (${formatTime(time - p.time)} ago)"))
      } yield ()

    def formatTime(time: Duration): String = {
      val days = time.toDays
      val hours = time.toHours % 24
      val minutes = time.toMinutes % 60
      val seconds = time.toSeconds % 60
      (if (days > 0) s"$days days " else "") +
        (if (hours > 0) s"$hours hours " else "") +
        (if (minutes > 0) s"$minutes minutes " else "") +
        (if (seconds > 0) s"$seconds seconds" else "")
    }

    def handleError(err: SocialError): F[Unit] =
      err match {
        case UserNotFound(name) => Sync[F].delay(println(s"User ${name.name} was not found"))
        case _ => Sync[F].delay(println("Unknown error occurred. Please contact the system administrator."))
      }
  }

}
