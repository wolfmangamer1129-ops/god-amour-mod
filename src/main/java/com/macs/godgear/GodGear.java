package com.macs.godgear;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BookMeta;
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
    private final Set<String> mining = new HashSet<>();
    private final Set<UUID> threeByThree = new HashSet<>();
    private static final String[] WEAPONS = {"pickaxe","sword","axe","spear","mace","bow"};
    private static final String[] ARMOR = {"helmet","chestplate","leggings","boots"};

    @Override public void onEnable() {
        key = new NamespacedKey(this,"god_item");
        getServer().getPluginManager().registerEvents(this,this);
        registerRecipes();
        Bukkit.getOnlinePlayers().forEach(this::ensureGuide);
        Objects.requireNonNull(getCommand("godgear")).setExecutor(this);
        getServer().getScheduler().runTaskTimer(this, () -> {
            for(Player p : Bukkit.getOnlinePlayers()) {
                ItemStack[] a = p.getInventory().getArmorContents();
                if(is(a[3],"helmet")) {
                    effect(p,PotionEffectType.NIGHT_VISION,240,0);
                    if(p.isInWater()) effect(p,PotionEffectType.DOLPHINS_GRACE,80,0);
                }
                if(is(a[2],"chestplate")) { effect(p,PotionEffectType.FIRE_RESISTANCE,80,0); effect(p,PotionEffectType.STRENGTH,80,0); }
                if(is(a[1],"leggings")) { effect(p,PotionEffectType.SPEED,80,0); effect(p,PotionEffectType.JUMP_BOOST,80,0); }
                if(is(a[0],"boots")) { effect(p,PotionEffectType.SPEED,80,1); effect(p,PotionEffectType.SLOW_FALLING,80,0); }
                if(Arrays.stream(a).allMatch(this::isGodArmor)) {
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
            case "spear" -> Material.NETHERITE_SPEAR;
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
            case "spear" -> { /* Native spear; movement powers are plugin-controlled. */ }
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
            List<String> ids=new ArrayList<>();if(args[2].equalsIgnoreCase("all")) {ids.addAll(Arrays.asList(WEAPONS));ids.addAll(Arrays.asList(ARMOR));} else ids.add(args[2].toLowerCase(Locale.ROOT));
            for(String s:ids) {ItemStack item=make(s);if(item==null){sender.sendMessage("Unknown item: "+s);return true;}p.getInventory().addItem(item);}
            sender.sendMessage("God Gear given to "+p.getName());return true;
        }
        if(args.length==2 && args[0].equalsIgnoreCase("upgrade") && sender instanceof Player p) {
            String target=args[1].toLowerCase(Locale.ROOT);
            ItemStack old=p.getInventory().getItemInMainHand(), replacement=make(target);
            if(replacement==null){p.sendMessage("Unknown item");return true;}
            if(old.getType()!=replacement.getType() || id(old)!=null) {p.sendMessage("Hold a normal "+replacement.getType()+" to upgrade.");return true;}
            if(old.getAmount()>1){p.sendMessage("Hold only one item.");return true;}
            p.getInventory().setItemInMainHand(replacement);p.sendMessage("Upgraded to God "+target);return true;
        }
        sender.sendMessage("/godgear give <player> <item|all> or /godgear upgrade <item>");return true;
    }
    private boolean ready(Player p,String action,long millis) {
        long now=System.currentTimeMillis();Map<String,Long> map=cooldowns.computeIfAbsent(p.getUniqueId(),u->new HashMap<>());
        long last=map.getOrDefault(action,0L);if(now-last<millis){p.sendMessage(ChatColor.GRAY+"Recharge: "+((millis-now+last+999)/1000)+"s");return false;}
        map.put(action,now);return true;
    }
    private List<LivingEntity> enemies(Player p,Location at,double radius) {
        List<LivingEntity> result=new ArrayList<>();
        for(Entity e:at.getWorld().getNearbyEntities(at,radius,radius,radius)) if(e instanceof LivingEntity l && e!=p && l.getLocation().distanceSquared(at)<=radius*radius)result.add(l);
        return result;
    }
    @EventHandler public void onInteract(PlayerInteractEvent e) {
        if(e.getHand()!=EquipmentSlot.HAND)return;
        Player p=e.getPlayer();String weapon=id(p.getInventory().getItemInMainHand());
        if(weapon==null)return;
        boolean right=e.getAction().isRightClick(),left=e.getAction().isLeftClick();
        if(weapon.equals("pickaxe") && right) {e.setCancelled(true);if(!threeByThree.add(p.getUniqueId()))threeByThree.remove(p.getUniqueId());p.sendMessage("3x3 mining: "+(threeByThree.contains(p.getUniqueId())?"ON":"OFF"));return;}
        if(weapon.equals("bow")) {
            if(left) {
                e.setCancelled(true);
                String next=bowMode.getOrDefault(p.getUniqueId(),"teleport").equals("teleport")?"impulse":"teleport";
                bowMode.put(p.getUniqueId(),next);
                p.sendMessage("Bow mode: "+next);
            }
            // Right-click is NOT cancelled: vanilla draws and fires a real arrow.
            return;
        }
        if(weapon.equals("spear")) {
            if(right){e.setCancelled(true);String mode=spearMode.getOrDefault(p.getUniqueId(),"lunge");mode=mode.equals("lunge")?"riptide":"lunge";spearMode.put(p.getUniqueId(),mode);p.sendMessage("Spear mode: "+mode);}
            else if(left && e.getAction()==Action.LEFT_CLICK_AIR){e.setCancelled(true);String mode=spearMode.getOrDefault(p.getUniqueId(),"lunge");
                if(mode.equals("lunge")){if(!ready(p,"lunge",3000))return;float food=p.getSaturation();int hunger=p.getFoodLevel();p.setVelocity(p.getEyeLocation().getDirection().multiply(1.7).setY(.45));p.setFoodLevel(hunger);p.setSaturation(food);}
                else {
                    long now=System.currentTimeMillis();
                    UUID uid=p.getUniqueId();
                    long resetAt=riptideReset.getOrDefault(uid,0L);
                    if(resetAt!=0L && now>=resetAt) {
                        riptideUses.remove(uid);
                        riptideReset.remove(uid);
                    }
                    int uses=riptideUses.getOrDefault(uid,0);
                    if(uses>=3) {
                        long remaining=Math.max(1,(riptideReset.getOrDefault(uid,now+10000L)-now+999)/1000);
                        p.sendMessage("Riptide recharging: "+remaining+"s");
                        return;
                    }
                    p.setVelocity(p.getEyeLocation().getDirection().multiply(2.3));
                    riptideUses.put(uid,uses+1);
                    if(uses+1==3)riptideReset.put(uid,now+10000L);
                }
            }return;
        }
        if(weapon.equals("axe") && right) {e.setCancelled(true);if(!ready(p,"axe",12000))return;
            for(LivingEntity l:enemies(p,p.getLocation(),5)){l.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,40,10));l.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,40,128));}
            p.getWorld().playSound(p.getLocation(),Sound.ENTITY_RAVAGER_ROAR,1,1);return;
        }
        if(weapon.equals("mace") && right) {e.setCancelled(true);if(!ready(p,"quake",12000))return;
            Location at=p.getLocation();p.getWorld().spawnParticle(Particle.EXPLOSION,at,4);
            for(LivingEntity l:enemies(p,at,6)){l.damage(7,p);Vector v=l.getLocation().toVector().subtract(at.toVector()).normalize().multiply(1.4).setY(1.1);l.setVelocity(v);}return;
        }
        if(weapon.equals("sword") && left && e.getAction()==Action.LEFT_CLICK_AIR && ready(p,"thunder",7000)) {
            Block b=p.getTargetBlockExact(25);Location at=b==null?p.getLocation().add(p.getLocation().getDirection().multiply(5)):b.getLocation();
            p.getWorld().strikeLightningEffect(at);for(LivingEntity l:enemies(p,at,5))l.damage(12,p);
        }
    }
    @EventHandler public void onHit(EntityDamageByEntityEvent e) {
        if(e.getDamager() instanceof Player p && is(p.getInventory().getItemInMainHand(),"sword") && e.getEntity() instanceof LivingEntity l) {
            if(ready(p,"thunder",7000)){Location at=l.getLocation();at.getWorld().strikeLightningEffect(at);for(LivingEntity target:enemies(p,at,5))target.damage(12,p);}
        }
    }
    @EventHandler public void onFall(EntityDamageEvent e) {
        if(e.getEntity() instanceof Player p && e.getCause()==EntityDamageEvent.DamageCause.FALL) {
            if(safeFall.remove(p.getUniqueId()) || is(p.getInventory().getBoots(),"boots"))e.setCancelled(true);
        }
    }
    @EventHandler public void onFlight(PlayerToggleFlightEvent e) {
        Player p=e.getPlayer();if(p.getGameMode()!=GameMode.SURVIVAL && p.getGameMode()!=GameMode.ADVENTURE)return;
        if(!is(p.getInventory().getItemInMainHand(),"mace"))return;
        e.setCancelled(true);p.setFlying(false);p.setAllowFlight(false);
        if(!ready(p,"mace_jump",10000))return;
        p.setVelocity(new Vector(0,1.65,0));safeFall.add(p.getUniqueId());
    }
    @EventHandler public void onMove(PlayerMoveEvent e) {
        Player p=e.getPlayer();
        if(p.getGameMode()!=GameMode.SURVIVAL && p.getGameMode()!=GameMode.ADVENTURE)return;
        if(is(p.getInventory().getItemInMainHand(),"mace") && p.isOnGround() && !p.getAllowFlight())p.setAllowFlight(true);
        if(!is(p.getInventory().getItemInMainHand(),"mace") && p.getAllowFlight())p.setAllowFlight(false);
    }
    // Additional breaks are handled directly, so they do not recursively trigger the ability.
    private void breakExtra(Block b,Player p,ItemStack tool) {
        if(b.getType().isAir() || !b.getType().isSolid()
                || b.getType()==Material.BEDROCK || b.getType()==Material.BARRIER
                || b.getType()==Material.END_PORTAL_FRAME) return;
        BlockBreakEvent extra=new BlockBreakEvent(b,p);
        Bukkit.getPluginManager().callEvent(extra);
        if(!extra.isCancelled()) b.breakNaturally(tool);
    }

    @EventHandler(ignoreCancelled=true) public void onBreak(BlockBreakEvent e) {
        Player p=e.getPlayer();
        ItemStack tool=p.getInventory().getItemInMainHand();
        if(!is(tool,"pickaxe"))return;
        Block origin=e.getBlock();
        Material originalType=origin.getType();

        // Vein mining: connected ore blocks, up to 256 blocks.
        String type=originalType.name();
        if(type.endsWith("_ORE") || type.equals("ANCIENT_DEBRIS")) {
            Queue<Block> queue=new ArrayDeque<>();
            Set<String> seen=new HashSet<>();
            queue.add(origin);
            int count=0;
            while(!queue.isEmpty() && count<256 && seen.size()<4096) {
                Block b=queue.poll();
                String k=b.getX()+":"+b.getY()+":"+b.getZ();
                if(!seen.add(k)||b.getType()!=originalType)continue;
                if(b!=origin) {
                    BlockBreakEvent extra=new BlockBreakEvent(b,p);
                    Bukkit.getPluginManager().callEvent(extra);
                    if(extra.isCancelled())continue;
                    b.breakNaturally(tool);
                    count++;
                }
                for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)
                    if(x!=0||y!=0||z!=0)queue.add(b.getRelative(x,y,z));
            }
        }

        if(!threeByThree.contains(p.getUniqueId()))return;
        // The 3x3 plane is perpendicular to the face the player is mining.
        BlockFace face=p.getTargetBlockFace(6);
        if(face==null) {
            Vector direction=p.getEyeLocation().getDirection();
            if(Math.abs(direction.getY())>0.7)
                face=direction.getY()>0?BlockFace.UP:BlockFace.DOWN;
            else if(Math.abs(direction.getX())>Math.abs(direction.getZ()))
                face=direction.getX()>0?BlockFace.EAST:BlockFace.WEST;
            else face=direction.getZ()>0?BlockFace.SOUTH:BlockFace.NORTH;
        }
        for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++) {
            if(a==0&&b==0)continue;
            Block other;
            if(face==BlockFace.UP || face==BlockFace.DOWN)
                other=origin.getRelative(a,0,b);
            else if(face==BlockFace.EAST || face==BlockFace.WEST)
                other=origin.getRelative(0,a,b);
            else other=origin.getRelative(a,b,0);
            breakExtra(other,p,tool);
        }
    }

    private Material base(String id) {
        return switch(id) {
            case "pickaxe" -> Material.NETHERITE_PICKAXE;
            case "sword" -> Material.NETHERITE_SWORD;
            case "axe" -> Material.NETHERITE_AXE;
            case "spear" -> Material.NETHERITE_SPEAR;
            case "mace" -> Material.MACE;
            case "bow" -> Material.BOW;
            case "helmet" -> Material.NETHERITE_HELMET;
            case "chestplate" -> Material.NETHERITE_CHESTPLATE;
            case "leggings" -> Material.NETHERITE_LEGGINGS;
            case "boots" -> Material.NETHERITE_BOOTS;
            default -> null;
        };
    }

    private final Map<String,Material[]> extras=new LinkedHashMap<>();
    private void registerRecipes() {
        extras.put("sword",new Material[]{Material.TRIDENT});
        extras.put("axe",new Material[]{Material.TOTEM_OF_UNDYING});
        extras.put("pickaxe",new Material[]{Material.ANCIENT_DEBRIS,Material.ANCIENT_DEBRIS,Material.DIAMOND,Material.OBSIDIAN,Material.OBSIDIAN});
        extras.put("spear",new Material[]{Material.HEART_OF_THE_SEA});
        extras.put("mace",new Material[]{Material.BREEZE_ROD});
        extras.put("bow",new Material[]{Material.ENDER_PEARL});
        extras.put("helmet",new Material[]{Material.NAUTILUS_SHELL});
        extras.put("chestplate",new Material[]{Material.NETHER_STAR});
        extras.put("leggings",new Material[]{Material.GOAT_HORN});
        extras.put("boots",new Material[]{Material.RABBIT_FOOT,Material.FEATHER});
        for(var entry:extras.entrySet()) {
            String id=entry.getKey();
            ShapelessRecipe recipe=new ShapelessRecipe(new NamespacedKey(this,"craft_"+id),make(id));
            recipe.addIngredient(base(id));
            recipe.addIngredient(Material.ECHO_SHARD);
            for(Material m:entry.getValue())recipe.addIngredient(m);
            Bukkit.addRecipe(recipe);
        }
    }

    @EventHandler public void onPrepare(PrepareItemCraftEvent event) {
        if(!(event.getRecipe() instanceof ShapelessRecipe recipe))return;
        if(!recipe.getKey().equals(new NamespacedKey(this,"craft_sword")))return;
        boolean channeling=false;
        for(ItemStack ingredient:event.getInventory().getMatrix()) {
            if(ingredient!=null && ingredient.getType()==Material.TRIDENT &&
               ingredient.containsEnchantment(Enchantment.CHANNELING))channeling=true;
        }
        if(!channeling)event.getInventory().setResult(null);
    }

    private ItemStack guide() {
        ItemStack item=new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta=(BookMeta)item.getItemMeta();
        meta.setTitle("Mac's God Gear Recipes");
        meta.setAuthor("Mac");
        meta.addPage("MAC'S GOD GEAR\n\nPut the exact ingredients anywhere in a 3x3 crafting table.\n\nEvery recipe requires one Echo Shard and its base item.");
        for(var entry:extras.entrySet()) {
            String id=entry.getKey();
            StringBuilder page=new StringBuilder("GOD "+id.toUpperCase(Locale.ROOT)+"\n\nBase: "+nice(base(id))+"\n1 Echo Shard\n");
            Map<Material,Integer> counts=new LinkedHashMap<>();
            for(Material m:entry.getValue())counts.merge(m,1,Integer::sum);
            for(var count:counts.entrySet())page.append(count.getValue()).append(" ").append(nice(count.getKey())).append("\n");
            if(id.equals("sword"))page.append("\nTrident must have Channeling.");
            meta.addPage(page.toString());
        }
        item.setItemMeta(meta);
        return item;
    }
    private String nice(Material material) {
        return material.name().toLowerCase(Locale.ROOT).replace('_',' ');
    }
    private void ensureGuide(Player player) {
        for(ItemStack item:player.getInventory().getContents()) {
            if(item!=null && item.getType()==Material.WRITTEN_BOOK &&
               item.getItemMeta() instanceof BookMeta book &&
               "Mac's God Gear Recipes".equals(book.getTitle()) &&
               "Mac".equals(book.getAuthor()))return;
        }
        Map<Integer,ItemStack> leftovers=player.getInventory().addItem(guide());
        leftovers.values().forEach(item->player.getWorld().dropItemNaturally(player.getLocation(),item));
    }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(this,()->ensureGuide(event.getPlayer()),2L);
    }
    @EventHandler public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(this,()->ensureGuide(event.getPlayer()),2L);
    }

    @EventHandler(ignoreCancelled=true) public void onBowShot(EntityShootBowEvent event) {
        if(!(event.getEntity() instanceof Player player) || !is(event.getBow(),"bow") ||
           !(event.getProjectile() instanceof Arrow arrow))return;
        String mode=bowMode.getOrDefault(player.getUniqueId(),"teleport");
        arrow.getPersistentDataContainer().set(new NamespacedKey(this,"special_arrow"),PersistentDataType.STRING,mode);
    }
    @EventHandler(ignoreCancelled=true) public void onArrowLand(ProjectileHitEvent event) {
        if(!(event.getEntity() instanceof Arrow arrow) || !(arrow.getShooter() instanceof Player player))return;
        String mode=arrow.getPersistentDataContainer().get(new NamespacedKey(this,"special_arrow"),PersistentDataType.STRING);
        if(mode==null)return;
        Location impact=arrow.getLocation();
        if(mode.equals("teleport")) {
            if(!ready(player,"bow_teleport",10000))return;
            Location destination=impact.clone().add(0,1,0);
            if(destination.getBlock().isPassable() && destination.clone().add(0,1,0).getBlock().isPassable())
                player.teleport(destination);
            else player.sendMessage("No safe landing spot.");
        } else {
            if(!ready(player,"bow_impulse",5000))return;
            impact.getWorld().spawnParticle(Particle.EXPLOSION,impact,2);
            for(LivingEntity living:enemies(player,impact,5)) {
                Vector v=living.getLocation().toVector().subtract(impact.toVector());
                if(v.lengthSquared()>0.001)living.setVelocity(v.normalize().multiply(2).setY(.7));
                living.damage(2,player);
            }
        }
    }

}
