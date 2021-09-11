package lib.http4k

class TestClientUtil: Http4kClient() {
  private val client:Http4kClient = Http4kClient()

    fun getApi(url: String):Http4kServerTest.ResponseData{
        return client.callHttp4kClient(Http4kClient.Http4kRequestType.GET,
                url,
                "TestClientUtil",
                "Not Able to fetch $url")
    }

    fun postApi(url: String, body: String):Http4kServerTest.ResponseData{
        return client.callHttp4kClient(Http4kClient.Http4kRequestType.POST,
                url,
                "TestClientUtil",
                "Not Able to fetch $url",
                 body)
    }

}