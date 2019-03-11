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

import akka.actor.{Actor, ActorLogging, PoisonPill}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-03-04
  */

class Ticker extends Actor with ActorLogging {
  override def receive: Receive = {
    case Consume(srvr, topics, nbMsg) ⇒ {
      topics.map(topic => srvr.consume(topic, nbMsg))
    }
    case Kill ⇒ {
      self ! PoisonPill
    }
    case _ ⇒ log.error("Ticker received unknown message")
  }
}

