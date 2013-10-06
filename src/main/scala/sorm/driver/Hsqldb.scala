package sorm.driver

import sorm._, ddl._, jdbc._
import sext._, embrace._
import org.joda.time.DateTime
import sql.Sql

class Hsqldb (protected val connection : JdbcConnection)
  extends DriverConnection
  with StdConnection
  with StdTransaction
  with StdAbstractSqlToSql
  with StdQuote
  with StdSqlRendering
  with StdStatement
  with StdQuery
  with StdModify
  with StdCreateTable
  with StdListTables
  with StdDropTables
  with StdDropAllTables
  with StdNow
{
  override def createTable(table: Table) {
    super.createTable(table)
    table.indexes.view.zipWithIndex.foreach{ case (cols, i) =>
      createIndexDdl(table.name, cols, table.name + "_idx_" + i) $
      (Statement(_)) $
      connection.executeUpdate
    }
  }
  protected def createIndexDdl( table: String, columns: Seq[String], name: String ): String
    = "CREATE INDEX " + quote(name) + " ON " + quote(table) + " (" + columns.view.map(quote).mkString(", ") + ")"
  override protected def indexDdl(columns: Seq[String]) = ""
  override protected def columnDdl(c: Column)
    = quote(c.name) + " " + columnTypeDdl(c.t) +
      c.autoIncrement.option(" GENERATED BY DEFAULT AS IDENTITY").mkString +
      ( if( c.nullable ) " NULL" else " NOT NULL" )
  override protected def quote(x: String) = "\"" + x + "\""
  override protected def template(sql: Sql) = {
    import sorm.sql.Sql._
    sql match {
      case Comparison(Value(l : Boolean), Value(r : Boolean), o) =>
        if ( o == Equal && l == r || o == NotEqual && l != r ) "TRUE"
        else "FALSE"
      case _ =>
        super.template(sql)
    }
  }

  override protected def data(sql: Sql) = {
    import sorm.sql.Sql._
    sql match {
      case Comparison(Value(l : Boolean), Value(r : Boolean), o) =>
        Stream()
      case _ =>
        super.data(sql)
    }
  }
  override protected def showTablesSql
    = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.SYSTEM_TABLES"
  override def now()
    = connection
        .executeQuery(Statement("VALUES(NOW())"))()
        .head.head
        .asInstanceOf[DateTime]

}