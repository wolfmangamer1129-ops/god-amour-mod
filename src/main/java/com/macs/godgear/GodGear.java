package com.macs.godgear;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import java.util.*;

public class GodGear extends JavaPlugin implements Listener, CommandExecutor {
    private NamespacedKey key;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, String> bowMode = new HashMap<>(), spearMode = new HashMap<>();
    private final Map<UUID, Integer> riptideUses = new HashMap<>();
    private final Map<UUID, Long> riptideReset = new HashMap<>();
    private final Set<UUID> jumpReady = new HashSet<>();
    private final Set<UUID> safeFall = new HashSet<>();
    private final Set<UUID> mining = new HashSet<>();
    private final Set<UUID> threeByThree = new HashSet<>();
    private static final String[] WEAPONS = {"pickaxe","sword","axe","spear","mace","bow"};
    private static final String[] ARMOR = {"helmet","chestplate","leggings","boots"};

    @Override public void onEnable() {
        key = new NamespacedKey(this,"god_item");
        getServer().getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(getCommand("godgear")).setExecutor(this);
        getServer().getScheduler().runTaskTimer(this, () -> {
            for(Player p : Bukkit.getOnlinePlayers()) {
                ItemStack[] a = p.getInventory().getArmorContents();
                if(is(a[3],"helmet")) {
                    effect(p,PotionEffectType.NIGHT_VISION,240,0);
                    if(p.isInWater()) effect(p,PotionEffectType.DOLPHINS_GRACE,80,0);
                }
                if(is(a[2],"chestplate")) effect(p,PotionEffectType.RESISTANCE,80,0);
                if(is(a[1],"leggings")) effect(p,PotionEffectType.STRENGTH,80,0);
                if(is(a[0],"boots")) effect(p,PotionEffectType.SPEED,80,1);
                if(is(a[3],"helmet") && is(a[2],"chestplate") && is(a[1],"leggings") && is(a[0],"boots")) {
                    effect(p,PotionEffectType.REGENERATION,80,1);
                    effect(p,PotionEffectType.FIRE_RESISTANCE,80,0);
                }
                if(is(p.getInventory().getItemInMainHand(),"sword")) effect(p,PotionEffectType.STRENGTH,50,1);
                if(is(p.getInventory().getItemInMainHand(),"pickaxe")) effect(p,PotionEffectType.HASTE,50,2);
            }
        },20L,20L);
    }
    private void effect(Player p,PotionEffectType t,int ticks,int level) {p.addPotionEffect(new PotionEffect(t,ticks,level,true,false,true));}
    private boolean isGodArmor(ItemStack item) {for(String s:ARMOR)if(is(item,s))return true;return false;}
    private boolean is(ItemStack item,String id) {return item!=null && item.hasItemMeta() && id.equals(item.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING));}
    private String id(ItemStack item) {return item!=null && item.hasItemMeta()?item.getItemMeta().getPersistentDataContainer().get(key,PersistentDataType.STRING):null;}
    private void ench(ItemMeta m,Enchantment e,int lvl) {m.addEnchant(e,lvl,true);}
    private ItemStack make(String id) {
        Material material=switch(id) {
            case "pickaxe" -> Material.NETHERITE_PICKAXE;
            case "sword" -> Material.NETHERITE_SWORD;
            case "axe" -> Material.NETHERITE_AXE;
            case "spear" -> Material.TRIDENT;
            case "mace" -> Material.MACE;
            case "bow" -> Material.BOW;
            case "helmet" -> Material.NETHERITE_HELMET;
            case "chestplate" -> Material.NETHERITE_CHESTPLATE;
            case "leggings" -> Material.NETHERITE_LEGGINGS;
            case "boots" -> Material.NETHERITE_BOOTS;
            default -> null;
        };
        if(material==null)return null;
        ItemStack item=new ItemStack(material); ItemMeta m=item.getItemMeta();
        m.setDisplayName(ChatColor.LIGHT_PURPLE+"God "+id.substring(0,1).toUpperCase()+id.substring(1));
        m.setUnbreakable(true);
        m.getPersistentDataContainer().set(key,PersistentDataType.STRING,id);
        switch(id) {
            case "pickaxe" -> {ench(m,Enchantment.EFFICIENCY,10);ench(m,Enchantment.FORTUNE,5);}
            case "sword" -> {ench(m,Enchantment.SHARPNESS,10);ench(m,Enchantment.SWEEPING_EDGE,5);ench(m,Enchantment.KNOCKBACK,2);}
            case "axe" -> {ench(m,Enchantment.SHARPNESS,7);ench(m,Enchantment.KNOCKBACK,1);}
            case "spear" -> ench(m,Enchantment.RIPTIDE,3);
            case "mace" -> {ench(m,Enchantment.DENSITY,7);ench(m,Enchantment.BREACH,4);ench(m,Enchantment.WIND_BURST,2);}
            case "bow" -> {ench(m,Enchantment.POWER,10);ench(m,Enchantment.PUNCH,3);ench(m,Enchantment.FLAME,1);ench(m,Enchantment.INFINITY,1);}
            case "helmet" -> {ench(m,Enchantment.PROTECTION,7);ench(m,Enchantment.RESPIRATION,3);}
            default -> {if(Arrays.asList(ARMOR).contains(id))ench(m,Enchantment.PROTECTION,7);}
        }
        item.setItemMeta(m);return item;
    }
    @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
        if(args.length>=3 && args[0].equalsIgnoreCase("give")) {
            if(!sender.hasPermission("godgear.admin")) {sender.sendMessage("No permission");return true;}
            Player p=Bukkit.getPlayerExact(args[1]);if(p==null){sender.sendMessage("Player not online");return true;}
