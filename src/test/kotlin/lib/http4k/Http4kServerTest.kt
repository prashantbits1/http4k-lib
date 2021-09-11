package lib.http4k

import org.http4k.core.*
import org.http4k.format.Jackson
import org.http4k.format.Jackson.asPrettyJsonString
import org.http4k.format.Jackson.auto
import org.http4k.lens.BiDiBodyLens
import org.http4k.routing.RoutingHttpHandler


class Http4kServerTest {

    data class TestData(val id: String, val name: String)
    data class ResponseData(val id: Int, val name: String, val mappingId: String)

    private fun block():HttpHandler = { request ->
        val id = request.query("id")
        val test = TestData(id!!, "Hero")
         Response(Status.OK).header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                            .body(Jackson.asJsonObject(test).asPrettyJsonString())
    }

    private fun processPOSTRequestForObject(test: TestData): Response{
        val responseData = ResponseData(test.id.toInt(), test.name, "${test.id}-${test.name}")
        return Response(Status.OK).header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                                  .body(Jackson.asJsonObject(responseData).asPrettyJsonString())
    }

    private fun processPOSTRequestQuery(request: Request): Response{
        val dataLens: BiDiBodyLens<TestData> = Body.auto<TestData>().toLens()
        val responseData = ResponseData(request.query("id")!!.toInt(), request.query("name")!!,
                "${request.query("id")}-${request.query("name")}")
        return if(dataLens.extract(request).id == request.query("id")!!)
           {
               Response(Status.OK).header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                                  .body(Jackson.asJsonObject(responseData).asPrettyJsonString())
           } else{
               Response(Status.NOT_FOUND).header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                                         .body(Jackson.asJsonObject(responseData).asPrettyJsonString())
            }
    }
    
    private fun testApiRoute(function:(String?)->Response): RoutingHttpHandler {
        val block: HttpHandler = { request -> function(request.query("id"))}
        val b: () -> HttpHandler = { block }
        return apiRoute(Method.POST, "/testApiRoute", block = b)
    }

    private fun processGetApiMessage(request: Request): Response{
        val responseData = ResponseData(request.query("id")!!.toInt(), request.query("name")!!,
                "${request.query("id")}-${request.query("name")}")
        return Response(Status.OK).header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                                  .body(Jackson.asJsonObject(responseData).asPrettyJsonString())
    }

    fun testHttp4kServer(){
        // define routes, block, processGetApiMessage, processPOSTRequestForObject and processPOSTRequestQuery are the business methods.
        val httpServer = buildRoutes(
                routes = listOf(
                        apiRoute(Method.GET,"/test"){ block() },
                        getApiRoute("/getApiMsg") { request:Request -> processGetApiMessage(request) },
                        postApiRoute("/postApiMsg"){ testData:TestData -> processPOSTRequestForObject(testData)},
                        postApiRouteForRequest("/postApiReqMsg"){ request:Request -> processPOSTRequestQuery(request)})

        ).asHttp4kServer(port = 8086)
        // start server
        httpServer.start()

        // Using Htt4kClient to call the exposed GET-POST API
        val host = "http://localhost:8086"
        try {
            // GET Api
            val testClientUtil = TestClientUtil()
            val getApiResponse = testClientUtil.getApi("$host/getApiMsg?id=11&name=Ram")
            assert(getApiResponse.id == 1)
            assert(getApiResponse.name == "Ram")
            println(getApiResponse)

            // POST Api with request Body and request params
            val body = "{\n" +
                    "    \"id\": \"23\",\n" +
                    "    \"name\": \"Ram\"\n" +
                    "}"
            val postApiResponse = testClientUtil.postApi("$host/postApiReqMsg?id=23&name=Ram", body)
            assert(postApiResponse.id == 23)
            assert(postApiResponse.name == "Ram")
            println(postApiResponse)

            // POST Api with request Body
            val postApiResponseData = testClientUtil.postApi("$host/postApiMsg", body)
            assert(postApiResponseData.id == 1)
            assert(postApiResponseData.name == "Ram")
            println(postApiResponseData)

            // shutting down server
            httpServer.stop()
            httpServer.close()
        }catch (e:Exception){
            println(e)
            httpServer.stop()
            httpServer.close()
        }finally {
            httpServer.stop()
            httpServer.close()
        }
    }

}

fun main() {
    val http4kServerTest= Http4kServerTest()
    http4kServerTest.testHttp4kServer()
}