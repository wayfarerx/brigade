package net.wayfarerx.aws

import java.nio.ByteBuffer

import org.reactivestreams.Publisher
import software.amazon.awssdk.core.async.AsyncResponseHandler

final class InjectResponseAsyncResponseHandlerWrapper[Response, Return](
  wrapped: AsyncResponseHandler[Response, Return]
) extends AsyncResponseHandler[Response, (Response, Return)] {

  @volatile
  private var response: Option[Response] = null

  override def responseReceived(response: Response): Unit = {
    this.response = Some(response)
    wrapped.responseReceived(response)
  }

  override def onStream(publisher: Publisher[ByteBuffer]): Unit =
    wrapped.onStream(publisher)

  override def exceptionOccurred(throwable: Throwable): Unit =
    wrapped.exceptionOccurred(throwable)

  override def complete(): (Response, Return) =
    response.get -> wrapped.complete()

}
