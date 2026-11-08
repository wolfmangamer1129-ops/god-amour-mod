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
        Player p=e.getPlayer();String weapon=id(p.getInventory().getItemInMainHand());
        if(weapon==null)return;
        boolean right=e.getAction().isRightClick(),left=e.getAction().isLeftClick();
        if(weapon.equals("pickaxe") && right) {e.setCancelled(true);if(!threeByThree.add(p.getUniqueId()))threeByThree.remove(p.getUniqueId());p.sendMessage("3x3 mining: "+(threeByThree.contains(p.getUniqueId())?"ON":"OFF"));return;}
        if(weapon.equals("bow")) {
            if(left){e.setCancelled(true);String mode=bowMode.getOrDefault(p.getUniqueId(),"teleport");mode=mode.equals("teleport")?"impulse":"teleport";bowMode.put(p.getUniqueId(),mode);p.sendMessage("Bow mode: "+mode);}
            else if(right){e.setCancelled(true);String mode=bowMode.getOrDefault(p.getUniqueId(),"teleport");if(!ready(p,"bow_"+mode,mode.equals("teleport")?10000:5000))return;
                if(mode.equals("teleport")) {
                    Block target=p.getTargetBlockExact(30);if(target==null){p.sendMessage("Aim at a block within 30 blocks.");return;}
                    Location loc=target.getLocation().add(0.5,1,0.5);
                    if(!loc.getBlock().isPassable() || !loc.clone().add(0,1,0).getBlock().isPassable()){p.sendMessage("No safe landing spot.");return;}
                    loc.setYaw(p.getLocation().getYaw());loc.setPitch(p.getLocation().getPitch());p.teleport(loc);
                }else {
                    Location impact=p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(8));
                    p.getWorld().spawnParticle(Particle.EXPLOSION,impact,2);
                    for(LivingEntity l:enemies(p,impact,5)) {Vector v=l.getLocation().toVector().subtract(impact.toVector()).normalize().multiply(2.0).setY(.7);l.setVelocity(v);l.damage(2,p);}
                }
            }return;
        }
        if(weapon.equals("spear")) {
            if(right){e.setCancelled(true);String mode=spearMode.getOrDefault(p.getUniqueId(),"lunge");mode=mode.equals("lunge")?"riptide":"lunge";spearMode.put(p.getUniqueId(),mode);p.sendMessage("Spear mode: "+mode);}
            else if(left && e.getAction()==Action.LEFT_CLICK_AIR){e.setCancelled(true);String mode=spearMode.getOrDefault(p.getUniqueId(),"lunge");
                if(mode.equals("lunge")){if(!ready(p,"lunge",3000))return;float food=p.getSaturation();int hunger=p.getFoodLevel();p.setVelocity(p.getEyeLocation().getDirection().multiply(1.7).setY(.45));p.setFoodLevel(hunger);p.setSaturation(food);}
                else {long now=System.currentTimeMillis();if(now>=riptideReset.getOrDefault(p.getUniqueId(),0L)){riptideUses.put(p.getUniqueId(),0);riptideReset.put(p.getUniqueId(),Long.MAX_VALUE);}
                    int uses=riptideUses.getOrDefault(p.getUniqueId(),0);if(uses>=3){p.sendMessage("Riptide recharging.");return;}
                    p.setVelocity(p.getEyeLocation().getDirection().multiply(2.3));riptideUses.put(p.getUniqueId(),uses+1);
                    if(uses+1==3)riptideReset.put(p.getUniqueId(),now+60000);
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
    @EventHandler public void onBreak(BlockBreakEvent e) {
        Player p=e.getPlayer();if(!is(p.getInventory().getItemInMainHand(),"pickaxe"))return;
        Block origin=e.getBlock();String type=origin.getType().name();
        if(type.endsWith("_ORE") || type.equals("ANCIENT_DEBRIS")) {
            Queue<Block> queue=new ArrayDeque<>();Set<String> seen=new HashSet<>();queue.add(origin);
            int count=0;
            while(!queue.isEmpty() && count<256) {
                Block b=queue.poll();String k=b.getWorld().getUID()+":"+b.getX()+":"+b.getY()+":"+b.getZ();
                if(!seen.add(k)||b.getType()!=origin.getType())continue;
                if(b!=origin) {BlockBreakEvent extra=new BlockBreakEvent(b,p);Bukkit.getPluginManager().callEvent(extra);if(!extra.isCancelled()){b.breakNaturally(p.getInventory().getItemInMainHand());count++;}}
                for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)if(x!=0||y!=0||z!=0)queue.add(b.getRelative(x,y,z));
            }
        }
        if(threeByThree.contains(p.getUniqueId())) {
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) {
                if(x==0&&z==0)continue;Block b=origin.getRelative(x,0,z);
                if(!b.getType().isAir() && b.getType().isSolid()){BlockBreakEvent extra=new BlockBreakEvent(b,p);Bukkit.getPluginManager().callEvent(extra);if(!extra.isCancelled())b.breakNaturally(p.getInventory().getItemInMainHand());}
            }
        }
    }
}
