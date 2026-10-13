package de.yuuki.mobarmy;

import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

final class ResourcePacks implements Listener {
    static final UUID ID=UUID.fromString("b506a7f3-ea72-4454-b7bb-1b6519e4e124");
    final MobArmy p;
    final Map<UUID,String> statuses=new HashMap<>();
    ResourcePackHost host;
    String url="";
    byte[] hash;
    ResourcePacks(MobArmy p){this.p=p;start();}
    void start(){
        try{
            if(p.getConfig().getBoolean("testing.local-resource-pack")){
                byte[] zip=bundle();int port=p.getConfig().getInt("resource-pack.port",8765);
                if(port<1||port>65535)throw new IOException("Invalid resource-pack.port");
                host=new ResourcePackHost(zip,port);url=host.url();hash=host.hash;
                p.getLogger().info("Local item textures available at "+url);
            }else{
                url=p.getConfig().getString("resource-pack.url","");String sha=p.getConfig().getString("resource-pack.sha1","");
                if(!url.isBlank()&&!url.matches("https?://[\\x21-\\x7e]+"))throw new IOException("resource-pack.url must be an ASCII HTTP(S) URL");
                if(!sha.isBlank()){hash=HexFormat.of().parseHex(sha);if(hash.length!=20)throw new IOException("resource-pack.sha1 must have 40 hex digits");}
            }
        }catch(Exception e){if(host!=null){host.close();host=null;}url="";hash=null;p.getLogger().warning("Item textures could not be served: "+e.getMessage()+". Use the included resource-pack ZIP manually or change resource-pack.port.");}
    }
    byte[] bundle()throws IOException{
        var bytes=new ByteArrayOutputStream();
        try(var index=p.getResource("mobarmy-pack/index.txt")){
            if(index==null)throw new IOException("Bundled pack index missing");
            List<String> paths=new String(index.readAllBytes(),StandardCharsets.UTF_8).lines().filter(s->!s.isBlank()).toList();
            try(var zip=new ZipOutputStream(bytes)){
                for(String path:paths)try(var asset=p.getResource("mobarmy-pack/"+path)){
                    if(asset==null)throw new IOException("Missing pack asset "+path);
                    var entry=new ZipEntry(path);entry.setTime(0);zip.putNextEntry(entry);asset.transferTo(zip);zip.closeEntry();
                }
            }
        }
        return bytes.toByteArray();
    }
    void offer(Player player){
        if(url.isBlank()){p.say(player,"pack.unavailable");return;}
        statuses.put(player.getUniqueId(),"requested");
        player.removeResourcePack(ID);
        player.addResourcePack(ID,url,hash,"MobArmy: Custom-Items und Chaoswächter-Texturen laden?",false);
    }
    String status(Player player){return p.tr(player,"pack.state."+statuses.getOrDefault(player.getUniqueId(),url.isBlank()?"not_configured":"not_requested"));}
    @EventHandler public void quit(org.bukkit.event.player.PlayerQuitEvent e){statuses.remove(e.getPlayer().getUniqueId());}
    @EventHandler public void status(PlayerResourcePackStatusEvent e){
        if(!ID.equals(e.getID()))return;
        String state=e.getStatus().name().toLowerCase(Locale.ROOT);
        statuses.put(e.getPlayer().getUniqueId(),state);
        if(e.getStatus()==PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED){p.items.refresh(e.getPlayer());p.say(e.getPlayer(),"pack.loaded");}
        else if(Set.of("declined","failed_download","failed_reload","invalid_url","discarded").contains(state))p.say(e.getPlayer(),"pack.failed","state",status(e.getPlayer()));
    }
    void restart(){if(host!=null)host.close();host=null;url="";hash=null;start();}
    void close(){if(host!=null)host.close();statuses.clear();}
}
