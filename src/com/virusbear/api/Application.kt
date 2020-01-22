package com.virusbear.api

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.gzip
import io.ktor.gson.gson
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.Locations
import io.ktor.locations.post
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.netty.EngineMain
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.streams.toList

fun main(args: Array<String>) = EngineMain.main(args)

@KtorExperimentalAPI
@KtorExperimentalLocationsAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(Locations)

    install(Compression) {
        gzip {
            priority = 1.0
        }
    }

    install(ContentNegotiation) {
        gson {
            serializeNulls()
        }
    }

    val db = Database.connect("jdbc:mysql://localhost:3306/test","com.mysql.jdbc.Driver", "root", "" )

    transaction {
        SchemaUtils.create(Item)
    }

    routing {
        route("/") {
            test(db)
        }
    }
}


@KtorExperimentalLocationsAPI
@Location("test")
class Test()

@KtorExperimentalLocationsAPI
fun Route.test(db: Database) {
    post<Test> {

        val lines = this::class.java.classLoader.getResourceAsStream("test.csv")!!.reader().buffered().lines().toList().map { it.split(",") }

        newSuspendedTransaction(Dispatchers.IO, db) {
            lines.forEach { line ->
                Item.insert {
                    it[no] = line[0].toInt()
                    it[description] = line[1]
                }
            }
        }

        call.respond(HttpStatusCode.OK)
    }
}

object Item: IntIdTable("item") {
    val no = integer("no")
    val description = varchar("description", 100)
}