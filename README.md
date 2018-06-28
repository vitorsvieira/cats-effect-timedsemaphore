#### TimedSemaphore 


A specialized semaphore implementation that provides a number of permits in a given time frame.

In order to obtain a permit, the `acquire` and `acquireN` are still used, however, there is an additional
timing dimension: there is no release method for freeing a permit.

TimedSemaphore automatically releases all permits at the end of a configurable time frame.

If a thread calls `acquire` and the available permits are already exhausted for this time frame, the caller is blocked.
When the time frame ends all permits requested so far are restored, and blocking callers are waked up again, so that
they can try to acquire a new permit. This basically means that in the specified time frame only the given
number of operations is possible.

A use case for `TimedSemaphore` is to artificially limit the load produced by a process.
As an example consider an application that issues database queries on a production system in a background process
to gather statistical information. This background processing should not produce so much database load
that the functionality and the performance of the production system are impacted.

Here a TimedSemaphore could be installed to guarantee that only a given number of database queries are issued per second.

Another use case, similar to the above, is to RateLimit/Throttle a given set of endpoints in an web application.


#### Reference

- [cats-effect Semaphore](https://typelevel.org/cats-effect/concurrency/semaphore.html)
- [Apache Commons TimedSemaphore](https://commons.apache.org/proper/commons-lang/javadocs/api-3.1/org/apache/commons/lang3/concurrent/TimedSemaphore.html)
