package effectful.examples.effects.net


trait HttpClient[E[_]] {
  import HttpClient._

  def get(request: Request) : E[Response]
  def patch(request: Request, body: Body) : E[Response]
  def post(request: Request, body: Body) : E[Response]
  def put(request: Request, body: Body) : E[Response]
  def delete(request: Request) : E[Response]
  def head(request: Request) : E[Response]
  def options(request: Request) : E[Response]
}

object HttpClient {
  case class Cookie(
    domain : String,
    name : Option[String],
    value : Option[String],
    path : String,
    expires : Option[Long],
    maxAge : Option[Int],
    secure : Boolean
  )

  case class Response(
    headers: Map[String,String],
    status : Int,
    statusText : String,
    cookies : Seq[Cookie],
    body : String
  )

  sealed trait Body
  case object EmptyBody extends Body
  case class FileBody(file: java.io.File) extends Body
  case class InMemoryBody(bytes: Array[Byte]) extends Body

  case class Request(
    url: String,
    body: Body,
    headers: Map[String,Seq[String]] = Map.empty,
    queryString: Map[String,Seq[String]] = Map.empty,
  //  proxyServer : Option[ProxyServer] = None,
  //  calc: Option[SignatureCalculator] = None
  //  auth: Option[(String,String,AuthScheme)] = None
    followRedirects: Option[Boolean] = None,
    requestTimeout : Option[Int] = None
  //  virtualHost: Option[String] = None,
  )
}