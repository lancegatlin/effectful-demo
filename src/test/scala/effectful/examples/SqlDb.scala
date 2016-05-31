package effectful.examples

import com.mchange.v2.c3p0.ComboPooledDataSource

object SqlDb {

  val pool = new ComboPooledDataSource()
  pool.setDriverClass("org.h2.Driver")
  pool.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
  pool.setUser("")
  pool.setPassword("")
  pool.setMinPoolSize(5)
  pool.setAcquireIncrement(5)
  pool.setMaxPoolSize(20)

  // init
  {
    val connect = pool.getConnection
    def run(sql: String) : Unit = {
      val statement = connect.createStatement()
      println(s"Running...\n$sql\n")
      val result = statement.execute(sql)
      println(s"Result = $result")
      statement.close()
    }

    run(
  """
  CREATE TABLE Users(
    id varchar(256) primary key,
    username varchar(256) NOT NULL,
    password_digest varchar(512) NOT NULL,
    created timestamp NOT NULL,
    last_updated timestamp NOT NULL,
    removed timestamp
  )
  """
    )

    run(
  """
  CREATE TABLE Tokens(
    id varchar(256) primary key,
    user_id varchar(256) NOT NULL,
    device_id varchar(256),
    last_validated timestamp NOT NULL,
    expires_on timestamp NOT NULL,
    created timestamp NOT NULL,
    last_updated timestamp NOT NULL,
    removed timestamp
  )
  """
    )

    val uuid = "ad0421f3-9d7e-48ce-9cac-11a7f9c3d2dd"
    val now = "2016-01-01 00:00:00"

    run(
  s"""
  INSERT INTO Users (id,username,password_digest,created,last_updated,removed) VALUES (
    '$uuid','test','not a digest','$now','$now',NULL
  )
  """
    )

    val rs = connect.createStatement().executeQuery(
      "SELECT * FROM Users"
    )
    assert(rs.isBeforeFirst == true)
    rs.next()
    assert(rs.getString(1) == uuid)
    rs.close()
    connect.close()
  }

  // todo: use in-memory h2
  // todo: generate schema
  // todo: initialize with schema
}