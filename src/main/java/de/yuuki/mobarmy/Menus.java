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
    View view(Player player,String kind,String portal,int page,int item,String title){View v=new View(player.getUniqueId(),kind,portal,page,item);v.inventory=Bukkit.createInventory(v,54,MobArmy.color(title));return v;}
    ItemStack button(Material type,String name,String... lore){ItemStack stack=new ItemStack(type);var meta=stack.getItemMeta();meta.setDisplayName(MobArmy.color(name));meta.setLore(Arrays.stream(lore).map(MobArmy::color).toList());stack.setItemMeta(meta);return stack;}
    void navigation(Player pl,View v){v.inventory.setItem(45,button(Material.ARROW,p.tr(pl,"gui.back")));v.inventory.setItem(46,button(Material.ARROW,p.tr(pl,"gui.next")));}
    void party(Player player,int page){
        Session s=p.session(player.getUniqueId());if(s==null){mail(player,0);return;}
        View v=view(player,"party",s.portal.id,page,-1,p.tr(player,"party.title","stage",s.stage));
        List<UUID> members=new ArrayList<>(s.members);int start=Math.max(0,page)*28;
        for(int i=start;i<Math.min(start+28,members.size());i++){
            UUID id=members.get(i);Player member=Bukkit.getPlayer(id);String name=member==null?id.toString():member.getName();int slot=TextLayout.gridSlot(i-start);v.people.put(slot,id);
            long votes=s.votes.values().stream().filter(id::equals).count();v.inventory.setItem(slot,button(Material.PLAYER_HEAD,(id.equals(s.leader)?"&6★ ":"&f")+name,"",p.tr(player,"party.vote"),p.tr(player,"party.votes","votes",votes)));
        }
        navigation(player,v);
        v.inventory.setItem(47,button(Material.BOOK,p.tr(player,"party.stage","stage",s.stage),"",p.tr(player,"party.majority"),p.tr(player,"party.share","share",String.format(Locale.ROOT,"%.1f",100*Rules.bossShare(s.arena.minimum())))));
        v.inventory.setItem(48,button(Material.EMERALD_BLOCK,p.tr(player,"party.start"),"",p.tr(player,"party.only-leader"),p.tr(player,"party.count","count",s.members.size(),"minimum",s.arena.minimum()),p.tr(player,"party.phase","phase",p.tr(player,"phase."+s.phase))));
        v.inventory.setItem(50,button(Material.CHEST,p.tr(player,"mail.title"),"",p.tr(player,"mail.hint"),p.tr(player,"mail.give")));
        v.inventory.setItem(53,button(Material.BARRIER,p.tr(player,"party.leave")));p.language.open(player,v.inventory);
    }
    void timer(Player player,Portal portal){
        View v=view(player,"timer",portal.id,0,-1,p.tr(player,"timer.title"));int[] minutes={15,30,60,120};
        for(int i=0;i<5;i++)v.inventory.setItem(20+i,button(i==4?Material.BARRIER:Material.CLOCK,i==4?p.tr(player,"timer.training"):p.tr(player,"timer.duration","minutes",minutes[i]),"",p.tr(player,"timer.midnight")));
        p.language.open(player,v.inventory);
    }
    void mail(Player player,int page){
        List<ItemStack> mail=p.items.mailbox(player.getUniqueId());View v=view(player,"mail","",Math.max(0,page),-1,p.tr(player,"mail.title"));
        int start=v.page*45;for(int i=start;i<Math.min(mail.size(),start+45);i++)v.inventory.setItem(i-start,mail.get(i).clone());navigation(player,v);
        v.inventory.setItem(49,button(Material.BOOK,p.tr(player,"mail.hint"),"",p.tr(player,"mail.give")));v.inventory.setItem(53,button(Material.OAK_DOOR,p.tr(player,"party.menu")));p.language.open(player,v.inventory);
    }
    List<UUID> eligible(Player player){
        Session s=p.session(player.getUniqueId());if(s!=null&&s.leader.equals(player.getUniqueId()))return new ArrayList<>(s.members);
        return p.state.getStringList("loot-recipients."+player.getUniqueId()).stream().map(UUID::fromString).toList();
    }
    void recipients(Player player,int index,int page){
        List<UUID> members=eligible(player);if(members.isEmpty()){p.say(player,"mail.no-party");return;}
        View v=view(player,"give","",page,index,p.tr(player,"mail.give-title"));
        for(int i=page*28;i<Math.min(page*28+28,members.size());i++){UUID id=members.get(i);String name=Objects.toString(Bukkit.getOfflinePlayer(id).getName(),id.toString());int slot=TextLayout.gridSlot(i-page*28);v.people.put(slot,id);v.inventory.setItem(slot,button(Material.PLAYER_HEAD,"&f"+name));}
        navigation(player,v);p.language.open(player,v.inventory);
    }
    @EventHandler public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof View v))return;
        event.setCancelled(true);if(!(event.getWhoClicked() instanceof Player player)||!v.owner.equals(player.getUniqueId()))return;
        int slot=event.getRawSlot();if(slot<0||slot>=54)return;
        switch(v.kind){
            case "timer"->{Portal portal=p.portals.get(v.portal);if(portal==null||!player.hasPermission("mobarmy.timer"))return;long[] times={900000,1800000,3600000,7200000,0};if(slot<20||slot>24)return;portal.open=true;if(times[slot-20]>0)portal.setTimer(times[slot-20]);else{portal.deadline=0;p.saveState();}player.closeInventory();p.say(player,"timer.ready","id",portal.id);}
            case "party"->{Session s=p.session(player.getUniqueId());if(s==null||!s.portal.id.equals(v.portal)){player.closeInventory();return;}if(v.people.containsKey(slot)){p.engine.vote(player,v.people.get(slot));party(player,v.page);}else if(slot==48)p.engine.ready(player);else if(slot==50)mail(player,0);else if(slot==53){player.closeInventory();p.engine.leave(player,false);}else if(slot==45)party(player,Math.max(0,v.page-1));else if(slot==46&&s.members.size()>(v.page+1)*28)party(player,v.page+1);}
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
                if(slot==45){recipients(player,v.item,Math.max(0,v.page-1));return;}if(slot==46){if(eligible(player).size()>(v.page+1)*28)recipients(player,v.item,v.page+1);return;}
                UUID recipient=v.people.get(slot);if(recipient==null||!eligible(player).contains(recipient))return;
                List<ItemStack> mail=p.items.mailbox(player.getUniqueId());if(v.item>=mail.size())return;ItemStack item=mail.remove(v.item);p.items.mailbox(player.getUniqueId(),mail);p.items.deliver(recipient,item);mail(player,0);
            }
        }
    }
    @EventHandler public void drag(InventoryDragEvent event){if(event.getView().getTopInventory().getHolder() instanceof View)event.setCancelled(true);}
}
