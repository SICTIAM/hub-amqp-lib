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
import akka.stream.alpakka.amqp.scaladsl.{AmqpRpcFlow, AmqpSink, AmqpSource}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.util.ByteString
import fr.sictiam.amqp.api._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-25
  */

abstract class AmqpRpcTopicServer(val exchangeName: String, val serviceName: String)(implicit val system: ActorSystem, val materializer: ActorMaterializer, val ec: ExecutionContext) extends AmqpGenericRpcServer with Exchange {

  override def init: Unit = {}

  override val exchangeType: ExchangeTypes.ExchangeTypeVal = ExchangeTypes.Topic

  override def publish(topic: String, messages: Vector[AmqpMessage]) = {

    // declare a RPC flow with a sink connected to an exchange
    val amqpRpcFlow: Flow[ByteString, ByteString, Future[String]] = AmqpRpcFlow.simple(
      AmqpSinkSettings(connectionProvider)
        .withExchange(exchangeName)
        .withDeclaration(exchangeDeclaration)
        .withRoutingKey(topic),
      1
    )

    // Declare a Sink for processing messages from reply queue
    val resultSink = Sink.foreach[ByteString] { msg =>
      beforeReply(msg)
      onReply(msg)
      afterReply(msg)
    }

    beforePublish(topic, messages)

    // Send messages through the flow
    val (_, done: Future[Done]) = Source(messages).map { s => ByteString(s.toString) }.viaMat(amqpRpcFlow)(Keep.right).toMat(resultSink)(Keep.both).run

    done.onComplete {
      case Success(_) => afterPublish(topic, messages)
      case Failure(err) => onError(topic, messages, err)
    }

    done
  }

  def consume(topic: String, nbMsgToTake: Long, noReply: Boolean = false): Future[Done] = {

    logger.debug(s"consume $topic, $nbMsgToTake")
    val amqpSource = AmqpSource.atMostOnceSource(
      TemporaryQueueSourceSettings(connectionProvider, exchangeName)
        .withDeclaration(exchangeDeclaration)
        .withRoutingKey(topic),
      bufferSize = prefetchCount
    )
    val amqpSink = if (noReply) Sink.ignore else {
      AmqpSink.replyTo(AmqpReplyToSinkSettings(connectionProvider))
    } // declare a reply to Sink

    amqpSource
      .mapAsync(4) { msg: IncomingMessage => onMessage(msg, topic) }
      .throttle(elements = nbMsgToTake.toInt, per = 1 second, maximumBurst = nbMsgToTake.toInt, mode = ThrottleMode.shaping)
      .runWith(amqpSink)
  }
}
