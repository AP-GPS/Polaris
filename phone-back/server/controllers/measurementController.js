const pool = require("../db/index");

async function addMeasurement(req, res) {
  try {
    const userId = req.user.id;
    const {
      timestamp,
      latitude,
      longitude,
      cellId,
      lac,
      mcc,
      mnc,
      signalStrength,
      networkType,
      downloadSpeed,
      uploadSpeed,
      pingTime
    } = req.body;
    const sql = `
      INSERT INTO measurements
      (userId, timestamp, latitude, longitude, cellId, lac, mcc, mnc, signalStrength, networkType, downloadSpeed, uploadSpeed, pingTime)
      VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    `;
    const params = [userId, timestamp, latitude, longitude, cellId, lac, mcc, mnc, signalStrength, networkType, downloadSpeed, uploadSpeed, pingTime];
    await (await pool).execute(sql, params);
    res.status(201).json({ message: "Measurement saved" });
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
}

async function getMeasurements(req, res) {
  try {
    const userId = req.user.id;
    const [rows] = await (await pool).execute(
      "SELECT * FROM measurements WHERE userId = ? ORDER BY timestamp DESC LIMIT 1000",
      [userId]
    );
    res.json(rows);
  } catch (err) {
    res.status(500).json({ message: err.message });
  }
}

module.exports = { addMeasurement, getMeasurements };
