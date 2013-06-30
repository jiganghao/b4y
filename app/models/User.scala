package models

import _root_.util.{DbUtil, DbId}
import beans.BeanProperty
import java.util.Date
import play.api.Play.current
import org.codehaus.jackson.annotate.JsonProperty
import play.modules.mongodb.jackson.MongoDB
import models.User.UserItem
import scala.collection.JavaConverters._
import java.util
import net.vz.mongodb.jackson.{DBUpdate, JacksonDBCollection}
import models.UserWithProductItems.UserItemWithProductItem

class User(@BeanProperty @JsonProperty var firstName: String,
           @BeanProperty @JsonProperty var lastName: String,
           @BeanProperty @JsonProperty var email: String,
           @BeanProperty @JsonProperty var password: String,
           @BeanProperty @JsonProperty var accountStatus: Int,
           @BeanProperty @JsonProperty var userItems: java.util.List[UserItem]
            ) extends DbId {
  def this() = this(firstName = "", lastName = "", email = "", password = "", accountStatus = 0,
                    new java.util.ArrayList[UserItem]())

  //todo: if user item of same product already exists, append user item
  def addItem(userItem: UserItem){
    if (null == userItems)
      userItems = new java.util.ArrayList[UserItem]()
    userItems.add(userItem)
  }

//  def hasItem = CollectionUtils.isNotEmpty(this.userItems)
  def hasItem = this.userItems != null && !this.userItems.isEmpty
}

object User {
  val DbFieldEmail = "email"
  val DbFieldAccountStatus = "accountStatus"
  val AccountStatusPending = 0
  val AccountStatusActive = 1
  private lazy val db = MongoDB.collection("user", classOf[User], classOf[String])
  private val dbDelegate = db.asInstanceOf[JacksonDBCollection[AnyRef,  String]]
  def load(id: String) = DbUtil.load(dbDelegate, id).asInstanceOf[User]
  def findAll() = DbUtil.findAll(dbDelegate).asInstanceOf[List[User]]
  def findByField(field: String, value: String) = DbUtil.findOneByField(dbDelegate, field, value).asInstanceOf[User]
  def isFieldValueInDb(field: String, value: String) = DbUtil.isFieldValueInDb(dbDelegate, field, value)
  def save(user: User) = DbUtil.save(dbDelegate, user).asInstanceOf[User]
  def delete(id: String) = db.removeById(id)
  def activateAccount(userId:String) = db.updateById(userId, DBUpdate.set(DbFieldAccountStatus, AccountStatusActive))

  class UserItem(@BeanProperty @JsonProperty var itemId: String,
                 @BeanProperty @JsonProperty var orderDate: Date,
                 @BeanProperty @JsonProperty var priceOriginal: Int,
                 @BeanProperty @JsonProperty var priceExpected: Int
                  ) {
    def this() = this(itemId = "", orderDate = null, priceOriginal = 0, priceExpected = 0)
  }
}

class UserWithProductItems(@BeanProperty @JsonProperty var userItemsWithProductItem: java.util.ArrayList[UserItemWithProductItem])
  extends User {
  def this() =  this(new  java.util.ArrayList[UserItemWithProductItem]())

  def this(user: User) = {
    this()
    this.id = user.id
    this.firstName = user.firstName
    this.lastName = user.lastName
    this.email = user.email
    this.password = user.password
    this.userItems = user.userItems
     val userItemsWithProductItems = user.userItems.asScala.map(userItem => {
      val userItemWithProductItem = new UserItemWithProductItem(ProductItem.load(userItem.itemId))
       userItemWithProductItem.itemId = userItem.itemId
       userItemWithProductItem.orderDate = userItem.orderDate
       userItemWithProductItem.priceOriginal = userItem.priceOriginal
       userItemWithProductItem.priceExpected = userItem.priceExpected
       userItemWithProductItem
    }).asJava
    this.userItemsWithProductItem = new util.ArrayList[UserItemWithProductItem]()
    this.userItemsWithProductItem.addAll(userItemsWithProductItems)
  }
}

object UserWithProductItems {
  class UserItemWithProductItem(@BeanProperty @JsonProperty var item: ProductItem) extends UserItem

}