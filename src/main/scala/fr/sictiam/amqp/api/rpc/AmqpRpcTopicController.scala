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
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.{AmqpRpcFlow, AmqpSink, AmqpSource}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import fr.sictiam.amqp.api.{AmqpGenericAgent, AmqpMessage, Exchange, ExchangeTypes}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-25
  */

abstract class AmqpRpcTopicController(val exchangeName: String, val serviceName: String)(implicit val system: ActorSystem, val materializer: ActorMaterializer, val ec: ExecutionContext) extends AmqpGenericAgent with Exchange {

  override def init: Unit = {}

  override val exchangeType: ExchangeTypes.ExchangeTypeVal = ExchangeTypes.Topic

  def publish(topic: String, messages: Vector[AmqpMessage]) = {

    // declare a RPC flow with a sink connected to an exchange
    val amqpRpcFlow: Flow[ByteString, ByteString, Future[String]] = AmqpRpcFlow.simple(
      AmqpSinkSettings(connectionProvider)
        .withExchange(exchangeName)
        .withDeclaration(exchangeDeclaration)
        .withRoutingKey(topic),
      1
    )

    // Declare a Sink for processing messages from reply queue
    val resultSink = Sink.foreach[ByteString] { msg => println(msg.utf8String) }

    // Send messages through the flow
    val (_, done: Future[Done]) = Source(messages).map(s => ByteString(s.toString)).viaMat(amqpRpcFlow)(Keep.right).toMat(resultSink)(Keep.both).run

    done.onComplete {
      case Success(_) => logger.info(s"Command processed")
      case Failure(err) => err.printStackTrace()
    }
    done
  }

  def consume(topic: String, nbMsgToTake: Long, noReply: Boolean = false): Future[Done] = {

    val amqpSource = AmqpSource.atMostOnceSource(
      TemporaryQueueSourceSettings(connectionProvider, exchangeName)
        .withDeclaration(exchangeDeclaration)
        .withRoutingKey(topic),
      bufferSize = prefetchCount
    )

    val amqpSink = if (noReply) Sink.ignore else AmqpSink.replyTo(AmqpReplyToSinkSettings(connectionProvider)) // declare a reply to Sink

    amqpSource.map { msg: IncomingMessage => onCommand(msg) }.runWith(amqpSink)

  }

  def onCommand(msg: IncomingMessage): OutgoingMessage

}
