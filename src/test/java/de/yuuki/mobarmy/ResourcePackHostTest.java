package de.yuuki.mobarmy;

import java.net.*;
import java.net.http.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.zip.*;
import java.io.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResourcePackHostTest {
    @Test void localhostDeliversPackAndRejectsOtherPathsAndMethods()throws Exception{
        var buffer=new ByteArrayOutputStream();try(var zip=new ZipOutputStream(buffer)){zip.putNextEntry(new ZipEntry("pack.mcmeta"));zip.write("{}".getBytes());zip.closeEntry();}
        byte[] pack=buffer.toByteArray();
        try(var host=new ResourcePackHost(pack,0);var client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()){
            assertEquals("127.0.0.1",host.server.getAddress().getAddress().getHostAddress());assertArrayEquals(MessageDigest.getInstance("SHA-1").digest(pack),host.hash);
            var get=client.send(HttpRequest.newBuilder(URI.create(host.url())).timeout(Duration.ofSeconds(5)).build(),HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200,get.statusCode());assertArrayEquals(pack,get.body());assertEquals("application/zip",get.headers().firstValue("Content-Type").orElseThrow());
            var head=client.send(HttpRequest.newBuilder(URI.create(host.url())).method("HEAD",HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200,head.statusCode());assertEquals(0,head.body().length);assertEquals(Integer.toString(pack.length),head.headers().firstValue("Content-Length").orElseThrow());
            String base="http://127.0.0.1:"+host.server.getAddress().getPort();
            assertEquals(404,client.send(HttpRequest.newBuilder(URI.create(base+"/mobarmy.zip/other")).build(),HttpResponse.BodyHandlers.discarding()).statusCode());
            assertEquals(405,client.send(HttpRequest.newBuilder(URI.create(host.url())).POST(HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.discarding()).statusCode());
        }
    }
}
