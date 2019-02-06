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

/**
  * Created by Nicolas DELAFORGE (nicolas.delaforge@mnemotix.com).
  * Date: 2019-02-06
  */

object ExchangeTypes {

  sealed abstract class ExchangeTypeVal(val label: String)

  case object Direct extends ExchangeTypeVal("direct")

  case object Topic extends ExchangeTypeVal("topic")

  case object Headers extends ExchangeTypeVal("headers")

  case object Fanout extends ExchangeTypeVal("fanout")

  val values = Seq(Direct, Topic, Headers, Fanout)

}
