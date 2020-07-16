package io.github.plusls.McAuth.db;

import io.github.plusls.McAuth.McAuthMod;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;

public class Database {
    private Connection conn;
    private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS `user` (\n"
            + "	`uuid` TEXT PRIMARY KEY,\n"
            + " `username` TEXT NOT NULL,\n"
            + "	`password` TEXT,\n"
            + "	`online_mode` INTEGER,\n"
            + " `x` REAL,\n"
            + " `y` REAL,\n"
            + " `z` REAL,\n"
            + " `world` TEXT\n"
            + ");";

    public Database(Path path) throws SQLException {
        path = path.normalize();
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + path.toString());
        PreparedStatement pStmt = conn.prepareStatement(SQL_CREATE_TABLE);
        pStmt.executeUpdate();
        closePStmt(pStmt);
    }

    private static void closePStmt(PreparedStatement pStmt) {
        try {
            if (pStmt != null) {
                pStmt.close();
            }
        } catch (SQLException e) {
            McAuthMod.LOGGER.error("close pStmt fail.", e);
        }
    }

    private static void closeResultSet(ResultSet resultSet) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
        } catch (SQLException e) {
            McAuthMod.LOGGER.error("close ResultSet fail.", e);
        }
    }

    private static final String SQL_INSERT_USER = "INSERT INTO `user`(`uuid`, `username`, `password`, "
            + "`online_mode`, `x`, `y`, `z`, `world`) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

    public boolean createUser(User user) {
        boolean ret = false;
        PreparedStatement pStmt = null;
        try {
            pStmt = this.conn.prepareStatement(SQL_INSERT_USER);
            pStmt.setString(1, user.uuid.toString());
            pStmt.setString(2, user.username);
            pStmt.setString(3, user.password);
            pStmt.setBoolean(4, user.onlineMode);
            pStmt.setDouble(5, user.x);
            pStmt.setDouble(6, user.y);
            pStmt.setDouble(7, user.z);
            pStmt.setString(8, user.world.getValue().toString());
            pStmt.executeUpdate();
            ret = true;
        } catch (SQLException e) {
            McAuthMod.LOGGER.error("createUser fail.", e);
        } finally {
            closePStmt(pStmt);
        }
        return ret;
    }

    private static final String SQL_UPDATE_PASSWORD = "UPDATE `user` SET `password` = ? WHERE `uuid` = ?";

    public boolean updatePasswordByUUID(UUID uuid, String password) {
        boolean ret = false;
        PreparedStatement pStmt = null;
        try {
            pStmt = this.conn.prepareStatement(SQL_UPDATE_PASSWORD);
            pStmt.setString(1, password);
            pStmt.setString(2, uuid.toString());
            if (pStmt.executeUpdate() != 0) {
                ret = true;
            } else {
                McAuthMod.LOGGER.error(String.format("updatePasswordByUUID 0 row, uuid=%s", uuid.toString()));
            }
        } catch (SQLException e) {
            McAuthMod.LOGGER.error("updatePasswordByUUID fail.", e);
        } finally {
            closePStmt(pStmt);
        }
        return ret;
    }

    private static final String SQL_UPDATE_POS = "UPDATE `user` SET `x` = ?, `y` = ?, `z` = ?, `world` = ? WHERE `uuid` = ?";

    public boolean updatePosByUUID(UUID uuid, double x, double y, double z, RegistryKey<World> world) {
        boolean ret = false;
        PreparedStatement pStmt = null;
        try {
            pStmt = this.conn.prepareStatement(SQL_UPDATE_POS);
            pStmt.setDouble(1, x);
            pStmt.setDouble(2, y);
            pStmt.setDouble(3, z);
            pStmt.setString(4, world.getValue().toString());
            pStmt.setString(5, uuid.toString());
            if (pStmt.executeUpdate() != 0) {
                ret = true;
            } else {
                McAuthMod.LOGGER.error(String.format("updatePosByUUID 0 row, uuid=%s", uuid.toString()));
            }
        } catch (SQLException e) {
            McAuthMod.LOGGER.error("updatePosByUUID fail.", e);
        } finally {
            closePStmt(pStmt);
        }
        return ret;
    }

    private static final String SQL_UPDATE_ALL_POS = "UPDATE `user` SET `x` = ?, `y` = ?, `z` = ?, `world` = ?";

    public int updateAllPos(double x, double y, double z, RegistryKey<World> world) {
        int ret = 0;
        PreparedStatement pStmt = null;
        try {
            pStmt = this.conn.prepareStatement(SQL_UPDATE_ALL_POS);
            pStmt.setDouble(1, x);
            pStmt.setDouble(2, y);
            pStmt.setDouble(3, z);
            pStmt.setString(4, world.getValue().toString());
            ret = pStmt.executeUpdate();
        } catch (SQLException e) {
            McAuthMod.LOGGER.error("updateAllPos fail.", e);
        } finally {
            closePStmt(pStmt);
        }
        return ret;
    }

    private static final String SQL_QUERY_USER = "SELECT * FROM `user` WHERE `uuid` = ?";

    private ResultSet getUserResultSetByUUID(UUID uuid) {
        ResultSet results = null;
        PreparedStatement pStmt = null;
        try {
            pStmt = this.conn.prepareStatement(SQL_QUERY_USER);
            pStmt.setString(1, uuid.toString());
            results = pStmt.executeQuery();
        } catch (SQLException e) {
            McAuthMod.LOGGER.error("getUserResultSetByUUID fail.", e);
        }
        return results;
    }

    private static final String SQL_QUERY_USER_BY_USERNAME = "SELECT * FROM `user` WHERE `username` = ?";

    public User getUserByUsername(String username) {
        User user = null;
        ResultSet results = null;
        PreparedStatement pStmt = null;
        try {
            pStmt = this.conn.prepareStatement(SQL_QUERY_USER_BY_USERNAME);
            pStmt.setString(1, username);
            results = pStmt.executeQuery();
            if (results.next()) {
                user = new User(
                        UUID.fromString(results.getString(1)),
                        results.getString(2),
                        results.getString(3),
                        results.getBoolean(4),
                        results.getDouble(5),
                        results.getDouble(6),
                        results.getDouble(7),
                        RegistryKey.of(Registry.DIMENSION, new Identifier(results.getString(8)))
                );
            }
        } catch (SQLException e) {
            McAuthMod.LOGGER.error("getUserByUsername fail.", e);
        } finally {
            closeResultSet(results);
            closePStmt(pStmt);
        }
        return user;
    }

    public User getUserByUUID(UUID uuid) {
        User user = null;
        ResultSet results = null;
        try {
            results = getUserResultSetByUUID(uuid);
            if (results.next()) {
                user = new User(
                        UUID.fromString(results.getString(1)),
                        results.getString(2),
                        results.getString(3),
                        results.getBoolean(4),
                        results.getDouble(5),
                        results.getDouble(6),
                        results.getDouble(7),
                        RegistryKey.of(Registry.DIMENSION, new Identifier(results.getString(8)))
                );
            }
        } catch (SQLException e) {
            McAuthMod.LOGGER.error("getUserByUUID fail.", e);
        } finally {
            closeResultSet(results);
        }
        return user;
    }

    public boolean userExistsByUUID(UUID uuid) {
        boolean ret = false;
        ResultSet results = null;
        try {
            results = getUserResultSetByUUID(uuid);
            ret = results.next();
        } catch (SQLException e) {
            McAuthMod.LOGGER.error("userExistsByUUID fail.", e);
        } finally {
            closeResultSet(results);
        }
        return ret;
    }

    private static final String SQL_DELETE_USER = "DELETE FROM `user` WHERE `uuid` = ?";

    public boolean deleteUserByUUID(UUID uuid) {
        boolean ret;
        PreparedStatement pStmt = null;
        try {
            pStmt = this.conn.prepareStatement(SQL_DELETE_USER);
            pStmt.setString(1, uuid.toString());
            if (pStmt.executeUpdate() != 0) {
                ret = true;
            }
        } catch (SQLException e) {
            McAuthMod.LOGGER.error("deleteUserByUUID fail.", e);
        } finally {
            closePStmt(pStmt);
        }
        return true;
    }

    public void close() {
        try {
            this.conn.close();
        } catch (SQLException e) {
            McAuthMod.LOGGER.error("close fail.", e);
        }
    }
}