package me.rerere.matrix.gui;

/** Command that opens the check and player status menus. */
public final class ChecksMenu extends me.rerere.matrix.commands.AbstractCommandDescriptor {
    private final me.rerere.matrix.thirdparty.kotlin.Lazy<me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1<me.rerere.matrix.gui.InventoryMenuItem, me.rerere.matrix.thirdparty.kotlin.Unit>> callbackFactory;

    public ChecksMenu() {
        super("gui", "", "Open the gui", "matrix.gui");
        this.callbackFactory = me.rerere.matrix.thirdparty.kotlin.LazyKt.lazy(() -> createPanelCallback(this));
    }

    private static String matrixVersion() {
        return "Matrix " + me.rerere.matrix.Matrix.Companion.matrixInstance()
            .getDescription().getVersion();
    }
    private static String matrixVersion(String version) { return "Matrix " + version; }

    private static String enabled(String value) { return "§a" + value; }
    private static String disabled(String value) { return "§c" + value; }
    private static String verboseState(String value) { return "§aVerbose has been " + value; }
    private static String notifyState(String value) { return "§aNotify has been " + value; }
    private static String statusText(String value) { return "§7Status: " + value; }
    private static String clientText(String value) { return "§7Client: §a" + value; }
    private static String pingText(String value) { return "§7Ping: §a" + value; }
    private static String currentPage(int page) { return "§7Current Page: §a" + page; }
    private static String violationCount(int total) {
        return "§7Total Violations: §a" + total;
    }
    private static String checkCount(String name, int count) { return "§f- §7" + name + ": §a" + count; }
    private static String playerEntryCount(String name, int count) { return "§a" + name + " " + count + " "; }
    private static String leftClickHelp(String action) { return "§fLeft Click §7to " + action + " §7Verbose"; }
    private static String rightClickHelp(String action) { return "§fRight Click §7to " + action + " §7Notify"; }
    private static String temporaryCheckHelp(String action) { return "§fClick §7to §ctemporarily " + action + " §7this check"; }
    private static String activeCheckRatio(int active, int total) { return "§7Status: §a" + active + "§7/§f" + total; }
    private static String shortStatus(String value) { return "§7" + value; }
    private static String playerStatusTitle() { return "Player Status"; }
    private static String checksTitle() { return "Checks"; }

    /** Shared behavior of the three identical no-argument pane factories. */
    private static org.bukkit.inventory.ItemStack fillerPane() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
        setDisplayName(item, "§r");
        return item;
    }

    /** Main checks item: count enabled enum values and show that count against the enum size. */
    private static org.bukkit.inventory.ItemStack checksIcon() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.COMPASS);
        setDisplayName(item, "§aChecks");
        java.util.Collection<?> checks = (java.util.Collection<?>) me.rerere.matrix.checks.CheckCategory
            .enumEntries();
        int enabledCount = 0;
        for (Object categoryValue : checks) {
            if (((me.rerere.matrix.checks.CheckCategory) categoryValue).isEnabled()) enabledCount++;
        }
        setLore(item, java.util.Arrays.asList("", "§7Manage the checks of Matrix", "",
            activeCheckRatio(enabledCount, checks.size())));
        return item;
    }

    /** Player-status menu item and its three-line description. */
    private static org.bukkit.inventory.ItemStack playerStatusIcon() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD);
        setDisplayName(item, "§aPlayer Status");
        setLore(item, java.util.Arrays.asList("", "§7Manage the status of players", ""));
        return item;
    }

    /** Return-to-parent navigation item. */
    private static org.bukkit.inventory.ItemStack backToMenuIcon() {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.BARRIER);
        setDisplayName(item, "§cBack to menu");
        return item;
    }

    private static void setDisplayName(org.bukkit.inventory.ItemStack item, String name) {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) meta.setDisplayName(name);
        item.setItemMeta(meta);
    }

    private static void setLore(org.bukkit.inventory.ItemStack item, java.util.List<String> lore) {
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) meta.setLore(lore);
        item.setItemMeta(meta);
    }

    /** Predicate used by the player list to keep only players with positive recorded counts. */
    private static boolean hasRecordedViolations(java.util.Map.Entry<?, ?> entry) {
        me.rerere.matrix.player.PlayerCheckContext playerRecord =
            (me.rerere.matrix.player.PlayerCheckContext) entry.getValue();
        int count = playerRecord.playerStorage().sumTotals();
        return count > 0;
    }

    private static org.bukkit.inventory.ItemStack previousPageButton(
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$IntRef pageIndex) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW);
        setDisplayName(item, "§aPrevious Page");
        setLore(item, java.util.Arrays.asList("", currentPage(pageIndex.element + 1)));
        return item;
    }

    private static org.bukkit.inventory.ItemStack nextPageButton(
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$IntRef pageIndex) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW);
        setDisplayName(item, "§aNext Page");
        setLore(item, java.util.Arrays.asList("", currentPage(pageIndex.element + 1)));
        return item;
    }

    private static org.bukkit.inventory.ItemStack checkItem(me.rerere.matrix.checks.CheckCategory check) {
        org.bukkit.inventory.ItemStack item = me.rerere.matrix.inventory.ItemStackBuilder
            .identity(new org.bukkit.inventory.ItemStack(
                check.isEnabled() ? org.bukkit.Material.GREEN_WOOL : org.bukkit.Material.RED_WOOL));
        me.rerere.matrix.inventory.ItemStackBuilder.setDisplayName(item,
            check.isEnabled() ? enabled(check.name()) : disabled(check.name()));
        java.util.List<String> lore = java.util.Arrays.asList(
            "", statusText(check.isEnabled() ? "§aEnable" : "§cDisable"), "",
            shortStatus(check.getDescription()), "",
            temporaryCheckHelp(check.isEnabled() ? "§cdisable" : "§aenable"));
        me.rerere.matrix.inventory.ItemStackBuilder.replaceLore(item, lore);
        return item;
    }

    private static org.bukkit.inventory.ItemStack playerToggleItem(org.bukkit.entity.Player player) {
        me.rerere.matrix.player.PlayerCheckContext verboseRecord = me.rerere.matrix.Matrix
            .Companion.matrixInstance().getPlayerDataManager()
            .find(player.getUniqueId());
        boolean verbose = verboseRecord != null && verboseRecord.verboseEnabled();
        me.rerere.matrix.player.PlayerCheckContext notifyRecord = me.rerere.matrix.Matrix
            .Companion.matrixInstance().getPlayerDataManager()
            .find(player.getUniqueId());
        boolean notify = notifyRecord != null && notifyRecord.checksActive();

        org.bukkit.inventory.ItemStack item = me.rerere.matrix.inventory.ItemStackBuilder
            .identity(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOOK));
        me.rerere.matrix.inventory.ItemStackBuilder.setDisplayName(item, "§aSwitch");
        java.util.List<String> lore = java.util.Arrays.asList(
            leftClickHelp(verbose ? "§cDisable" : "§aEnable"),
            rightClickHelp(notify ? "§cDisable" : "§aEnable"));
        me.rerere.matrix.inventory.ItemStackBuilder.replaceLore(item, lore);
        return item;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit registerInitialPane(
        me.rerere.matrix.gui.InventoryMenuItem itemEntry) {
        itemEntry.setIconSupplier(() -> fillerPane());
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit registerMenuPane(
        me.rerere.matrix.gui.InventoryMenuItem itemEntry) {
        itemEntry.setIconSupplier(() -> fillerPane());
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit registerReturnPane(
        me.rerere.matrix.gui.InventoryMenuItem itemEntry) {
        itemEntry.setIconSupplier(() -> fillerPane());
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1<me.rerere.matrix.gui.InventoryMenuItem, me.rerere.matrix.thirdparty.kotlin.Unit>
    createPanelCallback(ChecksMenu menu) {
        return entry -> registerBackPane(menu, entry);
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit registerBackPane(
        ChecksMenu menu, me.rerere.matrix.gui.InventoryMenuItem entry) {
        entry.setIconSupplier(() -> backToMenuIcon());
        entry.setClickHandler((row, player, click) -> backToMenuClick(menu, entry,
            (org.bukkit.entity.Player) player, (org.bukkit.event.inventory.ClickType) click));
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit backToMenuClick(
        ChecksMenu menu, me.rerere.matrix.gui.InventoryMenuItem entry, org.bukkit.entity.Player player,
        org.bukkit.event.inventory.ClickType click) {
        menu.buildMainMenu(player);
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private void buildMainMenu(org.bukkit.entity.Player player) {
        me.rerere.matrix.gui.InventoryLayoutMenu panel = new me.rerere.matrix.gui.InventoryLayoutMenu();
        panel.setTitleSupplier(() -> matrixVersion());
        panel.setLayout(new String[] {"#########", "##+#+#+##", "#########"});
        panel.registerButton((char) 35, entry -> registerReturnPane((me.rerere.matrix.gui.InventoryMenuItem) entry));
        panel.registerAction((char) 43, () -> playerMenuEntries(this, player, panel));
        panel.open(player);
    }

    private me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1<me.rerere.matrix.gui.InventoryMenuItem, me.rerere.matrix.thirdparty.kotlin.Unit>
    lazyCallback() {
        return callbackFactory.getValue();
    }

    public void execute(org.bukkit.command.CommandSender sender, String[] arguments) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage("§cOnly player can use this command");
            return;
        }
        buildMainMenu((org.bukkit.entity.Player) sender);
    }

    @Override
    public java.util.List<?> getTabCompletions() {
        // The canonical implementation returns its nullable legacy field, which is never initialized.
        return null;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit openPlayerList(ChecksMenu menu,
        me.rerere.matrix.gui.InventoryMenuItem itemEntry, org.bukkit.entity.Player player,
        org.bukkit.event.inventory.ClickType click) {
        menu.buildPlayerList(player);
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit openChecks(ChecksMenu menu,
        me.rerere.matrix.gui.InventoryMenuItem itemEntry, org.bukkit.entity.Player player,
        org.bukkit.event.inventory.ClickType click) {
        menu.buildChecks(player);
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private void buildChecks(org.bukkit.entity.Player player) {
        me.rerere.matrix.gui.InventoryLayoutMenu panel = new me.rerere.matrix.gui.InventoryLayoutMenu();
        panel.setTitleSupplier(() -> checksTitle());
        panel.setLayout(new String[] {"#########", "#+++++++#", "#+++++++#", "#+++++++#", "####x####"});
        panel.registerButton((char) 35, entry -> registerInitialPane((me.rerere.matrix.gui.InventoryMenuItem) entry));
        panel.registerButton((char) 120, lazyCallback());
        panel.registerAction((char) 43, () -> checksForPanel(panel));
        panel.open(player);
    }

    private void buildPlayerList(org.bukkit.entity.Player player) {
        me.rerere.matrix.gui.InventoryLayoutMenu panel = new me.rerere.matrix.gui.InventoryLayoutMenu();
        panel.setTitleSupplier(() -> playerStatusTitle());
        panel.setLayout(new String[] {"#########", "#+++++++#", "#+++++++#", "#+++++++#", "##<#x#>##"});
        panel.registerButton((char) 35, entry -> registerMenuPane((me.rerere.matrix.gui.InventoryMenuItem) entry));
        panel.registerButton((char) 120, lazyCallback());
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$IntRef pageIndex =
            new me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$IntRef();
        panel.registerButton((char) 60, entry -> registerPreviousPage(pageIndex, panel,
            (me.rerere.matrix.gui.InventoryMenuItem) entry));
        panel.registerButton((char) 62, entry -> registerNextPage(pageIndex, panel,
            (me.rerere.matrix.gui.InventoryMenuItem) entry));
        panel.registerAction((char) 43, () -> playerRecordItems(pageIndex));
        panel.open(player);
    }

    private static java.util.List<?> playerRecordItems(
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$IntRef pageIndex) {
        java.util.Map<?, ?> records = me.rerere.matrix.Matrix.Companion
            .matrixInstance().getPlayerDataManager().sessions();
        java.util.List<java.util.Map.Entry<?, ?>> filtered = new java.util.ArrayList<>();
        for (Object item : records.entrySet()) {
            java.util.Map.Entry<?, ?> entry = (java.util.Map.Entry<?, ?>) item;
            if (hasRecordedViolations(entry)) filtered.add(entry);
        }
        me.rerere.matrix.violation.ViolationLevelComparator comparator = new me.rerere.matrix.violation.ViolationLevelComparator();
        filtered.sort((left, right) -> comparator.compare(left, right));
        if (filtered.isEmpty()) return me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt.emptyList();
        java.util.List<?> slice = me.rerere.matrix.util.collections.ListPageSlicer.slice(
            filtered, pageIndex.element, 21);
        java.util.List<me.rerere.matrix.gui.InventoryMenuItem> mapped = new java.util.ArrayList<>();
        for (Object item : slice) {
            java.util.Map.Entry<?, ?> entry = (java.util.Map.Entry<?, ?>) item;
            me.rerere.matrix.player.PlayerCheckContext record =
                (me.rerere.matrix.player.PlayerCheckContext) entry.getValue();
            mapped.add(me.rerere.matrix.gui.InventoryMenuBuilders.item(
                ignored -> registerPlayerRecord(record, pageIndex, (me.rerere.matrix.gui.InventoryMenuItem) ignored)));
        }
        return me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt.toList(mapped);
    }

    private static java.util.List<?> checksForPanel(me.rerere.matrix.gui.InventoryLayoutMenu panel) {
        Iterable<?> values = (Iterable<?>) me.rerere.matrix.checks.CheckCategory
            .enumEntries();
        java.util.List<me.rerere.matrix.gui.InventoryMenuItem> items = new java.util.ArrayList<>();
        for (Object categoryValue : values) {
            me.rerere.matrix.checks.CheckCategory check = (me.rerere.matrix.checks.CheckCategory) categoryValue;
            items.add(me.rerere.matrix.gui.InventoryMenuBuilders.item(
                entry -> registerCheck(check, panel, (me.rerere.matrix.gui.InventoryMenuItem) entry)));
        }
        return items;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit registerCheck(
        me.rerere.matrix.checks.CheckCategory check, me.rerere.matrix.gui.InventoryLayoutMenu panel,
        me.rerere.matrix.gui.InventoryMenuItem entry) {
        entry.setIconSupplier(() -> checkItem(check));
        entry.setClickHandler((row, player, click) -> toggleCheck(check, panel,
            (me.rerere.matrix.gui.InventoryMenuItem) row, (org.bukkit.entity.Player) player,
            (org.bukkit.event.inventory.ClickType) click));
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit toggleCheck(
        me.rerere.matrix.checks.CheckCategory check, me.rerere.matrix.gui.InventoryLayoutMenu panel,
        me.rerere.matrix.gui.InventoryMenuItem entry, org.bukkit.entity.Player player,
        org.bukkit.event.inventory.ClickType click) {
        check.setEnabled(!check.isEnabled());
        panel.refresh();
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit registerPlayerRecord(
        me.rerere.matrix.player.PlayerCheckContext record,
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$IntRef pageIndex,
        me.rerere.matrix.gui.InventoryMenuItem entry) {
        entry.setIconSupplier(() -> playerRecordItem(record, pageIndex));
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static org.bukkit.inventory.ItemStack playerRecordItem(
        me.rerere.matrix.player.PlayerCheckContext record,
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$IntRef pageIndex) {
        org.bukkit.inventory.ItemStack item = me.rerere.matrix.inventory.ItemStackBuilder
            .identity(new org.bukkit.inventory.ItemStack(org.bukkit.Material.PLAYER_HEAD));
        me.rerere.matrix.inventory.ItemStackBuilder.setDisplayName(item,
            playerEntryCount(record.playerName(), pageIndex.element));
        me.rerere.matrix.inventory.ItemStackBuilder.setSkullOwner(item,
            record.cachedPlayer());

        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("");
        me.rerere.matrix.checks.player.ViolationTotals playerChecks = record.playerStorage();
        int totalViolations = playerChecks.sumTotals();
        lore.add(violationCount(totalViolations));

        java.util.Map<?, ?> checkCounts = playerChecks.positiveTotals();
        java.util.List<java.util.Map.Entry<?, ?>> sortedCounts = new java.util.ArrayList<>(checkCounts.entrySet());
        me.rerere.matrix.util.EntryValueComparator comparator = new me.rerere.matrix.util.EntryValueComparator();
        sortedCounts.sort((left, right) -> comparator.compare(left, right));
        for (java.util.Map.Entry<?, ?> countEntry : sortedCounts) {
            me.rerere.matrix.checks.CheckCategory check =
                (me.rerere.matrix.checks.CheckCategory) countEntry.getKey();
            me.rerere.matrix.violation.Violation counter =
                (me.rerere.matrix.violation.Violation) countEntry.getValue();
            lore.add(checkCount(check.name(), counter.getLevel()));
        }

        lore.add("");
        lore.add(pingText(kotlinDurationToString(record.smoothedLatency())));
        lore.add(clientText(record.clientVersion().getReleaseName()));
        lore.add("");
        me.rerere.matrix.inventory.ItemStackBuilder.replaceLore(item, lore);
        return item;
    }

    private static String kotlinDurationToString(long encodedDuration) {
        try {
            java.lang.invoke.MethodHandle toString = java.lang.invoke.MethodHandles.publicLookup().findStatic(
                me.rerere.matrix.thirdparty.kotlin.time.Duration.class, "toString-impl",
                java.lang.invoke.MethodType.methodType(String.class, long.class));
            return (String) toString.invokeExact(encodedDuration);
        } catch (Throwable failure) {
            return ChecksMenu.<RuntimeException, String>rethrow(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T rethrow(Throwable failure) throws E {
        throw (E) failure;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit registerNextPage(
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$IntRef pageIndex,
        me.rerere.matrix.gui.InventoryLayoutMenu panel, me.rerere.matrix.gui.InventoryMenuItem entry) {
        entry.setIconSupplier(() -> nextPageButton(pageIndex));
        entry.setClickHandler((row, player, click) -> incrementPage(pageIndex, panel,
            (me.rerere.matrix.gui.InventoryMenuItem) row, (org.bukkit.entity.Player) player,
            (org.bukkit.event.inventory.ClickType) click));
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit registerPreviousPage(
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$IntRef pageIndex,
        me.rerere.matrix.gui.InventoryLayoutMenu panel, me.rerere.matrix.gui.InventoryMenuItem entry) {
        entry.setIconSupplier(() -> previousPageButton(pageIndex));
        entry.setClickHandler((row, player, click) -> decrementPage(pageIndex, panel,
            (me.rerere.matrix.gui.InventoryMenuItem) row, (org.bukkit.entity.Player) player,
            (org.bukkit.event.inventory.ClickType) click));
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit decrementPage(
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$IntRef pageIndex,
        me.rerere.matrix.gui.InventoryLayoutMenu panel, me.rerere.matrix.gui.InventoryMenuItem entry,
        org.bukkit.entity.Player player, org.bukkit.event.inventory.ClickType click) {
        if (pageIndex.element > 0) {
            pageIndex.element--;
            panel.refresh();
        }
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit incrementPage(
        me.rerere.matrix.thirdparty.kotlin.jvm.internal.Ref$IntRef pageIndex,
        me.rerere.matrix.gui.InventoryLayoutMenu panel, me.rerere.matrix.gui.InventoryMenuItem entry,
        org.bukkit.entity.Player player, org.bukkit.event.inventory.ClickType click) {
        pageIndex.element++;
        panel.refresh();
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static java.util.List<?> playerMenuEntries(ChecksMenu menu, org.bukkit.entity.Player player,
        me.rerere.matrix.gui.InventoryLayoutMenu panel) {
        me.rerere.matrix.gui.InventoryMenuItem[] entries = new me.rerere.matrix.gui.InventoryMenuItem[3];
        entries[0] = me.rerere.matrix.gui.InventoryMenuBuilders.item(
            entry -> registerChecksButton(menu, (me.rerere.matrix.gui.InventoryMenuItem) entry));
        entries[1] = me.rerere.matrix.gui.InventoryMenuBuilders.item(
            entry -> registerPlayerButton(menu, (me.rerere.matrix.gui.InventoryMenuItem) entry));
        entries[2] = me.rerere.matrix.gui.InventoryMenuBuilders.item(
            entry -> registerPlayerControls(player, panel, (me.rerere.matrix.gui.InventoryMenuItem) entry));
        return me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt.listOf(entries);
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit registerChecksButton(ChecksMenu menu,
        me.rerere.matrix.gui.InventoryMenuItem entry) {
        entry.setIconSupplier(() -> checksIcon());
        entry.setClickHandler((row, clickedPlayer, click) -> openChecks(menu,
            (me.rerere.matrix.gui.InventoryMenuItem) row, (org.bukkit.entity.Player) clickedPlayer,
            (org.bukkit.event.inventory.ClickType) click));
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit registerPlayerButton(ChecksMenu menu,
        me.rerere.matrix.gui.InventoryMenuItem entry) {
        entry.setIconSupplier(() -> playerStatusIcon());
        entry.setClickHandler((row, clickedPlayer, click) -> openPlayerList(menu,
            (me.rerere.matrix.gui.InventoryMenuItem) row, (org.bukkit.entity.Player) clickedPlayer,
            (org.bukkit.event.inventory.ClickType) click));
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit registerPlayerControls(
        org.bukkit.entity.Player player, me.rerere.matrix.gui.InventoryLayoutMenu panel,
        me.rerere.matrix.gui.InventoryMenuItem entry) {
        entry.setIconSupplier(() -> playerToggleItem(player));
        entry.setClickHandler((row, clickedPlayer, click) -> playerStatusClick(panel,
            (me.rerere.matrix.gui.InventoryMenuItem) row,
            (org.bukkit.entity.Player) clickedPlayer, (org.bukkit.event.inventory.ClickType) click));
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }

    private static me.rerere.matrix.thirdparty.kotlin.Unit playerStatusClick(
        me.rerere.matrix.gui.InventoryLayoutMenu panel, me.rerere.matrix.gui.InventoryMenuItem itemEntry,
        org.bukkit.entity.Player player,
        org.bukkit.event.inventory.ClickType click) {
        me.rerere.matrix.player.PlayerCheckContext record = me.rerere.matrix.Matrix
            .Companion.matrixInstance().getPlayerDataManager()
            .find(player.getUniqueId());
        if (record == null) return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
        switch (click) {
            case LEFT:
                if (((me.rerere.matrix.runtime.ModuleRegistry) me.rerere.matrix.permissions.PermissionCacheManager
                    .COMPANION).isAllowed(player, "matrix.verbose")) {
                    record.setVerboseEnabled(!record.verboseEnabled());
                    player.sendMessage(record.verboseEnabled()
                        ? verboseState("§aenabled") : verboseState("§cdisabled"));
                    panel.refresh();
                }
                break;
            case RIGHT:
                if (((me.rerere.matrix.runtime.ModuleRegistry) me.rerere.matrix.permissions.PermissionCacheManager
                    .COMPANION).isAllowed(player, "matrix.notify")) {
                    record.setChecksActive(!record.checksActive());
                    player.sendMessage(record.checksActive()
                        ? notifyState("§aenabled") : notifyState("§cdisabled"));
                    panel.refresh();
                }
                break;
            default:
                break;
        }
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }
}
