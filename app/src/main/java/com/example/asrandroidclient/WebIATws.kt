package com.example.asrandroidclient

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.orhanobut.logger.Logger
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


/**
 * Create by MJ on 2024/1/27.
 * Describe :
 */

class WebIATws : WebSocketListener() {
    private val hostUrl = "https://iat-api.xfyun.cn/v2/iat" //中英文，http url 不支持解析 ws/wss schema

    // private static final String hostUrl = "https://iat-niche-api.xfyun.cn/v2/iat";//小语种
//    private val appid = "xxxxxxx" //在控制台-我的应用获取
//
//    private val apiSecret = "xxxxxxx" //在控制台-我的应用-语音听写（流式版）获取
//
//    private val apiKey = "xxxxxxx" //在控制台-我的应用-语音听写（流式版）获取

    // private val file = "resource\\iat\\16k_10.pcm" // 中文
    val StatusFirstFrame = 0
    val StatusContinueFrame = 1
    val StatusLastFrame = 2
    val json = Gson()
    var decoder: Decoder = Decoder()

    // 开始时间
    private val dateBegin: Date = Date()

    // 结束时间
    private var dateEnd: Date = Date()

    @SuppressLint("SimpleDateFormat")
    private val sdf: SimpleDateFormat = SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS")
    private var data: ByteArray? = null


    // 需要大于Android8.0
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        Thread {
            //连接成功，开始发送数据
            //连接成功，开始发送数据
            val frameSize = 1280 //每一帧音频的大小,建议每 40ms 发送 122B
            val intervel = 40
            var status = 0 // 音频的状态
            val byteArrayInputStream = ByteArrayInputStream(data)
            try {
                val buffer = ByteArray(frameSize)
                // 发送音频
                end@ while (true) {
                    val len = byteArrayInputStream.read(buffer)
                    if (len == -1) {
                        status = StatusLastFrame //文件读完，改变status 为 2
                    }
                    when (status) {
                        StatusFirstFrame -> {
                            val frame = JsonObject()
                            val business =
                                JsonObject() //第一帧必须发送
                            val common = JsonObject() //第一帧必须发送
                            val data = JsonObject() //每一帧都要发送
                            // 填充common
                            common.addProperty("app_id", APPID)
                            //填充business
                            business.addProperty("language", "zh_cn")
                            //business.addProperty("language", "en_us");//英文
                            //business.addProperty("language", "ja_jp");//日语，在控制台可添加试用或购买
                            //business.addProperty("language", "ko_kr");//韩语，在控制台可添加试用或购买
                            //business.addProperty("language", "ru-ru");//俄语，在控制台可添加试用或购买
                            business.addProperty("domain", "iat")
                            business.addProperty(
                                "accent",
                                "mandarin"
                            ) //中文方言请在控制台添加试用，添加后即展示相应参数值
                            //business.addProperty("nunum", 0);
                            //business.addProperty("ptt", 0);//标点符号
                            //business.addProperty("rlang", "zh-hk"); // zh-cn :简体中文（默认值）zh-hk :繁体香港(若未授权不生效，在控制台可免费开通)
                            //business.addProperty("vinfo", 1);
                            business.addProperty("dwa", "wpgs") //动态修正(若未授权不生效，在控制台可免费开通)
                            //business.addProperty("nbest", 5);// 句子多候选(若未授权不生效，在控制台可免费开通)
                            //business.addProperty("wbest", 3);// 词级多候选(若未授权不生效，在控制台可免费开通)
                            //填充data
                            data.addProperty("status", StatusFirstFrame)
                            data.addProperty("format", "audio/L16;rate=16000")
                            data.addProperty("encoding", "raw")
                            data.addProperty(
                                "audio",
                                Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len))
                            )
                            //填充frame
                            frame.add("common", common)
                            frame.add("business", business)
                            frame.add("data", data)
                            webSocket.send(frame.toString())
                            status = StatusContinueFrame // 发送完第一帧改变status 为 1
                        }

                        StatusContinueFrame -> {
                            val frame1 = JsonObject()
                            val data1 = JsonObject()
                            data1.addProperty("status", StatusContinueFrame)
                            data1.addProperty("format", "audio/L16;rate=16000")
                            data1.addProperty("encoding", "raw")
                            data1.addProperty(
                                "audio",
                                Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, len))
                            )
                            frame1.add("data", data1)
                            webSocket.send(frame1.toString())
                        }

                        StatusLastFrame -> {
                            val frame2 = JsonObject()
                            val data2 = JsonObject()
                            data2.addProperty("status", StatusLastFrame)
                            data2.addProperty("audio", "")
                            data2.addProperty("format", "audio/L16;rate=16000")
                            data2.addProperty("encoding", "raw")
                            frame2.add("data", data2)
                            webSocket.send(frame2.toString())
                            Logger.i("sendlast")
                            break@end
                        }
                    }
                    Thread.sleep(intervel.toLong()) //模拟音频采样延时
                }
                Logger.i("all data is send")
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
        val resp: ResponseData = json.fromJson(text, ResponseData::class.java)
        if (resp != null) {
            if (resp.code !== 0) {
                Logger.i("返回结果：$resp")
                Logger.i("错误码查询链接：https://www.xfyun.cn/document/error-code")
                return
            }
            if (resp.data != null) {
                if (resp.data?.result != null) {
                    val te = resp.data?.result?.text
                    //System.out.Logger.i(te.toString());
                    te?.let {
                        try {
                            decoder.decode(te)
                            Logger.i("中间识别结果 ==》$decoder")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                if (resp.data?.status == 2) {
                    // todo  resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
                    Logger.i("session end ")
                    dateEnd = Date()
                    Logger.i(sdf.format(dateBegin) + "开始")
                    Logger.i(sdf.format(dateEnd) + "结束")
                    Logger.i("耗时:" + (dateEnd.time - dateBegin.time) + "ms")
                    Logger.i("最终识别结果 ==》$decoder")
                    Logger.i("本次识别sid ==》" + resp.sid)
                    decoder.discard()
                    webSocket.close(1000, "")
                } else {
                    // todo 根据返回的数据处理
                }
            }
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        try {
            if (null != response) {
                val code = response.code()
                Logger.i("onFailure code:$code")
                Logger.i("onFailure body:" + response.body()!!.string())
                if (101 != code) {
                    Logger.i("connection failed")
                    // System.exit(0)
                }
            }
        } catch (e: IOException) {
            Logger.e("IO error:${e.message}")
            // e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun initWebIA(data: ByteArray) {
        this.data = data
        // 构建鉴权url
        val authUrl = getAuthUrl()
        val client = OkHttpClient.Builder().build()
        //将url中的 schema http://和https://分别替换为ws:// 和 wss://
        val url = authUrl.replace("http://", "ws://").replace("https://", "wss://")
        Logger.i("init url:$url")
        val request: Request = Request.Builder().url(url).build()
        val webSocket = client.newWebSocket(request, this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getAuthUrl(): String {
        val url = URL(hostUrl)
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        format.timeZone = TimeZone.getTimeZone("GMT")
        val date = format.format(Date())
        val builder = StringBuilder("host:")
            .append(url.host)
            .append("\n")
            .append("date:").append(date)
            .append("\n")
            .append("GET")
            .append(url.path)
            .append(" HTTP/1.1")
        val charset = Charset.forName("UTF-8")
        val mac = Mac.getInstance("hmacsha256")
        val spec = SecretKeySpec(APISecret.toByteArray(charset), "hmacsha256")
        mac.init(spec)
        val hexDigits = mac.doFinal(builder.toString().toByteArray(charset))
        val sha = Base64.getEncoder().encodeToString(hexDigits)
        val authorization = String.format(
            "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
            APIKey,
            "hmac-sha256",
            "host date request-line",
            sha
        )
        val httpUrl = HttpUrl.parse("https://" + url.host + url.path)?.newBuilder()
            ?.addQueryParameter(
                "authorization",
                Base64.getEncoder().encodeToString(authorization.toByteArray(charset))
            )
            ?.addQueryParameter("date", date)
            ?.addQueryParameter("host", url.host)
            ?.build()
        return httpUrl.toString()
    }


    data class ResponseData(
        var code: Int = 0,
        var message: String?,
        var sid: String?,
        var data: Data?,
    )

    class Data {
        var status = 0
        var result: Result? = null
    }

    class Result {
        private var bg = 0
        private var ed = 0
        private var pgs: String? = null
        private var rg: IntArray? = null
        private var sn = 0
        private var ws: Array<Ws>? = null
        private var ls = false
        private var vad: JsonObject? = null
        val text: Text
            get() {
                val sb = StringBuilder()
                ws?.let {
                    for (ws in it) {
                        sb.append(ws.cw[0].w)
                    }
                }
                return Text(
                    sn, bg, ed, sb.toString(), pgs, rg, ls, if (vad == null) null else vad
                )
            }
    }


    data class Ws(
        var cw: Array<Cw>,
        var bg: Int = 0,
        var ed: Int = 0
    )


    data class Cw(
        var sc: Int = 0,
        var w: String
    )

    data class Text(
        var sn: Int = 0,
        var bg: Int = 0,
        var ed: Int = 0,
        var text: String?,
        var pgs: String?,
        var rg: IntArray?,
        var ls: Boolean = false,
        var vad: JsonObject?
    ) {
        var deleted: Boolean = false
    }

    //解析返回数据，仅供参考
    class Decoder {
        private var texts: Array<Text?>
        private var defc = 10

        init {
            texts = arrayOfNulls(defc)
        }

        @Synchronized
        fun decode(text: Text) {
            if (text.sn >= defc) {
                resize()
            }
            if ("rpl" == text.pgs) {
                val rg0 = text.rg?.get(0)
                val rg1 = text.rg?.get(1)
                if (rg0 != null && rg1 != null)
                    for (i in rg0..rg1) {
                        texts[i]?.deleted = true
                    }
            }
            texts[text.sn] = text
        }

        override fun toString(): String {
            val sb = StringBuilder()
            for (t in texts) {
                if (t != null && !t.deleted) {
                    sb.append(t.text)
                }
            }
            return sb.toString()
        }

        fun resize() {
            val oc = defc
            defc = defc shl 1
            val old = texts
            texts = arrayOfNulls(defc)
            for (i in 0 until oc) {
                texts[i] = old[i]
            }
        }

        fun discard() {
            for (i in texts.indices) {
                texts[i] = null
            }
        }
    }
}