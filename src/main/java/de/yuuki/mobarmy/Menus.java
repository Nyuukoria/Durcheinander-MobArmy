package de.yuuki.mobarmy;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import java.util.*;

final class Menus implements Listener {
    final MobArmy p;
    Menus(MobArmy p){this.p=p;}
    static final class View implements InventoryHolder {
        final UUID owner;final String kind;final String portal;final int page;final int item;
        final Map<Integer,UUID> people=new HashMap<>();Inventory inventory;
        View(UUID owner,String kind,String portal,int page,int item){this.owner=owner;this.kind=kind;this.portal=portal;this.page=page;this.item=item;}
        public Inventory getInventory(){return inventory;}
    }
    View view(Player player,String kind,String portal,int page,int item,String title){View v=new View(player.getUniqueId(),kind,portal,page,item);v.inventory=Bukkit.createInventory(v,54,MobArmy.color(p.text(player,title)));return v;}
    ItemStack button(Material type,String name,String... lore){ItemStack stack=new ItemStack(type);var meta=stack.getItemMeta();meta.setDisplayName(MobArmy.color(name));meta.setLore(Arrays.stream(lore).map(MobArmy::color).toList());stack.setItemMeta(meta);return stack;}
    void party(Player player,int page){
        Session s=p.session(player.getUniqueId());if(s==null){mail(player,0);return;}
        View v=view(player,"party",s.portal.id,page,-1,"&dDungeon • Stage "+s.stage);
        List<UUID> members=new ArrayList<>(s.members);int start=Math.max(0,page)*45;
        for(int i=start;i<Math.min(start+45,members.size());i++){
            UUID id=members.get(i);Player member=Bukkit.getPlayer(id);String name=member==null?id.toString():member.getName();
            v.people.put(i-start,id);long votes=s.votes.values().stream().filter(id::equals).count();
            v.inventory.setItem(i-start,button(Material.PLAYER_HEAD,(id.equals(s.leader)?"&6★ ":"&f")+name,"&7Klicken: Leader wählen","&7Stimmen: "+votes));
        }
        v.inventory.setItem(45,button(Material.ARROW,"&fZurück"));v.inventory.setItem(46,button(Material.ARROW,"&fWeiter"));
        v.inventory.setItem(47,button(Material.BOOK,"&dStage "+s.stage+" / 5","&7Leader wird per Mehrheit gewählt.",String.format(Locale.ROOT,"&7Boss-Anteil pro Spieler: %.1f%%",100*Rules.bossShare(s.arena.minimum()))));
        v.inventory.setItem(48,button(Material.EMERALD_BLOCK,"&aReady / Stage starten","&7Minimum: "+s.arena.minimum(),"&7Aktuell: "+s.members.size(),"&7Nach Ready: Teleport in die Arena.","&7Phase: "+s.phase));
        v.inventory.setItem(50,button(Material.CHEST,"&dLoot-Postfach","&7Deine Items abholen / verteilen"));
        v.inventory.setItem(53,button(Material.BARRIER,"&cDungeon verlassen"));p.language.open(player,v.inventory);
    }
    void timer(Player player,Portal portal){
        View v=view(player,"timer",portal.id,0,-1,"&dReward-Zeit wählen");
        String[] labels={"15 Minuten","30 Minuten","60 Minuten","120 Minuten","Keine Rewards"};
        for(int i=0;i<labels.length;i++)v.inventory.setItem(20+i,button(i==4?Material.BARRIER:Material.CLOCK,"&e"+labels[i],"&7Gilt bis spätestens Mitternacht"));
        p.language.open(player,v.inventory);
    }
    void mail(Player player,int page){
        List<ItemStack> mail=p.items.mailbox(player.getUniqueId());View v=view(player,"mail","",Math.max(0,page),-1,"&dLoot-Postfach");
        int start=v.page*45;for(int i=start;i<Math.min(mail.size(),start+45);i++)v.inventory.setItem(i-start,mail.get(i).clone());
        v.inventory.setItem(45,button(Material.ARROW,"&fZurück"));v.inventory.setItem(46,button(Material.ARROW,"&fWeiter"));
        v.inventory.setItem(49,button(Material.BOOK,"&fLinks: Abholen","&7Rechts: an Gruppenmitglied senden"));v.inventory.setItem(53,button(Material.OAK_DOOR,"&fDungeon-Menü"));p.language.open(player,v.inventory);
    }
    List<UUID> eligible(Player player){
        Session s=p.session(player.getUniqueId());if(s!=null&&s.leader.equals(player.getUniqueId()))return new ArrayList<>(s.members);
        return p.state.getStringList("loot-recipients."+player.getUniqueId()).stream().map(UUID::fromString).toList();
    }
    void recipients(Player player,int index,int page){
        List<UUID> members=eligible(player);if(members.isEmpty()){p.tell(player,"Keine Gruppe zum Verteilen gespeichert.");return;}
        View v=view(player,"give","",page,index,"&dLoot verschenken");
        for(int i=page*45;i<Math.min(page*45+45,members.size());i++){UUID id=members.get(i);String name=Objects.toString(Bukkit.getOfflinePlayer(id).getName(),id.toString());v.people.put(i-page*45,id);v.inventory.setItem(i-page*45,button(Material.PLAYER_HEAD,"&f"+name));}
        v.inventory.setItem(45,button(Material.ARROW,"&fZurück"));v.inventory.setItem(46,button(Material.ARROW,"&fWeiter"));p.language.open(player,v.inventory);
    }
    @EventHandler public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof View v))return;
        event.setCancelled(true);if(!(event.getWhoClicked() instanceof Player player)||!v.owner.equals(player.getUniqueId()))return;
        int slot=event.getRawSlot();if(slot<0||slot>=54)return;
        switch(v.kind){
            case "timer"->{Portal portal=p.portals.get(v.portal);if(portal==null||!player.hasPermission("mobarmy.timer"))return;long[] times={900000,1800000,3600000,7200000,0};if(slot<20||slot>24)return;portal.open=true;if(times[slot-20]>0)portal.setTimer(times[slot-20]);else{portal.deadline=0;p.saveState();}player.closeInventory();p.tell(player,"Portal bereit: "+portal.id+". /raid join "+portal.id);}
            case "party"->{Session s=p.session(player.getUniqueId());if(s==null||!s.portal.id.equals(v.portal)){player.closeInventory();return;}if(v.people.containsKey(slot)){p.engine.vote(player,v.people.get(slot));party(player,v.page);}else if(slot==48)p.engine.ready(player);else if(slot==50)mail(player,0);else if(slot==53){player.closeInventory();p.engine.leave(player,false);}else if(slot==45)party(player,Math.max(0,v.page-1));else if(slot==46&&s.members.size()>(v.page+1)*45)party(player,v.page+1);}
            case "mail"->{
                List<ItemStack> mail=p.items.mailbox(player.getUniqueId());int index=v.page*45+slot;
                if(slot<45&&index<mail.size()){
                    // Stale views must never extract a different mailbox item.
                    if(!Objects.equals(event.getCurrentItem(),mail.get(index))){mail(player,v.page);return;}
                    if(event.isRightClick()){recipients(player,index,0);return;}
                    ItemStack item=mail.remove(index);var overflow=player.getInventory().addItem(item);mail.addAll(index,overflow.values());p.items.mailbox(player.getUniqueId(),mail);mail(player,v.page);
                }else if(slot==45)mail(player,Math.max(0,v.page-1));else if(slot==46&&mail.size()>(v.page+1)*45)mail(player,v.page+1);else if(slot==53)party(player,0);
            }
            case "give"->{
                if(slot==45){recipients(player,v.item,Math.max(0,v.page-1));return;}if(slot==46){if(eligible(player).size()>(v.page+1)*45)recipients(player,v.item,v.page+1);return;}
                UUID recipient=v.people.get(slot);if(recipient==null||!eligible(player).contains(recipient))return;
                List<ItemStack> mail=p.items.mailbox(player.getUniqueId());if(v.item>=mail.size())return;ItemStack item=mail.remove(v.item);p.items.mailbox(player.getUniqueId(),mail);p.items.deliver(recipient,item);mail(player,0);
            }
        }
    }
    @EventHandler public void drag(InventoryDragEvent event){if(event.getView().getTopInventory().getHolder() instanceof View)event.setCancelled(true);}
}
