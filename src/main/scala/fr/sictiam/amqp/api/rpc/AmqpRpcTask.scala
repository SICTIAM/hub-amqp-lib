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
package fr.sictiam.amqp.api.rpc

import akka.Done
import akka.actor.ActorSystem
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.{AmqpSink, AmqpSource}
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, KillSwitches}
import fr.sictiam.amqp.api.{AmqpGenericRpcConsumer, Topic}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-26
  */

abstract class AmqpRpcTask()(implicit val system: ActorSystem, val materializer: ActorMaterializer, val ec: ExecutionContext) extends AmqpGenericRpcConsumer with Topic {

  val topic: String
  val exchangeName: String
  val serviceName: String = s"[$topic] RPC Task"
  lazy val amqpSource = AmqpSource.committableSource(sourceSettings, bufferSize = prefetchCount) // declare a basic consumer
  val killSwitch = KillSwitches.shared(s"serviceName-kill-switch")

  //  lazy val restartSrc = RestartSource.withBackoff(100 milliseconds, AmqpClientConfiguration.consumeInterval milliseconds, 0.2) { () => amqpSource }

  override def init: Unit = {}

  //  def start() = {
  //    val sink = AmqpSink.replyTo(AmqpReplyToSinkSettings(connectionProvider))
  //    restartSrc.mapAsync(4) { cm =>
  //      cm.ack()
  //      onMessage(cm.message, topic)
  //    }.runWith(sink)
  //
  //  }

  def start() = consume()

  def stop() = {
    logger.debug(s"Gracefully closing $serviceName.")
    killSwitch.shutdown()
  }

  def consume(noReply: Boolean = false): Future[Done] = {
    val amqpSink = if (noReply) Sink.ignore else AmqpSink.replyTo(AmqpReplyToSinkSettings(connectionProvider)) // declare a reply to Sink
    amqpSource.
      via(killSwitch.flow)
      .mapAsync(4) { cm =>
      cm.ack()
      onMessage(cm.message, topic)
    }.runWith(amqpSink)
  }

  //  override def onMessage(msg: IncomingMessage, params: String*)(implicit ec: ExecutionContext): Future[OutgoingMessage] = {
  //    process(Json.parse(msg.bytes.utf8String)).map(jsval => OutgoingMessage(ByteString(Json.stringify(jsval)), false, false).withProperties(msg.properties))
  //  }
  //  def process(parameter: JsValue)(implicit ec: ExecutionContext): Future[JsValue]
}
