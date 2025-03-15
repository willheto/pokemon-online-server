package com.g8e.gameserver.models;

public class Shop {
    final private String shopID;
    final private String shopName;
    final private int sellsAtPercentage;
    final private int buysAtPercentage;
    final private boolean buysAnything;

    private Stock[] stocks;

    public Shop(String shopID, String shopName,
            int sellsAtPercentage, int buysAtPercentage,
            boolean buysAnything,
            Stock[] stocks) {
        this.shopID = shopID;
        this.shopName = shopName;
        this.sellsAtPercentage = sellsAtPercentage;
        this.buysAtPercentage = buysAtPercentage;
        this.buysAnything = buysAnything;
        this.stocks = stocks;
    }

    public void removeStock(int itemID) {
        Stock[] newStocks = new Stock[stocks.length - 1];
        int j = 0;
        for (Stock stock : stocks) {
            if (stock.getItemID() != itemID) {
                newStocks[j] = stock;
                j++;
            }
        }
        stocks = newStocks;
    }

    public void addStock(Stock stock) {
        Stock[] newStocks = new Stock[stocks.length + 1];
        System.arraycopy(stocks, 0, newStocks, 0, stocks.length);
        newStocks[stocks.length] = stock;
        stocks = newStocks;
    }

    public boolean getBuysAnything() {
        return buysAnything;
    }

    public String getShopID() {
        return shopID;
    }

    public String getShopName() {
        return shopName;
    }

    public Stock[] getStocks() {
        return stocks;
    }

    public float getSellsAtPercentage() {
        return sellsAtPercentage / 100.0f;
    }

    public float getBuysAtPercentage() {
        return buysAtPercentage / 100.0f;
    }

    public void setStocks(Stock[] stocks) {
        this.stocks = stocks;
    }

    public Stock getStock(int itemID) {
        for (Stock stock : stocks) {
            if (stock.getItemID() == itemID) {
                return stock;
            }
        }
        return null;
    }

    public void restock() {
        for (Stock stock : stocks) {
            stock.restock();
        }
    }

    public void restock(int itemID) {
        for (Stock stock : stocks) {
            if (stock.getItemID() == itemID) {
                stock.restock();
            }
        }
    }

}
