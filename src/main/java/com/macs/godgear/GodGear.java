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
