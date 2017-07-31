/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import play.api.libs.json._
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.CreationAndLastModifiedDetail

import scala.collection.Set

case class Id(id: String)

object Id {

  import play.api.libs.json.{Format, Reads, Writes}

  implicit def stringToId(s: String): Id = new Id(s)

  private val idWrite: Writes[Id] = new Writes[Id] {
    override def writes(value: Id): JsValue = JsString(value.id)
  }

  private val idRead: Reads[Id] = new Reads[Id] {
    override def reads(js: JsValue): JsResult[Id] = js match {
      case v: JsString => v.validate[String].map(Id.apply)
      case noParsed => throw new Exception(s"Could not read Json value of 'id' in $noParsed")
    }
  }
  implicit val idFormats = Format(idRead, idWrite)
}


case class Cache(id: Id, data: Option[JsValue] = None,
                 modifiedDetails: CreationAndLastModifiedDetail = CreationAndLastModifiedDetail(),
                 atomicId: Option[BSONObjectID] = None) extends Cacheable {
}

object Cache {

  import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

  final val DataAttributeName = "data"

  implicit val format = ReactiveMongoFormats.objectIdFormats
  implicit val cacheFormat = Json.format[Cache]

  val mongoFormats = ReactiveMongoFormats.mongoEntity {

    implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats
    cacheFormat
  }
}


trait Cacheable {

  val id: Id
  val data: Option[JsValue]
  val modifiedDetails: CreationAndLastModifiedDetail

  def dataKeys(): Option[Set[String]] = data.map(js => js.as[JsObject].keys)
}