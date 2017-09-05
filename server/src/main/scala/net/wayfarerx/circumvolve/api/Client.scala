/*
 * Client.scala
 *
 * Copyright 2017 wayfarerx <x@wayfarerx.net> (@thewayfarerx)
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

package net.wayfarerx.circumvolve.api

import sx.blah.discord.api.{ClientBuilder, IDiscordClient}
import sx.blah.discord.util.DiscordException

trait Client {

}

object Client {

  def apply(token: String, login: Boolean = true): Either[DiscordException, Client] =
    Implementation(token, login)

  def apply(client: IDiscordClient): Client =
    Implementation(client)

  case class Implementation(client: IDiscordClient) extends Client {

    {
      val dispatcher = client.getDispatcher
      dispatcher.registerListener(???)
    }


  }

  object Implementation {

    def apply(token: String, login: Boolean = true): Either[DiscordException, Implementation] = {
      val clientBuilder = new ClientBuilder().withToken(token)
      try {
        if (login) Right(Implementation(clientBuilder.login()))
        else Right(Implementation(clientBuilder.build()))
      } catch {
        case e: DiscordException => Left(e)
      }
    }

  }

}