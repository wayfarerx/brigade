/*
 * SingleByteArrayAsyncRequestProvider.scala
 *
 * Copyright 2018 wayfarerx <x@wayfarerx.net> (@thewayfarerx)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.wayfarerx.aws

import java.nio.ByteBuffer

import org.reactivestreams.{Subscriber, Subscription}
import software.amazon.awssdk.core.async.AsyncRequestProvider

/**
 * Implementation of an async request provider derived from a single byte array.
 *
 * @param bytes The byte array to derive an async request provider from.
 */
final class SingleByteArrayAsyncRequestProvider(bytes: Array[Byte]) extends AsyncRequestProvider {

  /* Return the length of the array. */
  override def contentLength: Long = bytes.length

  /* Subscribe a subscriber to this request provider. */
  override def subscribe(subscriber: Subscriber[_ >: ByteBuffer]): Unit =
    subscriber.onSubscribe(new Subscription() {
      override def request(n: Long): Unit = if (n > 0) {
        subscriber.onNext(ByteBuffer.wrap(bytes))
        subscriber.onComplete()
      }

      override def cancel(): Unit = ()
    })

}