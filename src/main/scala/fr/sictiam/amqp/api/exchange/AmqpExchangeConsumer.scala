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

import akka.Done
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

abstract class AmqpExchangeConsumer(val exchangeName: String, val exchangeType: ExchangeTypes.ExchangeTypeVal, val serviceName: String)(implicit val system: ActorSystem, val materializer: ActorMaterializer, val ec: ExecutionContext) extends AmqpGenericConsumer with Exchange {

  override def init: Unit = {}

  override lazy val amqpSource = AmqpSource.atMostOnceSource(TemporaryQueueSourceSettings(connectionProvider, exchangeName).withDeclaration(exchangeDeclaration), bufferSize = prefetchCount)


  /**
    * Consumes a fixed number of messages from the queue/exchange
    *
    * @param nbMsgToTake the number of messages to consume from the queue
    * @return a future collection of messages
    */
  override def consume(nbMsgToTake: Long = 1, noReply: Boolean = false): Future[Done] = amqpSource.take(nbMsgToTake).map(msg => onMessage(msg)).runWith(Sink.ignore)
}
