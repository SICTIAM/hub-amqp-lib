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
package fr.sictiam.amqp.api

import com.typesafe.config.{Config, ConfigFactory}

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-01-30
  */

object AmqpClientConfiguration {

  lazy val conf: Config = Option(ConfigFactory.load().getConfig("amqp")).getOrElse(ConfigFactory.empty())
  lazy val user: String = Option(conf.getString("user")).getOrElse("admin")
  lazy val pwd: String = Option(conf.getString("password")).getOrElse("Pa55w0rd")
  lazy val host: String = Option(conf.getString("broker.host")).getOrElse("localhost")
  lazy val port: Int = Option(conf.getInt("broker.port")).getOrElse(5672)
  lazy val durable: Boolean = Option(conf.getBoolean("durable.messages")).getOrElse(false)
  lazy val fairDispatch: Boolean = Option(conf.getBoolean("qos.fairdispatch")).getOrElse(false)
  lazy val prefetchCount: Int = Option(conf.getInt("qos.prefetch.count")).getOrElse(10)
  lazy val consumeInterval: Int = Option(conf.getInt("consume.interval")).getOrElse(500)
  lazy val automaticRecoveryEnabled: Boolean = Option(conf.getBoolean("automatic.recovery.enabled")).getOrElse(false)
  lazy val topologyRecoveryEnabled: Boolean = Option(conf.getBoolean("topology.recovery.enabled")).getOrElse(false)
  lazy val timeout: Int = Option(conf.getInt("connection.timeout")).getOrElse(10000)
  lazy val exchangeName: String = Option(conf.getString("exchange.name")).getOrElse("default")

}
