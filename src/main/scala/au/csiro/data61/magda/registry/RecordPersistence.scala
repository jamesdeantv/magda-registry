package au.csiro.data61.magda.registry

import scalikejdbc._
import spray.json.JsonParser
import scala.util.Try
import scala.util.{Success, Failure}
import java.sql.SQLException
import spray.json.JsObject
import com.networknt.schema.ValidationMessage
import collection.JavaConverters._
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode

object RecordPersistence {
  def getAll(implicit session: DBSession): Iterable[Record] = {
    val result = 
      sql"""select recordID, Record.name as recordName, sectionID, Section.name as sectionName
            from Record
            left outer join RecordSection using (recordID)
            left outer join Section using (sectionID)"""
      .map(rs => (rs.string("recordID"), rs.string("recordName"), rs.string("sectionID"), rs.string("sectionName")))
      .list.apply()
      .groupBy(t => (t._1, t._2)) // group by recordID and recordName
      .map { case (key, value) => Record(id = key._1, name = key._2, sections = value.filter(_._3 != null).map(t => RecordSection(id = t._3, name = t._4, data = None))) }
      
    result
  }
  
//  def getById(implicit session: DBSession, id: String): Option[Record] = {
//    sql"select sectionID, name, jsonSchema from Section where sectionID=${id}".map(rs => rowToSection(rs)).single.apply()
//  }
//  
//  def putById(implicit session: DBSession, id: String, section: Section): Try[Section] = {
//    if (id != section.id) {
//      // TODO: we can do better than RuntimeException here.
//      return Failure(new RuntimeException("The provided ID does not match the section's id."))
//    }
//    
//    // Make sure we have a valid JSON Schema
//    val schemaValidationResult = validateJsonSchema(section.jsonSchema);
//    if (schemaValidationResult.size > 0) {
//      var lines = "The provided JSON Schema is not valid:" ::  
//                  schemaValidationResult.map(_.getMessage())
//      var message = lines.mkString("\n")
//      
//      // TODO: include details of the validation failure.
//      return Failure(new RuntimeException(message))
//    }
//    
//    // Make sure existing data for this section matches the new JSON Schema
//    // TODO
//
//    val jsonString = section.jsonSchema.compactPrint
//    sql"""insert into Section (sectionID, name, jsonSchema) values (${section.id}, ${section.name}, ${jsonString}::json)
//          on conflict (sectionID) do update
//          set name = ${section.name}, jsonSchema = ${jsonString}::json
//          """.update.apply()
//    Success(section)
//  }
//  
//  def create(implicit session: DBSession, section: Section): Try[Section] = {
//    try {
//      sql"insert into Section (sectionID, name, jsonSchema) values (${section.id}, ${section.name}, ${section.jsonSchema.compactPrint}::json)".update.apply()
//      Success(section)
//    } catch {
//      case e: SQLException => Failure(new RuntimeException("A section with the specified ID already exists."))
//    }
//  }
  
  private def rowToRecordSummary(rs: WrappedResultSet): Section = {
    ???    
//    new Record(
//      rs.string("recordID"), rs.string("recordName"), JsonParser(rs.string("jsonSchema")).asJsObject)
  }
  
//  private def validateJsonSchema(jsonSchema: JsObject): List[ValidationMessage] = {
//    // TODO: it's super inefficient format the JSON as a string only to parse it back using a different library.
//    //       it'd be nice if we had a spray-json based JSON schema validator.
//    val jsonString = jsonSchema.compactPrint
//    val jsonNode = new ObjectMapper().readValue(jsonString, classOf[JsonNode])
//    jsonSchemaSchema.validate(jsonNode).asScala.toList
//  }
//  
//  private val jsonSchemaSchema = new JsonSchemaFactory().getSchema(getClass.getResourceAsStream("/json-schema.json"))
}