package au.csiro.data61.magda.registry

import javax.ws.rs.Path

import akka.actor.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.Materializer
import akka.http.scaladsl.server.Directives._
import scalikejdbc._
import akka.http.scaladsl.model.StatusCodes
import io.swagger.annotations.{ApiImplicitParam, ApiImplicitParams, ApiOperation}

import scala.util.Failure
import scala.util.Success

@Path("/records")
@io.swagger.annotations.Api(value = "/records", produces = "application/json")
class RecordsService(system: ActorSystem, materializer: Materializer) extends Protocols {
  @ApiOperation(value = "Get a list of all records", nickname = "getAll", httpMethod = "GET", response = classOf[RecordSummary], responseContainer = "List")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "section", required = false, dataType = "string", paramType = "query", allowMultiple = true, value = "The sections for which to retrieve data, specified as multiple occurrences of this query parameter.  Only records that have at least one of these sections will be included in the response."),
    new ApiImplicitParam(name = "sections", required = false, dataType = "string", paramType = "query", value = "The sections for which to retrieve data, specified as a comma-separate list.  Only records that have at least one of these sections will be included in the response.")
  ))
  def getAll = get { pathEnd { parameters('section.*) { getAllWithSections } } }

  @ApiOperation(value = "Create a new record", nickname = "create", httpMethod = "POST", response = classOf[Record])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "record", required = true, dataType = "au.csiro.data61.magda.registry.Record", paramType = "body", value = "The definition of the new record.")
  ))
  def create = post { pathEnd { entity(as[Record]) { record =>
    DB localTx { session =>
      RecordPersistence.createRecord(session, record) match {
        case Success(result) => complete(result)
        case Failure(exception) => complete(StatusCodes.BadRequest, BadRequest(exception.getMessage))
      }
    }
  } } }

  @Path("/{id}")
  @ApiOperation(value = "Get a record by ID", nickname = "getById", httpMethod = "GET", response = classOf[Record],
    notes = "Gets a complete record, including data for all sections.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "ID of the record to be fetched.")
  ))
  def getById = get { path(Segment) { (id: String) => {
    DB readOnly { session =>
      RecordPersistence.getById(session, id) match {
        case Some(section) => complete(section)
        case None => complete(StatusCodes.NotFound, BadRequest("No record exists with that ID."))
      }
    }
  } } }

  @Path("/{id}")
  @ApiOperation(value = "Modify a record by ID", nickname = "putById", httpMethod = "PUT", response = classOf[Record],
    notes = "Modifies a record.  Sections included in the request are updated, but missing sections are not removed.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "ID of the record to be fetched."),
    new ApiImplicitParam(name = "record", required = true, dataType = "au.csiro.data61.magda.registry.Record", paramType = "body", value = "The record to save.")
  ))
  def putById = put { path(Segment) { (id: String) => {
    entity(as[Record]) { record =>
      DB localTx { session =>
        RecordPersistence.putRecordById(session, id, record) match {
          case Success(section) => complete(record)
          case Failure(exception) => complete(StatusCodes.BadRequest, BadRequest(exception.getMessage))
        }
      }
    }
  } } }

  val route =
    get { pathEnd { parameter('sections) { sections => getAllWithSections(sections.split(",").map(_.trim)) } } } ~
    getAll ~
    getById ~
    putById ~
    create ~
    get { path(Segment / "sections" / Segment) { getRecordSectionById } } ~
    put { path(Segment / "sections" / Segment) { putRecordSectionById } } ~
    post { path(Segment / "sections") { createRecordSection } }

  private def getAllWithSections(sections: Iterable[String]) = {
    complete {
      DB readOnly { session =>
        if (sections.isEmpty)
          RecordPersistence.getAll(session)
        else
          RecordPersistence.getAllWithSections(session, sections)
      }
    }
  }

  private def getRecordSectionById(recordID: String, sectionID: String) = {
    DB readOnly { session =>
      RecordPersistence.getRecordSectionById(session, recordID, sectionID) match {
        case Some(recordSection) => complete(recordSection)
        case None => complete(StatusCodes.NotFound, BadRequest("No record section exists with that ID."))
      }
    }
  }

  private def createRecordSection(recordID: String) = {
    entity(as[RecordSection]) { section =>
      DB localTx { session =>
        RecordPersistence.createRecordSection(session, recordID, section) match {
          case Success(result) => complete(result)
          case Failure(exception) => complete(StatusCodes.BadRequest, BadRequest(exception.getMessage))
        }
      }
    }
  }

  private def putRecordSectionById(recordID: String, sectionID: String) = {
    entity(as[RecordSection]) { section =>
      DB localTx { session =>
        RecordPersistence.putRecordSectionById(session, recordID, sectionID, section) match {
          case Success(result) => complete(result)
          case Failure(exception) => complete(StatusCodes.BadRequest, BadRequest(exception.getMessage))
        }
      }
    }
  }
}
