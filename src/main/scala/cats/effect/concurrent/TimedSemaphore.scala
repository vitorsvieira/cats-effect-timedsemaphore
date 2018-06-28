package cats.effect.concurrent


import cats.effect._
import cats.implicits._

import scala.concurrent.duration._

/**
  * A specialized semaphore implementation that provides a number of permits in a given time frame.
  *
  * In order to obtain a permit, the [[acquire]] and [[acquireN]] are still used, however, there is an additional
  * timing dimension: there is no release method for freeing a permit.
  *
  * TimedSemaphore automatically releases all permits at the end of a configurable time frame.
  *
  * If a thread calls [[acquire]] and the available permits are already exhausted for this time frame, the caller is blocked.
  * When the time frame ends all permits requested so far are restored, and blocking callers are waked up again, so that
  * they can try to acquire a new permit. This basically means that in the specified time frame only the given
  * number of operations is possible.
  *
  * A use case for [[TimedSemaphore]] is to artificially limit the load produced by a process.
  * As an example consider an application that issues database queries on a production system in a background process
  * to gather statistical information. This background processing should not produce so much database load
  * that the functionality and the performance of the production system are impacted.
  *
  * Here a TimedSemaphore could be installed to guarantee that only a given number of database queries are issued per second.
  *
  * Another use case, similar to the above, is to RateLimit/Throttle a given set of endpoints in an web application.
  */
abstract class TimedSemaphore[F[_]] {

  /**
   * Returns the number of permits currently available. Always non-negative.
   *
   * May be out of date the instant after it is retrieved.
   * Use `[[tryAcquire]]` or `[[tryAcquireN]]` if you wish to attempt an
   * acquire, returning immediately if the current count is not high enough
   * to satisfy the request.
   */
  def available: F[Long]

  /**
   * Obtains a snapshot of the current count. May be negative.
   *
   * Like [[available]] when permits are available but returns the number of permits
   * callers are waiting for when there are no permits available.
   */
  def count: F[Long]

  /**
   * Acquires `n` permits.
   *
   * The returned effect semantically blocks until all requested permits are
   * available. Note that acquires are statisfied in strict FIFO order, so given
   * `s: Semaphore[F]` with 2 permits available, an `acquireN(3)` will
   * always be satisfied before a later call to `acquireN(1)`.
   *
   * @param n number of permits to acquire - must be >= 0
   */
  def acquireN(n: Long): F[Unit]

  /** Acquires a single permit. Alias for `[[acquireN]](1)`. */
  def acquire: F[Unit] = acquireN(1)

  /**
   * Acquires `n` permits now and returns `true`, or returns `false` immediately. Error if `n < 0`.
   *
   * @param n number of permits to acquire - must be >= 0
   */
  def tryAcquireN(n: Long): F[Boolean]

  /** Alias for `[[tryAcquireN]](1)`. */
  def tryAcquire: F[Boolean] = tryAcquireN(1)

  /**
   * Returns an effect that acquires a permit, runs the supplied effect, and then releases the permit.
   */
  def withPermit[A](t: F[A]): F[A]
}

object TimedSemaphore {


  private final def setReleaseTimer[F[_]](
    n:         Long,
    duration:  FiniteDuration,
    semaphore: Semaphore[F]
  )(implicit F: Concurrent[F], T: Timer[F]): F[Unit] = {

    F.uncancelable(T.sleep(duration)) *>
      F.delay(println(s"[${Thread.currentThread().getName}] Releasing $n")) *>
      semaphore.releaseN(n) >>
      setReleaseTimer(n, duration, semaphore)
  }

  /**
    * Creates a new `TimedSemaphore`, initialized with `n` available permits between a given `duration`.
    * At the end of each scheduled `duration` the permits will be automatically released.
    */
  def apply[F[_]](
    n:        Long,
    duration: FiniteDuration
  )(implicit F: Concurrent[F], T: Timer[F]): F[TimedSemaphore[F]] = {

    for {
      s  <- Semaphore.apply(n)
      _  <- F.start(setReleaseTimer(n, duration, s))
      ts <- F.delay {
        new TimedSemaphore[F]{

          override def available: F[Long] = s.available

          override def count: F[Long] = s.count

          override def acquireN(n: Long): F[Unit] = s.acquireN(n)

          override def tryAcquireN(n: Long): F[Boolean] = s.tryAcquireN(n)

          override def withPermit[A](t: F[A]): F[A] = s.withPermit(t)
        }
      }
    } yield ts
  }

}