package de.yuuki.mobarmy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.*;
import java.util.HexFormat;
import java.util.concurrent.*;

/** Serves one immutable ZIP only on this PC, without internet or a separate web server. */
final class ResourcePackHost implements AutoCloseable {
    final HttpServer server;
    final ExecutorService worker;
    final byte[] hash;
    ResourcePackHost(byte[] zip,int port)throws IOException{
        try{hash=MessageDigest.getInstance("SHA-1").digest(zip);}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",port),8);
        worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"MobArmy-local-pack");t.setDaemon(true);return t;});
        server.setExecutor(worker);
        server.createContext("/mobarmy.zip",exchange->{
            try(exchange){
                if(!exchange.getRequestURI().getPath().equals("/mobarmy.zip")){exchange.sendResponseHeaders(404,-1);return;}
                String method=exchange.getRequestMethod();
                if(!method.equals("GET")&&!method.equals("HEAD")){exchange.getResponseHeaders().set("Allow","GET, HEAD");exchange.sendResponseHeaders(405,-1);return;}
                exchange.getResponseHeaders().set("Content-Type","application/zip");
                exchange.getResponseHeaders().set("Content-Length",Integer.toString(zip.length));
                exchange.getResponseHeaders().set("ETag",HexFormat.of().formatHex(hash));
                exchange.sendResponseHeaders(200,method.equals("HEAD")?-1:zip.length);
                if(method.equals("GET"))exchange.getResponseBody().write(zip);
            }
        });
        server.start();
    }
    String url(){return "http://127.0.0.1:"+server.getAddress().getPort()+"/mobarmy.zip?v="+HexFormat.of().formatHex(hash);}
    @Override public void close(){server.stop(0);worker.shutdownNow();}
}
