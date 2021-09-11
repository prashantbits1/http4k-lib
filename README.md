# HTTP4K utility library
This repository holds the http4k core basic library to play with web servers.
http4k-lib contains 2 utility:
- `Http4kClient` : To consume an API.
- `Http4kServer` : To expose a REST API.
 
## Table of content
- [Installation](#installation)
- [Example](#examples)
   - [How to use http4k client](#how-to-use-http4k-client)
   - [How to use http4k server](#how-to-use-http4k-server)




### Installation
```
./gradlew clean build
```
This will create a jar `http4k-lib.jar`. Import this jar into your project. Or push this into the maven repository and configure this into your gradle project. 
### How to use http4k client

Here are the steps to use **Http4kClient**:

1- Your client class(XYZ) should extend `Http4kClient` class.

2- Invoke this `callHttp4kClient` method by passing Http4kRequestType, url and other fields.   

Here is the sample to write http4k client:
```kotlin
data class ResponseData(val id: Int, val name: String, val mappingId: String)
class FeatureClient: Http4kClient() {
    val host = "http://localhost:8086" 
    fun getApi(url: String): ResponseData{
        val url = "$host/getFeature?id=1111"
        return callHttp4kClient(Http4kClient.Http4kRequestType.GET,
                url,
                "FeatureClient",
                "Not Able to fetch $url")
    }

    fun postApi(url: String, body: String): ResponseData{
         val url = "$host/insertFeature"
         val body = "{"id":23, "name":"Tee" }"
         return callHttp4kClient(Http4kClient.Http4kRequestType.POST,
                url,
                "FeatureClient",
                "Not Able to fetch $url",
                 body)
    }

}
```

### How to use http4k server

Follow the below steps to use Http4kServer:
- Define the Handler class to process request. It's similar how we define controller class in MVC pattern.
- Import the methods based on request type like for **GET** invoke `fun getApiRoute(url: String, function: (Request) -> Response): RoutingHttpHandler`
,**POST** invoke ` postApiRoute(url: String, crossinline function: (T) -> Response): RoutingHttpHandler`, `postApiRouteForRequest(url: String, crossinline function: (Request) -> Response): RoutingHttpHandler`
- Define the business class and invoke business methods into the handler.
- Now define the route(Integrate all handlers into the route) and start the server.

Here is the code sample:
```kotlin
import org.http4k.core.*
import org.http4k.format.Jackson
import org.http4k.format.Jackson.asPrettyJsonString
import org.http4k.format.Jackson.auto
import org.http4k.lens.BiDiBodyLens

data class TestData(val id: String, val name: String)
data class ResponseData(val id: Int, val name: String, val mappingId: String)
class FeatureHandler {

    fun processPOSTRequestForObject(test: TestData): Response {
        val responseData = ResponseData(test.id.toInt(), test.name, "${test.id}-${test.name}")
        return Response(Status.OK).header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                .body(Jackson.asJsonObject(responseData).asPrettyJsonString())
    }

    fun processPOSTRequestQuery(request: Request): Response {
        val dataLens: BiDiBodyLens<TestData> = Body.auto<TestData>().toLens()
        val responseData = ResponseData(request.query("id")!!.toInt(), request.query("name")!!,
                "${request.query("id")}-${request.query("name")}")
        return if (dataLens.extract(request).id == request.query("id")!!) {
            Response(Status.OK).header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                    .body(Jackson.asJsonObject(responseData).asPrettyJsonString())
        } else {
            Response(Status.NOT_FOUND).header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                    .body(Jackson.asJsonObject(responseData).asPrettyJsonString())
        }
    }

    fun processGetApiMessage(request: Request): Response {
        val responseData = ResponseData(request.query("id")!!.toInt(), request.query("name")!!,
                "${request.query("id")}-${request.query("name")}")
        return Response(Status.OK).header("Content-Type", ContentType.APPLICATION_JSON.toHeaderValue())
                .body(Jackson.asJsonObject(responseData).asPrettyJsonString())
    }
}

class FeatureApplication {
    private val featureHandler = FeatureHandler()
    fun testHttp4kServer() {
        // define routes
        val httpServer = buildRoutes(
                routes = listOf(
                        getApiRoute("/getApiMsg") { request: Request -> featureHandler.processGetApiMessage(request) },
                        postApiRoute("/postApiMsg") { testData: TestData -> featureHandler.processPOSTRequestForObject(testData) },
                        postApiRouteForRequest("/postApiReqMsg") { request: Request -> featureHandler.processPOSTRequestQuery(request) })

        ).asHttp4kServer(port = 8086)
        // start server
        httpServer.start()
    }
}
```