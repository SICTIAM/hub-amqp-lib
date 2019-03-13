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
package fr.sictiam.amqp.api.actors

import akka.actor.{ActorSystem, Cancellable, Props}
import fr.sictiam.amqp.api.AmqpClientConfiguration
import fr.sictiam.amqp.api.rpc.AmqpRpcTopicServer

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-03-01
  */

class Scheduler(val server: AmqpRpcTopicServer, val topics: Set[String], nbMsgToTake: Long)(implicit val system: ActorSystem, val ec: ExecutionContext) {

  val ticker = system.actorOf(Props[Ticker], "ticker")

  var cancellable: Cancellable = _

  def start() = {
    cancellable = system.scheduler.schedule(0 milliseconds, AmqpClientConfiguration.consumeInterval milliseconds) {
      ticker ! Consume(server, topics)
    }
  }

  def stop() = {
    cancellable.cancel()
  }

  def kill() = {
    ticker ! Kill
  }

  //  This cancels further Ticks to be sent
  sys addShutdownHook (stop())
}
