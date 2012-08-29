package sorm.core

import sorm._
import abstractSql.StandardSqlComposition
import query.AbstractSqlComposition
import sql.StandardRendering._
import persisted._
import reflection._
import save._
import structure._
import mapping._
import jdbc._
import resultSet._
import extensions.Extensions._

import query.Query._

import collection.immutable.Queue
import com.weiglewilczek.slf4s.Logging

class Fetcher
  [ T ]
  ( connection      : ConnectionAdapter,
    queryMapping    : EntityMapping,
    queryWhere      : Option[Where] = None,
    queryOrder      : Queue[Order] = Queue.empty,
    queryLimit      : Option[Int] = None,
    queryOffset     : Int = 0 )
  extends Logging
  {
    private def copy
      ( connection      : ConnectionAdapter = connection,
        queryMapping    : EntityMapping = queryMapping,
        queryWhere      : Option[Where] = queryWhere,
        queryOrder      : Queue[Order] = queryOrder,
        queryLimit      : Option[Int] = queryLimit,
        queryOffset     : Int = queryOffset )
      : Fetcher[T]
      = new Fetcher[T](
          connection, queryMapping, queryWhere, queryOrder, queryLimit, queryOffset
        )

    private def order ( p : String, r : Boolean = false )
      = copy( queryOrder = queryOrder enqueue Order(Path.mapping(queryMapping, p), r) )

    def orderAsc ( p : String )
      = order(p, false)

    def orderDesc ( p : String )
      = order(p, true)

    def limit ( x : Int )
      = copy( queryLimit = Some(x) )

    def offset ( x : Int )
      = copy( queryOffset = x )

    def filter ( w : Where )
      : Fetcher[T]
      = copy( 
          queryWhere = (queryWhere ++: List(w)) reduceOption And
        )
    private def filter ( p : String, v : Any, o : Operator )
      : Fetcher[T]
      = filter( Path.where( queryMapping, p, v, o ) )

    def filterEqual ( p : String, v : Any )
      = filter( p, v, Operator.Equal )

    def filterNotEqual ( p : String, v : Any )
      = filter( p, v, Operator.NotEqual )

    def filterLarger ( p : String, v : Any )
      = filter( p, v, Operator.Larger )

    def filterLargerOrEqual ( p : String, v : Any )
      = filter( p, v, Operator.LargerOrEqual )

    def filterSmaller ( p : String, v : Any ) 
      = filter( p, v, Operator.Smaller )

    def filterSmallerOrEqual ( p : String, v : Any )
      = filter( p, v, Operator.SmallerOrEqual )

    def filterLike( p : String, v : Any ) 
      = filter( p, v, Operator.Like )

    def filterNotLike( p : String, v : Any ) 
      = filter( p, v, Operator.NotLike )

    def filterRegex( p : String, v : Any ) 
      = filter( p, v, Operator.Regex )

    def filterNotRegex( p : String, v : Any ) 
      = filter( p, v, Operator.NotRegex )

    def filterIn ( p : String, v : Any ) 
      = filter( p, v, Operator.In )

    def filterNotIn ( p : String, v : Any ) 
      = filter( p, v, Operator.NotIn )

    def filterContains ( p : String, v : Any ) 
      = filter( p, v, Operator.Contains )

    def filterNotContains ( p : String, v : Any ) 
      = filter( p, v, Operator.NotContains )

    def filterConstitutes ( p : String, v : Any ) 
      = filter( p, v, Operator.Constitutes )

    def filterNotConstitutes ( p : String, v : Any ) 
      = filter( p, v, Operator.NotConstitutes )

    def filterIncludes ( p : String, v : Any ) 
      = filter( p, v, Operator.Includes )

    def filterNotIncludes ( p : String, v : Any ) 
      = filter( p, v, Operator.NotIncludes )


    private[sorm] def query( kind : Kind = Kind.Select )
      = Query(kind, queryMapping, queryWhere, queryOrder, queryLimit, queryOffset)

    private def statementAndResultMappings ( q : Query )
      = {
        val sql = StandardSqlComposition.sql(AbstractSqlComposition.resultSetSelect(q))
        Statement( sql.template, sql.data map JdbcValue.apply ) ->
        q.mapping.resultSetMappings
      }

    def fetchAll()
      : Seq[T with Persisted]
      = {
        val (stmt, resultSetMappings)
          = statementAndResultMappings( query(Kind.Select) )

        connection.executeQuery(stmt)
          .fetchInstancesAndClose(
            queryMapping,
            resultSetMappings.view.zipWithIndex.toMap
          )
          .asInstanceOf[Seq[T with Persisted]]
      }

    def fetchOne()
      = limit(1).fetchAll().headOption

    def fetchCount()
      : Int
      = {
        logger.warn("TODO: to implement effective fetchCount")
        fetchAll().size
      }

    def fetchExists()
      = {
        logger.warn("TODO: to implement effective fetchExists")
        fetchOne().isDefined
      }

  }
