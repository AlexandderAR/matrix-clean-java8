package me.rerere.matrix.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.inventory.PlayerInventoryMenuListener;
import me.rerere.matrix.gui.InventoryMenuItem;

/** Character-grid inventory menu with per-symbol items and click actions. */
public final class InventoryLayoutMenu {
    private Function0<?> titleSupplier = this::defaultTitle;
    private Inventory inventory;
    private final Map<Character, InventoryMenuItem> buttons = new LinkedHashMap<Character, InventoryMenuItem>();
    private final Map<Character, List<InventoryMenuItem>> repeatedButtons =
            new LinkedHashMap<Character, List<InventoryMenuItem>>();
    private final Map<Character, Function0<?>> actions = new LinkedHashMap<Character, Function0<?>>();
    private String[] layout = new String[0];

    public InventoryLayoutMenu() { }

    public String defaultTitle() {
        return "";
    }

    private int getSymbolOccurrence(int symbol, int slot) {
        List<Integer> slots = new ArrayList<Integer>();
        Inventory currentInventory = inventory;
        if (currentInventory == null) Intrinsics.throwUninitializedPropertyAccessException("inventory");
        int size = currentInventory.getSize();
        for (int index = 0; index < size; index++) {
            char current = layout[index / 9].charAt(index % 9);
            if (current == symbol) slots.add(Integer.valueOf(index));
        }
        return slots.indexOf(Integer.valueOf(slot));
    }

    private int findSymbolSlot(int symbol, int occurrence) {
        List<Integer> slots = new ArrayList<Integer>();
        Inventory currentInventory = inventory;
        if (currentInventory == null) Intrinsics.throwUninitializedPropertyAccessException("inventory");
        int size = currentInventory.getSize();
        for (int slot = 0; slot < size; slot++) {
            char current = layout[slot / 9].charAt(slot % 9);
            if (current == symbol) slots.add(Integer.valueOf(slot));
        }
        return slots.get(occurrence).intValue();
    }

    private void populate(Inventory target) {
        target.clear();
        int size = target.getSize();
        for (int slot = 0; slot < size; slot++) {
            char symbol = layout[slot / 9].charAt(slot % 9);
            InventoryMenuItem button = buttons.get(Character.valueOf(symbol));
            if (button != null) {
                target.setItem(slot, button.getIcon());
            }
        }
        for (Map.Entry<Character, Function0<?>> action : actions.entrySet()) {
            Character symbol = action.getKey();
            Object generatedButtons = action.getValue().invoke();
            ((Map) repeatedButtons).put(symbol, generatedButtons);
        }
        for (Map.Entry<Character, List<InventoryMenuItem>> entry : repeatedButtons.entrySet()) {
            char symbol = entry.getKey().charValue();
            List<InventoryMenuItem> items = entry.getValue();
            int itemCount = items.size();
            for (int occurrence = 0; occurrence < itemCount; occurrence++) {
                InventoryMenuItem button = items.get(occurrence);
                int slot = findSymbolSlot(symbol, occurrence);
                target.setItem(slot, button.getIcon());
            }
        }
    }

    public final void refresh() {
        if (inventory == null) getInventory();
        Inventory currentInventory = inventory;
        if (currentInventory == null) {
            me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics
                    .throwUninitializedPropertyAccessException("inventory");
        }
        populate(currentInventory);
    }

    public void open(Player player) {
        if (inventory == null) getInventory();
        PlayerInventoryMenuListener.INSTANCE.put(player.getUniqueId(), this);
        Inventory currentInventory = inventory;
        if (currentInventory == null) Intrinsics.throwUninitializedPropertyAccessException("inventory");
        player.openInventory(currentInventory);
    }

    public void setLayout(String[] rows) {
        if (rows.length == 0) throw new IllegalArgumentException("Layout cannot be empty");
        for (String layoutRow : rows) {
            if (layoutRow.length() != 9) throw new IllegalArgumentException("Layout must be 9 characters long");
        }
        if (rows.length > 6) throw new IllegalArgumentException("Layout cannot be larger than 6");
        this.layout = Arrays.copyOf(rows, rows.length);
    }

    public boolean isInventory(Inventory candidate) {
        if (inventory == null) Intrinsics.throwUninitializedPropertyAccessException("inventory");
        return Objects.equals(candidate, inventory);
    }

    private Inventory getInventory() {
        inventory = createInventory();
        Inventory createdInventory = inventory;
        if (createdInventory == null) Intrinsics.throwUninitializedPropertyAccessException("inventory");
        populate(createdInventory);
        Inventory currentInventory = inventory;
        if (currentInventory == null) Intrinsics.throwUninitializedPropertyAccessException("inventory");
        return currentInventory;
    }

    public void closeFor(Player player) {
        PlayerInventoryMenuListener.INSTANCE.remove(player.getUniqueId());
        player.closeInventory();
    }

    public void handleClick(InventoryClickEvent event) {
        int row = event.getSlot() / 9;
        if (row >= layout.length) return;
        char symbol = layout[row].charAt(event.getSlot() % 9);
        InventoryMenuItem button = buttons.get(Character.valueOf(symbol));
        if (button != null) button.onClick(event);
        List<InventoryMenuItem> repeated = repeatedButtons.get(Character.valueOf(symbol));
        if (repeated != null) {
            int index = getSymbolOccurrence(symbol, event.getSlot());
            if (index >= 0 && index < repeated.size()) {
                InventoryMenuItem repeatedButton = repeated.get(index);
                if (repeatedButton != null) repeatedButton.onClick(event);
            }
        }
    }

    public void setTitleSupplier(Function0<?> supplier) {
        this.titleSupplier = supplier;
    }

    private Inventory createInventory() {
        return Bukkit.createInventory(null, 9 * layout.length, (String) titleSupplier.invoke());
    }

    public void registerAction(char symbol, Function0<?> action) {
        actions.put(Character.valueOf(symbol), action);
    }

    public void registerButton(char symbol, Function1<? super InventoryMenuItem, ?> configure) {
        InventoryMenuItem button = new InventoryMenuItem();
        configure.invoke(button);
        buttons.put(Character.valueOf(symbol), button);
    }
}
