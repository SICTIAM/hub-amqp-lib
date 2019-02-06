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
package fr.sictiam.amqp.api.exchange

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.AmqpSource
import akka.stream.scaladsl.Sink
import fr.sictiam.amqp.api._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-01-30
  */

class AmqpSingleTopicConsumer(val exchangeName: String, val exchangeType: ExchangeTypes.ExchangeTypeVal, val serviceName: String)(implicit val system: ActorSystem, val materializer: ActorMaterializer, val ec: ExecutionContext) extends AmqpGenericAgent with Exchange {

  override def init: Unit = {}

  def consume(topic: String, nbMsgToTake: Long): Future[Seq[AmqpMessage]] = {

    /*
      val exchangeName = "amqp.topic." + System.currentTimeMillis()
      val queueName = "amqp-conn-it-spec-simple-queue-" + System.currentTimeMillis()
      val exchangeDeclaration = ExchangeDeclaration(exchangeName, "topic")
      val queueDeclaration = QueueDeclaration(queueName)
      val bindingDeclaration = BindingDeclaration(queueName, exchangeName).withRoutingKey(getRoutingKey("*"))

      val amqpSink = AmqpSink(
        AmqpSinkSettings(connectionProvider)
          .withExchange(exchangeName)
          .withDeclarations(immutable.Seq(exchangeDeclaration, queueDeclaration, bindingDeclaration))
      )

      val amqpSource = AmqpSource.atMostOnceSource(
        NamedQueueSourceSettings(connectionProvider, queueName)
          .withDeclarations(immutable.Seq(exchangeDeclaration, queueDeclaration, bindingDeclaration)),
        bufferSize = 10
      )
     */

    val amqpSource = AmqpSource.atMostOnceSource(
      TemporaryQueueSourceSettings(connectionProvider, exchangeName)
        .withDeclaration(exchangeDeclaration)
        .withRoutingKey(topic),
      bufferSize = prefetchCount
    )
    val result = amqpSource.take(nbMsgToTake).runWith(Sink.seq)
    result.map(_.map(msg => AmqpMessage(msg.bytes.utf8String)))
  }
}
