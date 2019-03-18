/*
 *  Copyright (C) 2019 SICTIAM
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package fr.sictiam.amqp.rpc.tasks

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp.{IncomingMessage, OutgoingMessage}
import akka.util.ByteString
import fr.sictiam.amqp.api.rpc.AmqpRpcTask

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-03-12
  */

class FakeReadTask(override val topic: String, override val exchangeName: String)(implicit override val system: ActorSystem, override val materializer: ActorMaterializer, override val ec: ExecutionContext) extends AmqpRpcTask {

  //  def process(parameter: JsValue)(implicit ec: ExecutionContext): Future[JsValue] = {
  //    println("read task/" + Json.stringify(parameter))
  //    Future(JsString("Awesome processing"))(ec)
  //  }

  override def onMessage(msg: IncomingMessage, params: String*)(implicit ec: ExecutionContext): Future[OutgoingMessage] = {
    println("Read task/" + msg.bytes.utf8String)
    Future(OutgoingMessage(ByteString(s"Reponse received from $serviceName"), false, false).withProperties(msg.properties))(ec)
  }
}

