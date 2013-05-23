package controllers

import play.api._
import i18n.Messages
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models._
import templates.Html
import scala.collection.JavaConverters._
import java.util.Date

object Application extends Controller {
  val SessionNameIsLoggedIn = "IsLoggedIn"
  val SessionNameUserId = "UserId"
  val SessionValueIsLoggedIn = "true"
  def index = Action { implicit request => {
    val isLoggedIn = session.get(SessionNameIsLoggedIn)
    if (isLoggedIn.isDefined && isLoggedIn.get.equalsIgnoreCase(SessionValueIsLoggedIn))
          Redirect(routes.Application.items())
    else
      Redirect(routes.Application.signUp())
  }
//    Ok(views.html.index("Your new application is ready."))
  }

  def tasks = Action {
    Ok(views.html.index(Task.all(), taskForm))
  }

  def newTask = Action { implicit request =>
    taskForm.bindFromRequest.fold(
      errors => BadRequest(views.html.index(Task.all(), errors)),
      label => {
        Task.create(label)
        Redirect(routes.Application.tasks)
      }
    )
  }

  def deleteTask(id: String) = Action {
    Task.delete(id)
    Redirect(routes.Application.tasks)
  }

  val taskForm = Form(
    "label" -> nonEmptyText
  )



  def test = Action {
    Status(488)("Strange response type")
  }

  def main = Action {
    Ok(views.html.main(title = "jim")(content = Html("<h1> a test</h1>")))
  }

  def prodSearch = Action {
    Ok(views.html.productSearch("ProdSearch", prodSearchForm))
  }
  val prodSearchForm = Form(
    "prodSearchWord" -> nonEmptyText
  )

  val testClient = new TestClient
  def prodSearchResult = Action { implicit request =>
    prodSearchForm.bindFromRequest.fold(
      errors => BadRequest(views.html.index(Task.all(), errors)),
      prodSearchWord => {
        val itemsAws = testClient.runSearch(prodSearchWord)
        val items = itemsAws.asScala.map(ProductItem.convertProductItemFromAwsItem(_)).toList
//        val items = List(ProductItem.convertProductItemFromAwsItemAAA())

        Ok(views.html.productSearchResult("prodlist", items))
      }
    )
  }

  def selectItem(name: String, asin:String, price: String,  img: String) = Action {
    Ok(views.html.selectItem(name, asin, price, img, itemOrderForm))
  }

  def saveUserItem() = Action {implicit request =>{
    val data = itemOrderForm.bindFromRequest.data
    val (name, asin, img, currentPrice, newPrice) = (data.get("name").get,
      data.get("asin")get,
      data.get("img")get,
      data.get("price").get,
      data.get("newPrice").get)
      val itemSaved = ProductItem.save(name, asin, img, currentPrice, newPrice)
      val userIdOption = session.get(SessionNameUserId)
        val userId = userIdOption.get
      val userItem = UserItem("", userId, itemSaved.id, new Date, currentPrice, newPrice)
      val userItemSaved = UserItem.save(userItem)
      Redirect(routes.Application.items)
    }
  }
  val itemOrderForm = Form(tuple(
    "name" -> nonEmptyText,
    "asin" -> nonEmptyText,
    "img" -> nonEmptyText,
    "priceOriginal" -> nonEmptyText,
    "newPrice" -> nonEmptyText)
  )

  def items = Action {implicit request =>{
    val userId = session.get(SessionNameUserId).get
    val userItems = UserItem.findAll(userId)
    Ok(views.html.items(ProductItem.findByItemIds(userItems.map(_.itemId))))
  }
  }

  def deleteItem(id: String) = Action {
    UserItem.delete(id)
    Redirect(routes.Application.items)
  }



  def buyItem(id: String) = Action {
    val item = ProductItem.findProductItemById(id)
    val purchaseUrl = testClient.addToCart(item.asin, 1)
//    item.geta
    Redirect(purchaseUrl)
  }

  def signUp(email: String, password: String) = Action {
    val user = User(null, null, null, email, password)
    User.save(user)
    Redirect(routes.Application.items)
  }

  def start = Action {
    Ok(views.html.login(null))
  }
  val signUpForm = Form(tuple(
    "firstName" -> nonEmptyText,
    "lastName" -> nonEmptyText,
    "email" -> nonEmptyText,
    "password" -> nonEmptyText)
  )
  def signUp() = Action {implicit request =>{
    signUpForm.discardingErrors
    val data = signUpForm.bindFromRequest.data
    val (firstName, lastName, email, password) = (
      data.get("firstName").get,
      data.get("lastName")get,
      data.get("email")get,
      data.get("password").get)
    if (User.emailExisted(email)){
//      Redirect(routes.Application.signUp).flashing(Flash(signUpForm.data) +
//        ("error" -> Messages("contact.validation.errors")))
//      BadRequest(views.html.login(signUpForm))
      BadRequest(views.html.login(
        signUpForm.fill("firstName", "lastName", "email", "password")
          .withGlobalError("Email " + email + " already registered")))
//
//        InternalServerError(views.html.login())
    }
    else {
      val user = User("", firstName, lastName, email, password)
      User.save(user)
      Redirect(routes.Application.prodSearch)
        .withSession(session + (SessionNameIsLoggedIn -> SessionValueIsLoggedIn))
        .withSession(session + (SessionNameUserId -> user.id))
    }
   }
  }

  def signIn() = Action {implicit request =>{
    signUpForm.discardingErrors
    val data = signUpForm.bindFromRequest.data
    val (email, password) = (
      data.get("email")get,
      data.get("password").get)
    if (!User.emailExisted(email)){
      //      Redirect(routes.Application.signUp).flashing(Flash(signUpForm.data) +
      //        ("error" -> Messages("contact.validation.errors")))
      //      BadRequest(views.html.login(signUpForm))
      BadRequest(views.html.login(
        signUpForm.fill("firstName", "lastName", "email", "password")
          .withGlobalError("Email " + email + " is not registered")))
    }
    else {
      val user = User.findByEmail(email)
      if (user.password.equalsIgnoreCase(password))
        Redirect(routes.Application.items())
          .withSession(session + (SessionNameIsLoggedIn -> SessionValueIsLoggedIn))
          .withSession(session + (SessionNameUserId -> user.id))
      else
        BadRequest(views.html.login(
          signUpForm.fill("firstName", "lastName", "email", "password")
            .withGlobalError("invalid password for email  " + email)))
    }
  }
  }

  def signOut = Action { implicit request => {
      Redirect(routes.Application.signUp()).withNewSession
  }
    //    Ok(views.html.index("Your new application is ready."))
  }
}