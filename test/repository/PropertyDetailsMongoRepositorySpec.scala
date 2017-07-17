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

package repository

import builders.PropertyDetailsBuilder
import org.mockito.Mockito.{reset, when}
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import reactivemongo.api.DB
import reactivemongo.api.indexes.CollectionIndexesManager
import reactivemongo.json.collection.JSONCollection
import uk.gov.hmrc.mongo.{Awaiting, MongoSpecSupport}

import scala.concurrent.Future

class PropertyDetailsMongoRepositorySpec extends PlaySpec
  with OneServerPerSuite
  with MongoSpecSupport
  with Awaiting
  with MockitoSugar
  with BeforeAndAfterEach {


  def repository(implicit mongo: () => DB) = new PropertyDetailsReactiveMongoRepository


  lazy val propertyDetails1 = PropertyDetailsBuilder.getPropertyDetails("1")
  lazy val propertyDetails2 = PropertyDetailsBuilder.getPropertyDetails("2")
  lazy val propertyDetails3 = PropertyDetailsBuilder.getPropertyDetails("3")

  "Saving stuff in mongo" should {
    "do it" in {
      val created = await(repository.cachePropertyDetails(propertyDetails1))
    }

    "overwrite old object" in {
      await(repository.cachePropertyDetails(propertyDetails1))
      await(repository.fetchPropertyDetails("ated-ref-1")).isEmpty must be (false)
    }
  }

  "Retrieving stuff in mongo" should {
    "not find if something doesn't exist" in {
      await(repository.fetchPropertyDetails("ated-ref-2")).isEmpty must be (true)
    }

    "fetch the correct property details" in {

      await(repository.cachePropertyDetails(propertyDetails1))
      await(repository.cachePropertyDetails(propertyDetails2))

      await(repository.fetchPropertyDetails("ated-ref-1")).isEmpty must be (false)
      await(repository.fetchPropertyDetails("ated-ref-2")).isEmpty must be (false)

    }
  }

  "Retrieving stuff in mongo by property id" should {
    "not find if something doesn't exist" in {
      await(repository.fetchPropertyDetailsById("ated-ref-1", "1")).isEmpty must be (true)
    }

    "fetch the correct property details" in {
      await(repository.cachePropertyDetails(propertyDetails1))

      await(repository.fetchPropertyDetailsById("ated-ref-1", "1")).isEmpty must be (false)
      await(repository.fetchPropertyDetails("ated-ref-2")).isEmpty must be (true)

    }
  }

  "Deleting stuff in mongo" should {

    "delete the correct property details" in {

      await(repository.cachePropertyDetails(propertyDetails1))
      await(repository.cachePropertyDetails(propertyDetails2))

      await(repository.fetchPropertyDetails("ated-ref-1")).isEmpty must be (false)
      await(repository.fetchPropertyDetails("ated-ref-2")).isEmpty must be (false)

      await(repository.deletePropertyDetails("ated-ref-1"))

      await(repository.fetchPropertyDetails("ated-ref-1")).isEmpty must be (true)
      await(repository.fetchPropertyDetails("ated-ref-2")).isEmpty must be (false)

    }
  }

  "deleting chargeable documents in mongo based on property id" in {

    await(repository.cachePropertyDetails(propertyDetails1))
    await(repository.cachePropertyDetails(propertyDetails2))

    await(repository.fetchPropertyDetails("ated-ref-1")).isEmpty must be (false)
    await(repository.fetchPropertyDetails("ated-ref-2")).isEmpty must be (false)

    await(repository.deletePropertyDetailsByfieldName("ated-ref-1", "1"))
    await(repository.deletePropertyDetailsByfieldName("ated-ref-1", "2"))

    await(repository.fetchPropertyDetails("ated-ref-1")).isEmpty must be (true)
    await(repository.fetchPropertyDetails("ated-ref-2")).isEmpty must be (false)

  }

  "updating chargeable documents for an existing property id" in {

    await(repository.cachePropertyDetails(propertyDetails1))
    await(repository.cachePropertyDetails(propertyDetails2))

    await(repository.deletePropertyDetailsByfieldName("ated-ref-1", "1"))

    await(repository.fetchPropertyDetails("ated-ref-1")).isEmpty must be (true)

  }

  val mockCollection = mock[JSONCollection]

  override def beforeEach(): Unit = {
    await(repository.drop)
    reset(mockCollection)
    setupIndexesManager
  }

  private def setupIndexesManager: CollectionIndexesManager = {
    val mockIndexesManager = mock[CollectionIndexesManager]
    when(mockCollection.indexesManager).thenReturn(mockIndexesManager)
    when(mockIndexesManager.dropAll) thenReturn Future.successful(0)
    mockIndexesManager
  }

  class TestPropertyDetailsReactiveMongoRepository extends PropertyDetailsReactiveMongoRepository {
    override lazy val collection = mockCollection
  }

}
