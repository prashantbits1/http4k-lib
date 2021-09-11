package lib.http4k

import org.http4k.core.*
import org.http4k.filter.CorsPolicy
import org.http4k.filter.ServerFilters
import org.http4k.lens.BiDiBodyLens
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.server.ApacheServer
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.http4k.format.Jackson.auto
import org.http4k.routing.routes


/**
 * This class is use to expose services as REST api.
 *
 * @author Prashant.Singh
 */

enum class Http4kServerName { ApacheServer, Undertow }

/**
 * This method is use to create Http4kServer instance for given http4kServer.
 *
 * @param http4kServerName: <Http4kServerName> Name of the server(Http4kServerName.Undertow).
 * @param port: <Int> Port number.
 *
 * @return Http4kServer
 */
fun RoutingHttpHandler.asHttp4kServer(
    http4kServerName: Http4kServerName = Http4kServerName.Undertow,
    port: Int = 8085
): Http4kServer {
    return ServerFilters.CatchLensFailure()
        .then(ServerFilters.Cors(CorsPolicy.UnsafeGlobalPermissive))
        .then(ServerFilters.GZip())
        .then(this)
        .asServer(getServer(http4kServerName, port))
}

/**
 * Method to get the different type of Server instances.
 *
 * @param http4kServerName:<Http4kServerName> Name of the server like ApacheServer, Undertow
 * @param port: <Int> Port number on which server start.
 *
 */
fun getServer(http4kServerName: Http4kServerName, port: Int): ServerConfig {
    return when (http4kServerName) {
        Http4kServerName.ApacheServer -> ApacheServer(port = port)
        Http4kServerName.Undertow -> Undertow(port = port, true)
        else -> throw Exception("Not found $http4kServerName")
    }

}

/**
 * This method is use to get the RoutingHttpHandler based on given request type(Method.GET, Method.POST etc).
 *
 * @param methodType: <org.http4k.core.Method> Request type.
 * @param url: <String> URL of the server to route the request.
 * @param block: <org.http4k.core.HttpHandler> [(): HttpHandler = {(Request) -> Response}]
 *
 * @return RoutingHttpHandler
 */
fun apiRoute(methodType: Method, url: String, block: () -> HttpHandler): RoutingHttpHandler =
        when (methodType) {
            Method.GET -> url bind Method.GET to { request: Request -> block().invoke(request) }
            Method.POST -> url bind Method.POST to { request: Request -> block().invoke(request) }
            else -> throw Exception("Not supported this requestType: $methodType")
        }

/**
 * GET Api route.
 * Example: getApiRoute("/msg") { request -> processMessage(request) }
 *
 * @param url:<String>
 * @param function:<> A function/method which process the request(org.http4k.core.Request)
 *                    and return org.http4k.core.Response. This function take Request as argument.
 *                    Example: function => fun processMessage(request: Request): Response
 *
 * @return RoutingHttpHandler
 */
fun getApiRoute(url: String, function: (Request) -> Response): RoutingHttpHandler {
    val block: HttpHandler = { request -> function(request) }
    return apiRoute(Method.GET, url, block = { block })
}


/**
 * POST Api route. This method internally convert the requestBody into given data class object.
 * If the data class is not matched with the requestBody data, it throws not parse exception.
 * Example: postApiRoute<TestData>("/postMsg"){ testData-> processPostMessage(testData)}
 *
 * @param url:<String>
 * @param function:<> A function/method which process the requestBody which is a T type of data class object
 *                    and return org.http4k.core.Response. This function take T data class as argument.
 *                    Example: function => fun processPostMessage(object: <T:Any>): Response
 *
 * @return RoutingHttpHandler
 */
inline fun <reified T : Any> postApiRoute(url: String, crossinline function: (T) -> Response): RoutingHttpHandler {
    val block: HttpHandler = { request ->
        val dataLens: BiDiBodyLens<T> = Body.auto<T>().toLens()
        function(dataLens.extract(request))
    }
    return apiRoute(Method.POST, url, block = { block })
}


/**
 * POST Api route.
 * Example: postApiRouteForRequest("/postMsg") { request:Request -> processPostMessageReq(request) }
 *
 * @param url:<String>
 * @param function:<> A function/method which process the request(org.http4k.core.Request)
 *                    and return org.http4k.core.Response. This function take Request as argument.
 *                    Example: function => fun processPostMessageReq(request: Request): Response
 *
 * @return RoutingHttpHandler
 */
inline fun postApiRouteForRequest(url: String, crossinline function: (Request) -> Response): RoutingHttpHandler {
    val block: HttpHandler = { request -> function(request) }
    return apiRoute(Method.POST, url, block = { block })
}


/**
 * This method is used to route the different type of request to corresponding HttpHandler.
 * Example-how to use: buildRoutes(routes = listOf(
 *                                                   getApiRoute("/msg") { req -> processMessage(req) },
 *                                                   postApiRoute<TestData>("/postMsg"){ testData -> precessPostMsg(testData)}
 *                                                   )
 *                                                   ).asHttp4kServer(port = 8086).start()
 * @param appName:<String> Name of the application.
 * @param routes:<List<RoutingHttpHandler>> list of the HttpHandlers.
 *
 * @return RoutingHttpHandler
 */
fun buildRoutes(appName: String="", routes: List<RoutingHttpHandler> = emptyList()): RoutingHttpHandler =
        routes(*routes.toTypedArray())


/**
 * Method to health check the service.
 */
fun serviceCheck(): RoutingHttpHandler = "healthcheck" bind Method.GET to {Response(Status.OK).body("Service is up..")}