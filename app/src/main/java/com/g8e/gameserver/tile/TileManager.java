package com.g8e.gameserver.tile;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.imageio.ImageIO;

import com.g8e.gameserver.World;
import com.g8e.gameserver.enums.Direction;

public class TileManager {

    private World world;
    public Tile[] tile;
    public int[][] mapTileNumLayer1;

    public int chunkSize = 50;

    public TileManager(World world) {
        this.world = world;
        tile = new Tile[8000];

        // Initialize tile maps for each layer
        mapTileNumLayer1 = new int[world.maxWorldCol][world.maxWorldRow];

        getTiles();
        loadMap("/data/map/pokemon_online_map.csv", 1); // Load layer 1 map

    }

    public TilePosition getClosestWalkableTile(int x, int y) {
        try {
            // consider x and y being the bottom left 8px * 8px of 16px * 16px tile

            // check if the tile is walkable
            if (!getCollisionByXandY(x, y)) {
                return new TilePosition(x, y);
            }

            // check if the tile to the right is walkable
            if (!getCollisionByXandY(x + 2, y)) {
                return new TilePosition(x + 2, y);
            }

            // check if the tile to the left is walkable
            if (!getCollisionByXandY(x - 1, y)) {
                return new TilePosition(x - 1, y);
            }

            // check if the tile to the top is walkable
            if (!getCollisionByXandY(x, y + 2)) {
                return new TilePosition(x, y + 2);
            }

            // check if the tile to the bottom is walkable
            if (!getCollisionByXandY(x, y - 1)) {
                return new TilePosition(x, y - 1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public int getChunkByWorldXandY(int worldX, int worldY) {
        try {
            // world is divided into chunks of 10x10 tiles
            // starting from top left corner of the world
            int chunkX = worldX / chunkSize;
            int chunkY = worldY / chunkSize;
            return chunkX + chunkY * (world.maxWorldCol / chunkSize);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public int[] getNeighborChunks(int chunk) {
        try {
            int[] neighbors = new int[8];
            int chunkX = chunk % (world.maxWorldCol / chunkSize);
            int chunkY = chunk / (world.maxWorldCol / chunkSize);

            neighbors[0] = chunkX - 1 + (chunkY - 1) * (world.maxWorldCol / chunkSize);
            neighbors[1] = chunkX + (chunkY - 1) * (world.maxWorldCol / chunkSize);
            neighbors[2] = chunkX + 1 + (chunkY - 1) * (world.maxWorldCol / chunkSize);
            neighbors[3] = chunkX - 1 + chunkY * (world.maxWorldCol / chunkSize);
            neighbors[4] = chunkX + 1 + chunkY * (world.maxWorldCol / chunkSize);
            neighbors[5] = chunkX - 1 + (chunkY + 1) * (world.maxWorldCol / chunkSize);
            neighbors[6] = chunkX + (chunkY + 1) * (world.maxWorldCol / chunkSize);
            neighbors[7] = chunkX + 1 + (chunkY + 1) * (world.maxWorldCol / chunkSize);

            return neighbors;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // Is this even needed anymore?
    public Tile getTileByXandY(int x, int y) {
        try {
            // For now, return the tile from Layer 1
            int index = mapTileNumLayer1[x][y];
            return tile[index];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean getCollisionByXandY(int x, int y) {
        try {
            // Check if x and y are within the bounds of the map arrays
            if (x < 0 || y < 0 || x >= mapTileNumLayer1.length || y >= mapTileNumLayer1[0].length) {
                return false; // Out of bounds, no collision
            }

            int index1 = mapTileNumLayer1[x][y];
            int index2 = mapTileNumLayer1[x + 1][y];

            // Check for valid tile indices and if any of them have a collision
            if ((index1 >= 0 && tile[index1].collision)) {
                return true;
            }

            if ((index2 >= 0 && tile[index2].collision)) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean getCollisionByXandY(int currentX, int currentY, int x, int y) {
        try {
            // Check if x and y are within the bounds of the map arrays
            if (x < 0 || y < 0 || x >= mapTileNumLayer1.length || y >= mapTileNumLayer1[0].length) {
                return false; // Out of bounds, no collision
            }

            int index1 = mapTileNumLayer1[x][y];
            int index2 = mapTileNumLayer1[x + 1][y];

            Direction directionComingFrom = currentX < x ? Direction.RIGHT
                    : currentX > x ? Direction.LEFT : currentY < y ? Direction.UP : Direction.DOWN;

            if (tile[index1].numberRepresentation == 249 && directionComingFrom != Direction.UP) {
                return true;
            }

            if (tile[index2].numberRepresentation == 249 && directionComingFrom != Direction.UP) {
                return true;
            }

            // Check for valid tile indices and if any of them have a collision
            if ((index1 >= 0 && tile[index1].collision)) {
                return true;
            }

            if ((index2 >= 0 && tile[index2].collision)) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void getTiles() {
        try {
            BufferedImage tileSheet = ImageIO
                    .read(getClass().getResourceAsStream("/data/tilesheets/pokemon_online_tilesheet.png"));

            int numTilesAcross = tileSheet.getWidth() / 8;
            tile = new Tile[numTilesAcross * 100];
            for (int col = 0; col < numTilesAcross; col++) {
                for (int row = 0; row < 100; row++) {
                    int index = col + row * numTilesAcross;

                    if (index >= 310 && index <= 313) {
                        tile[index] = new Tile(false, index);
                    } else if (index == 869) {
                        tile[index] = new Tile(false, index); // floor on house
                    } else if (index >= 1139 && index <= 1142) {
                        tile[index] = new Tile(false, index); // floor red on house
                    } else if (index >= 1201 && index <= 1204) {
                        tile[index] = new Tile(false, index); // floor red on house
                    } else if (index == 955 || index == 966 || index == 893 || index == 894) {
                        tile[index] = new Tile(false, index); // floor red on house
                    } else if (index == 261 || index == 262 || index == 323 || index == 324) {
                        tile[index] = new Tile(false, index); // doors
                    } else if (index >= 226 && index <= 229) {
                        tile[index] = new Tile(false, index); // doors
                    } else if (index == 2046 || index == 2047 || index == 1984 || index == 1985) { // ladder gray
                        tile[index] = new Tile(false, index); // doors
                    } else if (index == 2003 || index == 2004 || index == 2065 || index == 2066) { // ladder gray
                        tile[index] = new Tile(false, index); // doors
                    } else if (index == 709 || index == 771 || index == 772) {
                        tile[index] = new Tile(false, index); // white red flooring
                    } else if (index == 1139 || index == 1140 || index == 1141 || index == 1142
                            || index == 1201 || index == 1202 || index == 1203 || index == 1204) {
                        tile[index] = new Tile(false, index); // white red flooring
                    } else if (index == 2346 || index == 2347 || index == 2348 || index == 2349
                            || index == 2498 || index == 2499 || index == 2500 || index == 2501) {
                        tile[index] = new Tile(false, index); // white red flooring
                    } else if (index == 705 || index == 706 || index == 707 || index == 708
                            || index == 767 || index == 768 || index == 769 || index == 770) {
                        tile[index] = new Tile(false, index); // white red flooring
                    } else if (index == 1207 || index == 1208 || index == 1209 || index == 1210) {
                        tile[index] = new Tile(false, index); // brown flooring
                    } else if (index == 2436 || index == 2437 || index == 2438 || index == 2438) {
                        tile[index] = new Tile(false, index); // brown flooring
                    } else if (index == 2498 || index == 2499 || index == 2500 || index == 2501) {
                        tile[index] = new Tile(false, index); // brown flooring
                    } else if (index == 2434) {
                        tile[index] = new Tile(false, index); // gray flooring
                    } else if (index == 1449 || index == 1511) {
                        tile[index] = new Tile(false, index); // pkmn center floor

                    } else if (index == 1585 || index == 1586 || index == 1587 || index == 1588) {
                        tile[index] = new Tile(false, index); // pkmn center door red
                    } else if (index == 1647 || index == 1648 || index == 1649 || index == 1650) {
                        tile[index] = new Tile(false, index); // pkmn center door red
                    } else if (index == 3170) {
                        tile[index] = new Tile(false, index); // mart green floor

                    } else if (index == 3294 || index == 3295) {
                        tile[index] = new Tile(false, index); // mart door red
                    } else if (index == 3356 || index == 3357) {
                        tile[index] = new Tile(false, index); // mart door red
                    } else if (index == 3600 || index == 3662) { // connecting routes floor
                        tile[index] = new Tile(false, index);
                    } else if (index == 3726 || index == 3727 || index == 3728 || index == 3729) { // connecting routes
                                                                                                   // doorway
                        tile[index] = new Tile(false, index);
                    } else if (index == 3788 || index == 3789 || index == 3790 || index == 3791) {
                        // doorway
                        tile[index] = new Tile(false, index);
                    } else if (index == 3478 || index == 3479 || index == 3480 || index == 3481) { // blue doors
                        // doorway
                        tile[index] = new Tile(false, index);
                    } else if (index == 3540 || index == 3541 || index == 3542 || index == 3543) { // blue doors
                        // doorway
                        tile[index] = new Tile(false, index);
                    } else if (index == 2624) { // pink flooring
                        tile[index] = new Tile(false, index); // mart door red
                    } else if (index == 249) { // jumpable cliff
                        tile[index] = new Tile(false, index);
                    }

                    else {
                        tile[index] = new Tile(true, index);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setup(int index, boolean collision) {
        try {
            tile[index] = new Tile(collision, index);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to load maps for different layers
    public void loadMap(String filePath, int layer) {
        try {
            InputStream is = getClass().getResourceAsStream(filePath);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            int col = 0;
            int row = 0;

            while (col < world.maxWorldCol && row < world.maxWorldRow) {
                String line = br.readLine();

                while (col < world.maxWorldCol) {
                    String numbers[] = line.split(",");
                    int num = Integer.parseInt(numbers[col]);

                    // Depending on the layer, assign the number to the respective map
                    if (layer == 1) {
                        mapTileNumLayer1[col][row] = num;
                    }
                    col++;
                }
                if (col == world.maxWorldCol) {
                    col = 0;
                    row++;
                }
            }
            br.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean canWildPokemonAppearOnTile(int x, int y) {
        try {
            Tile tile = getTileByXandY(x, y);
            if (tile.numberRepresentation == 312) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
