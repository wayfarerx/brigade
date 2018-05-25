/*
 * InjectResponseAsyncResponseHandlerWrapper.scala
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

import org.reactivestreams.Publisher

import software.amazon.awssdk.core.async.AsyncResponseHandler

/**
 * An async response handler that wraps another response handler and includes the initial response in the result.
 *
 * @tparam Response The type of response to handle.
 * @tparam Return The type of result to produce.
 * @param wrapped The async response handler that is being wrapped.
 */
private[aws] final class InjectResponseAsyncResponseHandlerWrapper[Response, Return](
  wrapped: AsyncResponseHandler[Response, Return]
) extends AsyncResponseHandler[Response, (Response, Return)] {

  /** The most recent response. */
  @volatile
  private var response: Option[Response] = None

  /* Record the response and defer to the wrapped handler. */
  override def responseReceived(response: Response): Unit = {
    this.response = Some(response)
    wrapped.responseReceived(response)
  }

  /* Defer to the wrapped handler. */
  override def onStream(publisher: Publisher[ByteBuffer]): Unit =
    wrapped.onStream(publisher)

  /* Defer to the wrapped handler. */
  override def exceptionOccurred(throwable: Throwable): Unit =
    wrapped.exceptionOccurred(throwable)

  /* Inject the response into the result. */
  override def complete(): (Response, Return) =
    response.get -> wrapped.complete()

}
