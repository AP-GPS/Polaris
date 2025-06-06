const mysql = require("mysql2/promise");
const dotenv = require("dotenv");
const fs = require("fs");
dotenv.config();

async function initDB() {
  const connection = await mysql.createConnection({
    host: process.env.DB_HOST,
    port: Number(process.env.DB_PORT),
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    ssl: {
      ca: fs.readFileSync(process.env.DB_SSL_CA_PATH)
    }
  });
  await connection.query(`CREATE DATABASE IF NOT EXISTS \`${process.env.DB_NAME}\`;`);
  await connection.end();

  const pool = mysql.createPool({
    host: process.env.DB_HOST,
    port: Number(process.env.DB_PORT),
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    waitForConnections: true,
    ssl: {
      ca: fs.readFileSync(process.env.DB_SSL_CA_PATH)
    }
  });

  return pool;
}

module.exports = initDB();
