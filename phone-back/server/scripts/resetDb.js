const poolPromise = require("../db/index");

(async () => {
  const pool = await poolPromise;
  const connection = await pool.getConnection();
  try {
    await connection.query("SET FOREIGN_KEY_CHECKS = 0");
    const tables = ["measurements", "config", "users"];
    for (const table of tables) {
      await connection.query(`TRUNCATE TABLE \`${table}\``);
    }
    await connection.query("SET FOREIGN_KEY_CHECKS = 1");
    console.log("All tables truncated");
  } catch (err) {
    console.error("Error resetting database:", err);
    process.exit(1);
  } finally {
    connection.release();
    await pool.end();
  }
})();
