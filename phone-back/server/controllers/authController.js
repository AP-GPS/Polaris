const bcrypt = require("bcrypt");
const jwt = require("jsonwebtoken");
const pool = require("../db/index");
const dotenv = require("dotenv");
dotenv.config();

async function register(req, res) {
  try {
    const { username, password } = req.body;
    const hashed = await bcrypt.hash(password, 12);
    const [result] = await (await pool).execute(
      "INSERT INTO users (username, password) VALUES (?, ?)",
      [username, hashed]
    );
    const userId = result.insertId;
    const token = jwt.sign({ id: userId, role: "user" }, process.env.JWT_SECRET, { expiresIn: process.env.JWT_EXPIRES_IN });
    res.status(201).json({ token });
  } catch (err) {
    if (err.code === "ER_DUP_ENTRY") {
      return res.status(400).json({ message: "Username already exists" });
    }
    res.status(500).json({ message: err.message });
  }
}

async function login(req, res) {
  try {
    const { username, password } = req.body;
    const [rows] = await (await pool).execute("SELECT * FROM users WHERE username = ?", [username]);
    if (!rows.length) {
      return res.status(401).json({ message: "Invalid credentials" });
    }
    const user = rows[0];
    const match = await bcrypt.compare(password, user.password);
    if (!match) {
      return res.status(401).json({ message: "Invalid credentials" });
    }
    const token = jwt.sign({ id: user.id, role: user.role }, process.env.JWT_SECRET, { expiresIn: process.env.JWT_EXPIRES_IN });
    res.json({ token });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
}

module.exports = { register, login };
