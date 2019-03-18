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
import akka.stream.alpakka.amqp.scaladsl.AmqpRpcFlow
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import fr.sictiam.amqp.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-25
  */

class AmqpRpcTopicProducer(val exchangeName: String, val serviceName: String)(implicit val system: ActorSystem, val materializer: ActorMaterializer, val ec: ExecutionContext) extends AmqpGenericRpcProducer {

  override def init: Unit = {}

  val exchangeType: ExchangeTypes.ExchangeTypeVal = ExchangeTypes.Topic
  lazy val exchangeDeclaration = ExchangeDeclaration(exchangeName, exchangeType.label).withDurable(AmqpClientConfiguration.durable)

  override def publish(topic: String, messages: Vector[AmqpMessage]) = {
    /*

        val amqpRpcFlow = AmqpRpcFlow.committableFlow(
          AmqpSinkSettings(connectionProvider)
            .withRoutingKey(queueName)
            .withDeclaration(queueDeclaration),
          bufferSize = 10
        )
        val (rpcQueueF, probe) = Source(input).map(s => ByteString(s)).map(bytes => OutgoingMessage(bytes, false, false)).viaMat(amqpRpcFlow)(Keep.right).mapAsync(1)(cm => cm.ack().map(_ => cm.message)).toMat(TestSink.probe)(Keep.both).run

        val amqpSink = AmqpSink.replyTo(AmqpReplyToSinkSettings(connectionProvider))

        val amqpSource = AmqpSource.atMostOnceSource(
          NamedQueueSourceSettings(connectionProvider, queueName),
          bufferSize = 1
        )
        amqpSource
          .map(b => OutgoingMessage(b.bytes, false, false).withProperties(b.properties))
          .runWith(amqpSink)

    */

    // declare a RPC flow with a sink connected to an exchange
    val amqpRpcFlow = AmqpRpcFlow.committableFlow(
      AmqpSinkSettings(connectionProvider)
        .withExchange(exchangeName)
        .withDeclaration(exchangeDeclaration)
        .withRoutingKey(topic),
      bufferSize = 10
    )
    //
    //    val amqpRpcFlow: Flow[ByteString, ByteString, Future[String]] = AmqpRpcFlow.simple(
    //      AmqpSinkSettings(connectionProvider)
    //        .withExchange(exchangeName)
    //        .withDeclaration(exchangeDeclaration)
    //        .withRoutingKey(topic),
    //      1
    //    )

    // Declare a Sink for processing messages from reply queue
    val resultSink = Sink.foreach[IncomingMessage] { msg =>
      beforeReply(msg.bytes)
      onReply(msg.bytes)
      afterReply(msg.bytes)
    }
    //   // Declare a Sink for processing messages from reply queue
    //    val resultSink = Sink.foreach[ByteString] { msg =>
    //      beforeReply(msg)
    //      onReply(msg)
    //      afterReply(msg)
    //    }

    beforePublish(topic, messages)

    // Send messages through the flow
    val (_, done: Future[Done]) = Source(messages)
      .map(s => ByteString(s.toString))
      .map(bytes => OutgoingMessage(bytes, false, false))
      .viaMat(amqpRpcFlow)(Keep.right)
      .mapAsync(1)(cm => cm.ack().map(_ => cm.message))
      .toMat(resultSink)(Keep.both).run

    //    val (_, done: Future[Done]) = Source(messages)
    //      .map { s => ByteString(s.toString) }
    //      .viaMat(amqpRpcFlow)(Keep.right)
    //      .mapAsync(1)(cm => cm.ack().map(_ => cm.message.bytes))
    //      .toMat(resultSink)(Keep.both).run

    done.onComplete {
      case Success(_) => afterPublish(topic, messages)
      case Failure(err) => onError(topic, messages, err)
    }

    done
  }

}
