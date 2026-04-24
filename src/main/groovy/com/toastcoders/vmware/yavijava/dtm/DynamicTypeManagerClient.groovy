package com.toastcoders.vmware.yavijava.dtm

import groovy.xml.XmlSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

class DynamicTypeManagerClient {

    private static final Logger log = LoggerFactory.getLogger(DynamicTypeManagerClient)

    private final String url
    private final String user
    private final String pass
    private final boolean strictCert
    private String sessionCookie
    private final MoTypeInfoParser parser = new MoTypeInfoParser()

    DynamicTypeManagerClient(String url, String user, String pass, boolean strictCert) {
        this.url = url
        this.user = user
        this.pass = pass
        this.strictCert = strictCert
        if (!strictCert) installTrustAllSsl()
    }

    Map<String, MoTypeInfo> fetchAll() {
        log.info("DTM: logging in to ${url} as ${user}")
        login()
        try {
            log.info("DTM: discovering DynamicTypeManager MOR")
            String dtmValue = discoverDtmMor()
            log.info("DTM: enumerating MO types")
            List<String> typeNames = enumerateTypes(dtmValue)
            log.info("DTM: fetching type info for ${typeNames.size()} types")
            Map<String, MoTypeInfo> all = [:]
            typeNames.each { name ->
                try {
                    MoTypeInfo t = fetchTypeInfo(dtmValue, name)
                    if (t != null) all[t.name] = t
                } catch (Exception e) {
                    log.warn("DTM: skipping ${name}: ${e.message}")
                }
            }
            return all
        } finally {
            try { logout() } catch (Exception e) { log.warn("DTM: logout failed: ${e.message}") }
        }
    }

    private void login() {
        String body = """\
<Login xmlns="urn:vim25"><_this type="SessionManager">SessionManager</_this><userName>${user}</userName><password>${pass}</password></Login>"""
        def conn = post(envelope(body))
        sessionCookie = conn.getHeaderField("Set-Cookie")
        if (sessionCookie == null) throw new RuntimeException("DTM login failed; no session cookie returned")
        sessionCookie = sessionCookie.split(";")[0]
        def env = new XmlSlurper().parseText(conn.inputStream.text)
        if (env.depthFirst().any { it.name() == "Fault" }) {
            throw new RuntimeException("DTM login fault: ${conn.inputStream.text}")
        }
    }

    private void logout() {
        String body = '<Logout xmlns="urn:vim25"><_this type="SessionManager">SessionManager</_this></Logout>'
        post(envelope(body))
    }

    private String discoverDtmMor() {
        for (String candidate : ["DynamicTypeManager", "InternalDynamicTypeManager"]) {
            try {
                enumerateTypes(candidate)
                log.info("DTM: using MOR value=${candidate}")
                return candidate
            } catch (Exception ignored) { /* try next */ }
        }
        throw new RuntimeException("DTM not exposed on this endpoint (tried both well-known MOR values)")
    }

    private List<String> enumerateTypes(String dtmValue) {
        String body = """\
<DynamicTypeMgrQueryMoInstances xmlns="urn:reflect"><_this type="InternalDynamicTypeManager">${dtmValue}</_this></DynamicTypeMgrQueryMoInstances>"""
        def conn = post(envelope(body))
        def env = new XmlSlurper().parseText(conn.inputStream.text)
        List<String> out = []
        env.depthFirst().findAll { it.name() == "moInstance" }.each {
            out << it.id.text()
        }
        if (out.isEmpty()) {
            env.depthFirst().findAll { it.name() == "returnval" }.each { rv ->
                rv.depthFirst().findAll { it.name() == "name" }.each { out << it.text() }
            }
        }
        return out.unique()
    }

    private MoTypeInfo fetchTypeInfo(String dtmValue, String typeName) {
        String body = """\
<DynamicTypeMgrQueryTypeInfo xmlns="urn:reflect"><_this type="InternalDynamicTypeManager">${dtmValue}</_this><filterSpec xsi:type="DynamicTypeMgrTypeFilterSpec" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><typeSubstr>${typeName}</typeSubstr></filterSpec></DynamicTypeMgrQueryTypeInfo>"""
        def conn = post(envelope(body))
        return parser.parseSingle(conn.inputStream.text)
    }

    private String envelope(String body) {
        return """\
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <soapenv:Body>
${body}
  </soapenv:Body>
</soapenv:Envelope>"""
    }

    private HttpURLConnection post(String envelope) {
        URL u = new URL(url)
        HttpURLConnection conn = (HttpURLConnection) u.openConnection()
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8")
        conn.setRequestProperty("SOAPAction", "urn:vim25/9.0.0.0")
        if (sessionCookie) conn.setRequestProperty("Cookie", sessionCookie)
        conn.outputStream.withWriter("UTF-8") { it.write(envelope) }
        int code = conn.responseCode
        if (code != 200) {
            String err = conn.errorStream?.text ?: ""
            throw new RuntimeException("DTM POST returned HTTP ${code}: ${err}")
        }
        return conn
    }

    private void installTrustAllSsl() {
        TrustManager[] trustAll = [new X509TrustManager() {
            void checkClientTrusted(X509Certificate[] c, String a) {}
            void checkServerTrusted(X509Certificate[] c, String a) {}
            X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0] }
        }] as TrustManager[]
        SSLContext ctx = SSLContext.getInstance("TLS")
        ctx.init(null, trustAll, new java.security.SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory())
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            boolean verify(String hostname, SSLSession session) { return true }
        })
    }
}
