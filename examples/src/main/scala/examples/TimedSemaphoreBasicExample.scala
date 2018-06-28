package cats.effect.concurrent.examples

import cats.effect.{Concurrent, ExitCode, IO, IOApp, Timer}
import cats.effect.concurrent.TimedSemaphore
import cats.Parallel
import cats.implicits._
import scala.concurrent.duration._

/**
  * This example is an adaptation of the Semaphore example in cats-effect documentation.
  *
  * https://typelevel.org/cats-effect/concurrency/semaphore.html
  */
object TimedSemaphoreBasicExample extends IOApp {

  implicit val par: Parallel[IO, IO] = Parallel[IO, IO.Par].asInstanceOf[Parallel[IO, IO]]

  val program =
    for {
      s  <- TimedSemaphore[IO](2, 5.seconds)
      r0 = new PreciousResource[IO]("R0", s)
      r1 = new PreciousResource[IO]("R1", s)
      r2 = new PreciousResource[IO]("R2", s)
      r3 = new PreciousResource[IO]("R3", s)
      r4 = new PreciousResource[IO]("R4", s)
      r5 = new PreciousResource[IO]("R5", s)
      r6 = new PreciousResource[IO]("R6", s)
      r7 = new PreciousResource[IO]("R7", s)
      r8 = new PreciousResource[IO]("R8", s)
      r9 = new PreciousResource[IO]("R9", s)
      _  <- List(
        r0.use,
        r1.use,
        r2.use,
        r3.use,
        r4.use,
        r5.use,
        r6.use,
        r7.use,
        r8.use,
        r9.use
      ).parSequence.void
    } yield ()



  override def run(args: List[String]): IO[ExitCode] = {
    program.as(ExitCode.Success)
  }
}


class PreciousResource[F[_]](name: String, s: TimedSemaphore[F])(implicit F: Concurrent[F], T: Timer[F]) {

  def use: F[Unit] =
    for {
      x <- s.available
      _ <- F.delay(println(s"[${Thread.currentThread().getName}][$name] Checking | Current slots: $x"))
      _ <- s.acquire
      y <- s.available
      _ <- F.delay(println(s"[${Thread.currentThread().getName}][$name] Started  | Current slots: $y"))
      _ <- T.sleep(10.seconds)
      z <- s.available
      _ <- F.delay(println(s"[${Thread.currentThread().getName}][$name] Done     | Current slots: $z"))
    } yield ()

}

