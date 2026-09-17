package com.sahraflix.data.pairing

import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.flow.MutableSharedFlow
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject

class PairingServer @Inject constructor() : NanoHTTPD(HOST, PORT) {
    val submissions = MutableSharedFlow<PairingSubmission>(extraBufferCapacity = 1)
    private val random = SecureRandom()
    @Volatile private var currentPin: String = newPin()

    fun regeneratePin(): String {
        currentPin = newPin()
        return currentPin
    }

    fun pin(): String = currentPin

    override fun serve(session: IHTTPSession): Response = when {
        session.method == Method.POST && session.uri == "/api/submit" -> handleSubmission(session)
        session.method == Method.GET && session.uri == "/xtream" -> htmlResponse(XTREAM_HTML)
        session.method == Method.GET && session.uri == "/m3u" -> htmlResponse(M3U_HTML)
        session.method == Method.GET && session.uri == "/stalker" -> htmlResponse(STALKER_HTML)
        else -> newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
    }

    private fun handleSubmission(session: IHTTPSession): Response {
        return try {
            val files = mutableMapOf<String, String>()
            session.parseBody(files)
            val body = files["postData"].orEmpty()
            val json = JSONObject(body)
            val submittedPin = json.optString("pin")
            if (!MessageDigest.isEqual(submittedPin.toByteArray(), currentPin.toByteArray())) {
                jsonResponse(Response.Status.UNAUTHORIZED, "Invalid TV PIN")
            } else {
                val submission = PairingSubmission(
                    type = json.optString("type"),
                    host = json.optString("host").ifBlank { json.optString("portal") },
                    url = json.optString("url"),
                    username = json.optString("username"),
                    password = json.optString("password"),
                    mac = json.optString("mac")
                )
                if (submission.type.isBlank()) jsonResponse(Response.Status.BAD_REQUEST, "Missing source type")
                else {
                    submissions.tryEmit(submission)
                    jsonResponse(Response.Status.OK, "Connected")
                }
            }
        } catch (_: Exception) {
            jsonResponse(Response.Status.BAD_REQUEST, "Invalid request")
        }
    }

    private fun htmlResponse(html: String) = newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)

    private fun jsonResponse(status: Response.Status, message: String): Response =
        newFixedLengthResponse(status, "application/json", JSONObject(mapOf("message" to message)).toString())

    private fun newPin(): String = (100000 + random.nextInt(900000)).toString()

    companion object {
        const val HOST = "0.0.0.0"
        const val PORT = 18081
        const val SOCKET_READ_TIMEOUT = 5_000
        const val BASE_CSS = "body{font-family:Segoe UI,sans-serif;background:#0f0f0f;color:#fff;display:flex;justify-content:center;align-items:center;min-height:100vh;margin:0}.card{background:#1a1a1a;padding:2rem;border-radius:12px;width:100%;max-width:400px;box-sizing:border-box}h2{color:#e50914}input{width:100%;padding:12px;margin-bottom:1rem;border-radius:6px;border:1px solid #333;background:#222;color:#fff;box-sizing:border-box}button{width:100%;padding:12px;background:#e50914;color:#fff;border:0;border-radius:6px}.pin{font-size:24px;text-align:center;letter-spacing:8px;font-weight:bold}"
        const val XTREAM_HTML = """<!doctype html><html><head><meta name=viewport content='width=device-width,initial-scale=1'><title>SAHRAFLIX Xtream</title><style>$BASE_CSS</style></head><body><div class=card><h2>Xtream Codes Setup</h2><form method=post action=/api/submit onsubmit='return submitData(event)'><input id=pin class=pin maxlength=6 placeholder='TV PIN'><input id=host placeholder='Host URL'><input id=username placeholder=Username><input id=password type=password placeholder=Password><button>Activate TV</button></form><p id=error></p></div><script>async function submitData(e){e.preventDefault();let p={pin:pin.value,type:'xtream',host:host.value,username:username.value,password:password.value};let r=await fetch('/api/submit',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(p)});if(r.ok)document.body.innerHTML='<div class=card><h2>Success</h2><p>TV Connected.</p></div>';else error.textContent=(await r.json()).message;return false}</script></body></html>"""
        const val M3U_HTML = """<!doctype html><html><head><meta name=viewport content='width=device-width,initial-scale=1'><title>SAHRAFLIX M3U</title><style>$BASE_CSS</style></head><body><div class=card><h2>M3U Playlist Setup</h2><form onsubmit='return submitData(event)'><input id=pin class=pin maxlength=6 placeholder='TV PIN'><input id=url type=url placeholder='M3U Playlist URL'><button>Activate TV</button></form><p id=error></p></div><script>async function submitData(e){e.preventDefault();let p={pin:pin.value,type:'m3u',url:url.value};let r=await fetch('/api/submit',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(p)});if(r.ok)document.body.innerHTML='<div class=card><h2>Success</h2><p>TV Connected.</p></div>';else error.textContent=(await r.json()).message;return false}</script></body></html>"""
        const val STALKER_HTML = """<!doctype html><html><head><meta name=viewport content='width=device-width,initial-scale=1'><title>SAHRAFLIX Stalker</title><style>$BASE_CSS</style></head><body><div class=card><h2>Stalker Portal Setup</h2><form onsubmit='return submitData(event)'><input id=pin class=pin maxlength=6 placeholder='TV PIN'><input id=portal type=url placeholder='Portal URL'><input id=mac placeholder='MAC Address'><button>Activate TV</button></form><p id=error></p></div><script>async function submitData(e){e.preventDefault();let p={pin:pin.value,type:'stalker',portal:portal.value,mac:mac.value};let r=await fetch('/api/submit',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(p)});if(r.ok)document.body.innerHTML='<div class=card><h2>Success</h2><p>TV Connected.</p></div>';else error.textContent=(await r.json()).message;return false}</script></body></html>"""
    }
}

data class PairingSubmission(
    val type: String,
    val host: String = "",
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val mac: String = ""
)
