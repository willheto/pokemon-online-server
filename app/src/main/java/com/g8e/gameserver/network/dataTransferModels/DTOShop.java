package com.g8e.gameserver.network.dataTransferModels;

public class DTOShop {

    final private String shopName;
    final private int[] itemIDs;
    final private int[] itemAmounts;
    final private int[] itemPrices;

    public DTOShop(String shopName, int[] itemIDs, int[] itemAmounts, int[] itemPrices) {
        this.shopName = shopName;
        this.itemIDs = itemIDs;
        this.itemAmounts = itemAmounts;
        this.itemPrices = itemPrices;
    }

    public String getShopName() {
        return shopName;
    }

    public int[] getItemIDs() {
        return itemIDs;
    }

    public int[] getItemAmounts() {
        return itemAmounts;
    }

    public int[] getItemPrices() {
        return itemPrices;
    }

}
