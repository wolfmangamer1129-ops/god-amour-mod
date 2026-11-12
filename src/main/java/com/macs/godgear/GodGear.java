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
